package com.rcq.messenger.service

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import timber.log.Timber
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import javax.inject.Singleton

// Mirrors iOS PINVault — same security parameters, same slot layout:
// Vault file: [version:1][salt:16][slot0:348][slot1:348][slot2:348] = 1061 bytes
// Slot: [nonce:12][ciphertext+tag:336] where plaintext = [len_hi][len_lo][JSON][padding]
// KDF: PBKDF2-HMAC-SHA256, 400k rounds; pepper from Android Keystore
@Singleton
class PINVaultManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val VERSION: Byte = 2
        private const val SALT_LEN = 16
        private const val PAYLOAD_LEN = 320
        private const val NONCE_LEN = 12
        private const val TAG_BITS = 128
        private const val TAG_LEN = TAG_BITS / 8
        private const val SLOT_LEN = NONCE_LEN + PAYLOAD_LEN + TAG_LEN  // 348
        private const val SLOT_COUNT = 3
        private const val KDF_ROUNDS = 400_000
        private const val VAULT_FILE = "pin-vault.bin"
        private const val PEPPER_KEY_ALIAS = "rcq_pin_pepper"
        private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    }

    enum class SlotMode { real, decoy, wipe }

    @Serializable
    data class Layout(
        val realSlot: Int,
        val decoySlot: Int? = null,
        val decoyDataKey: String? = null,
        val decoyUIN: Long? = null,
        val decoyNickname: String? = null,
        val wipeSlot: Int? = null
    )

    @Serializable
    data class SlotPayload(
        val mode: SlotMode,
        val dataKey: String? = null,
        val layout: Layout? = null,
        val decoyUIN: Long? = null,
        val decoyNickname: String? = null
    )

    data class Unlock(val payload: SlotPayload, val slotKey: SecretKey)

    private val vaultFile get() = context.filesDir.resolve(VAULT_FILE)

    val isConfigured: Boolean get() = readVault() != null

    fun vaultSalt(): ByteArray? = readVault()?.salt

    fun createWithRealPIN(pin: String): Unlock {
        ensurePepper()
        val salt = randomBytes(SALT_LEN)
        val realSlot = (0 until SLOT_COUNT).random()
        val dataKey = java.util.Base64.getEncoder().encodeToString(randomBytes(32))
        val layout = Layout(realSlot = realSlot)
        val payload = SlotPayload(mode = SlotMode.real, dataKey = dataKey, layout = layout)
        val key = deriveKey(pin, salt)
        val slots = (0 until SLOT_COUNT).map { randomBytes(SLOT_LEN) }.toMutableList()
        slots[realSlot] = sealSlot(payload, key)
        writeVault(VaultFile(salt, slots))
        return Unlock(payload, key)
    }

    fun unlock(pin: String): Unlock? {
        val vault = readVault() ?: return null
        val key = deriveKey(pin, vault.salt)
        for (slot in vault.slots) {
            openSlot(slot, key)?.let { return Unlock(it, key) }
        }
        return null
    }

    fun writeSlot(index: Int, payload: SlotPayload?, key: SecretKey?) {
        val vault = readVault() ?: error("No vault")
        require(index in 0 until SLOT_COUNT) { "Invalid slot index: $index" }
        vault.slots[index] = if (payload != null && key != null) sealSlot(payload, key)
        else randomBytes(SLOT_LEN)
        writeVault(vault)
    }

    fun freeSlotIndex(layout: Layout): Int? {
        val used = setOfNotNull(layout.realSlot, layout.decoySlot, layout.wipeSlot)
        return (0 until SLOT_COUNT).firstOrNull { it !in used }
    }

    fun destroy() {
        runCatching { vaultFile.delete() }
        runCatching { context.filesDir.resolve("pin-pepper.bin").delete() }
        runCatching {
            KeyStore.getInstance("AndroidKeyStore").apply { load(null) }.deleteEntry(PEPPER_KEY_ALIAS)
        }
    }

    // -----------------------------------------------------------------------
    // KDF: PBKDF2-HMAC-SHA256 — password = pin.utf8 + pepper bytes
    // iOS uses CCKeyDerivationPBKDF with same parameters
    // -----------------------------------------------------------------------
    fun deriveKey(pin: String, salt: ByteArray): SecretKey {
        val pepper = loadPepper() ?: run { ensurePepper(); loadPepper()!! }
        val secretBytes = pin.toByteArray(Charsets.UTF_8) + pepper
        val secretChars = CharArray(secretBytes.size) { secretBytes[it].toInt().and(0xFF).toChar() }
        val spec = PBEKeySpec(secretChars, salt, KDF_ROUNDS, 256)
        return try {
            SecretKeySpec(
                SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded,
                "AES"
            )
        } finally {
            spec.clearPassword()
        }
    }

    // -----------------------------------------------------------------------
    // Slot sealing/opening: AES/GCM/NoPadding, no AAD
    // -----------------------------------------------------------------------
    private fun sealSlot(payload: SlotPayload, key: SecretKey): ByteArray {
        val jsonBytes = json.encodeToString(payload).toByteArray(Charsets.UTF_8)
        require(jsonBytes.size + 2 <= PAYLOAD_LEN) { "Slot payload too large: ${jsonBytes.size}" }
        val plaintext = ByteArray(PAYLOAD_LEN).also { arr ->
            arr[0] = (jsonBytes.size shr 8).toByte()
            arr[1] = (jsonBytes.size and 0xFF).toByte()
            jsonBytes.copyInto(arr, 2)
            randomBytes(PAYLOAD_LEN - 2 - jsonBytes.size).copyInto(arr, 2 + jsonBytes.size)
        }
        val nonce = randomBytes(NONCE_LEN)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(TAG_BITS, nonce))
        return nonce + cipher.doFinal(plaintext)
    }

    fun openSlot(data: ByteArray, key: SecretKey): SlotPayload? {
        if (data.size != SLOT_LEN) return null
        val nonce = data.copyOf(NONCE_LEN)
        val cipherTag = data.copyOfRange(NONCE_LEN, data.size)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(TAG_BITS, nonce))
        val plain = runCatching { cipher.doFinal(cipherTag) }.getOrNull() ?: return null
        if (plain.size < 2) return null
        val len = (plain[0].toInt() and 0xFF shl 8) or (plain[1].toInt() and 0xFF)
        if (len <= 0 || 2 + len > plain.size) return null
        return runCatching {
            json.decodeFromString<SlotPayload>(String(plain, 2, len, Charsets.UTF_8))
        }.onFailure { Timber.w("PINVault: openSlot JSON failed: ${it.message}") }.getOrNull()
    }

    // -----------------------------------------------------------------------
    // Vault file I/O
    // -----------------------------------------------------------------------
    private data class VaultFile(val salt: ByteArray, val slots: MutableList<ByteArray>)

    private fun readVault(): VaultFile? = runCatching {
        if (!vaultFile.exists()) return null
        val raw = vaultFile.readBytes()
        val expected = 1 + SALT_LEN + SLOT_COUNT * SLOT_LEN
        if (raw.size != expected || raw[0] != VERSION) return null
        var off = 1
        val salt = raw.copyOfRange(off, off + SALT_LEN); off += SALT_LEN
        val slots = (0 until SLOT_COUNT).map { raw.copyOfRange(off, off + SLOT_LEN).also { off += SLOT_LEN } }
        VaultFile(salt, slots.toMutableList())
    }.onFailure { Timber.w("PINVault: read failed: ${it.message}") }.getOrNull()

    private fun writeVault(v: VaultFile) {
        val raw = ByteArray(1 + SALT_LEN + SLOT_COUNT * SLOT_LEN)
        raw[0] = VERSION
        v.salt.copyInto(raw, 1)
        v.slots.forEachIndexed { i, slot -> slot.copyInto(raw, 1 + SALT_LEN + i * SLOT_LEN) }
        vaultFile.writeBytes(raw)
    }

    // -----------------------------------------------------------------------
    // Pepper: AES-256 key in Android Keystore encrypts 32 random bytes
    // -----------------------------------------------------------------------
    private fun ensurePepper() {
        if (loadPepper() != null) return
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        if (!ks.containsAlias(PEPPER_KEY_ALIAS)) {
            KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
                init(
                    KeyGenParameterSpec.Builder(
                        PEPPER_KEY_ALIAS,
                        KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                    ).setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                        .setKeySize(256).build()
                )
            }.generateKey()
        }
        val pepper = randomBytes(32)
        val key = ks.getKey(PEPPER_KEY_ALIAS, null) as SecretKey
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key)
        context.filesDir.resolve("pin-pepper.bin").writeBytes(cipher.iv + cipher.doFinal(pepper))
    }

    private fun loadPepper(): ByteArray? = runCatching {
        val pepperFile = context.filesDir.resolve("pin-pepper.bin")
        if (!pepperFile.exists()) return null
        val data = pepperFile.readBytes()
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        val key = ks.getKey(PEPPER_KEY_ALIAS, null) as? SecretKey ?: return null
        val nonce = data.copyOf(NONCE_LEN)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(TAG_BITS, nonce))
        cipher.doFinal(data.copyOfRange(NONCE_LEN, data.size))
    }.onFailure { Timber.w("PINVault: loadPepper failed: ${it.message}") }.getOrNull()

    // -----------------------------------------------------------------------
    // Brute-force throttle state
    // -----------------------------------------------------------------------
    @Serializable
    data class AttemptState(val failedCount: Int = 0, val lockoutUntilMs: Long = 0L)

    fun loadAttemptState(): AttemptState {
        val raw = context.getSharedPreferences("rcq_pin_vault", Context.MODE_PRIVATE)
            .getString("attempt_state", null) ?: return AttemptState()
        return runCatching { json.decodeFromString<AttemptState>(raw) }.getOrElse { AttemptState() }
    }

    fun saveAttemptState(s: AttemptState) =
        context.getSharedPreferences("rcq_pin_vault", Context.MODE_PRIVATE)
            .edit().putString("attempt_state", json.encodeToString(s)).apply()

    fun clearAttemptState() =
        context.getSharedPreferences("rcq_pin_vault", Context.MODE_PRIVATE)
            .edit().remove("attempt_state").apply()

    fun lockoutDurationMs(failedCount: Int): Long = when {
        failedCount < 5  -> 0L
        failedCount == 5 -> 30_000L
        failedCount == 6 -> 60_000L
        failedCount == 7 -> 300_000L
        failedCount == 8 -> 900_000L
        else             -> 3_600_000L
    }

    private fun randomBytes(n: Int) = ByteArray(n).also { SecureRandom().nextBytes(it) }
}

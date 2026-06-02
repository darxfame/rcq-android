package com.rcq.messenger.service

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.biometric.BiometricManager.Authenticators
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import timber.log.Timber
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

// Mirrors iOS BiometricUnlock — AES-256 key in Android Keystore requires biometric auth.
// SlotPayload JSON encrypted with biometric-gated key; key invalidated on new enrollment.
@Singleton
class BiometricManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val KEY_ALIAS = "rcq.biometric.key"
        private const val PREFS = "rcq_biometric"
        private const val KEY_BLOB = "biometric_blob"
        private const val NONCE_LEN = 12
        private const val TAG_BITS = 128
        private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    }

    private val prefs get() = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    val isAvailable: Boolean get() {
        val bm = androidx.biometric.BiometricManager.from(context)
        return bm.canAuthenticate(Authenticators.BIOMETRIC_STRONG) ==
            androidx.biometric.BiometricManager.BIOMETRIC_SUCCESS
    }

    val isEnabled: Boolean get() {
        if (!prefs.contains(KEY_BLOB)) return false
        return runCatching {
            KeyStore.getInstance("AndroidKeyStore").apply { load(null) }.containsAlias(KEY_ALIAS)
        }.getOrElse { false }
    }

    fun enable(payload: PINVaultManager.SlotPayload): Boolean = runCatching {
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        if (ks.containsAlias(KEY_ALIAS)) ks.deleteEntry(KEY_ALIAS)

        KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
            init(
                KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                ).setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .setUserAuthenticationRequired(true)
                    .setInvalidatedByBiometricEnrollment(true)
                    .build()
            )
        }.generateKey()

        val key = ks.getKey(KEY_ALIAS, null) as SecretKey
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key)
        val payloadBytes = json.encodeToString(payload).toByteArray(Charsets.UTF_8)
        val enc = cipher.doFinal(payloadBytes)
        prefs.edit().putString(KEY_BLOB,
            java.util.Base64.getEncoder().encodeToString(cipher.iv + enc)).apply()
        Timber.d("BiometricManager: enabled")
        true
    }.onFailure { Timber.e("BiometricManager: enable failed: ${it.message}") }.getOrElse { false }

    fun disable() {
        prefs.edit().remove(KEY_BLOB).apply()
        runCatching {
            KeyStore.getInstance("AndroidKeyStore").apply { load(null) }.deleteEntry(KEY_ALIAS)
        }
        Timber.d("BiometricManager: disabled")
    }

    // Initialize cipher for decryption — pass as BiometricPrompt.CryptoObject
    fun initDecryptCipher(): Cipher? = runCatching {
        val blob = prefs.getString(KEY_BLOB, null) ?: return null
        val data = java.util.Base64.getDecoder().decode(blob)
        val nonce = data.copyOf(NONCE_LEN)
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        val key = ks.getKey(KEY_ALIAS, null) as? SecretKey ?: return null
        Cipher.getInstance("AES/GCM/NoPadding").apply {
            init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(TAG_BITS, nonce))
        }
    }.onFailure { Timber.e("BiometricManager: initDecryptCipher failed: ${it.message}") }.getOrNull()

    // Decrypt payload using the cipher authorized by BiometricPrompt
    fun decryptPayload(cipher: Cipher): PINVaultManager.SlotPayload? = runCatching {
        val blob = prefs.getString(KEY_BLOB, null) ?: return null
        val data = java.util.Base64.getDecoder().decode(blob)
        val encrypted = data.copyOfRange(NONCE_LEN, data.size)
        val plain = cipher.doFinal(encrypted)
        json.decodeFromString<PINVaultManager.SlotPayload>(String(plain, Charsets.UTF_8))
    }.onFailure { Timber.e("BiometricManager: decryptPayload failed: ${it.message}") }.getOrNull()

    fun promptDecrypt(
        activity: FragmentActivity,
        cipher: Cipher,
        onSuccess: (Cipher) -> Unit,
        onError: (String) -> Unit,
        onCancel: () -> Unit
    ) {
        val executor = ContextCompat.getMainExecutor(activity)
        BiometricPrompt(activity, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                result.cryptoObject?.cipher?.let { onSuccess(it) }
                    ?: onError("Cipher not returned")
            }
            override fun onAuthenticationError(code: Int, msg: CharSequence) {
                if (code == BiometricPrompt.ERROR_USER_CANCELED ||
                    code == BiometricPrompt.ERROR_NEGATIVE_BUTTON) onCancel()
                else onError(msg.toString())
            }
            override fun onAuthenticationFailed() {}
        }).authenticate(
            BiometricPrompt.PromptInfo.Builder()
                .setTitle("Разблокировка RCQ")
                .setSubtitle("Используйте биометрию для входа")
                .setNegativeButtonText("Ввести PIN")
                .setAllowedAuthenticators(Authenticators.BIOMETRIC_STRONG)
                .build(),
            BiometricPrompt.CryptoObject(cipher)
        )
    }
}

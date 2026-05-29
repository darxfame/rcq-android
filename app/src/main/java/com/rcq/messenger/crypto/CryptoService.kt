package com.rcq.messenger.crypto

import android.util.Base64
import org.json.JSONObject
import org.signal.libsignal.protocol.ecc.Curve
import org.signal.libsignal.protocol.kem.KEMKeyPair
import org.signal.libsignal.protocol.kem.KEMKeyType
import org.signal.libsignal.protocol.message.CiphertextMessage
import org.signal.libsignal.protocol.message.PreKeySignalMessage
import org.signal.libsignal.protocol.message.SignalMessage
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CryptoService @Inject constructor(
    private val sessionManager: SessionManager,
    private val keyStore: SignalKeyStore,
    val ecies: EciesCrypto
) {

    /**
     * Encrypt message using Signal Protocol Double Ratchet
     */
    fun encryptMessage(recipientUin: Long, text: String): EncryptedMessage {
        val ciphertext = sessionManager.encryptMessage(recipientUin, text)
        return EncryptedMessage(
            ciphertext = Base64.encodeToString(
                ciphertext.serialize(),
                Base64.NO_WRAP
            ),
            signalType = ciphertext.type
        )
    }

    /**
     * Decrypt message using Signal Protocol Double Ratchet
     */
    fun decryptMessage(senderUin: Long, ciphertextBase64: String, signalType: Int): String {
        val ciphertextBytes = Base64.decode(ciphertextBase64, Base64.NO_WRAP)

        val ciphertext = when (signalType) {
            CiphertextMessage.PREKEY_TYPE -> PreKeySignalMessage(ciphertextBytes)
            CiphertextMessage.WHISPER_TYPE -> SignalMessage(ciphertextBytes)
            else -> throw IllegalArgumentException("Unknown signal type: $signalType")
        }

        return sessionManager.decryptMessage(senderUin, ciphertext)
    }

    /**
     * Check if we have an established session with the user
     */
    fun hasSession(recipientUin: Long): Boolean = sessionManager.hasSession(recipientUin)

    fun buildSession(recipientUin: Long, bundle: com.rcq.messenger.data.api.PreKeyBundleResponse) {
        sessionManager.buildSession(recipientUin, bundle)
    }

    fun deleteSession(recipientUin: Long) {
        sessionManager.deleteSession(recipientUin)
    }

    /**
     * Get our identity key for sharing with other users
     */
    fun getIdentityKey(): String {
        return Base64.encodeToString(
            keyStore.identityKeyPair.publicKey.serialize(),
            Base64.NO_WRAP
        )
    }

    /**
     * Generate registration bundle for new user registration (legacy — used only for /auth/register)
     */
    fun generateRegistrationBundle(): RegistrationBundle {
        val identityKey = getIdentityKey()
        val preKeys = sessionManager.generatePreKeys()
        val signedPreKey = sessionManager.generateSignedPreKey()
        return RegistrationBundle(identityKey = identityKey, preKeys = preKeys, signedPreKey = signedPreKey)
    }

    /**
     * Generate full Signal key bundle and upload to /keys/bundle.
     * Includes signed prekey, Kyber prekey, and one-time prekeys.
     */
    fun generateSignalBundle(): com.rcq.messenger.data.api.RegisterBundleRequest {
        val identityKeyB64 = Base64.encodeToString(
            keyStore.identityKeyPair.publicKey.serialize(), Base64.NO_WRAP
        )
        val registrationId = keyStore.getLocalRegistrationId()

        val signedPreKeyRecord = keyStore.generateSignedPreKey(1, keyStore.identityKeyPair)
        val signedPreKeyData = com.rcq.messenger.data.api.SignedPreKeyData(
            id = signedPreKeyRecord.id,
            key = Base64.encodeToString(signedPreKeyRecord.keyPair.publicKey.serialize(), Base64.NO_WRAP),
            signature = Base64.encodeToString(signedPreKeyRecord.signature, Base64.NO_WRAP)
        )

        val kyberKeyPair = KEMKeyPair.generate(KEMKeyType.KYBER_1024)
        val kyberSig = Curve.calculateSignature(
            keyStore.identityKeyPair.privateKey, kyberKeyPair.publicKey.serialize()
        )
        val kyberPreKeyData = com.rcq.messenger.data.api.KyberPreKeyData(
            id = 1,
            key = Base64.encodeToString(kyberKeyPair.publicKey.serialize(), Base64.NO_WRAP),
            signature = Base64.encodeToString(kyberSig, Base64.NO_WRAP)
        )

        val preKeyRecords = keyStore.generatePreKeys(1, 100)
        val oneTimePreKeys = preKeyRecords.map { pk ->
            com.rcq.messenger.data.api.PreKeyData(
                id = pk.id,
                key = Base64.encodeToString(pk.keyPair.publicKey.serialize(), Base64.NO_WRAP)
            )
        }

        return com.rcq.messenger.data.api.RegisterBundleRequest(
            signalIdentityKey = identityKeyB64,
            registrationId = registrationId,
            signedPreKey = signedPreKeyData,
            kyberPreKey = kyberPreKeyData,
            oneTimePreKeys = oneTimePreKeys
        )
    }

    /**
     * Encrypt plaintext as iOS-compatible ECIES v=1 when ECIES is ready,
     * otherwise fall back to Android v=0 (Signal-only) for backward compat.
     *
     * [recipientIdentityKeyB64] — raw 32-byte Curve25519 pub of the recipient
     *   (the `identity_key` field the recipient registered with).
     */
    fun encryptWrapped(
        senderUin: Long,
        recipientUin: Long,
        text: String,
        recipientIdentityKeyB64: String? = null
    ): EncryptedWrapped {
        // Prefer ECIES v=1 when we have the recipient's ECIES identity key
        if (ecies.isReady() && !recipientIdentityKeyB64.isNullOrEmpty()) {
            return runCatching {
                val msgId = java.util.UUID.randomUUID().toString()
                val envJson = ecies.buildTextEnvelope(msgId, text)
                val payload = ecies.encryptV1(envJson, recipientIdentityKeyB64)
                EncryptedWrapped(payload = payload, envelopeType = "message", signalType = 0)
            }.getOrElse {
                android.util.Log.w("CryptoService", "ECIES v=1 encrypt failed, falling back: ${it.message}")
                encryptV0(senderUin, recipientUin, text)
            }
        }
        return encryptV0(senderUin, recipientUin, text)
    }

    private fun encryptV0(senderUin: Long, recipientUin: Long, text: String): EncryptedWrapped {
        val ciphertext = sessionManager.encryptMessage(recipientUin, text)
        val kind = if (ciphertext.type == CiphertextMessage.PREKEY_TYPE) "prekey" else "signal"
        val msgB64 = Base64.encodeToString(ciphertext.serialize(), Base64.NO_WRAP)
        val inner = """{"v":0,"from":$senderUin,"kind":"$kind","msg":"$msgB64"}"""
        val payload = Base64.encodeToString(inner.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
        return EncryptedWrapped(payload = payload, envelopeType = "message", signalType = ciphertext.type)
    }

    /**
     * Decrypt a payload — handles ECIES v=1 (iOS), Android v=0, and raw plaintext fallback.
     * For ECIES results, [DecryptedWrapped.signerPubKeyB64] is populated; callers MUST
     * verify it against the stored signing_key for the sender (TOFU / key pinning).
     */
    fun decryptWrapped(payload: String): DecryptedWrapped? {
        // Try ECIES v=1 first (iOS-sent messages)
        if (ecies.isReady()) {
            val eciesResult = ecies.decryptV1(payload)
            if (eciesResult != null) {
                val content = ecies.parseEnvelopeText(eciesResult.envelopeJson) ?: ""
                return DecryptedWrapped(
                    senderUin = eciesResult.senderUin,
                    content = content,
                    signerPubKeyB64 = eciesResult.signerPubKeyB64
                )
            }
        }
        // Fallback: Android v=0 format
        return runCatching {
            val jsonStr = String(Base64.decode(payload, Base64.NO_WRAP), Charsets.UTF_8)
            val obj = JSONObject(jsonStr)
            if (obj.optInt("v", -1) != 0) return null
            val senderUin = obj.getLong("from")
            val kind = obj.getString("kind")
            val msgB64 = obj.getString("msg")
            val signalType = if (kind == "prekey") CiphertextMessage.PREKEY_TYPE else CiphertextMessage.WHISPER_TYPE
            val content = decryptMessage(senderUin, msgB64, signalType)
            DecryptedWrapped(senderUin = senderUin, content = content)
        }.getOrNull()
    }

    data class RegistrationBundle(
        val identityKey: String,
        val preKeys: List<String>,
        val signedPreKey: String
    )

    data class EncryptedMessage(
        val ciphertext: String,
        val signalType: Int
    )

    data class EncryptedWrapped(
        val payload: String,
        val envelopeType: String,
        val signalType: Int
    )

    data class DecryptedWrapped(
        val senderUin: Long,
        val content: String,
        /** Non-null for ECIES v=1 messages; caller must do TOFU / pinning check. */
        val signerPubKeyB64: String? = null
    )
}
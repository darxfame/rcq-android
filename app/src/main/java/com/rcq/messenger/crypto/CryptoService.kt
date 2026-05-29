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
    private val keyStore: SignalKeyStore
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
     * Encrypt plaintext and wrap with sender UIN so receiver can decrypt
     * without server revealing the sender (Android wire format v=0).
     */
    fun encryptWrapped(senderUin: Long, recipientUin: Long, text: String): EncryptedWrapped {
        val ciphertext = sessionManager.encryptMessage(recipientUin, text)
        val kind = if (ciphertext.type == CiphertextMessage.PREKEY_TYPE) "prekey" else "signal"
        val msgB64 = Base64.encodeToString(ciphertext.serialize(), Base64.NO_WRAP)
        val inner = """{"v":0,"from":$senderUin,"kind":"$kind","msg":"$msgB64"}"""
        val payload = Base64.encodeToString(inner.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
        val envelopeType = if (ciphertext.type == CiphertextMessage.PREKEY_TYPE) "prekey_message" else "message"
        return EncryptedWrapped(payload = payload, envelopeType = envelopeType, signalType = ciphertext.type)
    }

    /**
     * Decrypt a wrapped payload produced by encryptWrapped().
     * Returns null if the payload is not the Android v=0 format (e.g. iOS format).
     */
    fun decryptWrapped(payload: String): DecryptedWrapped? {
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
        val content: String
    )
}
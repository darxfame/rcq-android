package com.rcq.messenger.crypto

import java.util.Base64
import timber.log.Timber
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

    private val ENC = Base64.getEncoder()
    private val DEC = Base64.getDecoder()

    fun encryptMessage(recipientUin: Long, text: String): EncryptedMessage {
        val ciphertext = sessionManager.encryptMessage(recipientUin, text)
        return EncryptedMessage(
            ciphertext = ENC.encodeToString(ciphertext.serialize()),
            signalType = ciphertext.type
        )
    }

    fun decryptMessage(senderUin: Long, ciphertextBase64: String, signalType: Int): String {
        val ciphertextBytes = DEC.decode(ciphertextBase64)
        val ciphertext = when (signalType) {
            CiphertextMessage.PREKEY_TYPE -> PreKeySignalMessage(ciphertextBytes)
            CiphertextMessage.WHISPER_TYPE -> SignalMessage(ciphertextBytes)
            else -> throw IllegalArgumentException("Unknown signal type: $signalType")
        }
        return sessionManager.decryptMessage(senderUin, ciphertext)
    }

    fun hasSession(recipientUin: Long): Boolean = sessionManager.hasSession(recipientUin)

    fun buildSession(recipientUin: Long, bundle: com.rcq.messenger.data.api.PreKeyBundleResponse) {
        sessionManager.buildSession(recipientUin, bundle)
    }

    fun deleteSession(recipientUin: Long) {
        sessionManager.deleteSession(recipientUin)
    }

    fun getIdentityKey(): String =
        ENC.encodeToString(keyStore.identityKeyPair.publicKey.serialize())

    fun generateRegistrationBundle(): RegistrationBundle {
        val identityKey = getIdentityKey()
        val preKeys = sessionManager.generatePreKeys()
        val signedPreKey = sessionManager.generateSignedPreKey()
        return RegistrationBundle(identityKey = identityKey, preKeys = preKeys, signedPreKey = signedPreKey)
    }

    fun generateSignalBundle(): com.rcq.messenger.data.api.RegisterBundleRequest {
        val identityKeyB64 = ENC.encodeToString(keyStore.identityKeyPair.publicKey.serialize())
        val registrationId = keyStore.getLocalRegistrationId()

        val signedPreKeyRecord = keyStore.generateSignedPreKey(1, keyStore.identityKeyPair)
        val signedPreKeyData = com.rcq.messenger.data.api.SignedPreKeyData(
            id = signedPreKeyRecord.id,
            key = ENC.encodeToString(signedPreKeyRecord.keyPair.publicKey.serialize()),
            signature = ENC.encodeToString(signedPreKeyRecord.signature)
        )

        val kyberKeyPair = KEMKeyPair.generate(KEMKeyType.KYBER_1024)
        val kyberSig = Curve.calculateSignature(
            keyStore.identityKeyPair.privateKey, kyberKeyPair.publicKey.serialize()
        )
        val kyberPreKeyData = com.rcq.messenger.data.api.KyberPreKeyData(
            id = 1,
            key = ENC.encodeToString(kyberKeyPair.publicKey.serialize()),
            signature = ENC.encodeToString(kyberSig)
        )

        val preKeyRecords = keyStore.generatePreKeys(1, 100)
        val oneTimePreKeys = preKeyRecords.map { pk ->
            com.rcq.messenger.data.api.PreKeyData(
                id = pk.id,
                key = ENC.encodeToString(pk.keyPair.publicKey.serialize())
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

    fun encryptWrapped(
        senderUin: Long,
        recipientUin: Long,
        text: String,
        recipientIdentityKeyB64: String? = null
    ): EncryptedWrapped {
        if (ecies.isReady() && !recipientIdentityKeyB64.isNullOrEmpty()) {
            return runCatching {
                val msgId = java.util.UUID.randomUUID().toString()
                val envJson = ecies.buildTextEnvelope(msgId, text)
                val payload = ecies.encryptV1(envJson, recipientIdentityKeyB64)
                EncryptedWrapped(payload = payload, envelopeType = "message", signalType = 0)
            }.getOrElse {
                Timber.w("ECIES v=1 encrypt failed, falling back: ${it.message}")
                encryptV0(senderUin, recipientUin, text)
            }
        }
        return encryptV0(senderUin, recipientUin, text)
    }

    private fun encryptV0(senderUin: Long, recipientUin: Long, text: String): EncryptedWrapped {
        val ciphertext = sessionManager.encryptMessage(recipientUin, text)
        val kind = if (ciphertext.type == CiphertextMessage.PREKEY_TYPE) "prekey" else "signal"
        val msgB64 = ENC.encodeToString(ciphertext.serialize())
        val inner = """{"v":0,"from":$senderUin,"kind":"$kind","msg":"$msgB64"}"""
        val payload = ENC.encodeToString(inner.toByteArray(Charsets.UTF_8))
        return EncryptedWrapped(payload = payload, envelopeType = "message", signalType = ciphertext.type)
    }

    fun decryptWrapped(payload: String): DecryptedWrapped? {
        if (ecies.isReady()) {
            val eciesResult = ecies.decryptV1(payload)
            if (eciesResult != null) {
                val env = eciesResult.envelopeJson
                val kind = env.optString("kind", "text")
                val messageId = env.optString("id").takeIf { it.isNotEmpty() }
                    ?: java.util.UUID.randomUUID().toString()
                val content = when (kind) {
                    "text" -> env.optString("text", "")
                    "photo", "video", "voice", "file" -> env.optString("caption", "")
                    "location" -> env.optString("caption", "")
                    else -> ""
                }
                return DecryptedWrapped(
                    senderUin = eciesResult.senderUin,
                    messageId = messageId,
                    kind = kind,
                    content = content,
                    signerPubKeyB64 = eciesResult.signerPubKeyB64,
                    envelopeJson = env
                )
            }
        }
        return runCatching {
            val jsonStr = String(DEC.decode(payload), Charsets.UTF_8)
            val obj = JSONObject(jsonStr)
            if (obj.optInt("v", -1) != 0) return null
            val senderUin = obj.getLong("from")
            val signalKind = obj.getString("kind")
            val msgB64 = obj.getString("msg")
            val signalType = if (signalKind == "prekey") CiphertextMessage.PREKEY_TYPE else CiphertextMessage.WHISPER_TYPE
            val plaintext = decryptMessage(senderUin, msgB64, signalType)
            val env = runCatching { JSONObject(plaintext) }.getOrNull()
            val kind = env?.optString("kind", "text") ?: "text"
            val messageId = env?.optString("id")?.takeIf { it.isNotEmpty() }
                ?: java.util.UUID.randomUUID().toString()
            val content = env?.optString("text", plaintext) ?: plaintext
            DecryptedWrapped(senderUin = senderUin, messageId = messageId, kind = kind, content = content)
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
        val messageId: String,
        val kind: String = "text",
        val content: String,
        val signerPubKeyB64: String? = null,
        val envelopeJson: org.json.JSONObject? = null
    )
}

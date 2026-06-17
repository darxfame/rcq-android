package com.rcq.messenger.crypto

import com.rcq.messenger.data.api.PreKeyBundleResponse
import org.signal.libsignal.protocol.IdentityKey
import org.signal.libsignal.protocol.SessionBuilder
import org.signal.libsignal.protocol.SessionCipher
import org.signal.libsignal.protocol.SignalProtocolAddress
import org.signal.libsignal.protocol.ecc.Curve
import org.signal.libsignal.protocol.message.CiphertextMessage
import org.signal.libsignal.protocol.message.PreKeySignalMessage
import org.signal.libsignal.protocol.message.SignalMessage
import org.signal.libsignal.protocol.state.PreKeyBundle
import org.signal.libsignal.protocol.state.PreKeyRecord
import org.signal.libsignal.protocol.state.SignedPreKeyRecord
import org.signal.libsignal.protocol.util.KeyHelper
import java.util.Base64
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionManager @Inject constructor(
    private val signalKeyStore: SignalKeyStore
) {
    companion object {
        // RCQ server is single-device per UIN; matches iOS ProtocolAddress(name:uin, deviceId:1)
        const val DEVICE_ID = 1
    }

    fun encryptMessage(recipientUin: Long, plaintext: String): CiphertextMessage {
        val address = SignalProtocolAddress(recipientUin.toString(), DEVICE_ID)
        val cipher = SessionCipher(signalKeyStore, address)
        return cipher.encrypt(plaintext.toByteArray(Charsets.UTF_8))
    }

    fun decryptMessage(senderUin: Long, ciphertext: CiphertextMessage): String {
        val address = SignalProtocolAddress(senderUin.toString(), DEVICE_ID)
        val cipher = SessionCipher(signalKeyStore, address)

        val decrypted = when (ciphertext.type) {
            CiphertextMessage.PREKEY_TYPE -> {
                cipher.decrypt(ciphertext as PreKeySignalMessage)
            }
            CiphertextMessage.WHISPER_TYPE -> {
                cipher.decrypt(ciphertext as SignalMessage)
            }
            else -> throw IllegalArgumentException("Unknown ciphertext type: ${ciphertext.type}")
        }

        return String(decrypted, Charsets.UTF_8)
    }

    fun hasSession(recipientUin: Long): Boolean {
        val address = SignalProtocolAddress(recipientUin.toString(), DEVICE_ID)
        return signalKeyStore.containsSession(address)
    }

    // Build a Signal session from the recipient's pre-key bundle fetched from server.
    // Must be called before the first encrypt() to a new recipient.
    fun buildSession(recipientUin: Long, bundle: PreKeyBundleResponse) {
        val address = SignalProtocolAddress(recipientUin.toString(), 1)
        val preKeyBundle = PreKeyBundle(
            bundle.registrationId,
            DEVICE_ID,
            bundle.preKey?.id ?: 0,
            bundle.preKey?.let { Curve.decodePoint(Base64.getDecoder().decode(it.key), 0) },
            bundle.signedPreKey.id,
            Curve.decodePoint(Base64.getDecoder().decode(bundle.signedPreKey.key), 0),
            Base64.getDecoder().decode(bundle.signedPreKey.signature),
            IdentityKey(Base64.getDecoder().decode(bundle.identityKey), 0)
        )
        SessionBuilder(signalKeyStore, address).process(preKeyBundle)
    }

    fun deleteSession(recipientUin: Long) {
        val address = SignalProtocolAddress(recipientUin.toString(), DEVICE_ID)
        signalKeyStore.deleteSession(address)
    }

    /**
     * Generate pre-keys for registration
     */
    fun generatePreKeys(): List<String> {
        val preKeys = (1..100).map { id ->
            PreKeyRecord(id, Curve.generateKeyPair())
        }
        return preKeys.map { preKey ->
            Base64.getEncoder().encodeToString(preKey.serialize())
        }
    }

    /**
     * Generate signed pre-key for registration
     */
    fun generateSignedPreKey(): String {
        val signedKeyPair = Curve.generateKeyPair()
        val signature = Curve.calculateSignature(
            signalKeyStore.identityKeyPair.privateKey,
            signedKeyPair.publicKey.serialize()
        )
        val signedPreKey = SignedPreKeyRecord(
            1,
            System.currentTimeMillis(),
            signedKeyPair,
            signature
        )
        return Base64.getEncoder().encodeToString(signedPreKey.serialize())
    }
}

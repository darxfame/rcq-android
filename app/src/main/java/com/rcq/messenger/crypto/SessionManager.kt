package com.rcq.messenger.crypto

import java.util.Base64
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
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionManager @Inject constructor(
    private val signalKeyStore: SignalKeyStore
) {
    companion object {
        const val DEVICE_ID = 1
        private val DEC = Base64.getDecoder()
        private val ENC = Base64.getEncoder()
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
            CiphertextMessage.PREKEY_TYPE -> cipher.decrypt(ciphertext as PreKeySignalMessage)
            CiphertextMessage.WHISPER_TYPE -> cipher.decrypt(ciphertext as SignalMessage)
            else -> throw IllegalArgumentException("Unknown ciphertext type: ${ciphertext.type}")
        }

        return String(decrypted, Charsets.UTF_8)
    }

    fun hasSession(recipientUin: Long): Boolean {
        val address = SignalProtocolAddress(recipientUin.toString(), DEVICE_ID)
        return signalKeyStore.containsSession(address)
    }

    fun buildSession(recipientUin: Long, bundle: PreKeyBundleResponse) {
        val address = SignalProtocolAddress(recipientUin.toString(), 1)
        val preKeyBundle = PreKeyBundle(
            bundle.registrationId,
            DEVICE_ID,
            bundle.preKey?.id ?: 0,
            bundle.preKey?.let { Curve.decodePoint(DEC.decode(it.key), 0) },
            bundle.signedPreKey.id,
            Curve.decodePoint(DEC.decode(bundle.signedPreKey.key), 0),
            DEC.decode(bundle.signedPreKey.signature),
            IdentityKey(DEC.decode(bundle.identityKey), 0)
        )
        SessionBuilder(signalKeyStore, address).process(preKeyBundle)
    }

    fun deleteSession(recipientUin: Long) {
        val address = SignalProtocolAddress(recipientUin.toString(), DEVICE_ID)
        signalKeyStore.deleteSession(address)
    }

    fun generatePreKeys(): List<String> {
        val preKeys = (1..100).map { id -> PreKeyRecord(id, Curve.generateKeyPair()) }
        return preKeys.map { ENC.encodeToString(it.serialize()) }
    }

    fun generateSignedPreKey(): String {
        val signedKeyPair = Curve.generateKeyPair()
        val signature = Curve.calculateSignature(
            signalKeyStore.identityKeyPair.privateKey,
            signedKeyPair.publicKey.serialize()
        )
        val signedPreKey = SignedPreKeyRecord(1, System.currentTimeMillis(), signedKeyPair, signature)
        return ENC.encodeToString(signedPreKey.serialize())
    }
}

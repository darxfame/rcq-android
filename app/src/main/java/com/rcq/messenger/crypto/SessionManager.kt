package com.rcq.messenger.crypto

import android.util.Base64
import org.signal.libsignal.protocol.SessionCipher
import org.signal.libsignal.protocol.SignalProtocolAddress
import org.signal.libsignal.protocol.message.CiphertextMessage
import org.signal.libsignal.protocol.message.PreKeySignalMessage
import org.signal.libsignal.protocol.message.SignalMessage
import org.signal.libsignal.protocol.state.PreKeyRecord
import org.signal.libsignal.protocol.state.SignedPreKeyRecord
import org.signal.libsignal.protocol.util.KeyHelper
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionManager @Inject constructor(
    private val signalKeyStore: SignalKeyStore
) {

    fun encryptMessage(recipientUin: Long, plaintext: String): CiphertextMessage {
        val address = SignalProtocolAddress(recipientUin.toString(), 1)
        val cipher = SessionCipher(signalKeyStore, address)
        return cipher.encrypt(plaintext.toByteArray(Charsets.UTF_8))
    }

    fun decryptMessage(senderUin: Long, ciphertext: CiphertextMessage): String {
        val address = SignalProtocolAddress(senderUin.toString(), 1)
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
        val address = SignalProtocolAddress(recipientUin.toString(), 1)
        return signalKeyStore.containsSession(address)
    }

    fun deleteSession(recipientUin: Long) {
        val address = SignalProtocolAddress(recipientUin.toString(), 1)
        signalKeyStore.deleteSession(address)
    }

    /**
     * Generate pre-keys for registration
     */
    fun generatePreKeys(): List<String> {
        val preKeys = (1..100).map { id ->
            PreKeyRecord.generate(id)
        }
        return preKeys.map { preKey ->
            Base64.encodeToString(preKey.serialize(), Base64.NO_WRAP)
        }
    }

    /**
     * Generate signed pre-key for registration
     */
    fun generateSignedPreKey(): String {
        val signedPreKey = SignedPreKeyRecord.generate(
            1,
            signalKeyStore.identityKeyPair
        )
        return Base64.encodeToString(signedPreKey.serialize(), Base64.NO_WRAP)
    }
}
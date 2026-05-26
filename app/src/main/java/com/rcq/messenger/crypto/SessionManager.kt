package com.rcq.messenger.crypto

import org.signal.libsignal.protocol.SessionCipher
import org.signal.libsignal.protocol.SignalProtocolAddress
import org.signal.libsignal.protocol.message.CiphertextMessage
import org.signal.libsignal.protocol.message.PreKeySignalMessage
import org.signal.libsignal.protocol.message.SignalMessage
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
}
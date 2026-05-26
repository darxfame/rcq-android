package com.rcq.messenger.crypto

import android.util.Base64
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
    fun hasSession(recipientUin: Long): Boolean {
        return sessionManager.hasSession(recipientUin)
    }

    /**
     * Delete session with user (for security or troubleshooting)
     */
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

    data class EncryptedMessage(
        val ciphertext: String, // Base64-encoded encrypted content
        val signalType: Int // Signal Protocol message type (1=PreKey, 2=Whisper)
    )
}
package com.rcq.messenger.crypto

import android.util.Base64
import com.rcq.messenger.util.CryptoUtils
import java.security.SecureRandom

/**
 * E2EE service matching iOS CryptoService implementation.
 */
object CryptoService {

    /**
     * Generate registration bundle with identity and signing keys.
     */
    fun generateRegistrationBundle(): RegistrationBundle {
        val identityKey = generateRandomKey()
        val signingKey = generateRandomKey()
        return RegistrationBundle(
            identityKey = identityKey,
            signingKey = signingKey
        )
    }

    /**
     * Generate a random Base64-encoded key (32 bytes).
     */
    fun generateRandomKey(): String {
        val bytes = ByteArray(32)
        SecureRandom().nextBytes(bytes)
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    /**
     * Encrypt a message for a recipient using AES-GCM.
     */
    fun encrypt(plaintext: String, recipientPublicKey: String): String {
        val key = deriveKey(recipientPublicKey)
        val encrypted = CryptoUtils.encrypt(plaintext.toByteArray(Charsets.UTF_8), key)
        return CryptoUtils.bytesToBase64(encrypted)
    }

    /**
     * Decrypt a message.
     */
    fun decrypt(ciphertext: String, privateKey: String): String {
        val key = deriveKey(privateKey)
        val encryptedData = CryptoUtils.base64ToBytes(ciphertext)
        val decrypted = CryptoUtils.decrypt(encryptedData, key)
        return String(decrypted, Charsets.UTF_8)
    }

    private fun deriveKey(publicKey: String): ByteArray {
        val digest = java.security.MessageDigest.getInstance("SHA-256")
        return digest.digest(publicKey.toByteArray(Charsets.UTF_8))
    }
}

data class RegistrationBundle(
    val identityKey: String,
    val signingKey: String
)

data class PeerBundle(
    val uin: Long,
    val identityKey: String,
    val signingKey: String
)

sealed class Envelope {
    data class Text(val id: String, val text: String) : Envelope()
    data class Photo(val id: String, val mediaId: String, val mediaKey: String, val caption: String?) : Envelope()
    data class Voice(val id: String, val mediaId: String, val mediaKey: String, val durationSec: Double) : Envelope()
}

data class DecryptedEnvelope(
    val senderUin: Long,
    val content: Envelope
)
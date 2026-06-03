package app.rcq.android.crypto

import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Per-blob media encryption, byte-compatible with the iOS MediaService
 * (CryptoKit `AES.GCM.seal(...).combined`). Each media item gets a fresh
 * random AES-256 key; the ciphertext blob is uploaded out-of-band and the
 * key travels inside the (already ECIES-sealed) message envelope
 * (rcq-spec 9.4).
 *
 * Combined layout matches CryptoKit: nonce(12) || ciphertext || tag(16).
 */
object MediaCrypto {
    fun newKey(): ByteArray = ByteArray(32).also { SecureRandom().nextBytes(it) }

    fun seal(plain: ByteArray, key: ByteArray): ByteArray {
        val nonce = ByteArray(12).also { SecureRandom().nextBytes(it) }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(128, nonce))
        return nonce + cipher.doFinal(plain) // doFinal returns ciphertext||tag
    }

    fun open(combined: ByteArray, key: ByteArray): ByteArray {
        val nonce = combined.copyOfRange(0, 12)
        val ctTag = combined.copyOfRange(12, combined.size)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(128, nonce))
        return cipher.doFinal(ctTag)
    }
}

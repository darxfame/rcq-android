package app.rcq.android.crypto

import org.bouncycastle.crypto.digests.SHA256Digest
import org.bouncycastle.crypto.generators.HKDFBytesGenerator
import org.bouncycastle.crypto.generators.X25519KeyPairGenerator
import org.bouncycastle.crypto.params.HKDFParameters
import org.bouncycastle.crypto.params.X25519KeyGenerationParameters
import org.bouncycastle.crypto.params.X25519PrivateKeyParameters
import org.bouncycastle.crypto.params.X25519PublicKeyParameters
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * Encryption for Radio (BLE + Wi-Fi-Direct) local chat — the Android port of
 * the iOS `RadioCrypto`. Session-scoped keys; nothing persists past disconnect.
 *
 * NB Radio never interoperates with iOS — Apple MultipeerConnectivity bridges
 * to no Android API, so a Radio session is always Android↔Android. That frees
 * us from byte-exactness across platforms, but we mirror the iOS *design* so
 * the two stacks stay conceptually identical (and a future bridge would be a
 * small step):
 *   - Room key: open rooms = SHA256(roomID); password rooms = PBKDF2-SHA256
 *     over the password salted with the roomID (100k iters, ~32 bytes).
 *   - 1:1 ephemeral: Curve25519 (X25519) ECDH after exchanging raw 32-byte
 *     public keys, then HKDF-SHA256 with the "rcq-radio-1to1" info string for
 *     domain separation.
 * Both feed AES-GCM with a per-message 12-byte nonce; the combined layout is
 * nonce(12) || ciphertext || tag(16), identical to [MediaCrypto].
 *
 * Uses the BouncyCastle lightweight API directly (same reasoning as
 * [IdentityKeys]/[SealedSender]: JCA's XDH generator only exists from API 33,
 * minSdk here is 26) for X25519, and JCA for AES-GCM / PBKDF2 / SHA-256.
 */
object RadioCrypto {

    /** A freshly minted ephemeral X25519 keypair (raw 32-byte halves). */
    class EphemeralKeys(val priv: ByteArray, val pub: ByteArray)

    fun makeEphemeralKeys(): EphemeralKeys {
        val gen = X25519KeyPairGenerator().apply {
            init(X25519KeyGenerationParameters(SecureRandom()))
        }
        val kp = gen.generateKeyPair()
        val priv = (kp.private as X25519PrivateKeyParameters).encoded
        val pub = (kp.public as X25519PublicKeyParameters).encoded
        return EphemeralKeys(priv, pub)
    }

    /**
     * ECDH against [theirPub] + HKDF-SHA256 (empty salt, "rcq-radio-1to1"
     * info) → a 32-byte AES key. Both sides derive the same key because X25519
     * is symmetric: `derive(privA, pubB) == derive(privB, pubA)`.
     */
    fun deriveSessionKey(myPriv: ByteArray, theirPub: ByteArray): ByteArray {
        val priv = X25519PrivateKeyParameters(myPriv, 0)
        val pub = X25519PublicKeyParameters(theirPub, 0)
        val secret = ByteArray(32)
        priv.generateSecret(pub, secret, 0)
        val hkdf = HKDFBytesGenerator(SHA256Digest())
        hkdf.init(HKDFParameters(secret, ByteArray(0), INFO_1TO1))
        val out = ByteArray(32)
        hkdf.generateBytes(out, 0, 32)
        return out
    }

    /**
     * Open rooms: SHA256(roomID) — anyone who can see the row may join.
     * Password rooms: PBKDF2-SHA256(password, salt=roomID, 100k iterations).
     * The password is trimmed first so host and joiner agree on the key.
     */
    fun roomKey(roomID: String, password: String?): ByteArray {
        val salt = roomID.toByteArray(Charsets.UTF_8)
        val pw = password?.trim()
        if (!pw.isNullOrEmpty()) {
            val spec = PBEKeySpec(pw.toCharArray(), salt, 100_000, 256)
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                .generateSecret(spec).encoded
        }
        return MessageDigest.getInstance("SHA-256").digest(salt)
    }

    /** AES-GCM combined: nonce(12) || ciphertext || tag(16). */
    fun seal(plain: ByteArray, key: ByteArray): ByteArray {
        val nonce = ByteArray(12).also { SecureRandom().nextBytes(it) }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(128, nonce))
        return nonce + cipher.doFinal(plain) // doFinal returns ciphertext||tag
    }

    /** Throws on a wrong key / tampered ciphertext (GCM tag mismatch). */
    fun open(combined: ByteArray, key: ByteArray): ByteArray {
        val nonce = combined.copyOfRange(0, 12)
        val ctTag = combined.copyOfRange(12, combined.size)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(128, nonce))
        return cipher.doFinal(ctTag)
    }

    private val INFO_1TO1 = "rcq-radio-1to1".toByteArray(Charsets.UTF_8)
}

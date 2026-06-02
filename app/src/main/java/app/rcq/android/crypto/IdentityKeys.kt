package app.rcq.android.crypto

import org.bouncycastle.crypto.digests.SHA256Digest
import org.bouncycastle.crypto.generators.Ed25519KeyPairGenerator
import org.bouncycastle.crypto.generators.HKDFBytesGenerator
import org.bouncycastle.crypto.generators.X25519KeyPairGenerator
import org.bouncycastle.crypto.params.Ed25519KeyGenerationParameters
import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters
import org.bouncycastle.crypto.params.Ed25519PublicKeyParameters
import org.bouncycastle.crypto.params.HKDFParameters
import org.bouncycastle.crypto.params.X25519KeyGenerationParameters
import org.bouncycastle.crypto.params.X25519PrivateKeyParameters
import org.bouncycastle.crypto.params.X25519PublicKeyParameters
import java.security.SecureRandom

/**
 * Raw key material for a freshly minted identity. Every field is the raw
 * 32-byte representation; the backend's `/auth/register` wants base64 of
 * the two PUBLIC keys (rcq-spec 2.2), and the two PRIVATE keys are written
 * to encrypted storage and never leave the device (rcq-spec 2.6).
 */
class GeneratedIdentity(
    val identityPublic: ByteArray,   // X25519 public  (32 bytes)
    val identityPrivate: ByteArray,  // X25519 private (32 bytes)
    val signingPublic: ByteArray,    // Ed25519 public  (32 bytes)
    val signingPrivate: ByteArray,   // Ed25519 private (32 bytes)
)

/**
 * Generates the long-term identity keypairs.
 *
 * Uses the BouncyCastle lightweight API directly (not via a JCA Provider)
 * to avoid clashing with the cut-down BC that ships inside Android, and
 * because JCA's "XDH"/"Ed25519" generators only exist from API 33 while
 * minSdk here is 26. The `.encoded` accessors return the raw 32-byte keys,
 * which is exactly what the wire format expects.
 *
 * This covers v=1 sealed-sender envelopes (X25519 ECDH + Ed25519 signing).
 * The libsignal v=2 stack (PQXDH prekey bundle + Double Ratchet) is a
 * later milestone and will layer on top.
 */
object IdentityKeys {
    fun generate(): GeneratedIdentity {
        val rng = SecureRandom()

        val xGen = X25519KeyPairGenerator().apply {
            init(X25519KeyGenerationParameters(rng))
        }
        val xKp = xGen.generateKeyPair()
        val xPriv = xKp.private as X25519PrivateKeyParameters
        val xPub = xKp.public as X25519PublicKeyParameters

        val edGen = Ed25519KeyPairGenerator().apply {
            init(Ed25519KeyGenerationParameters(rng))
        }
        val edKp = edGen.generateKeyPair()
        val edPriv = edKp.private as Ed25519PrivateKeyParameters
        val edPub = edKp.public as Ed25519PublicKeyParameters

        return GeneratedIdentity(
            identityPublic = xPub.encoded,
            identityPrivate = xPriv.encoded,
            signingPublic = edPub.encoded,
            signingPrivate = edPriv.encoded,
        )
    }

    /**
     * Deterministically derive the SAME identity every time from a 32-byte
     * recovery seed — the basis of seed-phrase account recovery. Both private
     * keys come from HKDF-SHA256 over the seed with distinct info strings, so a
     * backed-up seed reproduces the exact keypairs (and therefore the same
     * server-side UIN). The derivation is fixed + platform-agnostic (iOS must
     * match byte-for-byte), so DO NOT change the info strings.
     */
    fun fromSeed(seed: ByteArray): GeneratedIdentity {
        val xPriv = X25519PrivateKeyParameters(hkdf(seed, INFO_X25519, 32), 0)
        val edPriv = Ed25519PrivateKeyParameters(hkdf(seed, INFO_ED25519, 32), 0)
        return GeneratedIdentity(
            identityPublic = xPriv.generatePublicKey().encoded,
            identityPrivate = xPriv.encoded,
            signingPublic = edPriv.generatePublicKey().encoded,
            signingPrivate = edPriv.encoded,
        )
    }

    /** A fresh 32-byte recovery seed (256 bits → a 24-word BIP39 phrase). */
    fun newSeed(): ByteArray = ByteArray(32).also { SecureRandom().nextBytes(it) }

    private fun hkdf(ikm: ByteArray, info: String, len: Int): ByteArray {
        val gen = HKDFBytesGenerator(SHA256Digest())
        gen.init(HKDFParameters(ikm, null, info.toByteArray(Charsets.UTF_8)))
        return ByteArray(len).also { gen.generateBytes(it, 0, len) }
    }

    private const val INFO_X25519 = "rcq-recovery-x25519-v1"
    private const val INFO_ED25519 = "rcq-recovery-ed25519-v1"
}

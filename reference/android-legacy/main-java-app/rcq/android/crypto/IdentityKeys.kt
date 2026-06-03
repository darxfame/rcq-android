package app.rcq.android.crypto

import org.bouncycastle.crypto.generators.Ed25519KeyPairGenerator
import org.bouncycastle.crypto.generators.X25519KeyPairGenerator
import org.bouncycastle.crypto.params.Ed25519KeyGenerationParameters
import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters
import org.bouncycastle.crypto.params.Ed25519PublicKeyParameters
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
}

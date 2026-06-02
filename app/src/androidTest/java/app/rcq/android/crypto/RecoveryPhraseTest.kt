package app.rcq.android.crypto

import android.util.Base64
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.bouncycastle.crypto.params.Ed25519PublicKeyParameters
import org.bouncycastle.crypto.signers.Ed25519Signer
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * On-device verification of seed-phrase account recovery crypto (the part the
 * emulator CAN prove, no server needed): BIP39 round-trips the seed, seed→keys
 * derivation is deterministic, and the recovery challenge signature verifies
 * under the derived signing public key. The full register→recover loop against
 * the live `/auth/recover` is a separate end-to-end check.
 */
@RunWith(AndroidJUnit4::class)
class RecoveryPhraseTest {
    private val ctx = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun wordlistIsTheStandard2048() {
        val wl = RecoveryPhrase.wordlist(ctx)
        assertEquals(2048, wl.size)
        assertEquals("abandon", wl.first())
        assertEquals("zoo", wl.last())
    }

    @Test
    fun phraseRoundTrips() {
        repeat(20) {
            val seed = IdentityKeys.newSeed()
            val words = RecoveryPhrase.encode(seed, ctx)
            assertEquals(24, words.size)
            assertArrayEquals("seed must survive encode→decode", seed, RecoveryPhrase.decode(words, ctx))
        }
    }

    @Test
    fun decodeRejectsBadInput() {
        val good = RecoveryPhrase.encode(IdentityKeys.newSeed(), ctx)
        assertNull("wrong word count → null", RecoveryPhrase.decode(good.take(23), ctx))
        assertNull("out-of-list word → null", RecoveryPhrase.decode(listOf("notarealword") + good.drop(1), ctx))
        // Spacing/case tolerance via parse().
        val joined = good.joinToString("   ").uppercase()
        assertArrayEquals(RecoveryPhrase.decode(good, ctx), RecoveryPhrase.decode(RecoveryPhrase.parse(joined), ctx))
    }

    @Test
    fun seedDerivationIsDeterministic() {
        val seed = IdentityKeys.newSeed()
        val a = IdentityKeys.fromSeed(seed)
        val b = IdentityKeys.fromSeed(seed)
        assertArrayEquals(a.identityPublic, b.identityPublic)
        assertArrayEquals(a.identityPrivate, b.identityPrivate)
        assertArrayEquals(a.signingPublic, b.signingPublic)
        assertArrayEquals(a.signingPrivate, b.signingPrivate)
        assertEquals(32, a.identityPublic.size)
        assertEquals(32, a.signingPublic.size)
        // A different seed → different identity.
        assertFalse(a.signingPublic.contentEquals(IdentityKeys.fromSeed(IdentityKeys.newSeed()).signingPublic))
    }

    @Test
    fun challengeSignatureVerifies() {
        val id = IdentityKeys.fromSeed(IdentityKeys.newSeed())
        val challenge = "eyJ0eXAiOiJyZWNvdmVyIn0.fake.challenge"
        val sig = Base64.decode(RecoveryPhrase.signChallenge(id.signingPrivate, challenge), Base64.NO_WRAP)
        val msg = challenge.toByteArray(Charsets.UTF_8)
        val verifier = Ed25519Signer().apply {
            init(false, Ed25519PublicKeyParameters(id.signingPublic, 0))
            update(msg, 0, msg.size)
        }
        assertTrue("signature must verify under the derived signing pubkey", verifier.verifySignature(sig))
    }
}

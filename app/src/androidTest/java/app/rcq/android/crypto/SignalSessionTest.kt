package app.rcq.android.crypto

import android.util.Base64
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import app.rcq.android.net.RcqApi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.signal.libsignal.protocol.IdentityKeyPair
import org.signal.libsignal.protocol.ecc.ECKeyPair
import org.signal.libsignal.protocol.kem.KEMKeyPair
import org.signal.libsignal.protocol.kem.KEMKeyType
import org.signal.libsignal.protocol.state.KyberPreKeyRecord
import org.signal.libsignal.protocol.state.PreKeyRecord
import org.signal.libsignal.protocol.state.SignedPreKeyRecord

/**
 * On-device verification of v=2 forward secrecy (steps 4 + 5). Runs the real
 * libsignal native lib through a full two-party Double Ratchet round-trip —
 * Alice and Bob each get their own [SignalStores] (separate per-account DBs),
 * exactly as two real devices would. No server and no second emulator: each
 * party is bootstrapped locally and its public bundle handed to the other,
 * standing in for the `/keys/{uin}/bundle` fetch.
 *
 * This proves the Android side is self-consistent (establish → encrypt → wrap
 * → unwrap → decrypt, prekey→signal ratchet transition, version dispatch). The
 * iOS↔Android byte-compat gate (Android libsignal 0.86.5 vs iOS 0.93.1) is a
 * separate cross-device test.
 */
@RunWith(AndroidJUnit4::class)
class SignalSessionTest {
    private val ctx = InstrumentationRegistry.getInstrumentation().targetContext

    private fun freshStores(id: String): SignalStores =
        SignalStores(SignalStoreDb(ctx, id)).also { it.wipe() }

    private fun b64(b: ByteArray) = Base64.encodeToString(b, Base64.NO_WRAP)

    /**
     * Bootstrap [stores] for [uin] locally (no upload) and return the public
     * bundle the peer would fetch from the server. Mirrors [SignalBootstrap]
     * but keeps the published material in-process.
     */
    private fun bootstrapLocal(stores: SignalStores, uin: Int): RcqApi.PeerBundle {
        val identity = IdentityKeyPair.generate()
        val regId = (1..16380).random()
        stores.storeLocalIdentity(uin, identity, regId)
        val now = System.currentTimeMillis()

        val signedId = 1001
        val signedKp = ECKeyPair.generate()
        val signedPub = signedKp.publicKey.serialize()
        val signedSig = identity.privateKey.calculateSignature(signedPub)
        stores.storeSignedPreKey(signedId, SignedPreKeyRecord(signedId, now, signedKp, signedSig))

        val kyberId = 2002
        val kyberKp = KEMKeyPair.generate(KEMKeyType.KYBER_1024)
        val kyberPub = kyberKp.publicKey.serialize()
        val kyberSig = identity.privateKey.calculateSignature(kyberPub)
        stores.storeKyberPreKey(kyberId, KyberPreKeyRecord(kyberId, now, kyberKp, kyberSig))

        val opkId = 3003
        val opkKp = ECKeyPair.generate()
        stores.storePreKey(opkId, PreKeyRecord(opkId, opkKp))

        return RcqApi.PeerBundle(
            uin = uin,
            registration_id = regId,
            signal_identity_key = b64(identity.publicKey.serialize()),
            signed_prekey = RcqApi.SignedPreKeyDto(signedId, b64(signedPub), b64(signedSig)),
            kyber_prekey = RcqApi.KyberPreKeyDto(kyberId, b64(kyberPub), b64(kyberSig)),
            one_time_prekey = RcqApi.OneTimePreKeyDto(opkId, b64(opkKp.publicKey.serialize())),
        )
    }

    @Test
    fun twoPartyRatchetRoundTrip() {
        val aliceUin = 111_111
        val bobUin = 222_222
        val alice = freshStores("test-fs-alice")
        val bob = freshStores("test-fs-bob")
        try {
            bootstrapLocal(alice, aliceUin)
            val bobBundle = bootstrapLocal(bob, bobUin)

            // X25519 messaging identity keys for the OUTER ECIES wrap (distinct
            // from the libsignal identity inside the ratchet).
            val aliceMsg = IdentityKeys.generate()
            val bobMsg = IdentityKeys.generate()

            // STEP 4: Alice establishes a session to Bob from Bob's bundle.
            assertTrue("establishSession should succeed", SignalSession.establishSession(alice, bobBundle))

            // STEP 5: Alice -> Bob #1 — a self-contained prekey message, v=2 wire.
            val p1 = SignalSession.encrypt(alice, Envelope.text("fs-hello-1"), bobMsg.identityPublic, bobUin, aliceUin)
            assertEquals("wire version", 2, SealedSender.wireVersion(p1))
            val d1 = SignalSession.decrypt(bob, p1, bobMsg.identityPrivate, bobMsg.identityPublic)
            assertEquals(aliceUin, d1.senderUin)
            assertEquals("fs-hello-1", (d1.envelope as Envelope.Text).text)

            // Alice -> Bob #2 — still a prekey message (Bob hasn't replied yet),
            // so it must also stand alone and decrypt on Bob's now-seeded session.
            val p2 = SignalSession.encrypt(alice, Envelope.text("fs-hello-2"), bobMsg.identityPublic, bobUin, aliceUin)
            val d2 = SignalSession.decrypt(bob, p2, bobMsg.identityPrivate, bobMsg.identityPublic)
            assertEquals("fs-hello-2", (d2.envelope as Envelope.Text).text)

            // Bob -> Alice reply. Bob already has the inbound session from the
            // prekey messages — no establishSession needed on his side.
            val r1 = SignalSession.encrypt(bob, Envelope.text("fs-reply-1"), aliceMsg.identityPublic, aliceUin, bobUin)
            val dr1 = SignalSession.decrypt(alice, r1, aliceMsg.identityPrivate, aliceMsg.identityPublic)
            assertEquals(bobUin, dr1.senderUin)
            assertEquals("fs-reply-1", (dr1.envelope as Envelope.Text).text)

            // After Bob's reply, Alice's chain advances — her next message is a
            // "signal" (whisper) on the established session. Must still decrypt.
            val p3 = SignalSession.encrypt(alice, Envelope.text("fs-hello-3"), bobMsg.identityPublic, bobUin, aliceUin)
            val d3 = SignalSession.decrypt(bob, p3, bobMsg.identityPrivate, bobMsg.identityPublic)
            assertEquals("fs-hello-3", (d3.envelope as Envelope.Text).text)

            // Distinct ciphertext per message: the ratchet is advancing (no key
            // reuse), which is the whole point of forward secrecy.
            assertNotEquals(p1, p2)
            assertNotEquals(p2, p3)
        } finally {
            alice.wipe(); bob.wipe()
        }
    }

    @Test
    fun wireVersionDispatch() {
        // A v=1 payload must report version 1 so [Session.decryptInbound] keeps
        // it on the legacy ECIES path (and never routes it through libsignal).
        val recipient = IdentityKeys.generate()
        val sender = IdentityKeys.generate()
        val v1 = SealedSender.encryptV1(
            Envelope.text("legacy"),
            recipient.identityPublic,
            42,
            sender.signingPrivate,
            sender.signingPublic,
        )
        assertEquals(1, SealedSender.wireVersion(v1))
    }
}

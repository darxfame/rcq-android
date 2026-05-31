package app.rcq.android.crypto

import android.util.Base64
import android.util.Log
import app.rcq.android.net.RcqApi
import org.signal.libsignal.protocol.IdentityKey
import org.signal.libsignal.protocol.SessionBuilder
import org.signal.libsignal.protocol.SessionCipher
import org.signal.libsignal.protocol.SignalProtocolAddress
import org.signal.libsignal.protocol.ecc.ECPublicKey
import org.signal.libsignal.protocol.kem.KEMPublicKey
import org.signal.libsignal.protocol.message.CiphertextMessage
import org.signal.libsignal.protocol.message.PreKeySignalMessage
import org.signal.libsignal.protocol.message.SignalMessage
import org.signal.libsignal.protocol.state.PreKeyBundle

/**
 * v=2 forward secrecy — libsignal Double Ratchet session layer (steps 4 + 5).
 * Android port of the iOS SignalSession + CryptoService.encryptStage3 /
 * decryptV2. The outer ECIES wrap is shared with v=1 and lives in
 * [SealedSender] ([SealedSender.wrapV2] / [unwrapV2]); this file owns the
 * libsignal half: PQXDH session establishment and per-message ratchet
 * encrypt/decrypt.
 *
 * Everything here is ADDITIVE over v=1. The caller negotiates: v=2 when a
 * session exists or can be established, else v=1 — so a peer without a
 * published bundle (or a transient failure) always falls back to the proven
 * v=1 path, never a broken send.
 *
 * Trust: TOFU, identical to iOS (see [SignalStores.isTrustedIdentity]). Safety
 * numbers / fingerprint verification is a separate future feature.
 */
object SignalSession {
    private const val TAG = "RCQsignal"
    // libsignal uses a per-account single device; we always address device 1
    // (matching iOS `ProtocolAddress(name:deviceId:1)`).
    private const val DEVICE_ID = 1

    private fun addressOf(uin: Int) = SignalProtocolAddress(uin.toString(), DEVICE_ID)

    // ── STEP 4: session establishment (X3DH / PQXDH) ─────────────────

    /**
     * Idempotent session establishment for [uin]. If a libsignal session
     * already exists locally, returns immediately. Otherwise fetches the
     * peer's pre-key bundle (consuming one one-time pre-key on the server)
     * and runs [SessionBuilder.process] to seed a SessionRecord. Suspends on
     * the HTTP fetch; call once before the first v=2 encrypt to a peer.
     *
     * Returns true if a usable session exists afterwards, false if the peer
     * has no bundle / the fetch failed — in which case the caller stays on
     * v=1. Never throws for the "no bundle" case; the API layer's error is
     * caught and reported as false.
     */
    suspend fun ensureSession(stores: SignalStores, api: RcqApi, uin: Int): Boolean {
        val addr = addressOf(uin)
        // Fast path: existing session. The lock keeps the ratchet read-modify-
        // write atomic against a concurrent encrypt/decrypt on the same account
        // (see [encrypt]/[decrypt]); it is never held across the network fetch.
        synchronized(stores) { if (stores.loadSession(addr) != null) return true }
        val bundle = try {
            api.fetchPeerBundle(uin)
        } catch (e: Exception) {
            // No published bundle (peer hasn't bootstrapped v=2) or a transient
            // error: stay on v=1.
            Log.d(TAG, "no v2 bundle for $uin (${e.javaClass.simpleName}); using v1")
            return false
        }
        return establishSession(stores, bundle)
    }

    /**
     * Seed a session into [stores] from an already-fetched [bundle] (no
     * network). Split out from [ensureSession] so it can be driven directly
     * in tests with a locally-built bundle, and as a seam for a future
     * session warm-up. Returns true if a session exists afterwards.
     */
    fun establishSession(stores: SignalStores, bundle: RcqApi.PeerBundle): Boolean {
        val addr = addressOf(bundle.uin)
        return try {
            val preKeyBundle = buildPreKeyBundle(bundle)
            synchronized(stores) {
                // Another sender may have established it while we fetched.
                if (stores.loadSession(addr) != null) return true
                // SessionBuilder takes the four non-Kyber stores (the initiator
                // never consults its own Kyber store — it uses the peer's Kyber
                // pub from the bundle).
                SessionBuilder(stores, stores, stores, stores, addr).process(preKeyBundle)
            }
            true
        } catch (e: Exception) {
            Log.w(TAG, "session establish failed for ${bundle.uin}: ${e.javaClass.simpleName}: ${e.message}")
            false
        }
    }

    private fun buildPreKeyBundle(b: RcqApi.PeerBundle): PreKeyBundle {
        val identityKey = IdentityKey(b64d(b.signal_identity_key))
        val signedPub = ECPublicKey(b64d(b.signed_prekey.publicKey))
        val signedSig = b64d(b.signed_prekey.signature)
        val kyberPub = KEMPublicKey(b64d(b.kyber_prekey.publicKey))
        val kyberSig = b64d(b.kyber_prekey.signature)
        val opk = b.one_time_prekey
        return if (opk != null) {
            PreKeyBundle(
                b.registration_id,
                DEVICE_ID,
                opk.id,
                ECPublicKey(b64d(opk.publicKey)),
                b.signed_prekey.id,
                signedPub,
                signedSig,
                identityKey,
                b.kyber_prekey.id,
                kyberPub,
                kyberSig,
            )
        } else {
            // Pool exhausted server-side — PQXDH still proceeds without the
            // one-time pre-key (we lose one contributory secret). preKey is
            // nullable in libsignal when preKeyId == NULL_PRE_KEY_ID.
            Log.i(TAG, "bundle for ${b.uin} had no OPK; establishing without one")
            PreKeyBundle(
                b.registration_id,
                DEVICE_ID,
                PreKeyBundle.NULL_PRE_KEY_ID,
                null,
                b.signed_prekey.id,
                signedPub,
                signedSig,
                identityKey,
                b.kyber_prekey.id,
                kyberPub,
                kyberSig,
            )
        }
    }

    // ── STEP 5: v=2 send / recv ──────────────────────────────────────

    /**
     * Encrypt [envelope] to [recipientUin] over the established libsignal
     * session, then wrap the ratchet ciphertext in the v=2 outer ECIES
     * addressed to [recipientMessagingPub] (the peer's X25519 messaging
     * identity key — the same key v=1 uses; distinct from the libsignal
     * identity key inside the ratchet). [ensureSession] must have run first.
     *
     * libsignal emits a self-contained PreKeySignalMessage ("prekey") on
     * every send until the peer replies, then switches to SignalMessage
     * ("signal"). That property is what makes "encrypt once, resend the
     * identical bytes on retry" safe: a lost first message is always a
     * prekey, so any later retry still carries full session-init material.
     */
    fun encrypt(
        stores: SignalStores,
        envelope: Envelope,
        recipientMessagingPub: ByteArray,
        recipientUin: Int,
        ownUin: Int,
    ): String {
        val addr = addressOf(recipientUin)
        val (kind, libsignalBytes) = synchronized(stores) {
            val cipher = SessionCipher(stores, stores, stores, stores, stores, addr)
            val ciphertext: CiphertextMessage = cipher.encrypt(envelope.toJsonBytes())
            val k = when (ciphertext.type) {
                CiphertextMessage.PREKEY_TYPE -> "prekey"
                CiphertextMessage.WHISPER_TYPE -> "signal"
                else -> error("unexpected libsignal ciphertext type ${ciphertext.type}")
            }
            k to ciphertext.serialize()
        }
        return SealedSender.wrapV2(libsignalBytes, kind, recipientMessagingPub, ownUin)
    }

    /**
     * Decrypt a v=2 payload: peel the outer ECIES, then run the inner
     * libsignal ciphertext through the Double Ratchet. A "prekey" message
     * auto-establishes the inbound session (no server round-trip), so the
     * receive path needs no [ensureSession] call. Synchronous (no network).
     */
    fun decrypt(
        stores: SignalStores,
        payloadB64: String,
        ownIdentityPriv: ByteArray,
        ownIdentityPub: ByteArray,
    ): SealedSender.Decrypted {
        val u = SealedSender.unwrapV2(payloadB64, ownIdentityPriv, ownIdentityPub)
        val addr = addressOf(u.senderUin)
        val plain: ByteArray = synchronized(stores) {
            val cipher = SessionCipher(stores, stores, stores, stores, stores, addr)
            when (u.kind) {
                "prekey" -> cipher.decrypt(PreKeySignalMessage(u.msgBytes))
                "signal" -> cipher.decrypt(SignalMessage(u.msgBytes))
                else -> throw SealedSender.DecryptException("unknown v2 kind ${u.kind}")
            }
        }
        // v=2 carries no separate signing pub — the ratchet authenticates the
        // sender — so report an empty one (ingest never reads it).
        return SealedSender.Decrypted(u.senderUin, Envelope.fromJsonBytes(plain), ByteArray(0))
    }

    private fun b64d(s: String): ByteArray = Base64.decode(s, Base64.NO_WRAP)
}

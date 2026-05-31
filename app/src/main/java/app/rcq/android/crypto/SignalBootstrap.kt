package app.rcq.android.crypto

import android.util.Base64
import app.rcq.android.net.RcqApi
import org.signal.libsignal.protocol.IdentityKeyPair
import org.signal.libsignal.protocol.ecc.ECKeyPair
import org.signal.libsignal.protocol.kem.KEMKeyPair
import org.signal.libsignal.protocol.kem.KEMKeyType
import org.signal.libsignal.protocol.state.KyberPreKeyRecord
import org.signal.libsignal.protocol.state.PreKeyRecord
import org.signal.libsignal.protocol.state.SignedPreKeyRecord

/**
 * Idempotent libsignal identity bootstrap (v=2 step 3) — Android port of iOS
 * SignalIdentityBootstrap. Ensures the local libsignal identity, signed
 * pre-key, Kyber pre-key (PQXDH) and one-time pre-key pool exist, and the
 * matching PUBLIC material is uploaded to the server so peers can start v=2
 * sessions with us. Best-effort: if anything throws, the caller swallows it
 * and the encrypt path simply stays on v=1.
 */
object SignalBootstrap {
    private const val TARGET_OPK = 100
    private const val TOPUP_THRESHOLD = 25
    // libsignal pre-key ids: 31-bit positive, matching iOS.
    private const val MAX_ID = 0x7FFFFFFF

    suspend fun ensureBootstrapped(stores: SignalStores, api: RcqApi, ownUin: Int) {
        val localUin = stores.localUin()
        if (localUin != null) {
            if (localUin != ownUin) {
                // UIN drift (local stores survived a server-side wipe + new UIN).
                stores.wipe()
            } else {
                topUpIfNeeded(stores, api)
                return
            }
        }
        freshBootstrap(stores, api, ownUin)
    }

    private suspend fun freshBootstrap(stores: SignalStores, api: RcqApi, ownUin: Int) {
        val identity = IdentityKeyPair.generate()
        val registrationId = (1..16380).random()           // 14-bit, backend-enforced range
        stores.storeLocalIdentity(ownUin, identity, registrationId)
        val nowMs = System.currentTimeMillis()

        // Signed pre-key (EC), signed by the identity key.
        val signedId = (1..MAX_ID).random()
        val signedKp = ECKeyPair.generate()
        val signedPub = signedKp.publicKey.serialize()
        val signedSig = identity.privateKey.calculateSignature(signedPub)
        stores.storeSignedPreKey(signedId, SignedPreKeyRecord(signedId, nowMs, signedKp, signedSig))

        // Kyber pre-key (PQXDH post-quantum half). Single rotating key; reuse OK
        // since forward secrecy comes from the EC ephemeral side.
        val kyberId = (1..MAX_ID).random()
        val kyberKp = KEMKeyPair.generate(KEMKeyType.KYBER_1024)
        val kyberPub = kyberKp.publicKey.serialize()
        val kyberSig = identity.privateKey.calculateSignature(kyberPub)
        stores.storeKyberPreKey(kyberId, KyberPreKeyRecord(kyberId, nowMs, kyberKp, kyberSig))

        // One-time pre-key pool (consumed at X3DH/PQXDH initiation).
        val opks = ArrayList<RcqApi.OneTimePreKeyDto>(TARGET_OPK)
        repeat(TARGET_OPK) {
            val id = (1..MAX_ID).random()
            val kp = ECKeyPair.generate()
            stores.storePreKey(id, PreKeyRecord(id, kp))
            opks.add(RcqApi.OneTimePreKeyDto(id, b64(kp.publicKey.serialize())))
        }

        api.uploadKeysBundle(
            RcqApi.KeysBundleBody(
                signal_identity_key = b64(identity.publicKey.serialize()),
                registration_id = registrationId,
                signed_prekey = RcqApi.SignedPreKeyDto(signedId, b64(signedPub), b64(signedSig)),
                kyber_prekey = RcqApi.KyberPreKeyDto(kyberId, b64(kyberPub), b64(kyberSig)),
                one_time_prekeys = opks,
            )
        )
    }

    private suspend fun topUpIfNeeded(stores: SignalStores, api: RcqApi) {
        val status = runCatching { api.keysStatus() }.getOrNull() ?: return
        if (!status.has_bundle) {
            // Server forgot us (db wipe) → rebuild from scratch.
            val uin = stores.localUin() ?: return
            stores.wipe()
            freshBootstrap(stores, api, uin)
            return
        }
        if (status.one_time_prekey_count < TOPUP_THRESHOLD) {
            replenishOpks(stores, api, (TARGET_OPK - status.one_time_prekey_count).coerceAtLeast(0))
        }
    }

    private suspend fun replenishOpks(stores: SignalStores, api: RcqApi, count: Int) {
        if (count <= 0) return
        val batch = ArrayList<RcqApi.OneTimePreKeyDto>(count)
        repeat(count) {
            val id = (1..MAX_ID).random()
            val kp = ECKeyPair.generate()
            stores.storePreKey(id, PreKeyRecord(id, kp))
            batch.add(RcqApi.OneTimePreKeyDto(id, b64(kp.publicKey.serialize())))
        }
        api.replenishPrekeys(RcqApi.PrekeysBody(batch))
    }

    private fun b64(bytes: ByteArray): String = Base64.encodeToString(bytes, Base64.NO_WRAP)
}

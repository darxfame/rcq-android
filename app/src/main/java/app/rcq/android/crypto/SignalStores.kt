package app.rcq.android.crypto

import org.signal.libsignal.protocol.IdentityKey
import org.signal.libsignal.protocol.IdentityKeyPair
import org.signal.libsignal.protocol.InvalidKeyIdException
import org.signal.libsignal.protocol.SignalProtocolAddress
import org.signal.libsignal.protocol.ecc.ECPublicKey
import org.signal.libsignal.protocol.state.IdentityKeyStore
import org.signal.libsignal.protocol.state.KyberPreKeyRecord
import org.signal.libsignal.protocol.state.KyberPreKeyStore
import org.signal.libsignal.protocol.state.PreKeyRecord
import org.signal.libsignal.protocol.state.PreKeyStore
import org.signal.libsignal.protocol.state.SessionRecord
import org.signal.libsignal.protocol.state.SessionStore
import org.signal.libsignal.protocol.state.SignedPreKeyRecord
import org.signal.libsignal.protocol.state.SignedPreKeyStore

/**
 * libsignal protocol stores backed by [SignalStoreDb], for v=2 forward
 * secrecy. Android port of the iOS SignalProtocolStores. One instance per
 * active account (its [SignalStoreDb] is per-account).
 *
 * Trust model mirrors iOS: TOFU + accept rotations ([isTrustedIdentity]
 * always true). Key-fingerprint verification (safety numbers) is a separate
 * future feature; until then a malicious server could MITM by key
 * substitution, same caveat as iOS.
 */
class SignalStores(private val db: SignalStoreDb) :
    IdentityKeyStore, PreKeyStore, SignedPreKeyStore, KyberPreKeyStore, SessionStore {

    private fun addressKey(address: SignalProtocolAddress): String =
        "${address.name}:${address.deviceId}"

    // ── local identity helpers ───────────────────────────────────────
    fun localUin(): Int? = db.loadLocalIdentity()?.first

    fun hasLocalIdentity(): Boolean = db.loadLocalIdentity() != null

    fun localAddress(): SignalProtocolAddress {
        val uin = localUin() ?: error("no local libsignal identity (not bootstrapped)")
        return SignalProtocolAddress(uin.toString(), 1)
    }

    fun storeLocalIdentity(uin: Int, identityKeyPair: IdentityKeyPair, registrationId: Int) {
        db.storeLocalIdentity(uin, identityKeyPair.serialize(), registrationId)
    }

    // ── IdentityKeyStore ─────────────────────────────────────────────
    override fun getIdentityKeyPair(): IdentityKeyPair {
        val row = db.loadLocalIdentity() ?: error("no local libsignal identity")
        return IdentityKeyPair(row.second)
    }

    override fun getLocalRegistrationId(): Int =
        db.loadLocalIdentity()?.third ?: error("no local libsignal identity")

    override fun saveIdentity(address: SignalProtocolAddress, identityKey: IdentityKey): IdentityKeyStore.IdentityChange {
        val key = addressKey(address)
        val existing = db.blobByAddress("identities", "identity_key", key)
        val serialized = identityKey.serialize()
        db.putBlobByAddress("identities", "identity_key", key, serialized)
        return if (existing != null && !existing.contentEquals(serialized)) {
            IdentityKeyStore.IdentityChange.REPLACED_EXISTING
        } else {
            IdentityKeyStore.IdentityChange.NEW_OR_UNCHANGED
        }
    }

    override fun isTrustedIdentity(
        address: SignalProtocolAddress,
        identityKey: IdentityKey,
        direction: IdentityKeyStore.Direction,
    ): Boolean = true

    override fun getIdentity(address: SignalProtocolAddress): IdentityKey? {
        val blob = db.blobByAddress("identities", "identity_key", addressKey(address)) ?: return null
        return IdentityKey(blob)
    }

    // ── PreKeyStore ──────────────────────────────────────────────────
    override fun loadPreKey(id: Int): PreKeyRecord {
        val blob = db.recordByInt("prekeys", "prekey_id", id) ?: throw InvalidKeyIdException("no prekey $id")
        return PreKeyRecord(blob)
    }

    override fun storePreKey(id: Int, record: PreKeyRecord) =
        db.putRecordByInt("prekeys", "prekey_id", id, record.serialize())

    override fun containsPreKey(id: Int): Boolean = db.containsInt("prekeys", "prekey_id", id)

    override fun removePreKey(id: Int) = db.deleteByInt("prekeys", "prekey_id", id)

    // ── SignedPreKeyStore ────────────────────────────────────────────
    override fun loadSignedPreKey(id: Int): SignedPreKeyRecord {
        val blob = db.recordByInt("signed_prekeys", "signed_prekey_id", id)
            ?: throw InvalidKeyIdException("no signed prekey $id")
        return SignedPreKeyRecord(blob)
    }

    override fun loadSignedPreKeys(): MutableList<SignedPreKeyRecord> =
        db.allRecords("signed_prekeys").mapTo(ArrayList()) { SignedPreKeyRecord(it) }

    override fun storeSignedPreKey(id: Int, record: SignedPreKeyRecord) =
        db.putRecordByInt("signed_prekeys", "signed_prekey_id", id, record.serialize())

    override fun containsSignedPreKey(id: Int): Boolean = db.containsInt("signed_prekeys", "signed_prekey_id", id)

    override fun removeSignedPreKey(id: Int) = db.deleteByInt("signed_prekeys", "signed_prekey_id", id)

    // ── KyberPreKeyStore ─────────────────────────────────────────────
    override fun loadKyberPreKey(id: Int): KyberPreKeyRecord {
        val blob = db.recordByInt("kyber_prekeys", "kyber_prekey_id", id)
            ?: throw InvalidKeyIdException("no kyber prekey $id")
        return KyberPreKeyRecord(blob)
    }

    override fun loadKyberPreKeys(): MutableList<KyberPreKeyRecord> =
        db.allRecords("kyber_prekeys").mapTo(ArrayList()) { KyberPreKeyRecord(it) }

    override fun storeKyberPreKey(id: Int, record: KyberPreKeyRecord) =
        db.putRecordByInt("kyber_prekeys", "kyber_prekey_id", id, record.serialize())

    override fun containsKyberPreKey(id: Int): Boolean = db.containsInt("kyber_prekeys", "kyber_prekey_id", id)

    /** No-op: single rotating last-resort Kyber pre-key, reusable (iOS parity). */
    override fun markKyberPreKeyUsed(id: Int, signedPreKeyId: Int, baseKey: ECPublicKey) = Unit

    // ── SessionStore ─────────────────────────────────────────────────
    override fun loadSession(address: SignalProtocolAddress): SessionRecord? {
        val blob = db.blobByAddress("sessions", "record", addressKey(address)) ?: return null
        return SessionRecord(blob)
    }

    override fun loadExistingSessions(addresses: MutableList<SignalProtocolAddress>): MutableList<SessionRecord> =
        addresses.mapNotNullTo(ArrayList()) { loadSession(it) }

    override fun getSubDeviceSessions(name: String): MutableList<Int> =
        db.addressesWithName("sessions", name)
            .mapNotNull { it.substringAfterLast(':').toIntOrNull() }
            .filter { it != 1 }
            .toMutableList()

    override fun storeSession(address: SignalProtocolAddress, record: SessionRecord) =
        db.putBlobByAddress("sessions", "record", addressKey(address), record.serialize())

    override fun containsSession(address: SignalProtocolAddress): Boolean =
        db.containsAddress("sessions", addressKey(address))

    override fun deleteSession(address: SignalProtocolAddress) =
        db.deleteByAddress("sessions", addressKey(address))

    override fun deleteAllSessions(name: String) = db.deleteByAddressPrefix("sessions", name)
}

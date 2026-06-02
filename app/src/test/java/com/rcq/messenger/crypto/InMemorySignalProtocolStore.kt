package com.rcq.messenger.crypto

import org.signal.libsignal.protocol.IdentityKey
import org.signal.libsignal.protocol.IdentityKeyPair
import org.signal.libsignal.protocol.SignalProtocolAddress
import org.signal.libsignal.protocol.groups.state.SenderKeyRecord
import org.signal.libsignal.protocol.state.IdentityKeyStore
import org.signal.libsignal.protocol.state.KyberPreKeyRecord
import org.signal.libsignal.protocol.state.PreKeyRecord
import org.signal.libsignal.protocol.state.SessionRecord
import org.signal.libsignal.protocol.state.SignalProtocolStore
import org.signal.libsignal.protocol.state.SignedPreKeyRecord

/**
 * In-memory SignalProtocolStore for unit tests — avoids Room dependency.
 */
class InMemorySignalProtocolStore(
    private val identityKeyPair: IdentityKeyPair,
    private val registrationId: Int = 0
) : SignalProtocolStore {

    private val preKeys = mutableMapOf<Int, PreKeyRecord>()
    private val signedPreKeys = mutableMapOf<Int, SignedPreKeyRecord>()
    private val sessions = mutableMapOf<SignalProtocolAddress, SessionRecord>()
    private val trustedKeys = mutableMapOf<SignalProtocolAddress, IdentityKey>()
    private val senderKeys = mutableMapOf<Pair<SignalProtocolAddress, java.util.UUID>, SenderKeyRecord>()

    override fun getIdentityKeyPair(): IdentityKeyPair = identityKeyPair
    override fun getLocalRegistrationId(): Int = registrationId

    override fun saveIdentity(address: SignalProtocolAddress, identityKey: IdentityKey): Boolean {
        val prev = trustedKeys[address]
        trustedKeys[address] = identityKey
        return prev != null && prev != identityKey
    }

    override fun isTrustedIdentity(
        address: SignalProtocolAddress,
        identityKey: IdentityKey,
        direction: IdentityKeyStore.Direction
    ): Boolean {
        val stored = trustedKeys[address] ?: return true
        return stored == identityKey
    }

    override fun getIdentity(address: SignalProtocolAddress): IdentityKey? = trustedKeys[address]

    override fun loadPreKey(preKeyId: Int): PreKeyRecord =
        preKeys[preKeyId] ?: throw IllegalStateException("PreKey $preKeyId not found")

    override fun storePreKey(preKeyId: Int, record: PreKeyRecord) { preKeys[preKeyId] = record }
    override fun containsPreKey(preKeyId: Int): Boolean = preKeys.containsKey(preKeyId)
    override fun removePreKey(preKeyId: Int) { preKeys.remove(preKeyId) }

    override fun loadSignedPreKey(signedPreKeyId: Int): SignedPreKeyRecord =
        signedPreKeys[signedPreKeyId] ?: throw IllegalStateException("SignedPreKey $signedPreKeyId not found")

    override fun loadSignedPreKeys(): List<SignedPreKeyRecord> = signedPreKeys.values.toList()

    override fun storeSignedPreKey(signedPreKeyId: Int, record: SignedPreKeyRecord) {
        signedPreKeys[signedPreKeyId] = record
    }

    override fun containsSignedPreKey(signedPreKeyId: Int): Boolean = signedPreKeys.containsKey(signedPreKeyId)
    override fun removeSignedPreKey(signedPreKeyId: Int) { signedPreKeys.remove(signedPreKeyId) }

    override fun loadSession(address: SignalProtocolAddress): SessionRecord =
        sessions.getOrPut(address) { SessionRecord() }

    override fun loadExistingSessions(addresses: MutableList<SignalProtocolAddress>): MutableList<SessionRecord> =
        addresses.mapNotNull { sessions[it] }.toMutableList()

    override fun getSubDeviceSessions(name: String): List<Int> =
        sessions.keys.filter { it.name == name && it.deviceId != 1 }.map { it.deviceId }

    override fun storeSession(address: SignalProtocolAddress, record: SessionRecord) {
        sessions[address] = record
    }

    override fun containsSession(address: SignalProtocolAddress): Boolean = sessions.containsKey(address)

    override fun deleteSession(address: SignalProtocolAddress) { sessions.remove(address) }

    override fun deleteAllSessions(name: String) {
        sessions.keys.filter { it.name == name }.forEach { sessions.remove(it) }
    }

    override fun storeSenderKey(sender: SignalProtocolAddress, distributionId: java.util.UUID, record: SenderKeyRecord) {
        senderKeys[Pair(sender, distributionId)] = record
    }

    override fun loadSenderKey(sender: SignalProtocolAddress, distributionId: java.util.UUID): SenderKeyRecord? =
        senderKeys[Pair(sender, distributionId)]

    override fun loadKyberPreKey(kyberPreKeyId: Int): KyberPreKeyRecord =
        throw UnsupportedOperationException("Kyber not supported in test store")

    override fun loadKyberPreKeys(): MutableList<KyberPreKeyRecord> = mutableListOf()

    override fun storeKyberPreKey(kyberPreKeyId: Int, record: KyberPreKeyRecord) {}

    override fun containsKyberPreKey(kyberPreKeyId: Int): Boolean = false

    override fun markKyberPreKeyUsed(kyberPreKeyId: Int) {}
}

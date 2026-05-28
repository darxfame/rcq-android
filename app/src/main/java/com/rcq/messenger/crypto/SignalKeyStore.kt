package com.rcq.messenger.crypto

import org.signal.libsignal.protocol.IdentityKey
import org.signal.libsignal.protocol.IdentityKeyPair
import org.signal.libsignal.protocol.SignalProtocolAddress
import org.signal.libsignal.protocol.ecc.Curve
import org.signal.libsignal.protocol.groups.state.SenderKeyRecord
import org.signal.libsignal.protocol.state.IdentityKeyStore
import org.signal.libsignal.protocol.state.KyberPreKeyRecord
import org.signal.libsignal.protocol.state.PreKeyRecord
import org.signal.libsignal.protocol.state.SessionRecord
import org.signal.libsignal.protocol.state.SignalProtocolStore
import org.signal.libsignal.protocol.state.SignedPreKeyRecord
import javax.inject.Inject
import javax.inject.Singleton

/**
 * SignalKeyStore теперь делегирует все операции к PersistentSignalProtocolStore
 * для обеспечения персистентного хранения E2EE ключей
 */
@Singleton
class SignalKeyStore @Inject constructor(
    private val persistentStore: PersistentSignalProtocolStore
) : SignalProtocolStore {

    // Делегируем все методы к персистентному хранилищу
    override fun getIdentityKeyPair(): IdentityKeyPair = persistentStore.getIdentityKeyPair()

    override fun getLocalRegistrationId(): Int = persistentStore.getLocalRegistrationId()

    override fun saveIdentity(address: SignalProtocolAddress, identityKey: IdentityKey): Boolean =
        persistentStore.saveIdentity(address, identityKey)

    override fun isTrustedIdentity(
        address: SignalProtocolAddress,
        identityKey: IdentityKey,
        direction: IdentityKeyStore.Direction
    ): Boolean = persistentStore.isTrustedIdentity(address, identityKey, direction)

    override fun getIdentity(address: SignalProtocolAddress): IdentityKey? =
        persistentStore.getIdentity(address)

    override fun loadPreKey(preKeyId: Int): PreKeyRecord = persistentStore.loadPreKey(preKeyId)

    override fun storePreKey(preKeyId: Int, record: PreKeyRecord) =
        persistentStore.storePreKey(preKeyId, record)

    override fun containsPreKey(preKeyId: Int): Boolean = persistentStore.containsPreKey(preKeyId)

    override fun removePreKey(preKeyId: Int) = persistentStore.removePreKey(preKeyId)

    override fun loadSignedPreKey(signedPreKeyId: Int): SignedPreKeyRecord =
        persistentStore.loadSignedPreKey(signedPreKeyId)

    override fun loadSignedPreKeys(): List<SignedPreKeyRecord> = persistentStore.loadSignedPreKeys()

    override fun storeSignedPreKey(signedPreKeyId: Int, record: SignedPreKeyRecord) =
        persistentStore.storeSignedPreKey(signedPreKeyId, record)

    override fun containsSignedPreKey(signedPreKeyId: Int): Boolean =
        persistentStore.containsSignedPreKey(signedPreKeyId)

    override fun removeSignedPreKey(signedPreKeyId: Int) = persistentStore.removeSignedPreKey(signedPreKeyId)

    override fun loadSession(address: SignalProtocolAddress): SessionRecord =
        persistentStore.loadSession(address)

    override fun loadExistingSessions(addresses: MutableList<SignalProtocolAddress>): MutableList<SessionRecord> =
        persistentStore.loadExistingSessions(addresses)

    override fun getSubDeviceSessions(name: String): List<Int> =
        persistentStore.getSubDeviceSessions(name)

    override fun storeSession(address: SignalProtocolAddress, record: SessionRecord) =
        persistentStore.storeSession(address, record)

    override fun containsSession(address: SignalProtocolAddress): Boolean =
        persistentStore.containsSession(address)

    override fun deleteSession(address: SignalProtocolAddress) = persistentStore.deleteSession(address)

    override fun deleteAllSessions(name: String) = persistentStore.deleteAllSessions(name)

    override fun storeSenderKey(
        sender: SignalProtocolAddress,
        distributionId: java.util.UUID,
        record: SenderKeyRecord
    ) = persistentStore.storeSenderKey(sender, distributionId, record)

    override fun loadSenderKey(
        sender: SignalProtocolAddress,
        distributionId: java.util.UUID
    ): SenderKeyRecord? = persistentStore.loadSenderKey(sender, distributionId)

    override fun loadKyberPreKey(kyberPreKeyId: Int): KyberPreKeyRecord =
        persistentStore.loadKyberPreKey(kyberPreKeyId)

    override fun loadKyberPreKeys(): MutableList<KyberPreKeyRecord> = persistentStore.loadKyberPreKeys()

    override fun storeKyberPreKey(kyberPreKeyId: Int, record: KyberPreKeyRecord) =
        persistentStore.storeKyberPreKey(kyberPreKeyId, record)

    override fun containsKyberPreKey(kyberPreKeyId: Int): Boolean =
        persistentStore.containsKyberPreKey(kyberPreKeyId)

    override fun markKyberPreKeyUsed(kyberPreKeyId: Int) = persistentStore.markKyberPreKeyUsed(kyberPreKeyId)

    // Дополнительные методы для совместимости и инициализации
    fun getStoredIdentityKeyPair(): IdentityKeyPair = getIdentityKeyPair()

    fun getStoredRegistrationId(): Int = getLocalRegistrationId()

    fun hasActiveSession(address: SignalProtocolAddress): Boolean = containsSession(address)

    fun initializeIdentityKeyPair(keyPair: IdentityKeyPair) {
        persistentStore.storeIdentityKeyPair(keyPair)
    }

    // Pre-key management
    fun generatePreKeys(start: Int, count: Int): List<PreKeyRecord> {
        val preKeys = mutableListOf<PreKeyRecord>()
        for (i in start until start + count) {
            val preKey = PreKeyRecord(i, Curve.generateKeyPair())
            storePreKey(i, preKey)
            preKeys.add(preKey)
        }
        return preKeys
    }

    fun generateSignedPreKey(signedPreKeyId: Int, identityKeyPair: IdentityKeyPair): SignedPreKeyRecord {
        val signedKeyPair = Curve.generateKeyPair()
        val signature = Curve.calculateSignature(
            identityKeyPair.privateKey,
            signedKeyPair.publicKey.serialize()
        )
        val signedPreKey = SignedPreKeyRecord(
            signedPreKeyId,
            System.currentTimeMillis(),
            signedKeyPair,
            signature
        )
        storeSignedPreKey(signedPreKeyId, signedPreKey)
        return signedPreKey
    }
}
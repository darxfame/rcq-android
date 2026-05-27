package com.rcq.messenger.crypto

import kotlinx.coroutines.runBlocking
import org.signal.libsignal.protocol.IdentityKey
import org.signal.libsignal.protocol.IdentityKeyPair
import org.signal.libsignal.protocol.SignalProtocolAddress
import org.signal.libsignal.protocol.groups.state.SenderKeyRecord
import org.signal.libsignal.protocol.state.IdentityKeyStore
import org.signal.libsignal.protocol.state.PreKeyRecord
import org.signal.libsignal.protocol.state.SessionRecord
import org.signal.libsignal.protocol.state.SignalProtocolStore
import org.signal.libsignal.protocol.state.SignedPreKeyRecord
import com.rcq.messenger.data.db.SignalKeyDao
import com.rcq.messenger.domain.model.SignalKeyEntity
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Persistent implementation of SignalProtocolStore using Room database
 * Replaces InMemorySignalProtocolStore to maintain E2EE sessions across app restarts
 */
@Singleton
class PersistentSignalProtocolStore @Inject constructor(
    private val signalKeyDao: SignalKeyDao
) : SignalProtocolStore {

    private var localRegistrationId: Int = 0
    private var identityKeyPair: IdentityKeyPair? = null

    init {
        // Load registration ID and identity key pair on initialization
        runBlocking {
            localRegistrationId = signalKeyDao.getLocalRegistrationId() ?: generateRegistrationId()
            identityKeyPair = loadIdentityKeyPair()
        }
    }

    // IdentityKeyStore implementation
    override fun getIdentityKeyPair(): IdentityKeyPair {
        return identityKeyPair ?: throw IllegalStateException("Identity key pair not initialized")
    }

    override fun getLocalRegistrationId(): Int {
        return localRegistrationId
    }

    override fun saveIdentity(address: SignalProtocolAddress, identityKey: IdentityKey): Boolean {
        return runBlocking {
            try {
                val entity = SignalKeyEntity(
                    id = SignalKeyEntity.identityId(address.name),
                    keyType = SignalKeyEntity.TYPE_IDENTITY,
                    address = address.name,
                    keyId = null,
                    keyData = identityKey.serialize()
                )
                signalKeyDao.saveIdentity(entity)
                true
            } catch (e: Exception) {
                false
            }
        }
    }

    override fun isTrustedIdentity(
        address: SignalProtocolAddress,
        identityKey: IdentityKey,
        direction: IdentityKeyStore.Direction
    ): Boolean {
        return runBlocking {
            try {
                val stored = signalKeyDao.getIdentity(address.name)
                stored == null || stored.keyData.contentEquals(identityKey.serialize())
            } catch (e: Exception) {
                false
            }
        }
    }

    override fun getIdentity(address: SignalProtocolAddress): IdentityKey? {
        return runBlocking {
            try {
                val entity = signalKeyDao.getIdentity(address.name)
                entity?.let { IdentityKey(it.keyData, 0) }
            } catch (e: Exception) {
                null
            }
        }
    }

    // PreKeyStore implementation
    override fun loadPreKey(preKeyId: Int): PreKeyRecord {
        return runBlocking {
            val entity = signalKeyDao.loadPreKey(preKeyId)
                ?: throw IllegalStateException("PreKey $preKeyId not found")
            PreKeyRecord(entity.keyData)
        }
    }

    override fun storePreKey(preKeyId: Int, record: PreKeyRecord) {
        runBlocking {
            val entity = SignalKeyEntity(
                id = SignalKeyEntity.preKeyId(preKeyId),
                keyType = SignalKeyEntity.TYPE_PREKEY,
                address = null,
                keyId = preKeyId,
                keyData = record.serialize()
            )
            signalKeyDao.storePreKey(entity)
        }
    }

    override fun containsPreKey(preKeyId: Int): Boolean {
        return runBlocking {
            signalKeyDao.loadPreKey(preKeyId) != null
        }
    }

    override fun removePreKey(preKeyId: Int) {
        runBlocking {
            signalKeyDao.removePreKey(preKeyId)
        }
    }

    // SignedPreKeyStore implementation
    override fun loadSignedPreKey(signedPreKeyId: Int): SignedPreKeyRecord {
        return runBlocking {
            val entity = signalKeyDao.loadSignedPreKey(signedPreKeyId)
                ?: throw IllegalStateException("SignedPreKey $signedPreKeyId not found")
            SignedPreKeyRecord(entity.keyData)
        }
    }

    override fun loadSignedPreKeys(): List<SignedPreKeyRecord> {
        return runBlocking {
            signalKeyDao.loadSignedPreKeys().map { SignedPreKeyRecord(it.keyData) }
        }
    }

    override fun storeSignedPreKey(signedPreKeyId: Int, record: SignedPreKeyRecord) {
        runBlocking {
            val entity = SignalKeyEntity(
                id = SignalKeyEntity.signedPreKeyId(signedPreKeyId),
                keyType = SignalKeyEntity.TYPE_SIGNED_PREKEY,
                address = null,
                keyId = signedPreKeyId,
                keyData = record.serialize()
            )
            signalKeyDao.storeSignedPreKey(entity)
        }
    }

    override fun containsSignedPreKey(signedPreKeyId: Int): Boolean {
        return runBlocking {
            signalKeyDao.loadSignedPreKey(signedPreKeyId) != null
        }
    }

    override fun removeSignedPreKey(signedPreKeyId: Int) {
        runBlocking {
            signalKeyDao.removeSignedPreKey(signedPreKeyId)
        }
    }

    // SessionStore implementation
    override fun loadSession(address: SignalProtocolAddress): SessionRecord {
        return runBlocking {
            val entity = signalKeyDao.getSession(address.name)
            entity?.let { SessionRecord(it.keyData) } ?: SessionRecord()
        }
    }

    override fun getSubDeviceSessions(name: String): List<Int> {
        return runBlocking {
            signalKeyDao.getSubDeviceSessions(name).mapNotNull { address ->
                // Extract device ID from address format "name.deviceId"
                address.substringAfter(".", "").toIntOrNull()
            }
        }
    }

    override fun storeSession(address: SignalProtocolAddress, record: SessionRecord) {
        runBlocking {
            val entity = SignalKeyEntity(
                id = SignalKeyEntity.sessionId(address.name),
                keyType = SignalKeyEntity.TYPE_SESSION,
                address = address.name,
                keyId = null,
                keyData = record.serialize()
            )
            signalKeyDao.storeSession(entity)
        }
    }

    override fun containsSession(address: SignalProtocolAddress): Boolean {
        return runBlocking {
            signalKeyDao.getSession(address.name) != null
        }
    }

    override fun deleteSession(address: SignalProtocolAddress) {
        runBlocking {
            signalKeyDao.deleteSession(address.name)
        }
    }

    override fun deleteAllSessions(name: String) {
        runBlocking {
            signalKeyDao.deleteAllSessions(name)
        }
    }

    // SenderKeyStore implementation (for group messaging)
    override fun storeSenderKey(
        sender: SignalProtocolAddress,
        distributionId: java.util.UUID,
        record: SenderKeyRecord
    ) {
        // Implementation for group messaging sender keys
        // Can be added when group messaging is implemented
    }

    override fun loadSenderKey(
        sender: SignalProtocolAddress,
        distributionId: java.util.UUID
    ): SenderKeyRecord? {
        // Implementation for group messaging sender keys
        return null
    }

    override fun loadKyberPreKey(kyberPreKeyId: Int): KyberPreKeyRecord {
        // TODO: Implement Kyber pre-key storage when post-quantum cryptography is needed
        // For now, throw exception as this is not yet implemented
        throw UnsupportedOperationException("Kyber pre-keys not yet implemented")
    }

    // Helper methods
    private suspend fun loadIdentityKeyPair(): IdentityKeyPair? {
        return try {
            val entity = signalKeyDao.getIdentityKeyPair()
            entity?.let { IdentityKeyPair(it.keyData) }
        } catch (e: Exception) {
            null
        }
    }

    private fun generateRegistrationId(): Int {
        val registrationId = (Math.random() * 16380 + 1).toInt()
        runBlocking {
            val entity = SignalKeyEntity(
                id = "registration_id",
                keyType = "registration_id",
                address = null,
                keyId = registrationId,
                keyData = ByteArray(0)
            )
            signalKeyDao.storeLocalRegistrationId(entity)
        }
        return registrationId
    }

    fun storeIdentityKeyPair(keyPair: IdentityKeyPair) {
        runBlocking {
            val entity = SignalKeyEntity(
                id = SignalKeyEntity.identityKeyPairId(),
                keyType = SignalKeyEntity.TYPE_IDENTITY_KEYPAIR,
                address = null,
                keyId = null,
                keyData = keyPair.serialize()
            )
            signalKeyDao.storeIdentityKeyPair(entity)
            identityKeyPair = keyPair
        }
    }
}
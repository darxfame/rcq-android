package com.rcq.messenger.crypto

import com.rcq.messenger.data.db.SignalKeyDao
import com.rcq.messenger.data.api.KyberPreKeyData
import com.rcq.messenger.data.api.PreKeyBundleResponse
import com.rcq.messenger.data.api.PreKeyData
import com.rcq.messenger.data.api.SignedPreKeyData
import com.rcq.messenger.domain.model.SignalKeyEntity
import org.signal.libsignal.protocol.IdentityKeyPair
import java.util.Base64

object TestSignalStores {
    fun signalKeyStore(identityKeyPair: IdentityKeyPair = IdentityKeyPair.generate()): SignalKeyStore {
        val store = SignalKeyStore(PersistentSignalProtocolStore(FakeSignalKeyDao()))
        store.initializeIdentityKeyPair(identityKeyPair)
        return store
    }

    fun user(uin: Long): TestSignalUser {
        val identity = IdentityKeyPair.generate()
        val store = signalKeyStore(identity)
        val manager = SessionManager(store)
        return TestSignalUser(
            uin = uin,
            identityKeyPair = identity,
            store = store,
            manager = manager,
            service = CryptoService(manager, store)
        )
    }

    fun preKeyBundleFor(user: TestSignalUser): PreKeyBundleResponse {
        val preKey = user.store.generatePreKeys(1, 1).single()
        val signedPreKey = user.store.generateSignedPreKey(1, user.identityKeyPair)
        return PreKeyBundleResponse(
            uin = user.uin,
            registrationId = user.store.localRegistrationId,
            identityKey = b64(user.identityKeyPair.publicKey.serialize()),
            signedPreKey = SignedPreKeyData(
                id = signedPreKey.id,
                key = b64(signedPreKey.keyPair.publicKey.serialize()),
                signature = b64(signedPreKey.signature)
            ),
            kyberPreKey = KyberPreKeyData(id = 1, key = "", signature = ""),
            preKey = PreKeyData(id = preKey.id, key = b64(preKey.keyPair.publicKey.serialize()))
        )
    }

    private fun b64(bytes: ByteArray): String = Base64.getEncoder().encodeToString(bytes)
}

data class TestSignalUser(
    val uin: Long,
    val identityKeyPair: IdentityKeyPair,
    val store: SignalKeyStore,
    val manager: SessionManager,
    val service: CryptoService
)

class FakeSignalKeyDao : SignalKeyDao {
    private val keys = linkedMapOf<String, SignalKeyEntity>()

    override suspend fun getSession(address: String): SignalKeyEntity? =
        keys.values.firstOrNull { it.keyType == SignalKeyEntity.TYPE_SESSION && it.address == address }

    override suspend fun storeSession(session: SignalKeyEntity) {
        keys[session.id] = session
    }

    override suspend fun deleteSession(address: String) {
        keys.values.removeAll { it.keyType == SignalKeyEntity.TYPE_SESSION && it.address == address }
    }

    override suspend fun getSubDeviceSessions(name: String): List<String> =
        keys.values.mapNotNull { entity ->
            entity.address?.takeIf { entity.keyType == SignalKeyEntity.TYPE_SESSION && it.startsWith(name) }
        }

    override suspend fun deleteAllSessions(namePrefix: String) {
        keys.values.removeAll { it.keyType == SignalKeyEntity.TYPE_SESSION && it.address?.startsWith(namePrefix) == true }
    }

    override suspend fun loadPreKey(preKeyId: Int): SignalKeyEntity? =
        keys.values.firstOrNull { it.keyType == SignalKeyEntity.TYPE_PREKEY && it.keyId == preKeyId }

    override suspend fun storePreKey(preKey: SignalKeyEntity) {
        keys[preKey.id] = preKey
    }

    override suspend fun removePreKey(preKeyId: Int) {
        keys.values.removeAll { it.keyType == SignalKeyEntity.TYPE_PREKEY && it.keyId == preKeyId }
    }

    override suspend fun loadPreKeys(): List<Int> =
        keys.values.mapNotNull { it.keyId.takeIf { _ -> it.keyType == SignalKeyEntity.TYPE_PREKEY } }

    override suspend fun loadSignedPreKey(signedPreKeyId: Int): SignalKeyEntity? =
        keys.values.firstOrNull { it.keyType == SignalKeyEntity.TYPE_SIGNED_PREKEY && it.keyId == signedPreKeyId }

    override suspend fun loadSignedPreKeys(): List<SignalKeyEntity> =
        keys.values.filter { it.keyType == SignalKeyEntity.TYPE_SIGNED_PREKEY }

    override suspend fun storeSignedPreKey(signedPreKey: SignalKeyEntity) {
        keys[signedPreKey.id] = signedPreKey
    }

    override suspend fun removeSignedPreKey(signedPreKeyId: Int) {
        keys.values.removeAll { it.keyType == SignalKeyEntity.TYPE_SIGNED_PREKEY && it.keyId == signedPreKeyId }
    }

    override suspend fun getIdentityKeyPair(): SignalKeyEntity? =
        keys.values.firstOrNull { it.keyType == SignalKeyEntity.TYPE_IDENTITY_KEYPAIR }

    override suspend fun storeIdentityKeyPair(identityKeyPair: SignalKeyEntity) {
        keys[identityKeyPair.id] = identityKeyPair
    }

    override suspend fun getIdentity(address: String): SignalKeyEntity? =
        keys.values.firstOrNull { it.keyType == SignalKeyEntity.TYPE_IDENTITY && it.address == address }

    override suspend fun saveIdentity(identity: SignalKeyEntity) {
        keys[identity.id] = identity
    }

    override suspend fun getTrustedKeys(): List<String> =
        keys.values.mapNotNull { it.address.takeIf { _ -> it.keyType == SignalKeyEntity.TYPE_IDENTITY } }

    override suspend fun getLocalRegistrationId(): Int? =
        keys.values.firstOrNull { it.keyType == "registration_id" }?.keyId

    override suspend fun storeLocalRegistrationId(registrationId: SignalKeyEntity) {
        keys[registrationId.id] = registrationId
    }

    override suspend fun clearAllKeys() {
        keys.clear()
    }

    override suspend fun getKeyCount(keyType: String): Int =
        keys.values.count { it.keyType == keyType }

    override suspend fun cleanupOldKeys(cutoffTime: Long) {
        keys.values.removeAll { it.timestamp < cutoffTime }
    }
}

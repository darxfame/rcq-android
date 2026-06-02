package com.rcq.messenger.data.db

import androidx.room.*
import com.rcq.messenger.domain.model.SignalKeyEntity

/**
 * DAO for Signal Protocol keys storage
 * Provides CRUD operations for sessions, pre-keys, signed pre-keys, and identity keys
 */
@Dao
interface SignalKeyDao {

    // Session operations
    @Query("SELECT * FROM signal_keys WHERE keyType = 'session' AND address = :address")
    suspend fun getSession(address: String): SignalKeyEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun storeSession(session: SignalKeyEntity)

    @Query("DELETE FROM signal_keys WHERE keyType = 'session' AND address = :address")
    suspend fun deleteSession(address: String)

    @Query("SELECT address FROM signal_keys WHERE keyType = 'session' AND address IS NOT NULL AND address LIKE :name || '%'")
    suspend fun getSubDeviceSessions(name: String): List<String>

    @Query("DELETE FROM signal_keys WHERE keyType = 'session' AND address LIKE :namePrefix || '%'")
    suspend fun deleteAllSessions(namePrefix: String)

    // Pre-key operations
    @Query("SELECT * FROM signal_keys WHERE keyType = 'prekey' AND keyId = :preKeyId")
    suspend fun loadPreKey(preKeyId: Int): SignalKeyEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun storePreKey(preKey: SignalKeyEntity)

    @Query("DELETE FROM signal_keys WHERE keyType = 'prekey' AND keyId = :preKeyId")
    suspend fun removePreKey(preKeyId: Int)

    @Query("SELECT keyId FROM signal_keys WHERE keyType = 'prekey' AND keyId IS NOT NULL")
    suspend fun loadPreKeys(): List<Int>

    // Signed pre-key operations
    @Query("SELECT * FROM signal_keys WHERE keyType = 'signed_prekey' AND keyId = :signedPreKeyId")
    suspend fun loadSignedPreKey(signedPreKeyId: Int): SignalKeyEntity?

    @Query("SELECT * FROM signal_keys WHERE keyType = 'signed_prekey'")
    suspend fun loadSignedPreKeys(): List<SignalKeyEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun storeSignedPreKey(signedPreKey: SignalKeyEntity)

    @Query("DELETE FROM signal_keys WHERE keyType = 'signed_prekey' AND keyId = :signedPreKeyId")
    suspend fun removeSignedPreKey(signedPreKeyId: Int)

    // Identity operations
    @Query("SELECT * FROM signal_keys WHERE keyType = 'identity_keypair'")
    suspend fun getIdentityKeyPair(): SignalKeyEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun storeIdentityKeyPair(identityKeyPair: SignalKeyEntity)

    @Query("SELECT * FROM signal_keys WHERE keyType = 'identity' AND address = :address")
    suspend fun getIdentity(address: String): SignalKeyEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveIdentity(identity: SignalKeyEntity)

    @Query("SELECT address FROM signal_keys WHERE keyType = 'identity' AND address IS NOT NULL")
    suspend fun getTrustedKeys(): List<String>

    // Registration ID operations
    @Query("SELECT keyId FROM signal_keys WHERE keyType = 'registration_id' LIMIT 1")
    suspend fun getLocalRegistrationId(): Int?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun storeLocalRegistrationId(registrationId: SignalKeyEntity)

    // Utility operations
    @Query("DELETE FROM signal_keys")
    suspend fun clearAllKeys()

    @Query("SELECT COUNT(*) FROM signal_keys WHERE keyType = :keyType")
    suspend fun getKeyCount(keyType: String): Int

    @Query("DELETE FROM signal_keys WHERE timestamp < :cutoffTime")
    suspend fun cleanupOldKeys(cutoffTime: Long)
}
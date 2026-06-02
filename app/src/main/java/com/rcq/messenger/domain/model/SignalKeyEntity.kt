package com.rcq.messenger.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity for storing Signal Protocol keys persistently
 * Supports sessions, pre-keys, signed pre-keys, and identity keys
 */
@Entity(tableName = "signal_keys")
data class SignalKeyEntity(
    @PrimaryKey
    val id: String, // Composite key: "type:address:keyId" or "type:keyId"

    val keyType: String, // "session", "prekey", "signed_prekey", "identity"

    val address: String?, // Signal address for sessions and identity keys

    val keyId: Int?, // Key ID for pre-keys

    val keyData: ByteArray, // Serialized key data

    val timestamp: Long = System.currentTimeMillis()
) {
    companion object {
        // Key type constants
        const val TYPE_SESSION = "session"
        const val TYPE_PREKEY = "prekey"
        const val TYPE_SIGNED_PREKEY = "signed_prekey"
        const val TYPE_IDENTITY = "identity"
        const val TYPE_IDENTITY_KEYPAIR = "identity_keypair"

        // Helper functions to create composite IDs
        fun sessionId(address: String): String = "$TYPE_SESSION:$address"
        fun preKeyId(keyId: Int): String = "$TYPE_PREKEY:$keyId"
        fun signedPreKeyId(keyId: Int): String = "$TYPE_SIGNED_PREKEY:$keyId"
        fun identityId(address: String): String = "$TYPE_IDENTITY:$address"
        fun identityKeyPairId(): String = TYPE_IDENTITY_KEYPAIR
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SignalKeyEntity

        if (id != other.id) return false
        if (keyType != other.keyType) return false
        if (address != other.address) return false
        if (keyId != other.keyId) return false
        if (!keyData.contentEquals(other.keyData)) return false
        if (timestamp != other.timestamp) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + keyType.hashCode()
        result = 31 * result + (address?.hashCode() ?: 0)
        result = 31 * result + (keyId ?: 0)
        result = 31 * result + keyData.contentHashCode()
        result = 31 * result + timestamp.hashCode()
        return result
    }
}
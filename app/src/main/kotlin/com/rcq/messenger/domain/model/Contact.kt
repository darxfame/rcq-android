package com.rcq.messenger.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Contact model — represents a person in user's contact list.
 * 
 * CRITICAL FIX (0.1):
 * - Primary key is now 'userId' (maps to 'uin' from server)
 * - Added @SerialName annotations for correct JSON deserialization
 * - Removed auto-generated 'id' field to prevent duplicates on sync
 * 
 * Fields synchronized with iOS via GET /contacts endpoint.
 * Cryptographic keys (identityKey, signingKey) will be added in Phase 5 (E2EE).
 */
@Serializable
data class Contact(
    @SerialName("uin")
    val userId: Long,
    
    val nickname: String,
    
    val status: UserStatus = UserStatus.OFFLINE,
    
    @SerialName("avatar_url")
    val avatarUrl: String? = null,
    
    @SerialName("last_seen")
    val lastSeen: Long = 0L,
    
    @SerialName("blocked")
    val isBlocked: Boolean = false,
    
    // Future: E2EE cryptographic keys (Phase 5)
    // val identityKey: String? = null,
    // val signingKey: String? = null,
    // val signalIdentityKey: String? = null,
) {
    fun toEntity(): ContactEntity = ContactEntity(
        userId = userId,
        nickname = nickname,
        status = status,
        avatarUrl = avatarUrl,
        lastSeen = lastSeen,
        isBlocked = isBlocked
    )
}

/**
 * Room database entity for Contact.
 * 
 * CRITICAL FIX (0.1):
 * - Changed primary key from auto-generated 'id' to 'userId' (UIN)
 * - This fixes duplicate contacts on sync with server
 * - Uses REPLACE strategy on insertAll() to handle updates
 */
@androidx.room.Entity(tableName = "contacts")
data class ContactEntity(
    @androidx.room.PrimaryKey
    val userId: Long,
    
    val nickname: String,
    val status: UserStatus = UserStatus.OFFLINE,
    val avatarUrl: String? = null,
    val lastSeen: Long = 0L,
    val isBlocked: Boolean = false
) {
    fun toDomain(): Contact = Contact(
        userId = userId,
        nickname = nickname,
        status = status,
        avatarUrl = avatarUrl,
        lastSeen = lastSeen,
        isBlocked = isBlocked
    )
}

package com.rcq.messenger.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey
    val id: String,
    val chatId: String,
    val senderId: Long,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val status: String = "SENT",
    val replyToId: String? = null,
    val editedAt: Long? = null,
    // E2EE fields
    val ciphertext: String? = null,
    val signalType: Int = 1,
    val isEncrypted: Boolean = false,
    // Phase 1 fields
    val isFromMe: Boolean = false,
    val kind: String = "TEXT",
    val mediaId: String? = null,
    val receivedWhileAway: Boolean = false,
    val deletedForEveryone: Boolean = false,
    val reactions: String? = null,
    val thumbnailB64: String? = null,
    val durationSec: Double = 0.0,
    val ttlSeconds: Int? = null,
    val forwardedFromName: String? = null,
    val replyToContent: String? = null,
    val replyToAuthorName: String? = null,
    val premiumPriceTokens: Int? = null,
    val premiumUnlocked: Boolean = false,
    val albumId: String? = null,
    val fileName: String? = null,
    val fileMime: String? = null,
    val fileSizeBytes: Long? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val pollId: String? = null,
    val mentionedUserIds: String? = null
)

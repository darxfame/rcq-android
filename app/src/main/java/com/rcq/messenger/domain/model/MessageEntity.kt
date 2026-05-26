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
    val type: String = "text", // "text", "image", "file", "audio", "video"
    val timestamp: Long = System.currentTimeMillis(),
    val status: String = "sending", // "sending", "sent", "delivered", "read"
    val replyToId: String? = null,
    val attachmentUrl: String? = null,
    val attachmentType: String? = null,
    val attachmentSize: Long? = null,
    val isEdited: Boolean = false,
    val editedAt: Long? = null,
    // E2EE fields
    val ciphertext: String? = null,
    val signalType: Int = 1, // Signal Protocol message type
    val isEncrypted: Boolean = false
)
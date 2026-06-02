package com.rcq.messenger.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pending_outbox")
data class PendingOutboxEntity(
    @PrimaryKey val localId: String,
    val chatId: String,
    val recipientUin: Long,
    val isGroup: Boolean = false,
    val plainContent: String,
    val messageKind: String = "TEXT",
    val retryCount: Int = 0,
    val maxRetries: Int = 5,
    val createdAt: Long = System.currentTimeMillis()
)

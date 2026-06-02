package com.rcq.messenger.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "calls")
data class CallEntity(
    @PrimaryKey
    val id: String,
    val type: String, // "audio", "video"
    val status: String, // "incoming", "outgoing", "missed", "declined", "ended"
    val participantIds: List<Long>,
    val initiatorId: Long,
    val startTime: Long = System.currentTimeMillis(),
    val endTime: Long? = null,
    val duration: Long = 0L, // in milliseconds
    val isGroupCall: Boolean = false
)
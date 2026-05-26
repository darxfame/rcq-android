package com.rcq.messenger.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chats")
data class ChatEntity(
    @PrimaryKey
    val id: String,
    val type: String, // "direct", "group"
    val name: String? = null,
    val avatarUrl: String? = null,
    val lastMessage: String? = null,
    val lastMessageTime: Long = 0L,
    val unreadCount: Int = 0,
    val isArchived: Boolean = false,
    val isMuted: Boolean = false,
    val participantIds: List<Long> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
package com.rcq.messenger.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "groups")
data class GroupEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val description: String? = null,
    val avatarUrl: String? = null,
    val creatorId: Long,
    val memberIds: List<Long> = emptyList(),
    val adminIds: List<Long> = emptyList(),
    val isPublic: Boolean = false,
    val inviteLink: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val pinnedText: String? = null
)

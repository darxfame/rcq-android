package com.rcq.messenger.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "stories")
data class StoryEntity(
    @PrimaryKey
    val id: String,
    val userId: Long,
    val type: String = "image", // "image", "video", "text"
    val content: String? = null,
    val mediaUrl: String? = null,
    val duration: Long = 24 * 60 * 60 * 1000L, // 24 hours in milliseconds
    val viewerIds: List<Long> = emptyList(),
    val isHighlighted: Boolean = false,
    val timestamp: Long = System.currentTimeMillis(),
    val expiresAt: Long = System.currentTimeMillis() + 24 * 60 * 60 * 1000L
)

@Entity(tableName = "story_items")
data class StoryItemEntity(
    @PrimaryKey
    val id: String,
    val storyId: String,
    val type: String = "image", // "image", "video", "text"
    val content: String? = null,
    val mediaUrl: String? = null,
    val duration: Long = 5000L, // 5 seconds default
    val timestamp: Long = System.currentTimeMillis()
)
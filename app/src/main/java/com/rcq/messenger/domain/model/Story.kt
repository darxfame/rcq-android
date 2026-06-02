package com.rcq.messenger.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Story(
    val id: String,
    val userId: Long,
    val nickname: String,
    val avatarUrl: String? = null,
    val items: List<StoryItem> = emptyList(),
    val viewerCount: Int = 0,
    val createdAt: Long,
    val expiresAt: Long,
    val isActive: Boolean = true
)

@Serializable
data class StoryItem(
    val id: String,
    val type: StoryItemType,
    val mediaUrl: String,
    val thumbnailUrl: String? = null,
    val caption: String? = null,
    val backgroundColor: String? = null,
    val duration: Int = 5000,
    val views: List<StoryView> = emptyList(),
    val reactions: List<Reaction> = emptyList(),
    val replies: List<StoryReply> = emptyList(),
    val createdAt: Long
)

@Serializable
enum class StoryItemType {
    IMAGE,
    VIDEO,
    TEXT
}

@Serializable
data class StoryView(
    val userId: Long,
    val viewedAt: Long
)

@Serializable
data class StoryReply(
    val id: String,
    val userId: Long,
    val nickname: String,
    val content: String,
    val timestamp: Long
)
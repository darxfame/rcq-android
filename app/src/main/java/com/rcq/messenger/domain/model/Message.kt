package com.rcq.messenger.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Message(
    val id: String,
    val chatId: String,
    @SerialName("senderUIN")
    val senderId: Long,
    @SerialName("isFromMe")
    val isFromMe: Boolean = false,
    @SerialName("kind")
    val kind: MessageKind = MessageKind.TEXT,
    @SerialName("text")
    val content: String = "",
    @SerialName("mediaID")
    val mediaId: String? = null,
    @SerialName("sentAt")
    val timestamp: Long = 0,
    @SerialName("deliveryState")
    val status: MessageStatus = MessageStatus.SENT,
    val receivedWhileAway: Boolean = false,
    val deletedForEveryone: Boolean = false,
    val reactions: Map<Long, String> = emptyMap(),
    val thumbnailB64: String? = null,
    val durationSec: Double = 0.0,
    val ttlSeconds: Int? = null,
    val forwardedFromName: String? = null,
    @SerialName("replyToID")
    val replyToId: String? = null,
    @SerialName("replyToSnippet")
    val replyToContent: String? = null,
    val replyToAuthorName: String? = null,
    val editedAt: Long? = null,
    val premiumPriceTokens: Int? = null,
    val premiumUnlocked: Boolean = false,
    @SerialName("albumID")
    val albumId: String? = null,
    val fileName: String? = null,
    val fileMime: String? = null,
    val fileSizeBytes: Long? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    @SerialName("pollID")
    val pollId: String? = null,
    val mentionedUserIds: List<Long> = emptyList()
) : java.io.Serializable

@Serializable
enum class MessageKind {
    @SerialName("text")
    TEXT,
    @SerialName("typing")
    TYPING,
    @SerialName("offline")
    OFFLINE,
    @SerialName("photo")
    PHOTO,
    @SerialName("video")
    VIDEO,
    @SerialName("voice")
    VOICE,
    @SerialName("file")
    FILE,
    @SerialName("location")
    LOCATION,
    @SerialName("systemNotice")
    SYSTEM_NOTICE,
    @SerialName("deleteForEveryone")
    DELETE_FOR_EVERYONE,
    @SerialName("premiumPhoto")
    PREMIUM_PHOTO,
    @SerialName("premiumVideo")
    PREMIUM_VIDEO,
    @SerialName("poll")
    POLL
}

@Serializable
enum class MessageStatus {
    SENDING,
    SENT,
    DELIVERED,
    READ,
    FAILED
}

@Serializable
data class Reaction(
    val userId: Long,
    val emoji: String,
    val timestamp: Long
)

@Serializable
data class Chat(
    val id: String,
    val targetId: Long,
    val targetNickname: String,
    val targetAvatar: String? = null,
    val lastMessage: Message? = null,
    val unreadCount: Int = 0,
    val isPinned: Boolean = false,
    val isMuted: Boolean = false,
    val isArchived: Boolean = false,
    val createdAt: Long,
    val updatedAt: Long,
    val typingUsers: List<Long> = emptyList()
) : java.io.Serializable

@Serializable
enum class ChatType {
    PRIVATE,
    GROUP,
    CHANNEL
}

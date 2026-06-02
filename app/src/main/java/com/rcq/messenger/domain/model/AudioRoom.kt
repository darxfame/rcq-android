package com.rcq.messenger.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class AudioRoom(
    val id: String,
    val chatId: String? = null,
    val title: String,
    val hostId: Long,
    val hostNickname: String,
    val speakers: List<RoomSpeaker> = emptyList(),
    val listeners: List<RoomListener> = emptyList(),
    val speakerCount: Int = 0,
    val listenerCount: Int = 0,
    val maxSpeakers: Int = 10,
    val maxListeners: Int = 100,
    val isPublic: Boolean = true,
    val createdAt: Long,
    val endedAt: Long? = null
)

@Serializable
data class RoomSpeaker(
    val userId: Long,
    val nickname: String,
    val avatarUrl: String? = null,
    val isMuted: Boolean = false,
    val isSpeaking: Boolean = false,
    val raisedHand: Boolean = false,
    val joinedAt: Long
)

@Serializable
data class RoomListener(
    val userId: Long,
    val nickname: String,
    val avatarUrl: String? = null,
    val raisedHand: Boolean = false,
    val joinedAt: Long
)
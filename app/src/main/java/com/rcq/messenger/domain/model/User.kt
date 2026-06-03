package com.rcq.messenger.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class User(
    @kotlinx.serialization.SerialName("uin") val id: Long,
    val nickname: String = "",
    @kotlinx.serialization.SerialName("avatar_url") val avatarUrl: String? = null,
    val status: UserStatus = UserStatus.OFFLINE,
    @kotlinx.serialization.SerialName("last_seen") val lastSeen: String? = null,
    @kotlinx.serialization.SerialName("status_message") val statusMessage: String? = null,
    val bio: String = "",
    @kotlinx.serialization.SerialName("blocked") val isBlocked: Boolean = false,
    val isFavorite: Boolean = false,
    val notificationSound: String? = null,
    val customNickname: String? = null,
    val tokens: Long = 0,
    @kotlinx.serialization.SerialName("is_premium") val isPremium: Boolean = false,
    @kotlinx.serialization.SerialName("identity_key") val identityKey: String? = null,
    @kotlinx.serialization.SerialName("signing_key") val signingKey: String? = null,
    @kotlinx.serialization.SerialName("signal_identity_key") val signalIdentityKey: String? = null
)

@Serializable
enum class UserStatus {
    @kotlinx.serialization.SerialName("online")
    ONLINE,
    @kotlinx.serialization.SerialName("away")
    AWAY,
    @kotlinx.serialization.SerialName("busy")
    BUSY,
    @kotlinx.serialization.SerialName("dnd")
    DND,
    @kotlinx.serialization.SerialName("invisible")
    INVISIBLE,
    @kotlinx.serialization.SerialName("offline")
    OFFLINE
}

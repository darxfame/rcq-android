package com.rcq.messenger.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class User(
    @kotlinx.serialization.SerialName("uin") val id: Long,
    val nickname: String = "",
    @kotlinx.serialization.SerialName("avatar_url") val avatarUrl: String? = null,
    val status: UserStatus = UserStatus.OFFLINE,
    @kotlinx.serialization.SerialName("last_seen") val lastSeen: String? = null,
    val bio: String = "",
    @kotlinx.serialization.SerialName("blocked") val isBlocked: Boolean = false,
    val isFavorite: Boolean = false,
    val notificationSound: String? = null,
    val customNickname: String? = null,
    @kotlinx.serialization.SerialName("equipped_pet") val equippedPet: EquippedPet? = null,
    val tokens: Long = 0,
    @kotlinx.serialization.SerialName("is_premium") val isPremium: Boolean = false
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
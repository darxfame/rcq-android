package com.rcq.messenger.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Contact(
    @SerialName("uin")
    val userId: Long,
    val nickname: String,
    @SerialName("avatar_url")
    val avatarUrl: String? = null,
    val status: UserStatus = UserStatus.OFFLINE,
    @SerialName("last_seen")
    val lastSeen: String? = null,
    @SerialName("blocked")
    val isBlocked: Boolean = false,
    val isFavorite: Boolean = false,
    val notificationSound: String? = null,
    val customNickname: String? = null,
    val chatId: String? = null,
    val unreadCount: Int = 0,
    val lastMessage: Message? = null
) {
    val id: Long get() = userId
}

@Serializable
data class ContactList(
    val contacts: List<Contact>,
    val pendingRequests: List<Contact>,
    val blockedUsers: List<Contact>
)
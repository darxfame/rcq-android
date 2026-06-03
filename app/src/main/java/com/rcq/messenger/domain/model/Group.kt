package com.rcq.messenger.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Group(
    val id: String,
    val name: String,
    val avatarUrl: String? = null,
    val description: String = "",
    val ownerId: Long,
    val adminIds: List<Long> = emptyList(),
    val memberIds: List<Long> = emptyList(),
    val memberCount: Int = 0,
    val createdAt: Long,
    val settings: GroupSettings = GroupSettings(),
    val lastMessage: Message? = null,
    val unreadCount: Int = 0,
    val isPinned: Boolean = false,
    val isMuted: Boolean = false,
    val pinnedText: String? = null
)

@Serializable
data class GroupSettings(
    val anyoneCanSend: Boolean = true,
    val adminsCanSend: Boolean = true,
    val ownerCanSend: Boolean = true,
    val membersCanChangeInfo: Boolean = false,
    val membersCanInvite: Boolean = true,
    val membersCanAddReactions: Boolean = true,
    val membersCanReply: Boolean = true,
    val preapprovedMembers: List<Long> = emptyList(),
    val bannedMembers: List<Long> = emptyList()
)

@Serializable
data class GroupMember(
    val userId: Long,
    val nickname: String,
    val avatarUrl: String? = null,
    val role: MemberRole = MemberRole.MEMBER,
    val joinedAt: Long,
    val isMuted: Boolean = false
)

@Serializable
enum class MemberRole {
    OWNER,
    ADMIN,
    MEMBER,
    GUEST
}

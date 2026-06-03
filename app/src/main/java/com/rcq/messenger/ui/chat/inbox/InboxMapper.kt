package com.rcq.messenger.ui.chat.inbox

import com.rcq.messenger.domain.model.Chat
import com.rcq.messenger.domain.model.Contact
import com.rcq.messenger.domain.model.Group
import com.rcq.messenger.domain.model.MessageKind
import com.rcq.messenger.domain.model.UserStatus
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class InboxMapper {

    fun buildState(
        chats: List<Chat>,
        contacts: List<Contact>,
        groups: List<Group>,
        isLoading: Boolean,
        hasLoadedOnce: Boolean,
        searchQuery: String = "",
        error: String? = null,
    ): InboxUiState {
        val rows = buildRows(chats = chats, contacts = contacts, groups = groups)
        return InboxUiState(
            rows = rows,
            searchQuery = searchQuery,
            searchResults = search(rows, searchQuery),
            isLoading = isLoading,
            hasLoadedOnce = hasLoadedOnce,
            error = error
        )
    }

    fun buildRows(
        chats: List<Chat>,
        contacts: List<Contact>,
        groups: List<Group>,
    ): List<InboxRow> {
        val groupIds = groups.map { it.id }.toSet()
        val contactStatusByUserId = contacts.associate { it.userId to it.status }
        val chatRows = chats
            .filterNot { it.isArchived }
            .sortedWith(compareByDescending<Chat> { it.isPinned }.thenByDescending { it.updatedAt })
            .map { it.toInboxRow(groupIds, contactStatusByUserId) }

        val representedContactIds = chats.mapNotNull { chat ->
            chat.targetId.takeIf { !groupIds.contains(chat.id) }
        }.toSet()
        val representedGroupIds = chatRows.mapNotNull { row ->
            (row.target as? InboxTarget.Group)?.groupId
        }.toSet()

        val groupRows = groups
            .filterNot { representedGroupIds.contains(it.id) }
            .sortedBy { it.name.lowercase() }
            .map { it.toInboxRow() }

        val contactRows = contacts
            .filterNot { representedContactIds.contains(it.userId) }
            .sortedBy { (it.customNickname ?: it.nickname).lowercase() }
            .map { it.toInboxRow() }

        return chatRows + groupRows + contactRows
    }

    fun search(rows: List<InboxRow>, query: String): InboxSearchResults {
        val normalized = query.trim().lowercase()
        if (normalized.isEmpty()) return InboxSearchResults()

        val matches = rows.filter { row -> row.searchableText.contains(normalized) }
        return InboxSearchResults(
            chats = matches.filter { it.target is InboxTarget.Chat },
            contacts = matches.filter { it.target is InboxTarget.Contact },
            groups = matches.filter { it.target is InboxTarget.Group },
        )
    }

    private fun Chat.toInboxRow(
        groupIds: Set<String>,
        contactStatusByUserId: Map<Long, UserStatus>
    ): InboxRow {
        val target = when {
            groupIds.contains(id) -> InboxTarget.Group(id)
            id.startsWith(GROUP_CHAT_PREFIX) -> InboxTarget.Group(id.removePrefix(GROUP_CHAT_PREFIX))
            else -> InboxTarget.Chat(id)
        }
        val preview = lastMessage?.previewText() ?: "No messages yet"
        return InboxRow(
            id = "chat:$id",
            title = targetNickname,
            subtitle = preview,
            preview = preview,
            timestamp = formatInboxTime(lastMessage?.timestamp ?: updatedAt),
            unreadCount = unreadCount,
            status = if (target is InboxTarget.Chat) contactStatusByUserId[targetId] else null,
            isMuted = isMuted,
            isPinned = isPinned,
            avatarUrl = targetAvatar,
            target = target,
            previewKind = lastMessage?.kind
        )
    }

    private fun Contact.toInboxRow(): InboxRow {
        val displayName = customNickname ?: nickname
        val preview = statusMessage?.takeIf { it.isNotBlank() } ?: userId.toString()
        return InboxRow(
            id = "contact:$userId",
            title = displayName,
            subtitle = preview,
            preview = preview,
            timestamp = lastMessage?.timestamp?.let(::formatInboxTime),
            unreadCount = unreadCount,
            status = status,
            isMuted = false,
            isPinned = isFavorite,
            avatarUrl = avatarUrl,
            target = InboxTarget.Contact(userId),
            previewKind = lastMessage?.kind
        )
    }

    private fun Group.toInboxRow(): InboxRow = InboxRow(
        id = "group:$id",
        title = name,
        subtitle = if (memberCount > 0) "$memberCount members" else description,
        preview = if (memberCount > 0) "$memberCount members" else description,
        timestamp = formatInboxTime(lastMessage?.timestamp ?: createdAt),
        unreadCount = unreadCount,
        status = null,
        isMuted = isMuted,
        isPinned = isPinned,
        avatarUrl = avatarUrl,
        target = InboxTarget.Group(id),
        previewKind = lastMessage?.kind
    )

    private fun com.rcq.messenger.domain.model.Message.previewText(): String =
        content.takeIf { it.isNotBlank() } ?: when (kind) {
            MessageKind.PHOTO, MessageKind.PREMIUM_PHOTO -> "Photo"
            MessageKind.VIDEO, MessageKind.PREMIUM_VIDEO -> "Video"
            MessageKind.VOICE -> "Voice message"
            MessageKind.FILE -> fileName ?: "File"
            MessageKind.LOCATION -> "Location"
            MessageKind.POLL -> "Poll"
            MessageKind.SYSTEM_NOTICE -> content
            else -> ""
        }

    private fun formatInboxTime(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp
        return when {
            diff < 60_000 -> "Now"
            diff < 3_600_000 -> "${diff / 60_000}m"
            diff < 86_400_000 -> SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))
            diff < 604_800_000 -> SimpleDateFormat("EEE", Locale.getDefault()).format(Date(timestamp))
            else -> SimpleDateFormat("dd/MM", Locale.getDefault()).format(Date(timestamp))
        }
    }

    private companion object {
        const val GROUP_CHAT_PREFIX = "group:"
    }
}

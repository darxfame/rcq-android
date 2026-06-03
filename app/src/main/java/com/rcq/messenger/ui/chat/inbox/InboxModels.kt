package com.rcq.messenger.ui.chat.inbox

import com.rcq.messenger.domain.model.MessageKind

data class InboxUiState(
    val rows: List<InboxRow> = emptyList(),
    val searchQuery: String = "",
    val searchResults: InboxSearchResults = InboxSearchResults(),
    val isLoading: Boolean = false,
    val hasLoadedOnce: Boolean = false,
    val error: String? = null,
) {
    val showEmptyState: Boolean
        get() = hasLoadedOnce && !isLoading && rows.isEmpty()
}

data class InboxSearchResults(
    val chats: List<InboxRow> = emptyList(),
    val contacts: List<InboxRow> = emptyList(),
    val groups: List<InboxRow> = emptyList(),
) {
    val isEmpty: Boolean get() = chats.isEmpty() && contacts.isEmpty() && groups.isEmpty()
}

data class InboxRow(
    val id: String,
    val title: String,
    val subtitle: String,
    val timestamp: Long?,
    val unreadCount: Int,
    val isMuted: Boolean,
    val isPinned: Boolean,
    val avatarUrl: String?,
    val target: InboxTarget,
    val previewKind: MessageKind? = null,
) {
    val searchableText: String = listOf(id, title, subtitle)
        .joinToString(separator = " ")
        .lowercase()
}

sealed interface InboxTarget {
    data class Chat(val chatId: String) : InboxTarget
    data class Contact(val userId: Long) : InboxTarget
    data class Group(val groupId: String) : InboxTarget
}

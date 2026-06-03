package com.rcq.messenger.core

import com.rcq.messenger.domain.model.Chat
import com.rcq.messenger.domain.model.Contact
import com.rcq.messenger.domain.model.Group
import com.rcq.messenger.domain.model.Message
import com.rcq.messenger.ui.chat.inbox.InboxMapper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InboxMapperTest {

    @Test
    fun `groups appear in inbox even when they have no messages`() {
        val state = InboxMapper().buildState(
            chats = emptyList(),
            contacts = emptyList(),
            groups = listOf(group(id = "21", name = "RCQ Beta")),
            isLoading = false,
            hasLoadedOnce = true
        )

        assertEquals(listOf("RCQ Beta"), state.rows.map { it.title })
        assertFalse(state.showEmptyState)
    }

    @Test
    fun `dev contact appears in inbox even when it has no messages`() {
        val state = InboxMapper().buildState(
            chats = emptyList(),
            contacts = listOf(contact(userId = DEV_UIN, nickname = ".Dev")),
            groups = emptyList(),
            isLoading = false,
            hasLoadedOnce = true
        )

        assertEquals(listOf(".Dev"), state.rows.map { it.title })
        assertFalse(state.showEmptyState)
    }

    @Test
    fun `search finds matching chats contacts and groups`() {
        val mapper = InboxMapper()
        val rows = mapper.buildRows(
            chats = listOf(chat(id = "peer:42", targetId = 42L, title = "Alice")),
            contacts = listOf(contact(userId = 77L, nickname = "Bob")),
            groups = listOf(group(id = "21", name = "RCQ Beta")),
        )

        val chatResults = mapper.search(rows, "ali")
        assertEquals(listOf("Alice"), chatResults.chats.map { it.title })

        val contactResults = mapper.search(rows, "77")
        assertEquals(listOf("Bob"), contactResults.contacts.map { it.title })

        val groupResults = mapper.search(rows, "beta")
        assertEquals(listOf("RCQ Beta"), groupResults.groups.map { it.title })
    }

    @Test
    fun `existing group chat replaces starter group row`() {
        val rows = InboxMapper().buildRows(
            chats = listOf(chat(id = "21", targetId = 1L, title = "RCQ Beta")),
            contacts = emptyList(),
            groups = listOf(group(id = "21", name = "RCQ Beta")),
        )

        assertEquals(listOf("RCQ Beta"), rows.map { it.title })
    }

    @Test
    fun `existing direct chat replaces starter contact row`() {
        val rows = InboxMapper().buildRows(
            chats = listOf(chat(id = "direct_77", targetId = 77L, title = "Bob")),
            contacts = listOf(contact(userId = 77L, nickname = "Bob")),
            groups = emptyList(),
        )

        assertEquals(listOf("Bob"), rows.map { it.title })
    }

    @Test
    fun `empty state is hidden until first load finishes`() {
        val loadingState = InboxMapper().buildState(
            chats = emptyList(),
            contacts = emptyList(),
            groups = emptyList(),
            isLoading = true,
            hasLoadedOnce = false
        )

        val loadedState = InboxMapper().buildState(
            chats = emptyList(),
            contacts = emptyList(),
            groups = emptyList(),
            isLoading = false,
            hasLoadedOnce = true
        )

        assertFalse(loadingState.showEmptyState)
        assertTrue(loadedState.showEmptyState)
    }

    private fun chat(
        id: String,
        targetId: Long,
        title: String,
        timestamp: Long = 100L,
    ) = Chat(
        id = id,
        targetId = targetId,
        targetNickname = title,
        lastMessage = Message(
            id = "message-$id",
            chatId = id,
            senderId = targetId,
            content = "Last message from $title",
            timestamp = timestamp
        ),
        createdAt = timestamp,
        updatedAt = timestamp
    )

    private fun contact(userId: Long, nickname: String) = Contact(
        userId = userId,
        nickname = nickname
    )

    private fun group(id: String, name: String) = Group(
        id = id,
        name = name,
        ownerId = 1L,
        memberCount = 12,
        createdAt = 100L
    )

    private companion object {
        const val DEV_UIN = 1_000_000_001L
    }
}

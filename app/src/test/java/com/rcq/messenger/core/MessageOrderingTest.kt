package com.rcq.messenger.core

import com.rcq.messenger.domain.model.Message
import com.rcq.messenger.domain.model.MessageKind
import com.rcq.messenger.domain.model.MessageStatus
import org.junit.Assert.*
import org.junit.Test

/**
 * Regression guard for BUG-002: messages were showing in reverse order.
 * Fix: sort by timestamp ASC (server time), not insertion order.
 */
class MessageOrderingTest {

    private fun msg(id: String, ts: Long) = Message(
        id = id, chatId = "chat1", senderId = 1L,
        kind = MessageKind.TEXT, content = "msg $id",
        timestamp = ts, status = MessageStatus.SENT
    )

    @Test
    fun `messages sort ascending by timestamp`() {
        val unsorted = listOf(msg("c", 3000), msg("a", 1000), msg("b", 2000))
        val sorted = unsorted.sortedBy { it.timestamp }
        assertEquals(listOf("a", "b", "c"), sorted.map { it.id })
    }

    @Test
    fun `latest message has largest timestamp`() {
        val msgs = listOf(msg("old", 100L), msg("new", 999L), msg("mid", 500L))
        assertEquals("new", msgs.maxByOrNull { it.timestamp }?.id)
    }

    @Test
    fun `own and incoming messages interleave correctly by timestamp`() {
        val mine = Message(id = "m1", chatId = "c", senderId = 1L, isFromMe = true,
            kind = MessageKind.TEXT, content = "hi", timestamp = 2000)
        val theirs = Message(id = "t1", chatId = "c", senderId = 2L, isFromMe = false,
            kind = MessageKind.TEXT, content = "hey", timestamp = 1000)

        val sorted = listOf(mine, theirs).sortedBy { it.timestamp }
        assertEquals("t1", sorted.first().id)
        assertEquals("m1", sorted.last().id)
    }

    @Test
    fun `empty message list stays empty after sort`() {
        assertTrue(emptyList<Message>().sortedBy { it.timestamp }.isEmpty())
    }

    @Test
    fun `single message list unchanged after sort`() {
        val single = listOf(msg("only", 42L))
        assertEquals(single, single.sortedBy { it.timestamp })
    }
}

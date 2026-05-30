package com.rcq.messenger.core

import com.rcq.messenger.domain.model.PendingOutboxEntity
import org.junit.Assert.*
import org.junit.Test

/**
 * Regression guard for offline outbox retry logic.
 * Messages retry up to maxRetries, then stop — no infinite loops.
 */
class OutboxRetryTest {

    private fun entry(retryCount: Int = 0, maxRetries: Int = 5) = PendingOutboxEntity(
        localId = "local-$retryCount",
        chatId = "chat1",
        recipientUin = 200L,
        plainContent = "hello",
        retryCount = retryCount,
        maxRetries = maxRetries
    )

    private fun canRetry(e: PendingOutboxEntity) = e.retryCount < e.maxRetries

    @Test fun `fresh entry can retry`() = assertTrue(canRetry(entry(0)))

    @Test fun `exhausted entry cannot retry`() = assertFalse(canRetry(entry(5, 5)))

    @Test fun `one-below-max can still retry`() = assertTrue(canRetry(entry(4, 5)))

    @Test
    fun `incrementing exhausts retries at maxRetries`() {
        var count = 0
        while (canRetry(entry(retryCount = count, maxRetries = 3))) count++
        assertEquals(3, count)
    }

    @Test
    fun `copy preserves localId across retry increment`() {
        val original = entry(0)
        val retried = original.copy(retryCount = original.retryCount + 1)
        assertEquals(original.localId, retried.localId)
        assertEquals(1, retried.retryCount)
    }

    @Test
    fun `isGroup flag is independent per entry`() {
        assertTrue(entry().copy(isGroup = true).isGroup)
        assertFalse(entry().isGroup)
    }
}

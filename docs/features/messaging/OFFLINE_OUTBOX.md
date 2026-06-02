# Offline Outbox — Design

**Status: NOT IMPLEMENTED — Phase 0 task**

## Problem

Messages sent while WS disconnected are lost. App kill mid-send = message lost.

## Solution

`pending_outbox` Room table + `OutboxProcessor` singleton.

```
Entity fields:
  localId: String (PK, UUID = client_msg_id for server dedup)
  chatId: String
  targetUin: Long
  isGroup: Boolean
  plaintextJson: String
  retryCount: Int (default 0)
  maxRetries: Int (default 5)
  createdAt: Long
  nextRetryAt: Long
  status: String (PENDING | SENDING | FAILED)
```

## Flow

```
WS reconnect event
  → Fetch PENDING from outbox (ORDER BY createdAt ASC)
  → For each: encrypt → send → on success DELETE; on fail increment retry
  → If retryCount >= maxRetries → status = FAILED
```

## Idempotency

Server deduplicates by `client_msg_id` = `localId`. Safe to retry.

## Ordering

Sequential per-chat (FIFO), parallel across chats.

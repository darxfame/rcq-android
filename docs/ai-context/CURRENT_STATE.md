# Current State — 2026-05-29

> Updated after each significant change.

## Build

- **Debug:** ✅ Passing
- **DB version:** 12 (pets table removed in Phase 0)
- **Tests:** ⚠️ Minimal

## Working ✅

- Registration + JWT auth + ECIES key generation
- ECIES v=1 iOS-compatible encryption/decryption
- Signal Protocol sessions (Double Ratchet, TOFU)
- Push notifications (FCM + deep links)
- Contact sync from server
- Pending contact requests (WS + pull)
- Direct message send (`/messages/sealed`)
- Group creation
- Chat list (Room DB)
- Bottom nav: Chats / Contacts / Settings

## Broken 🔴

| Bug ID | Symptom | Root Cause |
|---|---|---|
| BUG-001 | WS 500 on connect | Auth header mismatch? |
| BUG-002 | Messages in wrong order | System.currentTimeMillis() vs serverTime |
| BUG-003 | Groups show non-member groups | No membership JOIN in query |
| BUG-004 | Incoming DMs missing | Blocked by BUG-001 (no WS) |

## Deleted in Phase 0 🗑️

- `WebSocketManager.kt`, `WebSocketEvent.kt` — duplicate WS engine
- `GameRepository.kt`, `GamesScreen.kt`, `MarketplaceScreen.kt`
- `Game.kt`, `Marketplace.kt`, `PetEntity.kt`, `PetDao.kt`
- DB: pets table (migration 11→12)

## Missing — Not Started ❌

Offline outbox, typing indicators, presence, reactions, editing,
deletion, search, drafts, replies (wired), forwards, pins, archive,
mute, crash reporting, CI/CD, unit tests, screenshot tests.

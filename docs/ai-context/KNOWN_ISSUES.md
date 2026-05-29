# Known Issues

## Active

### BUG-001: WebSocket 500 on connect
- **Severity:** Critical
- **Symptom:** WS fails with HTTP 500 on `/ws/{uin}?token=...`
- **Hypothesis:** Auth header format mismatch with server expectations
- **Diagnose:** `adb logcat | grep "WebSocket\|OkHttp"` compare with iOS WS header
- **File:** `data/websocket/WebSocketService.kt`
- **Found:** 2026-05-29

### BUG-002: Message ordering reversed
- **Severity:** Critical
- **Root cause:** `System.currentTimeMillis()` used for sort, not `serverTime` from envelope
- **Fix:** `MessageDao` ORDER BY `server_time`, not `timestamp`
- **Files:** `data/db/MessageDao.kt`, `data/db/MessageEntity`
- **Found:** 2026-05-22

### BUG-003: Groups show non-member groups
- **Severity:** High (privacy)
- **Root cause:** No membership filter in GroupDao query
- **Fix:** JOIN groups with group_members WHERE user_uin = ownUin
- **Files:** `data/db/GroupDao.kt`
- **Found:** 2026-05-22

### BUG-004: Incoming DMs not displayed
- **Severity:** High
- **Root cause:** Blocked by BUG-001 (no WS = no realtime delivery)
- **Found:** 2026-05-29

## Resolved

| ID | Description | Fixed | Commit |
|---|---|---|---|
| - | ECIES v=1 iOS incompatibility | 2026-05-22 | ca88c42 |
| - | Signal session init race | 2026-05-22 | e7c4476 |
| - | Groups JSON wrapper | 2026-05-22 | fe008cc |
| - | Duplicate WS engines | 2026-05-29 | Phase 0 |

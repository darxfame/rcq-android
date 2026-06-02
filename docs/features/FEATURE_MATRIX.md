# Feature Matrix

## Tier 1 — Critical Core

| Feature | iOS | Android | Notes |
|---|---|---|---|
| Registration + auth | ✅ | ✅ | ECIES keys on register |
| ECIES E2EE | ✅ | ✅ | v=1, iOS-compatible |
| Signal Protocol | ✅ | ✅ | Double Ratchet |
| WebSocket connection | ✅ | 🔴 | BUG-001: 500 error |
| Send direct message | ✅ | ⚠️ | Works, ordering broken |
| Receive direct message | ✅ | 🔴 | Blocked by WS bug |
| Message ordering | ✅ | 🔴 | BUG-002 |
| Local DB persistence | ✅ | ✅ | Room v12 |
| Offline send queue | ✅ | 🔴 | Not implemented |
| Group messaging | ✅ | ⚠️ | Filter bug BUG-003 |
| Unread counters | ✅ | ⚠️ | Partial |
| Push notifications | ✅ | ✅ | FCM + deep links |
| WS reconnect/backoff | ✅ | ⚠️ | Partial |
| Delivery states | ✅ | ⚠️ | Partial |
| Contact sync | ✅ | ✅ | |

## Tier 2 — Communication

| Feature | iOS | Android | Notes |
|---|---|---|---|
| Photo messages | ✅ | ⚠️ | MediaService exists |
| Voice messages | ✅ | ⚠️ | VoiceRecorder exists |
| Message reactions | ✅ | ❌ | |
| Message editing | ✅ | ❌ | |
| Message deletion | ✅ | ❌ | |
| Typing indicator | ✅ | ❌ | WsEvent exists, no UI |
| Presence/status | ✅ | ❌ | |
| Search | ✅ | ❌ | |
| Reply to message | ✅ | ⚠️ | UI exists, not wired |
| Forward message | ✅ | ❌ | |
| Pinned messages | ✅ | ❌ | |
| Archive chats | ✅ | ❌ | |
| Mute per chat | ✅ | ❌ | |

## Tier 3 — Advanced

| Feature | iOS | Android | Notes |
|---|---|---|---|
| Voice calls | ✅ | ⚠️ | Infra exists, unstable |
| Video calls | ✅ | ❌ | |
| Stories | ✅ | ⚠️ | UI exists |

## Out of Scope

| Feature | Decision |
|---|---|
| Games | ❌ DELETED — not in contract |
| Marketplace | ❌ DELETED |
| Pets | ❌ DELETED |

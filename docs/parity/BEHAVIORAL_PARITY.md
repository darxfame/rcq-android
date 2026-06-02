# Behavioral Parity

## Critical Behaviors

| Behavior | iOS | Android | Status |
|---|---|---|---|
| Send msg offline → delivered on reconnect | ✅ | 🔴 | Phase 0 |
| Receive bg msg via FCM | ✅ | ✅ | |
| WS disconnect mid-send → queued | ✅ | 🔴 | Phase 0 |
| Message ordering (serverTime) | ✅ | 🔴 | BUG-002 |
| Group membership filter | ✅ | 🔴 | BUG-003 |
| ECIES cross-platform decrypt | ✅ | ✅ | Verified |
| Typing events | ✅ | ❌ | Phase 2 |
| Presence | ✅ | ❌ | Phase 2 |

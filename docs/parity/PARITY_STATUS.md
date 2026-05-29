# iOS ↔ Android Parity Status

**Updated:** 2026-05-29

## Protocol Parity (Non-negotiable) ✅

| Protocol | Status |
|---|---|
| ECIES v=1 wire format | ✅ Verified cross-device |
| Signal Protocol Double Ratchet | ✅ Verified |
| Message envelope structure | ✅ Verified |
| Auth flow (keys + UIN) | ✅ Verified |

## Behavioral Parity

| Behavior | iOS | Android | Gap |
|---|---|---|---|
| Message ordering (serverTime) | ✅ | 🔴 | BUG-002 |
| Offline send + retry | ✅ | 🔴 | Not impl |
| WS reconnect on network change | ✅ | ⚠️ | Backoff partial |
| Typing events | ✅ | ❌ | Phase 2 |
| Presence | ✅ | ❌ | Phase 2 |
| Group filter (membership) | ✅ | 🔴 | BUG-003 |

## Visual Parity

| Element | Match | Notes |
|---|---|---|
| Chat bubble colors | ~70% | Colors close, radius off |
| Delivery checkmarks | ~50% | Logic partial |
| Avatar + online dot | ~90% | Good |
| Typing indicator | 0% | Not implemented |
| Chat list swipe | 0% | Not implemented |
| Context menu | 0% | Not implemented |

# Next Steps

> Immediate tasks. Updated at end of each session.

## Phase 0 — Active

### Done ✅
- [x] Delete WebSocketManager + WebSocketEvent, migrate CallManager to WsEvent
- [x] Delete Games/Marketplace/Pets code and DB entities
- [x] Create docs structure
- [x] Write AI context files

### This Session
- [ ] FIX BUG-001: WS 500 — diagnose auth header vs iOS ref
- [ ] FIX BUG-002: Message ordering — use serverTime
- [ ] FIX BUG-003: Group filter — JOIN with membership

### This Week
- [ ] Add `pending_outbox` Room table + OutboxProcessor
- [ ] Add staging build variant
- [ ] Add Timber logging

### Next Week
- [ ] MVI refactor: ChatsViewModel + ChatViewModel
- [ ] Design tokens: SpacingTokens, ColorTokens, TypographyTokens
- [ ] GitHub Actions CI

## Priority Rule

1. Fix 🔴 Critical bugs first (BUG-001 → BUG-002 → BUG-003)
2. Then offline outbox (P0 UX)
3. Then infrastructure (CI, logging, variants)

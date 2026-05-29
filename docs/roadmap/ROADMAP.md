# Roadmap

## Strategy: Incremental Stabilization

- Big Bang: ❌ (existing crypto/E2EE code is valuable, don't break)
- Incremental: ✅ stabilize → foundation → features → polish

## Phase 0 — Emergency Stabilization (Week 1-2)

Fix critical bugs blocking basic messaging.

| Task | Status |
|---|---|
| Delete duplicate WS + dead code | ✅ Done |
| Fix WS 500 error (BUG-001) | 🔴 Pending |
| Fix message ordering (BUG-002) | 🔴 Pending |
| Fix group membership filter (BUG-003) | 🔴 Pending |
| Implement offline outbox queue | 🔴 Pending |

**Exit:** WS connects, messages ordered correctly, groups filtered, offline messages delivered.

## Phase 1 — Architecture Foundation (Week 3-4)

Establish patterns preventing future tech debt.

- MVI/UDF refactor (ChatsViewModel, ChatViewModel)
- Design tokens (SpacingTokens, ColorTokens, TypographyTokens)
- Timber logging
- Build variants (staging/production)
- GitHub Actions CI
- Crash reporting

**Exit:** New features built without accruing debt.

## Phase 2 — Tier 1 Feature Completion (Week 5-8)

All critical messaging features.

- Delivery states complete (PENDING→SENT→DELIVERED→READ)
- Typing indicators (send + receive)
- Presence/online status
- Unread counters accurate
- Offline send + retry

**Exit:** Feature parity with iOS for Tier 1.

## Phase 3 — Tier 2 Communication (Week 9-12)

- Reactions, editing, deletion
- Media pipeline complete
- Search (FTS4)
- Replies, forwards, pins, archive, mute

## Phase 4 — Polish + Parity (Week 13+)

- Visual parity with iOS
- Animations + transitions
- Voice/video calls stable
- Accessibility
- Screenshot parity tests

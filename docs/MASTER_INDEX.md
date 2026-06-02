# RCQ Android — Master Index

**Version:** 1.0 | **Updated:** 2026-05-29 | **Branch:** phase-1-core-messaging

## Quick Status

| Area | Status | Owner |
|---|---|---|
| Build | ✅ Passing | - |
| WebSocket | 🔴 500 error bug | Android dev |
| Message ordering | 🔴 Broken (local time) | Android dev |
| E2EE (ECIES/Signal) | ✅ iOS-compatible | - |
| Offline outbox | 🔴 Missing | Android dev |
| Push notifications | ✅ Working | - |
| Groups filter | 🔴 Shows all groups | Android dev |

## AI — Read in This Order

1. [AI Context](ai-context/AI_CONTEXT.md) — project state, conventions, what NOT to do
2. [Current State](ai-context/CURRENT_STATE.md) — what works, what is broken right now
3. [Next Steps](ai-context/NEXT_STEPS.md) — immediate action items
4. [Known Issues](ai-context/KNOWN_ISSUES.md) — active bug list
5. [Decisions Log](ai-context/DECISIONS_LOG.md) — why certain decisions were made

## Architecture

- [Architecture Overview](architecture/ARCHITECTURE_OVERVIEW.md)
- [Data Flow](architecture/DATA_FLOW.md)
- [WebSocket Protocol](architecture/WEBSOCKET_PROTOCOL.md)
- [Crypto Spec](architecture/CRYPTO_SPEC.md)
- [Sync Engine](architecture/SYNC_ENGINE.md)
- [Modularization](architecture/MODULARIZATION.md)

## ADRs (Architecture Decision Records)

- [ADR-001: Single Module](adr/ADR-001-single-module.md)
- [ADR-002: ECIES v1 iOS Compat](adr/ADR-002-ecies-v1-ios-compat.md)
- [ADR-003: Unified WS Engine](adr/ADR-003-unified-ws-engine.md)
- [ADR-004: Room DB](adr/ADR-004-room-database.md)

## Roadmap

- [Roadmap Overview](roadmap/ROADMAP.md)
- [Phase 0 Status — Emergency Stabilization](roadmap/PHASE_0_STATUS.md)
- [Phase 1 Status — Architecture Foundation](roadmap/PHASE_1_STATUS.md)
- [Phase 2 Status — Feature Completion](roadmap/PHASE_2_STATUS.md)

## Features

- [Feature Matrix](features/FEATURE_MATRIX.md)
- [Offline Outbox Design](features/messaging/OFFLINE_OUTBOX.md)
- [Delivery States](features/messaging/DELIVERY_STATES.md)
- [Group Messaging](features/messaging/GROUP_MESSAGING.md)
- [Account Recovery](features/auth/ACCOUNT_RECOVERY.md)
- [Signal + ECIES](features/crypto/SIGNAL_ECIES.md)

## Parity

- [Parity Status](parity/PARITY_STATUS.md)
- [Visual Parity](parity/VISUAL_PARITY.md)
- [Behavioral Parity](parity/BEHAVIORAL_PARITY.md)

## Migration

- [Migration Progress](migration/MIGRATION_PROGRESS.md)
- [iOS Reference Mapping](migration/IOS_REFERENCE_MAPPING.md)

## Tech Debt

- [Tech Debt Register](tech-debt/TECH_DEBT_REGISTER.md)

## QA

- [Test Matrix](qa/TEST_MATRIX.md)
- [Cross-Platform Tests](qa/CROSS_PLATFORM_TESTS.md)

## Release

- [Release Readiness](release/RELEASE_READINESS.md)

## Observability

- [Logging Guide](observability/LOGGING_GUIDE.md)

## Runbooks

- [WS Reconnect Debug](runbooks/WS_RECONNECT_DEBUG.md)
- [Crypto Session Reset](runbooks/CRYPTO_SESSION_RESET.md)
- [Release Process](runbooks/RELEASE_PROCESS.md)

# AI Context — RCQ Android

> Read this FIRST at the start of every session.

## Project Identity

- **App:** RCQ Messenger — privacy-first, 9-digit UIN, no phone/email, E2EE by default
- **Branch:** `phase-1-core-messaging`
- **Stack:** Kotlin + Jetpack Compose + Hilt + Room + libsignal
- **iOS ref:** `reference/ios/` (gitignored, available locally) — behavioral source of truth
- **API spec:** `docs/RCQ_API_SPEC.md` — backend source of truth

## What NOT to Implement (Contract)

```
NEVER implement:
  - Games (Crash, HiLo, Limbo)
  - Marketplace
  - Pets
  - Any feature absent from reference/ios/

Existing code for these = dead code → DELETE immediately.
```

## Current Phase: Phase 0 — Emergency Stabilization

Fixing critical bugs before new features. See CURRENT_STATE.md.

## Critical Files — Do Not Break

| File | Why Critical |
|---|---|
| `crypto/EciesCrypto.kt` | iOS-compatible ECIES v=1 — breaking = lost messages |
| `crypto/EciesKeyStore.kt` | Key generation must match iOS format |
| `crypto/CryptoService.kt` | Encryption facade for all messages |
| `crypto/SessionManager.kt` | Signal Protocol sessions — stateful |
| `data/db/RCQDatabase.kt` | DB migrations — wrong migration = data loss |
| `data/websocket/WebSocketService.kt` | ONLY WS implementation |

## Architecture Conventions

**WebSocket:** Single source = `WebSocketService.kt`. Event model = `WsEvent` sealed class. `WebSocketManager.kt` DELETED.

**Message ordering:** ALWAYS use `serverTime` from envelope. Never `System.currentTimeMillis()`.

**State management:** MVI/UDF — sealed Intent/State/Effect. No direct `_flow.value =` outside VM.

**DB changes:** Always add `MIGRATION_N_M` — no `fallbackToDestructiveMigration`.

## Git Conventions

```bash
# Commit messages: Russian
# After every commit push immediately:
git push origin phase-1-core-messaging
```

## Build

```bash
export ANDROID_HOME=/opt/android-sdk
./gradlew assembleDebug
./gradlew kspDebugKotlin  # faster: check KSP/annotations only
```

## Session Handoff Checklist

At end of session, update:
1. `docs/ai-context/CURRENT_STATE.md`
2. `docs/ai-context/KNOWN_ISSUES.md`
3. `docs/ai-context/NEXT_STEPS.md`

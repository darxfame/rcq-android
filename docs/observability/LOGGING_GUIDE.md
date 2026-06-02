# Logging Guide

## Current: android.util.Log → Target: Timber

## Tags

| Tag | Domain |
|---|---|
| `RCQ_WS` | WebSocket events, connection state |
| `RCQ_CRYPTO` | ECIES, Signal encrypt/decrypt |
| `RCQ_MSG` | Message send/receive/store |
| `RCQ_SYNC` | Sync engine, outbox drain |
| `RCQ_AUTH` | Auth, token, key generation |
| `RCQ_DB` | Room queries, migrations |
| `RCQ_NOTIF` | Push notifications |

## Rules

- NEVER log: JWT tokens, private keys, message plaintext, UIN combinations
- ALWAYS log: operation type, target UIN (anonymized if needed), success/failure
- ERROR logs include: throwable type, operation, context (never key material)

## Timber Setup (Phase 1)

```kotlin
// RCQApplication.onCreate():
if (BuildConfig.DEBUG) {
    Timber.plant(Timber.DebugTree())
} else {
    Timber.plant(ProductionTree())  // → Crashlytics/Sentry
}

// Usage:
Timber.tag("RCQ_WS").d("Connected to ws for uin=%d", uin)
Timber.tag("RCQ_CRYPTO").e(e, "Decrypt failed for uin=%d msgId=%s", senderUin, msgId)
```

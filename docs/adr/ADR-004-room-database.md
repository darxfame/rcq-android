# ADR-004: Room as Local Database

**Date:** 2026-05-18 | **Status:** Accepted

## Decision

`androidx.room:room-runtime:2.6.1` with KSP.

## Rationale

Compile-time SQL validation, Kotlin Flow, explicit migrations, KSP = fast builds.

## Schema Version History

| Version | Change |
|---|---|
| 6→7 | E2EE fields (ciphertext, signalType, isEncrypted) |
| 7→8 | signal_keys table |
| 8→9 | Phase 1 message fields, contacts, chats rebuild |
| 9→10 | lastMessage fields, pets recreated |
| 10→11 | identityKey, signingKey to contacts |
| 11→12 | Drop pets table (out of scope) |

**Current version: 12**

## Rules

1. Schema change = new version + explicit migration
2. Never `fallbackToDestructiveMigration` in production
3. Test migration with real data

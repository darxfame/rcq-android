# Decisions Log

> Non-obvious decisions and rationale. Read before architectural changes.

## 2026-05-29: Deleted WebSocketManager + WebSocketEvent

Removed `WebSocketManager.kt` and `WebSocketEvent.kt`. Migrated `CallManager` to `WebSocketService`/`WsEvent`.

**Why:** Two parallel WS implementations created race conditions. `WebSocketService` has typed sealed `WsEvent`; old `WebSocketManager` used untyped `WebSocketEvent(type: String, data: String?)` causing missed event routing.

## 2026-05-29: Removed Games/Marketplace/Pets

Deleted `GameRepository`, `GamesScreen`, `MarketplaceScreen`, all Pet entities/DAOs. DB migration 11→12 drops `pets` table. Stories kept (may be in iOS ref).

**Why:** Per project contract — not in iOS reference client, not in scope.

## 2026-05-22: ECIES v=1 Wire Format

Use ECIES v=1 (BouncyCastle ChaCha20-Poly1305 + Ed25519) for all messages.

**Why:** iOS client requires this exact format. Breaking = messages cannot be decrypted cross-platform.

**Warning:** Library pinned (`bcprov-jdk18on:1.78.1`). Do not upgrade without iOS validation.

## 2026-05-22: BouncyCastle Full Provider

Use `org.bouncycastle:bcprov-jdk18on` instead of Android built-in BC.

**Why:** Android's stripped BC has no ChaCha20. Full provider required for iOS ECIES compat.

## 2026-05-20: Single Module Architecture

All code in `:app`. No Gradle modules yet.

**Why:** Pre-1.0, small team. Modularization overhead not justified until architecture is stable. Plan: `:core:crypto` first after Phase 0.

## 2026-05-18: KSP over KAPT

**Why:** 2x faster annotation processing for Room + Hilt.

## 2026-05-18: minSdk 26

**Why:** `EncryptedSharedPreferences` requires API 23+; 26 gives modern crypto APIs and covers ~95% devices.

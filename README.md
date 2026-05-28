# RCQ Android

Android client for **RCQ** — privacy-first messenger with 9-digit IDs, no phone, no email, end-to-end encrypted by default.

> **Status:** Pre-alpha, in active development
> **Build:** ✅ GREEN (`assembleDebug` passing, DB v9)
> **Current work:** Phase 1 audit + polish → Phase 2 media messaging

---

## Project Status

| Phase | Focus | Status | Date |
|-------|-------|--------|------|
| **0** | Critical bug fixes | ✅ DONE | May 25, 2026 |
| **1** | Core messaging + E2EE | ✅ DONE | May 26, 2026 |
| **Build fix** | 123 compile errors resolved | ✅ DONE | May 28, 2026 |
| **1 audit** | Runtime fixes, WebSocket, UX polish | 🔄 IN PROGRESS | May 28, 2026 |
| **2** | Media messaging (photo/video/file/voice + E2EE) | 📋 NEXT | — |
| **3** | Calls + Audio rooms | 📋 PLANNED | — |
| **4** | Social (Stories, Games, Marketplace) | 📋 PLANNED | — |

**Overall progress:** ~65%

---

## Architecture

**Stack:** Kotlin + Jetpack Compose · Hilt DI · Room DB v9 · libsignal-android 0.33.0 · OkHttp WebSocket · Retrofit · Kotlinx Serialization · WebRTC

```
crypto/     Signal Protocol E2EE (Double Ratchet, X3DH, PersistentSignalProtocolStore)
data/
  api/      Retrofit service + DTOs
  db/       Room v9 (users, contacts, chats, messages, groups, stories, calls, pets, signal_keys)
  ws/       WebSocketManager — exponential backoff, 40+ typed events
  repository/ UserRepository, ChatRepository, GroupRepository, StoryRepository, ...
di/         Hilt AppModule
ui/
  auth/     WelcomeScreen, AccountRecoveryScreen
  chat/     ChatScreen, ChatsScreen + components (MediaMessageBubble, ReplyPreview)
  contacts/ ContactsScreen, AddContactScreen, CreateGroupScreen
  calls/    CallScreen, CallsScreen
  stories/  StoriesScreen, StoryViewerScreen
  profile/  ProfileScreen
  settings/ SettingsScreen
  games/    GamesScreen
  market/   MarketplaceScreen
  audio/    AudioRoomsScreen
```

---

## Database Schema (v9)

| Migration | Change |
|-----------|--------|
| v6→v7 | E2EE fields on messages (ciphertext, signalType, isEncrypted) |
| v7→v8 | signal_keys table for persistent Signal Protocol storage |
| v8→v9 | Phase 1 fields on messages (kind, mediaId, reactions, etc.), profile fields on contacts, new chats schema, pets recreated with game fields |

---

## Build

```bash
export ANDROID_HOME=/opt/android-sdk
./gradlew assembleDebug          # debug APK
./gradlew compileDebugKotlin     # Kotlin only (fast check)
./gradlew kspDebugKotlin         # Room/Hilt annotation processing
./gradlew test                   # unit tests
```

---

## Security

All messages use **Signal Protocol** (Double Ratchet + X3DH) via `libsignal-android:0.33.0`, fully compatible with the iOS RCQ client. Keys are persisted in Room via `PersistentSignalProtocolStore`. Sessions survive app restarts.

Security issues → `security@rcq.app` (disclose responsibly before filing a public issue).

---

## Docs

| File | Content |
|------|---------|
| `docs/RCQ_API_SPEC.md` | Complete API specification |
| `PHASE_1_PLAN.md` | Phase 1 E2EE plan |
| `DIAGNOSIS.md` | Known runtime issues |
| `TODO.md` | Feature checklist |
| `MIGRATION_PLAN.md` | Full roadmap |

---

**License:** GNU AGPL-3.0 · See NOTICE for third-party attributions.

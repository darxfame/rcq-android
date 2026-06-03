# Current State — 2026-06-03

## Build
- **Production Debug:** ✅ Passing via `./gradlew assembleProductionDebug`
- **DB:** v14 · **Branch:** `main`
- CI build fix: Gradle now includes only `app/libs/libbox.aar` and excludes the stale parallel `app.rcq.android` source tree from compilation.

## Working ✅
- Registration + JWT auth + ECIES key generation (iOS-compat)
- Outgoing messages (Android → iPad confirmed working)
- Incoming messages (with 🔒 placeholder if decrypt fails)
- Contact sync, group sync (all user groups shown correctly)
- WebSocket: connect, send, receive, reconnect with backoff
- Dark mode (persisted DataStore, applied at launch)
- JIMM retro mode toggle (status-grouped contacts when ON)
- JIMM/QIP flat UI: compact 50dp NavBar, compact typography, status-dot rows
- Startup connection probe → auto-enables bypass if server unreachable
- Stealth: ProxyManager AUTO/MANUAL/OFF, sing-box persistence across restarts
- PanicPIN / Biometric unlock
- Delivery states SENT/DELIVERED/READ, typing indicators, presence
- Edit/delete/reactions, pin/mute/archive, message search

## Broken / Partial 🔴
| Issue | Root Cause |
|-------|-----------|
| SingBox runtime needs device validation | `libbox.aar` is included; routing behavior still needs online ADB/logcat validation |
| Входящие DM иногда показывают 🔒 | Signal-сессия не установлена для нового собеседника |
| JIMM mode влияет только на контакты | Другие экраны не читают LocalRetroMode |
| P5 Звуки / P7 Смайлы JIMM | Не реализованы (0%) |

## Removed 🗑️
- `WebSocketManager.kt` — дублирующий WS движок
- `GameRepository`, `GamesScreen`, `MarketplaceScreen` — удалены по scope
- `PetEntity`, `PetDao` — pets table (DB migration 11→12)

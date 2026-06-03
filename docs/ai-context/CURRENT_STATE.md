# Current State — 2026-06-03

## Build
- **Production Debug:** ✅ Passing via `./gradlew assembleProductionDebug`
- **DB:** v14 · **Branch:** `main`
- CI build fix: Gradle now includes only `app/libs/libbox.aar`; stale `app.rcq.android` code and `rcqbox.aar` are moved to `reference/android-legacy` instead of being hidden by a custom source-set.

## Working ✅
- Registration + JWT auth + ECIES key generation (iOS-compat)
- Outgoing messages (Android → iPad confirmed working)
- Incoming messages (with 🔒 placeholder if decrypt fails)
- Contact sync, group sync (all user groups shown correctly)
- Chat list sync: client-only `/chats` is not called; chat rows are derived from Room rows and `/messages/queue`
- WebSocket: connect, send, receive, reconnect with backoff; Android WS URL builds `wss://` safely after OkHttp `HttpUrl` construction
- Connection diagnostics: client-only screens are reported locally, relay probes are capped, and OkHttp debug logging is BASIC to avoid large-body stalls
- Dark mode (persisted DataStore, applied at launch)
- JIMM retro mode toggle (status-grouped contacts when ON)
- JIMM/QIP flat UI: compact 50dp NavBar, compact typography, status-dot rows
- Startup connection probe mirrors iOS: direct `GET /health` before auto-bypass, clears stale auto sing-box when direct works
- Stealth: ProxyManager AUTO/MANUAL/OFF, sing-box persistence across restarts
- Embedded bypass is dual-engine on Android: sing-box remains for compatible relays; Xray-core executable handles VLESS Reality xHTTP. Device validation: `XrayTransport` started `relay-usa-amd-xhttp` on `127.0.0.1:1089`, `/groups` returned 200 and `GroupRepository` logged `RCQ Beta`.
- PanicPIN / Biometric unlock
- Delivery states SENT/DELIVERED/READ, typing indicators, presence
- Edit/delete/reactions, pin/mute/archive, message search

## Broken / Partial 🔴
| Issue | Root Cause |
|-------|-----------|
| UI still needs visual group-list verification | Backend sync now returns `RCQ Beta`; verify contacts/groups screen renders it after Xray/direct sync |
| Входящие DM иногда показывают 🔒 | Signal-сессия не установлена для нового собеседника |
| JIMM mode влияет только на контакты | Другие экраны не читают LocalRetroMode |
| P5 Звуки / P7 Смайлы JIMM | Не реализованы (0%) |

## Removed 🗑️
- `WebSocketManager.kt` — дублирующий WS движок
- `GameRepository`, `GamesScreen`, `MarketplaceScreen` — удалены по scope
- `PetEntity`, `PetDao` — pets table (DB migration 11→12)

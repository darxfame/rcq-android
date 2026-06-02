# Current State — 2026-05-31

## Build
- **Debug:** ✅ Passing · **DB:** v14 · **Branch:** `phase-1-core-messaging`

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
| SingBox не маршрутизирует трафик | Заглушка — бинарник отсутствует в APK |
| Входящие DM иногда показывают 🔒 | Signal-сессия не установлена для нового собеседника |
| JIMM mode влияет только на контакты | Другие экраны не читают LocalRetroMode |
| P5 Звуки / P7 Смайлы JIMM | Не реализованы (0%) |

## Removed 🗑️
- `WebSocketManager.kt` — дублирующий WS движок
- `GameRepository`, `GamesScreen`, `MarketplaceScreen` — удалены по scope
- `PetEntity`, `PetDao` — pets table (DB migration 11→12)

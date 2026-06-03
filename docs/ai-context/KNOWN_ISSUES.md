# Known Issues

> Updated: 2026-06-03

## Active

### BUG-015: Call UI still needs real SDP offer/answer wiring
- **Severity:** High
- **Symptom:** Android call signaling payloads now match the server contract, but live WebRTC calls are not device-validated.
- **Current state:** `WsEvent`, parser, outgoing payload builders, and `CallManager` send paths use WS-only call events. `CallManager` currently sends empty SDP strings because service binding/offer-answer creation is not coordinated in this fix-plan pass.
- **Fix:** Wire `CallManager` to `CallService.createOffer/createAnswer`, set remote descriptions from incoming offer/answer, send ICE through `sendCallIce`, and validate Android↔iOS calls on device.
- **File:** `call/CallManager.kt`, `service/CallService.kt`, `data/websocket/WebSocketService.kt`

### BUG-011: Main inbox UI still needs broad visual device validation
- **Severity:** High
- **Symptom:** user reported empty contacts/chats and missing mandatory `RCQ Beta`.
- **Current state:** fixed for mandatory starter rows. 2026-06-04 ADB validation confirmed Chats UI renders `RCQ Beta` and `.Dev`; Room contains `groups: 21|RCQ Beta` and only `.Dev` contact `911` (old `84048` absent). `/groups` still times out on the live route, so `GroupRepository` seeds mandatory local `RCQ Beta` before network sync and lets any successful server response overwrite it by id.
- **Fix:** device-verify remaining grouped search results, chat unread badges/dividers/status overlays, emoji picker, attachment bottom sheet, relay picker, custom VLESS flow, and group detail/navigation flows.
- **File:** `ui/chat/ChatsScreen.kt`, `ui/chat/ChatScreen.kt`, `ui/chat/inbox/InboxMapper.kt`, `ui/settings/ConnectionSettingsSheet.kt`, `ui/settings/StealthSettingsScreen.kt`

### BUG-017: New iOS-parity chat/group UI needs device validation
- **Severity:** Medium
- **Symptom:** Code-level parity controls are implemented but not visually validated on a device.
- **Current state:** Chat More menu, reaction picker, forward picker, in-chat search, pinned group banner, GroupInfoScreen, group join, audio-room join tap, Settings status picker, polished chat header, delivery ticks, reaction chips, and Miranda contact sections compile.
- **Fix:** Run emulator/device QA for chat menus/dialogs, bubble layout/ticks, contact sections, group info member actions, group browse join, room join, and status updates against live/staging backend.
- **File:** `ui/chat/ChatScreen.kt`, `ui/chat/ChatsViewModel.kt`, `ui/contacts/GroupInfoScreen.kt`, `ui/contacts/GroupBrowseScreen.kt`, `ui/settings/SettingsScreen.kt`

### BUG-012: Add Contact flow needs live device validation
- **Severity:** Medium
- **Symptom:** Add Contact code-level backend parity is fixed, but live validation cannot reach the Add request while transport is routed through a broken sing-box relay.
- **Current state:** Android now matches iOS for `POST /contacts/request` with `{ "to_uin": ... }`, refreshes contacts after success or duplicate, and maps HTTP 409 to a duplicate-contact error. 2026-06-03 ADB validation opened Add Contact and verified the empty/error UI path, but logs showed HTTPS/WS failures through `hysteria2[relay-do-fra-yandex-hy2]`: `SOCKS server general failure`, `operation not permitted`, `connection closed`.
- **Fix:** first force/verify direct transport or a working relay, then re-run Add Contact success, duplicate 409, empty search, and backend error states.
- **File:** `ui/contacts/AddContactViewModel.kt`, `data/repository/ChatRepository.kt`, `data/api/RCQApiService.kt`

### BUG-006: Входящие сообщения иногда не расшифровываются
- **Severity:** Medium
- **Symptom:** Показывается "🔒 Зашифрованное сообщение" вместо текста
- **Root cause:** Signal-сессия не установлена для нового собеседника, или ECIES ключ не загружен
- **Diagnose:** `adb logcat | grep "decryptWrapped\|ChatRepository"`
- **File:** `crypto/CryptoService.kt:decryptWrapped`

### Endpoint audit (2026-05-31, проверено против live `api.rcq.app`)
- **DNS:** `api.rcq.app` → 165.232.69.229 ✓ · `api.staging.rcq.app` → **нет записи** (staging-флавор не подключится)
- **Реальные эндпоинты (401=есть, нужна авт.):** `/health` 200, `/contacts`, `/contacts/pending`, `/messages/queue`, `/groups`, `/groups/browse?q=`, `/users/search?q=` — все ✓
- **Исправлено:** `getCurrentUser()` использовал `GET /users/me` → 405; правильный путь `GET /users/me/info` (по спеке)
- **Client-only (нет на сервере, не баг):** `/chats`, `/settings`, `/rooms` → 404 — деривируются локально из Room/queue; Android больше не вызывает `/chats` при загрузке списка чатов, а диагностика не делает сетевые проверки этих routes

---

## Resolved

| ID | Описание | Дата | Коммит |
|----|----------|------|--------|
| BUG-018 | iOS-parity UI plan blocks 1-6: ChatScreen actions, ViewModel state, GroupInfo screen, group route, group join, status picker | 2026-06-03 | uncommitted |
| BUG-016 | Backend WS/API parity plan blocks 1-9: call/room/group WsEvent fields and parsers, outgoing payloads, dead call REST removal, audio-room WS enter/leave, group preview/join/delete, GroupUpdated sync | 2026-06-03 | uncommitted |
| BUG-014 | Входящие control-envelope события (`read`, `reaction`, `delete`, `bounce`, `system`, `edit`, `visit`) обрабатывались как обычные сообщения | 2026-06-03 | uncommitted |
| BUG-013 | Отправка сообщений: статус после отправки не учитывал `queued`/`delivered` и возможен NPE при пустом ответе | 2026-06-03 | uncommitted |
| BUG-010 | Chats screen stayed in loading/error path because `syncChats()` called absent `/chats`; diagnostics also called client-only routes and stalled on large BODY logs. Fixed by syncing queue only, reporting client-only checks locally, capping relay probes, and using BASIC HTTP logging | 2026-06-03 | uncommitted |
| BUG-005 | Embedded xHTTP bypass did not work because sing-box cannot parse `transport: xhttp`. Added process-based Xray-core engine for VLESS Reality xHTTP and kept sing-box for compatible relays; device log confirms `XrayTransport` starts `relay-usa-amd-xhttp` | 2026-06-03 | uncommitted |
| BUG-009 | Crash after registration: `HttpUrl.Builder.scheme("wss")`; fixed by building HTTPS URL first, then converting string to `wss://` | 2026-06-03 | uncommitted |
| BUG-008 | Startup probe used `HEAD /health`, got 405, falsely enabled stale sing-box; now uses direct `GET /health` and clears stale auto bypass | 2026-06-03 | uncommitted |
| BUG-007 | statusMessage поле в ContactEntity + Migration 13→14 | 2026-05-31 | `2ec2980` |
| BUG-004 | Входящие DM тихо дропались (TOFU hard-drop) | 2026-05-31 | `0333e30` |
| BUG-003 | Группы не показывались (memberIds фильтр) | 2026-05-31 | `084a836` |
| — | Navbar скрывался под системными кнопками | 2026-05-31 | `084a836` |
| — | Dark mode не сохранялся (in-memory StateFlow) | 2026-05-31 | `0333e30` |
| — | HTTP timeout 60s → вечный лоадер | 2026-05-31 | `0333e30` |
| — | JIMM toggle не имел визуального эффекта | 2026-05-31 | `0333e30` |
| BUG-002 | Порядок сообщений обратный | 2026-05-29 | `03f9d86` |
| BUG-001 | WS 500 при подключении | 2026-05-29 | Phase 0 |
| — | ECIES v=1 iOS несовместимость | 2026-05-22 | `ca88c42` |
| — | Дублирующийся WS движок | 2026-05-29 | Phase 0 |

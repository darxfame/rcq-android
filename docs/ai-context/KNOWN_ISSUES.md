# Known Issues

> Updated: 2026-06-03

## Active

### BUG-011: Group/contact list UI still needs visual validation
- **Severity:** High
- **Symptom:** user reported “No contacts yet” and missing `RCQ Beta`.
- **Current state:** device log after Xray embedded bypass shows `/groups` HTTP 200 and `GroupRepository: syncGroups: got 1 groups: [21/RCQ Beta]`; `/contacts` and `/contacts/pending` return 200 without ViewModel timeout.
- **Fix:** verify Compose contacts/groups UI state and filters now that backend sync succeeds; ensure `.Dev` fallback/contact row and `RCQ Beta` render.
- **File:** `ui/contacts/ContactsScreen.kt`, `data/repository/ChatRepository.kt`

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

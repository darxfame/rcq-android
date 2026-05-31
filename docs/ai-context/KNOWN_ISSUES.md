# Known Issues

> Updated: 2026-05-31

## Active

### BUG-005: SingBox не маршрутизирует трафик
- **Severity:** High (stealth mode non-functional)
- **Symptom:** `BypassMode.AUTO` включается, но трафик всё равно идёт напрямую
- **Root cause:** `SingBoxTransport.start()` — заглушка, бинарник не включён в APK
- **Fix:** Добавить sing-box binary в `jniLibs/`, реализовать `SingBoxTransport`
- **File:** `service/SingBoxTransport.kt`

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
- **Client-only (нет на сервере, не баг):** `/chats`, `/settings`, `/rooms` → 404 — деривируются локально из Room/queue; диагностика больше не маркирует 401 как сбой

---

## Resolved

| ID | Описание | Дата | Коммит |
|----|----------|------|--------|
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

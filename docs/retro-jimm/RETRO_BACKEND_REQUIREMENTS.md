# JIMM 2026 — Backend Requirements

---

## Матрица настроек: Local vs Backend

| Настройка | Local | Backend | Sync | Примечание |
|-----------|:-----:|:-------:|:----:|-----------|
| Тема (light/dark/amoled) | ✅ | — | — | DataStore |
| Retro Mode on/off | ✅ | — | — | DataStore |
| Скин (theme.json) | ✅ | — | — | Файл |
| Размер шрифта | ✅ | — | — | DataStore |
| Компактный вид | ✅ | — | — | DataStore |
| Звуки on/off | ✅ | — | — | DataStore |
| Громкость звуков | ✅ | — | — | DataStore |
| Вибрация | ✅ | — | — | DataStore |
| Уведомления on/off | ✅ | — | — | DataStore |
| Превью сообщений | ✅ | — | — | DataStore |
| Статус (Online/Away/…) | — | ✅ | Push | WS event |
| Статус-сообщение | — | ✅ | Push | `/users/status` |
| Last seen visible | ✅ | ✅ | Sync | Приватность |
| Read receipts | ✅ | ✅ | Sync | Влияет на сервер |
| Online visible (invisible) | ✅ | ✅ | Sync | Invisible mode |
| Пользовательский статус | — | ✅ | Push | Новый API |
| Эмотиконы-паки | ✅ | 🔶 | Optional | CDN |
| Синхронизация темы | 🔶 | 🔶 | Optional | Phase 8+ |

---

## Функции, требующие Backend изменений

### 1. Расширенные Presence-статусы (Приоритет: 🔴 Высокий)

**Текущее:** Только Online/Offline.  
**Нужно:** Away, DND, Invisible, Free for Chat.

**New WS events:**
```json
{ "type": "set_presence", "status": "away", "status_message": "ушёл обедать" }
{ "type": "presence_changed", "uin": 123456789, "status": "away", "message": "ушёл обедать" }
```

**New API:**
```
PUT /users/me/presence
{ "status": "away", "message": "ушёл обедать", "expires_at": null }
```

**Migration impact:** Низкий — новый endpoint, не ломает существующих клиентов.

---

### 2. Статус-сообщение (Приоритет: 🔴 Высокий)

**Текущее:** Отсутствует.  
**Нужно:** Короткий текст под ником (до 100 символов).

**New API:**
```
PUT /users/me/status-message  { "text": "занята, отвечу позже" }
GET /users/{uin}/profile  →  добавить поле "status_message"
```

**Room Migration:** 13→14, `ALTER TABLE contacts ADD COLUMN statusMessage TEXT`.

---

### 3. Invisible Mode (Приоритет: 🟡 Средний)

**Логика:** клиент подключён к WS (получает сообщения), но сервер шлёт другим `presence_offline`.

```
PUT /users/me/presence { "status": "invisible" }
```

**Complexity:** Средний — серверная логика фильтрации presence.

---

### 4. Last Seen (Приоритет: 🟡 Средний)

```
GET /users/{uin}/last-seen  →  { "last_seen": "2026-05-30T14:35:00Z", "visible": true }
PUT /users/me/privacy  →  { "last_seen_visible_to": "everyone|contacts|nobody" }
```

---

### 5. Custom Status + Emoji (Приоритет: 🟢 Низкий)

```
PUT /users/me/custom-status
{ "emoji": "🎮", "text": "играю в игры", "expires_at": "2026-05-31T20:00:00Z" }
```

---

### 6. Push Notifications — расширение

Добавить в FCM payload `sound_id` для воспроизведения нужного звука:

```json
{
  "data": {
    "type": "msg_incoming",
    "sound_id": "msg_incoming",
    "from_uin": "123456789"
  }
}
```

---

### 7. Themes CDN (Приоритет: 🟢 Низкий, Phase 8+)

```
GET  /themes           — список тем
GET  /themes/{id}/dl   — скачать theme.json + preview.png
POST /themes           — опубликовать тему
```

---

## Сводная таблица приоритетов

| Приоритет | Функция | Сложность |
|-----------|---------|-----------|
| 🔴 | Статус-сообщение | Низкая |
| 🔴 | Away/DND presence | Средняя |
| 🟡 | Invisible mode | Средняя |
| 🟡 | Last seen | Низкая |
| 🟢 | Custom emoji status | Низкая |
| 🟢 | Themes CDN | Средняя |

# JIMM 2026 — Component Specifications

---

## Contact List Row — `RetroContactRow`

### Анатомия

```
┌──────────────────────────────────────────────────┐
│ ┌──────┐  Вася Пупкин              ★  [3]  14:35 │
│ │  В   │  🟢 зайди ко мне                        │
│ │  [●] │  123 456 789                            │
│ └──────┘                                          │
└──────────────────────────────────────────────────┘
```

**Слои:**
1. Аватар 42dp круг + статус-дот 9dp (bottom-end, белая обводка 1.5dp)
2. Имя — Roboto Bold 14sp, textPrimary
3. Статус-сообщение — Roboto Italic 12sp, textSecondary
4. UIN — JetBrains Mono 11sp, textMono
5. Звёздочка (favourite) — 16dp, statusAway
6. Счётчик — badge pill, 10sp bold
7. Время последнего сообщения — 11sp, textSecondary

### Состояния

| Состояние | Визуальное изменение |
|-----------|---------------------|
| Default | bgPrimary фон |
| Pressed | bgRowHover + ripple |
| Online | statusDot = statusOnline (#2E7D32) |
| Away | statusDot = statusAway (#F9A825) |
| Busy/DND | statusDot = statusBusy (#D32F2F) |
| Invisible | statusDot = statusInvisible (#6A1B9A) |
| Offline | statusDot = statusOffline (#757575) |
| Unread | badge виден, имя bold + accent underline |

### Compact Mode (настройка)

- rowPadV: 8dp → 5dp
- avatarLg: 42dp → 32dp
- Скрыть UIN строку
- Скрыть статус-сообщение если пусто

---

## Group Header — `RetroGroupHeader`

```
▼  ONLINE  (3)                                  [→]
```

- Высота: 32dp
- Фон: `headerGroup.copy(alpha = 0.08f)`
- Текст: 11sp uppercase, letterSpacing=1.2, цвет по статусу группы
- Sticky: `stickyHeader {}` в LazyColumn

| Группа | Цвет заголовка |
|--------|----------------|
| Online | statusOnline |
| Away/NA | statusAway |
| Busy/DND | statusBusy |
| Invisible | statusInvisible |
| Offline | textSecondary |

---

## Message Bubble — `RetroMessageBubble`

### Параметры

- Радиус: 6dp (JIMM-стиль)
- Хвостик: bottomEnd(self) / bottomStart(other) = 2dp
- Max width: 78% экрана
- Self bg: `bubbleSelf`  Other bg: `bubbleOther`
- Текст: Roboto 15sp, textPrimary (одинаковый для обоих)

### Состояния

| Тип | Вид |
|-----|-----|
| Текст | Бабл + текст |
| Удалено | Italic "Сообщение удалено", strikethrough, secondary |
| Отредактировано | Иконка ✏ перед временем |
| С реакциями | Row emoji badges под баблом |
| Медиа | Thumbnail + caption |

### Статусные метки

```
○ = SENDING    ✓ = SENT    ✓✓ = DELIVERED    ✓✓(accent) = READ    ! = FAILED
```

---

## Status Dot — `RetroStatusDot`

9dp круг с белой обводкой 1.5dp:

```kotlin
@Composable
fun RetroStatusDot(status: UserStatus, size: Dp = 9.dp) {
    Box(
        Modifier.size(size).border(1.5.dp, bgPrimary, CircleShape)
                .clip(CircleShape).background(statusColorFor(status))
    )
}
```

---

## Status Flower (RetroMode) — `RetroStatusFlower`

SVG 5-лепестковый цветок в духе ICQ:
- Размер: 16dp
- Заливка: цвет статуса
- Контур: 20% темнее
- Используется только при `retroMode = true`
- Иначе показывается `RetroStatusDot`

---

## Input Bar — `RetroInputBar`

### Modern (default)

```
[+] [________________________] [😊] [🎤/→]
```

### Retro Mode

```
[_________________________________] [Отпр]
[Смайлы]        [Прикрепить] [Меню]
```

Два ряда имитируют softkey-меню JIMM.

---

## System Message — `RetroSystemMessage`

```kotlin
@Composable
fun RetroSystemMessage(text: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically) {
        HorizontalDivider(Modifier.weight(1f), thickness = 0.5.dp)
        Text(text, style = RetroSystemMessageStyle, modifier = Modifier.padding(horizontal = 8.dp))
        HorizontalDivider(Modifier.weight(1f), thickness = 0.5.dp)
    }
}
// RetroSystemMessageStyle: JetBrains Mono, 11sp, italic, textSecondary, center
```

Примеры:
```
──────── [14:35] Вася Пупкин вошёл в сеть ────────
──────── [14:52] Вася Пупкин вышел из сети ───────
──────── [09:00] Начало диалога ──────────────────
```

---

## Emoticon Picker — `RetroEmoticonPicker`

### Паки
1. **Базовые Unicode** — 40 стандартных emoji
2. **Retro ICQ** — собственные SVG в духе JIMM (see RETRO_ASSETS.md)
3. **Аниме-пак** — дополнительный
4. **Xattab Classic** — pixelart коды

### Text-to-Emoji коды
```
:)  → 😊    :(  → 😢    :D  → 😄    :-P → 😛
;)  → 😉    :O  → 😮    :|  → 😐    >:( → 😡
8)  → 😎    (Y) → 👍    (N) → 👎    <3  → ❤️
```

---

## Context Menu (Long Press)

```
💬 Ответить
✏️  Редактировать    (только свои)
🗑  Удалить          (только свои)
↪  Переслать
👍 Реакция
📋 Копировать
ℹ️  Информация о сообщении
```

---

## Retro Mode Toggle (Settings)

Визуально выделяется в разделе Appearance:

```
┌─────────────────────────────────────────────┐
│ 🌸  Ретро-режим JIMM 2026                   │
│     Классический интерфейс в духе 2006      │
│                                    [  ●  ]  │
└─────────────────────────────────────────────┘
```

При включении (700ms transition):
1. Анимация "CRT flicker" или fade+scale
2. Смена ColorsScheme → RetroColors
3. StatusDot → StatusFlower
4. Включение ретро-звуков
5. Системные сообщения входа/выхода в чате
6. InputBar → двухрядный Retro вид
7. Сохранение в DataStore

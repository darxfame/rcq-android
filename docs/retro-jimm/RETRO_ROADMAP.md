# JIMM 2026 — Implementation Roadmap

| Фаза | Название | Срок | Зависимость |
|------|----------|------|-------------|
| P1 | Foundation | 1 нед | — |
| P2 | Design System | 1 нед | P1 |
| P3 | Contact List | 1 нед | P2 |
| P4 | Chat UI | 1.5 нед | P2, P3 |
| P5 | Sounds | 0.5 нед | P1 |
| P6 | Settings | 1 нед | P2, P5 |
| P7 | Retro Assets | 1.5 нед | P2 |
| P8 | Polish | 1 нед | P3–P7 |

---

## P1 — Foundation

**Задачи:**
- `RetroTheme.kt` — расширение `RCQColorScheme` с retro-полями
- `LocalRetroMode` CompositionLocal (Boolean)
- Feature flag `RETRO_MODE_ENABLED`
- DataStore key `pref_retro_mode_enabled`

**Acceptance:** `LocalRetroMode.current` доступен везде; смена темы без рестарта

---

## P2 — Design System

**Задачи:**
- `RetroLightColors`, `RetroDarkColors`, `RetroAmoledColors`, `RetroHighContrastColors`
- `RetroFontSize`, `RetroMetrics`
- `RetroTheme {}` wrapper Composable
- Snapshot тесты каждой палитры

**Acceptance:** Все 4 темы без крэша; WCAG AA проверен для HighContrast

---

## P3 — Contact List

**Задачи:**
- `RetroContactRow` — compact/comfortable режим, UIN monospace, статус-сообщение
- `RetroGroupHeader` — sticky, коллапс/раскрытие по статусам
- Сортировка: Online → Away → Busy → Invisible → Offline
- Room migration 13→14: `statusMessage TEXT` в `ContactEntity`
- Backend: `status_message` из `/users/{uin}/profile`

**Acceptance:** Группы коллапсируются; статус-дот меняется в реальном времени

**Риск:** Migration 13→14 ломает тесты → добавить `MigrationTest`

---

## P4 — Chat UI

**Задачи:**
- `RetroSystemMessage` (вход/выход из сети)
- `deletedForEveryone` placeholder стиль
- `editedAt` ✏ иконка в timestamp
- Реакции под баблом
- `RetroInputBar` — двухрядный в RetroMode

**Acceptance:** Системные события в чате при RetroMode=ON; ✏ у отредактированных

---

## P5 — Sounds

**Задачи:**
- `RetroSoundManager` — SoundPool wrapper
- 9 OGG файлов в `res/raw/` (see RETRO_ASSETS.md)
- Привязка к WS событиям
- Настройки on/off + громкость

**Acceptance:** Звуки при событиях; отключаются при DND; нет при RetroMode=OFF

---

## P6 — Settings

**Задачи:**
- Retro Mode toggle с анимацией 700ms
- Индивидуальные настройки звуков
- Compact mode для контактов
- Privacy: last seen, read receipts, online visible

**Acceptance:** Все настройки персистентны; privacy sync с сервером

---

## P7 — Retro Assets

**Задачи:**
- 30 SVG смайлов `RCQ Retro Pack` + 16×16 pixelart
- 7 статусных иконок-цветков SVG + `RetroStatusFlower` Canvas
- `RetroEmoticonPicker` с 4 табами
- Text → emoji парсер (`:)` → смайл)
- Логотип Evolution Flower SVG
- 6 встроенных тем (theme.json)

**Acceptance:** Смайлы корректны на всех DPI; `:)` конвертируется перед отправкой

---

## P8 — Polish

**Задачи:**
- CRT flicker или fade анимация при переключении темы
- Haptic feedback на ключевых действиях
- Onboarding "Добро пожаловать в JIMM 2026"
- Accessibility audit (TalkBack, content descriptions)
- Performance: 500+ контактов scroll 60fps

**Acceptance:** TalkBack читает всё; анимация ≤700ms; нет регрессий

---

## Accessibility Parity

| Элемент | Риск | Решение |
|---------|------|---------|
| Ретро-шрифт мелкий | Нечитаемо | Минимум 11sp + scaling |
| Статус-дот 9dp | Мало для tap | Touch target 48dp wrapper |
| Pixelart смайлы | Нечёткость на HiDPI | SVG fallback всегда |
| Низкий контраст | WCAG fail | HighContrast тема |
| Только звук | Глухие пользователи | Vibration альтернатива |

---

## Performance Considerations

| Решение | Плюс | Минус |
|---------|------|-------|
| LazyColumn для контактов | O(1) reuse | Сложный sticky header |
| CompositionLocal для темы | Instant recompose | Глубокий recompose tree |
| SoundPool preloaded | <5ms latency | ~500KB RAM для 9 звуков |
| SVG иконки | Чёткость HiDPI | Сложнее PNG |
| Canvas для цветка | Нет asset файла | CPU-рисование |
| AMOLED тема | -15% батарея | Только на OLED |

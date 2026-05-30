# JIMM 2026 — Progress Tracker

> Обновляется в каждой Claude-сессии.
> Статусы: ⏳ Не начато | 🔄 В процессе | ✅ Готово

---

## P1 — Foundation

| Задача | Статус | Коммит |
|--------|--------|--------|
| `RetroLightColors` / `RetroDarkColors` | ✅ | `64d1319` |
| `RCQColorScheme` + `LocalRCQColors` | ✅ | `64d1319` |
| `RCQMetrics` / `RCQFontSize` | ✅ | `64d1319` |
| `LocalRetroMode` CompositionLocal | ⏳ | — |
| DataStore key `pref_retro_mode_enabled` | ⏳ | — |
| AMOLED тема | ⏳ | — |
| High Contrast тема | ⏳ | — |

## P2 — Design System

| Задача | Статус | Коммит |
|--------|--------|--------|
| Retro palette (full) | 🔄 | `64d1319` (частично) |
| `RetroTheme {}` wrapper | ⏳ | — |
| Snapshot тесты | ⏳ | — |

## P3 — Contact List

| Задача | Статус | Коммит |
|--------|--------|--------|
| `RetroContactRow` (базовый) | 🔄 | `03f9d86` |
| UIN в monospace | ✅ | `03f9d86` |
| Compact mode | ⏳ | — |
| Статус-сообщение под ником | ⏳ | — |
| Sticky group headers по статусу | ⏳ | — |
| Сортировка Online→Away→Offline | ⏳ | — |
| Room migration 13→14 | ⏳ | — |

## P4 — Chat UI

| Задача | Статус | Коммит |
|--------|--------|--------|
| Bubble radius 6dp | ✅ | `745835f` |
| `bubbleSelf`/`bubbleOther` | ✅ | `745835f` |
| `deletedForEveryone` placeholder | ✅ | `ae9311b` |
| `RetroSystemMessage` | ⏳ | — |
| `editedAt` ✏ иконка в timestamp | ⏳ | — |
| Реакции под баблом (UI) | ⏳ | — |
| Retro двухрядный InputBar | ⏳ | — |

## P5 — Sounds

| Задача | Статус |
|--------|--------|
| `RetroSoundManager.kt` | ⏳ |
| 9 OGG файлов | ⏳ |
| Привязка к WS событиям | ⏳ |
| Настройки звуков | ⏳ |

## P6 — Settings

| Задача | Статус | Коммит |
|--------|--------|--------|
| Базовый Settings screen | ✅ | существующий |
| Retro Mode toggle | ⏳ | — |
| Compact mode toggle | ⏳ | — |
| Звуки настройки | ⏳ | — |

## P7 — Retro Assets

| Задача | Статус |
|--------|--------|
| 30 SVG смайлов | ⏳ |
| Pixelart 16×16 паки | ⏳ |
| 7 статусных иконок-цветков SVG | ⏳ |
| `RetroStatusFlower` Canvas | ⏳ |
| `RetroEmoticonPicker` | ⏳ |
| Text→emoji парсер | ⏳ |
| Логотип Evolution Flower | ⏳ |
| 6 тем (theme.json) | ⏳ |

## P8 — Polish

| Задача | Статус |
|--------|--------|
| Анимация переключения тем | ⏳ |
| Haptic feedback | ⏳ |
| Onboarding экран | ⏳ |
| TalkBack audit | ⏳ |
| Перф: 500+ контактов | ⏳ |

## Backend Tasks

| Задача | Статус |
|--------|--------|
| Away/DND presence API | ⏳ |
| Статус-сообщение API | ⏳ |
| Invisible mode | ⏳ |
| Last seen | ⏳ |
| FCM sound_id | ⏳ |

---

## Общий прогресс

```
P1 Foundation:    ████░░░░░░  40%
P2 Design System: ████░░░░░░  35%
P3 Contact List:  ███░░░░░░░  25%
P4 Chat UI:       █████░░░░░  50%
P5 Sounds:        ░░░░░░░░░░   0%
P6 Settings:      ████░░░░░░  40%
P7 Assets:        ░░░░░░░░░░   0%
P8 Polish:        ░░░░░░░░░░   0%
Backend:          ░░░░░░░░░░   0%

TOTAL: ~22%
```

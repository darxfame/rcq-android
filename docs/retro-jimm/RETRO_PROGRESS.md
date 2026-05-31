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
| `LocalRetroMode` CompositionLocal | ✅ | this |
| DataStore key `pref_retro_mode_enabled` | ✅ | this |
| AMOLED тема | ✅ | this |
| High Contrast тема | ✅ | this |

## P2 — Design System

| Задача | Статус | Коммит |
|--------|--------|--------|
| Retro palette (full) | ✅ | this |
| `RetroTheme {}` wrapper | ✅ | this (RCQTheme + retroMode param) |
| `AppPrefsViewModel` (DataStore bridge) | ✅ | this |
| Snapshot тесты | ⏳ | — |

## P3 — Contact List

| Задача | Статус | Коммит |
|--------|--------|--------|
| `RetroContactRow` (базовый) | 🔄 | `03f9d86` |
| UIN в monospace | ✅ | `03f9d86` |
| `StatusGroupedContactList` + sticky headers | ✅ | this |
| `StatusGroupHeader` (коллапс по клику) | ✅ | this |
| Сортировка Online→Away→Busy→Offline | ✅ | this |
| Offline свёрнут по умолчанию | ✅ | this |
| Compact mode | ✅ | existing |
| Статус-сообщение под ником | ✅ | `2ec2980` |
| Room migration 13→14 | ✅ | `2ec2980` |

## P4 — Chat UI

| Задача | Статус | Коммит |
|--------|--------|--------|
| Bubble radius 6dp | ✅ | `745835f` |
| `bubbleSelf`/`bubbleOther` | ✅ | `745835f` |
| `deletedForEveryone` placeholder | ✅ | this |
| `editedAt` ✏ иконка в timestamp | ✅ | this |
| `RetroSystemMessage` | ⏳ | — |
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
| Retro Mode toggle | ✅ | this |
| AMOLED Black toggle | ✅ | this |
| High Contrast toggle | ✅ | this |
| Compact mode toggle | ✅ | existing |
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
P1 Foundation:    ██████████ 100%
P2 Design System: █████████░  90%
P3 Contact List:  █████████░  90%
P4 Chat UI:       ███████░░░  70%
P5 Sounds:        ░░░░░░░░░░   0%
P6 Settings:      ██████████ 100%
P7 Assets:        ░░░░░░░░░░   0%
P8 Polish:        ░░░░░░░░░░   0%
Backend:          ░░░░░░░░░░   0%

TOTAL: ~46%
```

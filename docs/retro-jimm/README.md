# JIMM 2026 — Retro Redesign

> **Для Claude-сессий:** читай этот файл первым при любой работе над retro-дизайном.

## Файлы

| Файл | Содержание |
|------|-----------|
| [RETRO_VISION.md](RETRO_VISION.md) | Исследование JIMM, Design Vision, Color System, Typography |
| [RETRO_COMPONENTS.md](RETRO_COMPONENTS.md) | Спецификации Compose-компонентов |
| [RETRO_ASSETS.md](RETRO_ASSETS.md) | Смайлики, иконки, логотип, звуки, темы |
| [RETRO_BACKEND_REQUIREMENTS.md](RETRO_BACKEND_REQUIREMENTS.md) | Матрица настроек Local/Backend, API-изменения |
| [RETRO_ROADMAP.md](RETRO_ROADMAP.md) | 8 фаз внедрения, acceptance criteria, риски |
| [RETRO_PROGRESS.md](RETRO_PROGRESS.md) | Трекер задач — **обновлять в каждой сессии** |

## Суть

**Вопрос:** "Каким был бы JIMM, если бы развивался 20 лет без остановки?"

**Правило 70/30:** 70% современный Android + 30% ретро-акценты (цветки, звуки, monospace UIN)

**Нельзя:** ломать архитектуру, accessibility, тесты. Копировать защищённые ассеты ICQ.

## Что реализовано (~22%)

- ✅ ICQ-палитра (`Color.kt`): Online, Away, Bubble self/other
- ✅ `RCQColorScheme` + `LocalRCQColors` + `RCQMetrics`
- ✅ Bubble radius 6dp (JIMM-стиль)
- ✅ UIN в monospace (`ContactRow`)
- ✅ `StatusDot` 9dp с белой обводкой
- ✅ Typing indicator
- ✅ Delivery states (SENT/DELIVERED/READ)

## Следующий шаг (P1)

1. `LocalRetroMode` CompositionLocal + DataStore key
2. AMOLED и HighContrast темы
3. `RetroGroupHeader` sticky по статусам

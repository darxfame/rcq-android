# JIMM 2026 — Assets Specification

> Все ассеты — оригинальные. Никаких копий защищённых материалов ICQ/Mail.ru.

---

## Смайлики — "RCQ Retro Pack"

### Базовый набор (30 шт.), SVG 32×32

| Код | Имя | Описание визуала |
|-----|-----|-----------------|
| `:)` | happy | Улыбающийся кружок, зелёный контур |
| `:(` | sad | Грустный, синий контур |
| `:D` | laugh | Широкая улыбка, жёлтый |
| `:-P` | tongue | Язык, оранжевый |
| `;)` | wink | Подмигивающий |
| `:O` | shock | Удивлённый, рот-О |
| `>:(` | angry | Злой, красный |
| `8)` | cool | В солнечных очках |
| `:'(` | cry | Со слезой |
| `:*` | kiss | С поцелуем |
| `<3` | heart | Сердечко красное |
| `(Y)` | thumbsup | Палец вверх |
| `(N)` | thumbsdn | Палец вниз |
| `(!)` | exclaim | Восклицание оранжевое |
| `(?)` | question | Вопрос голубой |
| `*-*` | stars | Звёздные глаза |
| `B)` | nerd | Толстые очки |
| `:|` | neutral | Нейтральный |
| `:s` | confused | Смущённый |
| `>:)` | evil | Хитрый |
| `O:)` | angel | Ангел с нимбом |
| `>:O` | yell | Кричащий |
| `%-}` | dizzy | Закрученные глаза |
| `(Z)` | sleep | Спящий, zzz |
| `(wave)` | wave | Машущий рукой |
| `(clap)` | clap | Аплодисменты |
| `(sun)` | sun | Солнышко |
| `(moon)` | moon | Луна |
| `(flower)` | flower | Цветочек — ICQ tribute |
| `(skull)` | skull | Череп |

### Стиль рисования

- Flat design, простые кружки, без 3D
- Контуры: 2px stroke, на 30% темнее заливки
- Анимация: `animated_*.avd` (bounce/pulse) для selected
- Экспорт: SVG + PNG 32/64/128dp
- Лицензия: MIT/OFL — полностью открытые

### Pixelart Pack (Xattab Classic Mode)

16×16 пиксель-арт версии тех же 30 смайлов.
При `retroMode=true` → pixelart, иначе → SVG.

---

## Иконки — "RCQ Icon System"

### Стиль
- Основа: Material Icons с ICQ-акцентами
- Размеры: 24dp (standard), 18dp (compact)
- Stroke: 2dp outlined

### Статусные иконки (5-лепестковый цветок)

| Файл | Цвет | Описание |
|------|------|----------|
| `ic_status_online.svg` | #2E7D32 | Полный закрашенный цветок |
| `ic_status_away.svg` | #F9A825 | Половина лепестков закрашена |
| `ic_status_busy.svg` | #D32F2F | Цветок с красным крестом |
| `ic_status_dnd.svg` | #B71C1C | Цветок с горизонтальной линией |
| `ic_status_invisible.svg` | #6A1B9A | Контур цветка, прозрачный |
| `ic_status_offline.svg` | #757575 | Серый контур |
| `ic_status_ffc.svg` | #00C853 | Яркий цветок + chat bubble |

### Навигация и чат

- `ic_send` — бумажный самолётик (как в Xattab)
- `ic_attach` — скрепка
- `ic_emoticon` — смайлик с заострённой улыбкой
- `ic_knock_knock` — кулак (отсылка к ICQ knock-knock)
- `ic_history` — часы с загнутой стрелкой

---

## Логотип — "RCQ Identity"

### Концепция: Evolution Flower (рекомендуемая)

5-лепестковый цветок в современном flat стиле:
- Каждый лепесток = цвет одного статуса (зелёный, жёлтый, красный, фиолетовый, серый)
- Текст "RCQ" в Roboto Bold под цветком
- **Не копирует ICQ flower** — разные пропорции и форма лепестков

### Использование

| Контекст | Размер | Вариант |
|----------|--------|---------|
| App icon | 48–512dp | Полный цветок + RCQ |
| Notification | 24dp | Монохромный контур |
| Splash | fullscreen | Анимированное раскрытие лепестков |
| Favicon | 32px | Только цветок |

---

## Звуки — "RCQ Audio Pack"

> Оригинальные звуки. Вдохновлены эпохой, не скопированы.

| Файл | Событие | Длит. | Частота |
|------|---------|-------|---------|
| `msg_incoming.ogg` | Входящее | 0.3s | Часто |
| `msg_sent.ogg` | Отправлено | 0.1s | Очень часто |
| `contact_online.ogg` | Контакт онлайн | 0.5s | Умеренно |
| `contact_offline.ogg` | Контакт оффлайн | 0.4s | Умеренно |
| `msg_delivered.ogg` | Доставлено | 0.2s | Редко |
| `msg_read.ogg` | Прочитано | 0.15s | Редко |
| `send_error.ogg` | Ошибка | 0.3s | Редко |
| `typing_start.ogg` | Начал печатать | 0.05s | Часто |
| `notification.ogg` | Общее уведомление | 0.4s | Умеренно |

### Технические параметры (для синтеза)

**`msg_incoming.ogg`** — синус 880Hz→1320Hz, атака 10ms, спад 250ms, -12dBFS
**`contact_online.ogg`** — два тона 523Hz+659Hz одновременно, восходящий характер, 450ms
**`contact_offline.ogg`** — 659Hz→523Hz нисходящий, 350ms (инверсия online)
**`msg_sent.ogg`** — один тон 1047Hz, 80ms, быстрый спад, едва слышимый

---

## Темы (Skins)

### theme.json структура

```json
{
  "id": "classic_white",
  "name": "Classic White",
  "colors": {
    "bgPrimary": "#F8F6F0",
    "accent": "#2E7D32",
    "bubbleSelf": "#DCEEFC"
  },
  "metrics": { "bubbleRadius": 6, "rowPadV": 8 },
  "retroMode": true
}
```

### Встроенные темы

| Тема | Прообраз |
|------|----------|
| Classic White | JIMM стандарт |
| JIMM Dark | Black Edition |
| Nokia Blue | Nokia S40 UI |
| Terminal Green | CLI JIMM 0.6 |
| AMOLED Night | Современный |
| Modern Light | Material You |

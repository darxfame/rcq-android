# JIMM 2026 — Retro Vision
> Source of truth для Claude-сессий и команды. Обновлено: 2026-05-30.

---

## ЧАСТЬ 1 — Исследование оригинального JIMM

### 1.1 История и контекст

**JIMM** (Java ICQ Mobile Messenger) — Java ME клиент ICQ (~2002, автор Дмитрий Кузьменков).
Работал на телефонах MIDP 1.0/2.0. Культовый статус в России/СНГ в 2004–2008.

| Платформа | Разрешение | Цвет | Особенности |
|-----------|------------|------|-------------|
| Nokia S40 (3–5 серии) | 128×128 / 176×220 | 4096–65536 | Джойстик, 2 softkey |
| Nokia S60 (6/7/E/N) | 176×208 / 352×416 | 65536–16M | Symbian, полноценная ОС |
| Sony Ericsson K/W серии | 176×220 / 240×320 | 65536 | Walkman, хорошие экраны |
| Motorola E398/E1/RAZR V3 | 176×220 / 240×320 | 65536 | Раскладушки |
| Siemens CX/M/C серии | 128×128 / 132×176 | 4096–65536 | Кастомные темы |

**Популярные сборки:**
- **JIMM Aspro** — форк с улучшенным UI, поддержкой крупных экранов (SE K750i)
- **JIMM Xattab** — самая популярная русская сборка: XML-скины, доп. эмотиконы, расширенные статусы, фильтры контактов, звуки
- **JIMM Black Edition** — тёмная тема, "хакерский" стиль
- **JIMM 0.6 / 0.8** — официальные версии, базовый функционал

---

### 1.2 Визуальный анализ

#### Список контактов

```
┌────────────────────────────────┐
│ ▶ Online (3)                   │  ← зелёный заголовок группы
│  🟢 Вася Пупкин           [3] │  ← точка, ник, счётчик
│     зайди ко мне               │  ← статус-сообщение, серый italic
│  🟢 KsuSha_2007                │
│     занята :(                  │
│ ▶ Away (5)                     │  ← жёлтый заголовок
│  🟡 privet_ya_doma             │
│ ▼ Offline (12)                 │  ← свёрнутая группа
└────────────────────────────────┘
  [Меню]              [Написать]   ← 2 softkey
```

**Ключевые паттерны:**
- Сортировка: Online → Away/NA → DND → Invisible → Offline
- Статус-иконки: цветные точки/цветки 8–12 px
- Статус-сообщение под ником (серый italic)
- Счётчик непрочитанных `(3)` справа от имени
- Коллапсируемые группы, оффлайн свёрнуты по умолчанию

#### Экран чата

```
┌────────────────────────────────┐
│ ← Вася Пупкин          🟢     │  ← хедер + статус
├────────────────────────────────┤
│ 14:35 Вася:                    │
│  привет как дела?              │
│ 14:36 Я:                       │
│  норм ты как                   │
│ 14:37 Вася:                    │
│  тоже норм                     │
│  придёшь завтра?               │
├────────────────────────────────┤
│ [_____________________] [→]    │  ← поле ввода
│ [Смайлы]          [Меню]       │  ← softkeys
└────────────────────────────────┘
```

**Ключевые паттерны:**
- НЕТ баблов — строки с временем и именем
- "Я:" для своих, имя для чужих
- Мелкий плотный шрифт (8–10pt)
- Минимальные отступы — больше сообщений на экране

#### Система статусов ICQ

| Статус | Код | Цвет | Emoji |
|--------|-----|------|-------|
| Online | FFC | `#33CC33` | 🟢 |
| Free for Chat | FFC | `#00CC00` | 💚 |
| Away | Away | `#FFCC00` | 🟡 |
| Not Available | NA | `#FFAA00` | 🟠 |
| Occupied | Occ | `#FF6600` | 🔶 |
| DND | DND | `#FF3300` | 🔴 |
| Invisible | Inv | `#9900CC` | 🟣 |
| Offline | - | `#999999` | ⚫ |

---

### 1.3 Что переносится / адаптируется / нельзя переносить

| Элемент | Переносить? | Адаптация |
|---------|-------------|-----------|
| Статус-иконки (цветки) | ✅ Прямо | SVG вместо 16×16 bitmap |
| Статус-сообщение под ником | ✅ Прямо | Subtitle в ContactRow |
| Группы контактов с коллапсом | ✅ Прямо | LazyColumn sticky headers |
| UIN как monospace | ✅ Прямо | `FontFamily.Monospace` |
| Компактная плотность | ✅ Прямо | Уменьшенный padding |
| Звуки событий | ✅ Прямо | SoundPool (новые звуки) |
| Ретро статус-цвета | ✅ Уже реализованы | Color.kt |
| "Пишет..." | ✅ Уже реализован | TypingIndicator |
| Softkey-меню внизу | 🔄 Адаптировать | Bottom Sheet / BottomAppBar |
| Шрифт 8pt | ❌ Нельзя | Accessibility минимум 11sp |
| Вложенное меню 3+ уровня | ❌ Нельзя | Плоская навигация |
| Нет баблов в чате | ❌ Нельзя | Современные пользователи ждут баблы |
| Фиксированные иконки 16×16 | ❌ Нельзя | SVG для HiDPI |

---

## ЧАСТЬ 2 — Design Vision: "JIMM 2026"

### 2.1 Философия

> **"Привет из 2006-го, написанный кодом 2026-го"**

JIMM 2026 отвечает на вопрос: *"Каким был бы JIMM, если бы он развивался 20 лет без остановки?"*

**Три кита:**
1. **Узнаваемость** — пользователь 2006 года узнаёт JIMM с первой секунды
2. **Современность** — новый пользователь не чувствует устаревания
3. **Характер** — приложение имеет личность, не безликое

### 2.2 Дизайн-принципы

| # | Принцип | Реализация |
|---|---------|------------|
| 1 | **Density First** | Компактные строки, больше контактов на экране |
| 2 | **Status is King** | Статус = самый важный визуальный элемент |
| 3 | **Mono-ID Identity** | UIN в monospace — фирменный знак |
| 4 | **Retro Signals** | Малые ретро-акценты без регресса UX |
| 5 | **Sound as Feedback** | Звуки — полноправная часть UX |
| 6 | **Progressive Retro** | Classic Mode + Modern Mode |

### 2.3 Retro-Modern Balance

```
Чистый JIMM 2006 ←————————————→ Material You 2024
                         ↑
                   JIMM 2026
                  (70% Modern + 30% Retro)
```

**Правило 70/30:**
- 70% современного Android UX (жесты, edge-to-edge, Material 3)
- 30% ретро-акцентов (статус-цветки, звуки, monospace UIN, компактность)

---

## ЧАСТЬ 3 — Visual Design System

### 3.1 Цветовые палитры

#### Светлая тема — "JIMM Day"
```kotlin
object RetroLightColors {
    val bgPrimary       = Color(0xFFF8F6F0)  // тёплый бумажный белый
    val bgSecondary     = Color(0xFFEDEBE4)
    val bgRowHover      = Color(0xFFE6F0FA)  // голубой hover
    val textPrimary     = Color(0xFF1A1A1A)
    val textSecondary   = Color(0xFF666666)
    val textMono        = Color(0xFF333333)
    val accent          = Color(0xFF2E7D32)  // ICQ green, приглушённый
    val bubbleSelf      = Color(0xFFDCEEFC)  // голубой — свои
    val bubbleOther     = Color(0xFFF0EEE8)  // тёплый серый — чужие
    val headerGroup     = Color(0xFF1565C0)  // заголовки групп
    val divider         = Color(0xFFDDDDD5)
    val inputBg         = Color(0xFFFFFFFF)
    val navBar          = Color(0xFFEDEBE4)
    val statusOnline    = Color(0xFF2E7D32)
    val statusAway      = Color(0xFFF9A825)
    val statusBusy      = Color(0xFFD32F2F)
    val statusDnd       = Color(0xFFB71C1C)
    val statusInvisible = Color(0xFF6A1B9A)
    val statusOffline   = Color(0xFF757575)
    val unreadBadge     = Color(0xFFD32F2F)
}
```

#### Тёмная тема — "JIMM Night"
```kotlin
object RetroDarkColors {
    val bgPrimary       = Color(0xFF121212)
    val bgSecondary     = Color(0xFF1E1E1E)
    val bgRowHover      = Color(0xFF1F2A35)
    val textPrimary     = Color(0xFFE8E6E0)
    val textSecondary   = Color(0xFF9E9B94)
    val textMono        = Color(0xFFB0AEAA)
    val accent          = Color(0xFF4CAF50)  // ICQ green светится в темноте
    val bubbleSelf      = Color(0xFF1A3A4A)
    val bubbleOther     = Color(0xFF2A2A2A)
    val headerGroup     = Color(0xFF1976D2)
    val divider         = Color(0xFF2A2A2A)
    val inputBg         = Color(0xFF2A2A2A)
    val navBar          = Color(0xFF1A1A1A)
    // Статусы те же — они self-luminous
}
```

#### AMOLED тема — "JIMM Pure Black"
```kotlin
// Наследует RetroDarkColors, переопределяет:
val bgPrimary  = Color(0xFF000000)  // чистый чёрный
val bgSecondary = Color(0xFF0A0A0A)
val bgRow      = Color(0xFF000000)
// Экономия батареи ~15–20% на AMOLED-дисплеях
```

#### High Contrast (Accessibility)
```kotlin
// bgPrimary = #000000, text = #FFFFFF, accent = #00FF00
// Все пары текст/фон ≥ 4.5:1 (WCAG AA)
```

---

### 3.2 Типографика

```kotlin
object RetroFontSize {
    val bubbleText    = 15.sp   // сообщения — читаемость важнее компактности
    val bubbleTime    = 11.sp   // метка времени
    val contactName   = 14.sp   // ник контакта, bold
    val contactStatus = 12.sp   // статус-сообщение, italic
    val contactUin    = 11.sp   // UIN, monospace
    val chatName      = 15.sp   // имя чата в списке, bold
    val chatPreview   = 13.sp   // превью последнего сообщения
    val chatTime      = 11.sp   // время
    val groupHeader   = 11.sp   // заголовок группы, uppercase
    val badge         = 10.sp   // счётчик непрочитанных, bold
}
```

**Шрифты:**
- Основной: `Roboto` (системный, 95% текста)
- Ретро-акцент: `JetBrains Mono` (OFL) — UIN, recovery phrase, системные сообщения

**Системные сообщения (retro-стиль):**
```
[14:35] Вася Пупкин вошёл в сеть
[14:52] Вася Пупкин вышел из сети
```
→ JetBrains Mono, 11sp, italic, textSecondary, центровка

---

### 3.3 Метрики

```kotlin
object RetroMetrics {
    val avatarLg        = 42.dp   // список контактов
    val avatarSm        = 28.dp   // компактный режим
    val avatarChat      = 36.dp   // хедер чата
    val avatarBubble    = 24.dp   // аватар у чужого баббла
    val statusDot       = 9.dp    // ICQ-оригинал
    val statusDotBorder = 2.dp    // белая обводка
    val bubbleRadius    = 6.dp    // JIMM-стиль, минимальный
    val bubbleTailRadius = 2.dp   // хвостик
    val bubbleMaxWidth  = 0.78f   // 78% ширины
    val bubblePadH      = 10.dp
    val bubblePadV      = 6.dp
    val rowPadH         = 12.dp
    val rowPadV         = 8.dp    // компактный
    val rowPadVComfort  = 11.dp   // комфортный
    val dividerThickness = 0.5.dp
    val dividerIndent   = 56.dp
    val groupHeaderH    = 32.dp
}
```

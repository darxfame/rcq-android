# Visual Parity Guide

## Design Tokens (Phase 1 target)
```
SpacingTokens: xs=4dp, sm=8dp, md=16dp, lg=24dp, xl=32dp
ColorTokens: sentBubble=#0066CC, receivedBubble=#E5E5EA,
             onlineDot=#34C759, awayDot=#FF9500
TypographyTokens: messageBody=17sp/22sp, chatTitle=17sp SemiBold
```

## Platform-Native Adaptation
| iOS | Android |
|---|---|
| NavigationView back swipe | Predictive Back (API 33+) |
| UIContextMenu | DropdownMenu on long press |
| UIImpactFeedbackGenerator | VibrationEffect |
| UISearchController | SearchBar composable |

## Parity Process
1. iOS screenshot reference
2. Android Paparazzi snapshot
3. Diff > threshold → PR blocked
4. Sign-off: Design + QA both required

## JIMM/ICQ Design Direction (добавлено пользователем 2026-05-29)

**Цель:** Приложение должно выглядеть как JIMM ICQ — классический мобильный ICQ-клиент.

### Ключевые визуальные элементы JIMM/ICQ
- Зелёный акцент: #6BB12C (ICQ "flower" green)
- Белый фон в light, тёмно-серый в dark (#1A1A1A)
- Баблы: self = голубой (#DCEEFCA), other = серый
- Bubble radius: 6dp (не круглый, не квадратный)
- Status dots: online=зелёный, away=жёлтый, busy=красный, invisible=фиолетовый
- UIN отображается monospacед шрифтом
- Nickname: semibold, 15sp
- Timestamp: caption2, monospaced, 11sp

### Foundation уже в коде
- `LightColors`, `DarkColors` — точные цвета из iOS Theme.swift
- `RCQMetrics` — размеры из iOS Metrics
- `LocalRCQColors.current` — type-safe доступ к токенам в любом Composable

### Для дизайнеров
Использовать iOS ref `reference/ios/RCQ/` как источник правды.
Figma должен точно воспроизводить Theme.swift значения.

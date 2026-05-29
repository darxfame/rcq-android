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

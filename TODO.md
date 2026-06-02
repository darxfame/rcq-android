# TODO — phase-1-core-messaging

> Обновлено: 2026-05-31 (аудит)

## IMMEDIATE (pure code, no assets)

### P4 — Chat UI
- [ ] Retro двухрядный InputBar
- [ ] Реакции под баблом (emoji row, tap = отправить)

### P8 — Polish
- [ ] Haptic feedback на получение входящего сообщения (на отправку уже есть)
- [ ] JIMM mode на ChatsScreen и ChatScreen (читает LocalRetroMode)

## LATER (requires binary assets)

### P5 — Sounds
- [ ] RetroSoundManager.kt skeleton + 9 OGG placeholders
- [ ] Привязка к WS событиям

### P7 — Retro Assets
- [ ] RetroStatusFlower Canvas (drawPath, без SVG)
- [ ] RetroEmoticonPicker (текстовые коды :) :D)
- [ ] Text→emoji парсер

## BUGS
- [ ] BUG-005 SingBox stub — нужен libbox.aar
- [ ] BUG-006 входящие DM иногда 🔒

## DONE
- [x] Dev UIN auto-add (.Dev badge)
- [x] WS proxy fix — WebSocket через RcqProxySelector
- [x] Диагностика: 10 ручек + WS status
- [x] Room migration 13→14 (statusMessage)
- [x] Compact mode toggle (P6)
- [x] RetroSystemMessage (P4)

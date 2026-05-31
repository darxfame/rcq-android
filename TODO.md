# TODO — phase-1-core-messaging

> Обновлено: 2026-05-31 после коммита e80a060

## IMMEDIATE (pure code, no assets)

### P3 — Contact List
- [ ] Room migration 13→14: поле `statusMessage` в ContactEntity + Contact
- [ ] Статус-сообщение под ником в ContactItem / StatusGroupedContactList

### P4 — Chat UI
- [ ] RetroSystemMessage (серая строка по центру для системных событий)
- [ ] Retro двухрядный InputBar
- [ ] Реакции под баблом (emoji row, tap = отправить)

### P6 — Settings
- [ ] Compact mode toggle (DataStore + RCQMetrics.compact)

### P8 — Polish
- [ ] Haptic feedback на отправку/получение
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
- [ ] BUG-007 statusMessage нет поля → решается миграцией 13→14

## DONE THIS SESSION
- [x] Dev UIN auto-add (.Dev badge)
- [x] WS proxy fix — WebSocket через RcqProxySelector
- [x] Диагностика: 10 ручек + WS status

# TODO — текущая сессия (2026-05-31)

## Сделано ✅
- Dev UIN = **911** (.Dev badge, авто-добавление без заявки)
- WebSocket через RcqProxySelector (обход работает и для WS)
- Диагностика: 10 REST ручек + WS статус
- ContactEntity: поле `statusMessage` (миграция 13→14)
- PreferencesKeys: COMPACT_MODE ключ

## В процессе 🔄
- Migration 13→14 в RCQDatabase.kt
- CompactMode в AppPrefsViewModel + SettingsScreen
- Статус-сообщение в ContactItem
- RetroSystemMessage в ChatScreen
- Haptic feedback на отправку

## Осталось ⏳

### IMMEDIATE (чистый код, без ассетов)
- [ ] Migration 13→14: ALTER TABLE contacts ADD COLUMN statusMessage TEXT
- [ ] AppModule: добавить MIGRATION_13_14 в список
- [ ] ContactItem: показать statusMessage под UIN (italic, textSecondary)
- [ ] AppPrefsViewModel: compactMode StateFlow из DataStore
- [ ] SettingsScreen: Compact Mode toggle
- [ ] ChatScreen: RetroSystemMessage для SYSTEM_NOTICE kind
- [ ] ChatScreen: haptic feedback при отправке (LocalHapticFeedback)
- [ ] ChatsScreen: LocalRetroMode → компактные строки когда ON

### P5 Sounds (нужны OGG — сделать skeleton)
- [ ] RetroSoundManager.kt skeleton (SoundPool + enum)
- [ ] Привязка к WS событиям (message received → звук)

### P7 Retro Assets (без SVG, только код)
- [ ] RetroStatusFlower Canvas (drawPath цветок 8 лепестков)
- [ ] RetroEmoticonPicker (список текстовых кодов)
- [ ] Text→emoji парсер (:) → 😊, :D → 😃 и т.д.)

### Bugs
- [ ] BUG-006: входящие DM → 🔒 (Signal-сессия, отложено)
- [ ] BUG-005: SingBox stub → нужен libbox.aar (отложено)

## Dev UIN
UIN разработчика: **911**

# Roadmap

## Strategy: Incremental Stabilization

- Big Bang: ❌ (existing crypto/E2EE code is valuable, don't break)
- Incremental: ✅ stabilize → foundation → features → polish

## Phase 0 — Emergency Stabilization (Week 1-2)

Fix critical bugs blocking basic messaging.

| Task | Status |
|---|---|
| Delete duplicate WS + dead code | ✅ Done |
| Fix WS 500 error (BUG-001) | 🔴 Pending |
| Fix message ordering (BUG-002) | 🔴 Pending |
| Fix group membership filter (BUG-003) | 🔴 Pending |
| Implement offline outbox queue | 🔴 Pending |

**Exit:** WS connects, messages ordered correctly, groups filtered, offline messages delivered.

## Phase 1 — Architecture Foundation (Week 3-4)

Establish patterns preventing future tech debt.

- MVI/UDF refactor (ChatsViewModel, ChatViewModel)
- Design tokens (SpacingTokens, ColorTokens, TypographyTokens)
- Timber logging
- Build variants (staging/production)
- GitHub Actions CI
- Crash reporting

**Exit:** New features built without accruing debt.

## Phase 2 — Tier 1 Feature Completion (Week 5-8)

All critical messaging features.

- Delivery states complete (PENDING→SENT→DELIVERED→READ)
- Typing indicators (send + receive)
- Presence/online status
- Unread counters accurate
- Offline send + retry

**Exit:** Feature parity with iOS for Tier 1.

## Phase 3 — Tier 2 Communication (Week 9-12)

- Reactions, editing, deletion
- Media pipeline complete
- Search (FTS4)
- Replies, forwards, pins, archive, mute

## Phase 4 — Polish + Parity (Week 13+)

- Visual parity with iOS
- Animations + transitions
- Voice/video calls stable
- Accessibility
- Screenshot parity tests

## Phase UI — JIMM/ICQ Visual Redesign (добавлено 2026-05-29)

**Цель:** Воссоздать визуальный стиль классического JIMM ICQ клиента.
**Приоритет:** Высокий (между Phase 1 и Phase 2)

### Вдохновение
- JIMM — Java ICQ-клиент для мобильных (2003-2008)
- ICQ 2002 desktop: белый фон, зелёный акцент #6BB12C, голубые баблы
- iOS ref (`reference/ios/RCQ/Utils/Theme.swift`) — прямой источник токенов

### Задачи для дизайнеров
- Разработать компонентную библиотеку в стиле JIMM/ICQ:
  * Список чатов: avatar слева, nickname bold, preview серый, timestamp справа
  * Баббл: радиус 6dp, self=голубой #DCEEFCA, other=серый
  * Status dot: 9dp, цвета по спеке (green/yellow/red/purple/gray)
  * Tab bar: иконки в стиле ICQ (flower logo как акцент)
  * Input bar: закруглённый, кнопка Send зелёная
  * Контакты: по аналогии с ICQ contact list (группы, статусы)
- Figma-файл с токенами из Theme.swift (уже реализованы в Color.kt)
- Screenshot-сравнение iOS ref ↔ Android для каждого экрана

### Что уже сделано (foundation)
- Color.kt: ICQ-палитра (light + dark), все цвета из iOS Theme.swift ✅
- Theme.kt: RCQColorScheme, LocalRCQColors, light/dark поддержка ✅
- Metrics.kt: размеры из iOS (avatar, bubble radius, padding) ✅

### Что нужно дизайнерам → разработчикам
1. ChatListRow — переработать под JIMM-стиль
2. MessageBubble — радиус 6dp, ICQ-цвета, timestamp в стиле ICQ
3. ContactRow — статус-дот, monospace UIN
4. NavigationBar — иконки в ICQ-стиле
5. InputBar — закруглённый, зелёная кнопка
6. ProfileView — аватар, статус, UIN monospace

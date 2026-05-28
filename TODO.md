# RCQ Android — TODO

## Build & Infrastructure
- [x] assembleDebug green (123 compile errors fixed — May 28)
- [x] DB migration chain v6→v7→v8→v9→v10 working
- [x] Hilt DI wired for all DAOs including SignalKeyDao
- [x] KyberPreKey methods implemented (libsignal 0.33 compatibility)
- [x] GitHub Actions: APK published to GitHub Release for direct install

## Authentication
- [x] Welcome screen + registration
- [x] Recovery phrase generation/display/clipboard
- [x] Auth token via DataStore + interceptor
- [x] Logout
- [ ] Account recovery — challenge-response (Ed25519) — BACKEND PENDING
  - Android: MnemonicHelper.kt создан (Task 6 ✅), Tasks 7-9 ждут backend
  - Backend: patch в docs/superpowers/plans/account-recovery-backend.patch
- [ ] Biometric/PIN unlock

## Contacts
- [x] List contacts
- [x] Search locally
- [x] Add contact by UIN
- [x] Accept/decline requests
- [x] Remove / block / unblock
- [x] Edit contact nickname (custom alias per contact)
- [x] Favorite contacts (toggle + UI indicator)
- [ ] Push notifications for new contact requests

## Chats & Messaging
- [x] Chats list with last message preview + unread count badge
- [x] Load messages from DB + sync from server on open
- [x] Send / edit / delete message
- [x] Reply to message (long-press → context menu)
- [x] Forward message (stub — chat selector pending)
- [x] E2EE encryption via Signal Protocol
- [x] ChatEntity updated (targetId, targetNickname, isPinned, lastMessage)
- [ ] Real-time receive via WebSocket — wired, needs end-to-end testing
- [ ] Push notifications
- [ ] Forward message: chat selector UI

## Groups
- [x] GroupEntity, GroupDao, GroupRepository (compiles)
- [x] CreateGroupScreen + CreateGroupViewModel
- [ ] Create group — API call working end-to-end
- [ ] Group chat screen
- [ ] Group member management

## Stories
- [x] StoriesScreen UI (list)
- [x] StoryViewerScreen + StoryViewerViewModel
- [ ] Story upload
- [ ] Story reply

## Calls
- [x] CallEntity, CallDao (with getMissedCalls, limit overload)
- [x] CallScreen, CallsScreen UI
- [ ] WebRTC call initiation working end-to-end
- [ ] Incoming call notification

## Settings & Profile
- [x] Display UIN, nickname, recovery phrase, logout
- [x] Profile editing (nickname + bio) — saves to API + DataStore
- [x] Notifications settings (toggles: enabled, preview, sound, vibration)
- [x] Privacy settings (toggles: read receipts, last seen, online status)
- [x] Appearance settings (dark theme toggle — in-memory)
- [x] Storage section (cache info + clear stub)

## Phase 2: Media Messaging ✅ COMPLETE
- [x] Photo send (gallery picker → E2EE upload)
- [x] Photo receive + display (MediaMessageBubble)
- [x] Video send/receive
- [x] File attachment send/receive
- [x] Voice message record + send
- [x] Voice message playback (download → decrypt → play)
- [x] Location sharing (FusedLocationProvider + permission)
- [x] All media types encrypted via CryptoService + MediaService

## Phase 3: Calls (NEXT)
- [ ] WebRTC call initiation end-to-end (CallManager + CallService exist)
- [ ] Incoming call screen + notification
- [ ] Call controls (mute, speaker, camera flip)
- [ ] Audio rooms (AudioRoomService exists)

## Design: iOS Reference Parity
- [ ] Сверить иконки с rcq-ios (https://github.com/rcq-messenger/rcq-ios)
- [ ] Порт звуков уведомлений / системных звуков из iOS ресурсов
- [ ] Эмодзи-реакции в стиле iOS (picker + display в баблах)
- [ ] Типографика и отступы по iOS гайдлайнам (скруглия, shadow, spacing)
- [ ] Цветовая палитра/тема в соответствии с iOS дизайн-системой RCQ

## Phase 4: Social Features
- [ ] Stories upload + reply
- [ ] Games screen functional
- [ ] Marketplace screen functional
- [ ] Audio rooms screen functional

## Testing
- [ ] Unit tests for CryptoService
- [ ] Integration tests for DB migrations (v6→v10)
- [ ] E2EE round-trip test (encrypt → store → decrypt)
- [ ] Contact sync integration test

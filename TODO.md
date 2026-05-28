# RCQ Android — TODO

## Build & Infrastructure
- [x] assembleDebug green (123 compile errors fixed — May 28)
- [x] DB migration chain v6→v7→v8→v9 working
- [x] Hilt DI wired for all DAOs including SignalKeyDao
- [x] KyberPreKey methods implemented (libsignal 0.33 compatibility)

## Authentication
- [x] Welcome screen + registration
- [x] Recovery phrase generation/display/clipboard
- [x] Auth token via DataStore + interceptor
- [x] Logout
- [ ] Account recovery (restore from phrase) — UI exists, logic incomplete
- [ ] Biometric/PIN unlock

## Contacts
- [x] List contacts
- [x] Search locally
- [x] Add contact by UIN
- [x] Accept/decline requests
- [x] Remove / block / unblock
- [ ] Fix contact sync (API returns array, not `{contacts:[]}` wrapper) — DIAGNOSIS.md #1
- [ ] Fix user search timeout (GET /users/{uin}/info) — DIAGNOSIS.md #2
- [ ] Fix friend request endpoint ambiguity (POST /contacts vs /contacts/request) — DIAGNOSIS.md #3
- [ ] Edit contact nickname
- [ ] Favorite contacts

## Chats & Messaging
- [x] Chats list
- [x] Load messages from DB
- [x] Send / edit / delete message
- [x] E2EE encryption via Signal Protocol
- [x] ChatEntity updated (targetId, targetNickname, isPinned)
- [ ] Real-time receive via WebSocket — wired but needs validation
- [ ] Reply to message (UI exists, missing send logic)
- [ ] Forward message
- [ ] Push notifications

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
- [ ] Profile editing (avatar, bio, nickname)
- [ ] Privacy settings
- [ ] Notification settings
- [ ] Appearance/theme settings
- [ ] Storage settings

## Games / Marketplace / Pets / Audio Rooms
- [x] Entities and repositories compile
- [x] PetDao with equip/unequip
- [ ] Games screen functional
- [ ] Marketplace screen functional
- [ ] Audio rooms screen functional

## Phase 2: Media Messaging (NEXT)
- [ ] Photo send (camera + gallery picker)
- [ ] Photo receive + display
- [ ] Video send/receive
- [ ] File attachment send/receive
- [ ] Voice message record + send (VoiceRecorder exists)
- [ ] Voice message playback
- [ ] Location sharing
- [ ] All media types encrypted via CryptoService
- [ ] MediaService integration

## Testing
- [ ] Unit tests for CryptoService
- [ ] Integration tests for DB migrations (v6→v9)
- [ ] E2EE round-trip test (encrypt → store → decrypt)
- [ ] Contact sync integration test

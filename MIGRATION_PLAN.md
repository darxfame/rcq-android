# iOS → Android Migration Plan

## Current State Overview

Android app has: Auth, basic Contacts (broken), basic Chats/Messages (no E2EE), basic Groups, Audio Rooms, Calls, Stories, Marketplace, Games, Pets, Nearby, Settings stubs.

iOS app has: Everything above PLUS: E2EE (Signal Protocol), 47+ WebSocket events, sealed envelopes, media encryption, presence/status, push/voip, voice messages, disappearing messages, polls, trades, UIN auction, radio (P2P mesh), hood chat, random chat, profile editing, recovery phrase, proxy/VLESS, panic PIN, biometric unlock, and 15-language i18n.

---

## PHASE 0 — CRITICAL BUG FIXES (do first, 1-2 days)

### 0.1 ContactEntity Primary Key Fix
**Problem:** `ContactEntity` has `@PrimaryKey(autoGenerate = true) val id: Long = 0` — auto-generated ID causes duplicate contacts when syncing. iOS uses `uin: Int` as primary key.
**Fix:** Change to `@PrimaryKey val userId: Long` (maps to UIN). Remove `id` field. Update all DAO methods.
**Files:** `RCQDatabase.kt`, `ChatRepository.kt` (ContactEntity, ContactDao, extension functions)
**iOS reference:** `ContactService.swift` — contacts keyed by `[Int: Contact]` dictionary with `uin` as key.

### 0.2 Accept Contact Request — sync to local DB
**Problem:** After accepting a contact request, `syncContacts()` is called which fetches from server, but ContactEntity has wrong PK so the insert may fail or create duplicates.
**Fix:** After fixing PK in 0.1, verify that `syncContacts()` → `contactDao.insertAll()` works correctly (REPLACE strategy on `userId`).
**Files:** `ChatRepository.kt:142-149`, `RCQDatabase.kt:99-103`
**Status:** API-level fix DONE (Response<Unit> + syncContacts()). DB-level pending (depends on 0.1).

### 0.3 UserStatus — Missing states
**Problem:** Android `UserStatus` has only 4 values (ONLINE/AWAY/BUSY/OFFLINE). iOS has 5: `online, away, dnd, invisible, offline`. Missing `DND` and `INVISIBLE`.
**Fix:** Add `DND` and `INVISIBLE` to `UserStatus` enum. Map correctly in serialization. Update User model.
**Files:** `domain/model/User.kt`

### 0.4 UserStatus — WebSocket Presence Handling
**Problem:** Status model doesn't work at all because WebSocket only handles 2 event types. iOS sends/receives presence via WS events and `POST /presence/status`.
**Fix:** Add presence event handling to WebSocket (`presence_online`, `presence_away`, `presence_dnd`, `presence_invisible`, `presence_offline`). Add presence broadcasting when app foregrounds/backgrounds.
**Files:** `data/ws/WebSocketService.kt` (needs major rewrite), `data/repository/UserRepository.kt` (add presence update)
**iOS reference:** `PresenceService.swift`, `WebSocketService.swift:Event.presence*`

### 0.5 WebSocket Reconnection with Exponential Backoff
**Problem:** No reconnection logic. iOS has exponential backoff (1s, 2s, 4s, 8s, 16s, max 30s) with stale watchdog (90s).
**Fix:** Add reconnection with backoff. Add ping/pong (25s interval). Add stale watchdog.
**Files:** `data/ws/WebSocketService.kt`

### 0.6 WebSocket Message Model — Add All Event Types
**Problem:** Only 2 event types handled. iOS handles 47+ event types including message delivery, reactions, typing, calls, rooms, stories, marketplace, games, etc.
**Fix:** Model all event types as a sealed class. Add routing to appropriate repositories.
**Files:** `data/ws/` (create new models)

---

## PHASE 1 — CORE MESSAGING (1-2 weeks)

### 1.1 Message Model — Full Alignment with iOS
**iOS Message model has 32 params. Android has ~20. Missing:**

| Field | Type | iOS Source |
|-------|------|------------|
| `mediaUrl` / `mediaId` | String? | `Message.swift:kind` → `.photo(MediaID)` etc. |
| `mediaKey` (encrypted) | String? | `Message.swift:mediaID` split from token |
| `deliveryState` | enum | `Message.swift:deliveryState` (sending/sent/delivered/read/failed) |
| `replyToAuthorName` | String? | `Message.swift:replyToAuthorName` |
| `ttlSeconds` | Int? | `Message.swift:ttlSeconds` (disappearing) |
| `premiumPriceTokens` | Long? | `Message.swift:premiumPriceTokens` |
| `premiumUnlocked` | Boolean | `Message.swift:premiumUnlocked` |
| `albumId` | String? | `Message.swift:albumID` |
| `fileName` | String? | `Message.swift:fileName` |
| `fileMime` | String? | `Message.swift:fileMime` |
| `fileSizeBytes` | Long? | `Message.swift:fileSizeBytes` |
| `latitude` / `longitude` | Double? | `Message.swift:kind` → `.location` |
| `pollId` | String? | `Message.swift:pollID` |
| `forwardedFromName` | String? | `Message.swift:forwardedFromName` |
| `thumbnailB64` | String? | `Message.swift:thumbnailB64` |
| `durationSec` | Int? | `Message.swift:durationSec` (voice/video) |
| `receivedWhileAway` | Boolean | `Message.swift:receivedWhileAway` |

**Files:** `domain/model/Message.kt`, `data/db/RCQDatabase.kt` (MessageEntity), `data/repository/ChatRepository.kt` (mappers)

### 1.2 Message Type — Full Enum Alignment
**iOS has 17 envelope types. Android has ~6.**

Add: `photo`, `video`, `voice`, `file`, `location`, `premiumPhoto`, `premiumVideo`, `poll`, `deleteForEveryone`, `readReceipt`, `reaction`, `bounce`, `visit`, `edit`, `systemNotice`, `typing`.

**Files:** `domain/model/Message.kt`

### 1.3 Sealed Envelope Messaging (E2EE)
**iOS flow:**
1. `SignalSession.establish(peerUIN)` — fetches pre-key bundle from `GET /keys/{uin}/bundle`
2. `CryptoService.encrypt(payload, session)` — ECIES + Double Ratchet
3. `POST /messages/send` with `Envelope{to_uin, ciphertext, nonce, ephemeral_key, signal_type, message_type, ...}`
4. Server stores encrypted envelope, peer decrypts on receipt

**Android needs:**
- Signal Protocol library (libsignal-client Java bindings or signal-protocol-java)
- `CryptoService` — full ECIES + Double Ratchet implementation
- `SignalSession` — pre-key bundle fetch/store
- `SignalProtocolStore` — SQLite-based session/identity/pre-key storage
- Sealed envelope API endpoint + models
- Key generation + bundle upload on registration

**Files:** `data/crypto/` (new package), `data/api/` (new endpoints), `domain/model/` (envelope model)

### 1.4 Message Reactions
**iOS:** `POST /messages/react {message_id, reaction}` + WS `messageReaction`.
**Android:** API endpoint exists (`POST addReaction`) but UI missing.
**Files:** UI layer for reactions

### 1.5 Message Editing
**iOS:** `PUT` endpoint + WS `messageEdited`.
**Android:** API endpoint exists but UI missing.
**Files:** UI layer for edit

### 1.6 Message Delete For Everyone
**iOS:** `POST /messages/delete-for-everyone` + WS `messageDeletedForEveryone`.
**Android:** Missing entirely.
**Files:** API layer + repository + UI

### 1.7 Read Receipts
**iOS:** `POST /messages/read {message_ids}` + WS `messageRead`, sent automatically on thread open.
**Files:** API + repository + view model + UI

### 1.8 Typing Indicator
**iOS:** WS events `typingStarted` / `typingStopped`, sent on text change with 1s debounce.
**Android:** `Chat` model has `typingUsers` field but no WS event handling.
**Files:** WebSocket event handling + UI

### 1.9 Reply to Message
**iOS:** `Envelope.reply_to_id` + `reply_to_snippet` + `reply_to_author_name`.
**Android:** Models have `replyToId`/`replyToContent` but UI missing.
**Files:** Chat UI composable

### 1.10 Forward Messages
**iOS:** `POST /messages/forward {message_ids, to_uin?, group_id?}`, preserves `forwardedFromName`.
**Android:** Missing entirely.
**Files:** API + repository + UI

### 1.11 Disappearing Messages
**iOS:** `ChatSettingsStore` per-thread TTL (Off/1m/5m/1h/24h/7d). Timer-based deletion after read.
**Android:** Missing entirely.
**Files:** Per-chat settings, timer service, message deletion

---

## PHASE 2 — MEDIA & VOICE (1 week)

### 2.1 MediaService — Encrypted Upload/Download
**iOS:**
- `POST /media/upload` — Multipart encrypted data → `{media_id, token}`
- `GET /media/{id}` — Encrypted binary → AES-GCM decrypt
- Token format: `media_id|base64_key`
- 3-tier NSCache (images, thumbs, blurs)
- 2GB max, 50MB free tier

**Android needs:**
- `MediaRepository` + `MediaService`
- AES-GCM encryption before upload, decryption after download
- Media token management (parse `media_id|key` format)
- LRU cache (use Coil/Glide with custom fetcher for encrypted media)
- Upload progress tracking
- Thumbnail generation

**Files:** `data/media/` (new package), `data/api/RCQApiService.kt` (upload endpoints)

### 2.2 Voice Messages — Recorder
**iOS:** `VoiceRecorder.swift` — AAC 32kbps mono, 22050Hz, .m4a, max 120s, min 0.3s.
**Android:** Use `MediaRecorder` with similar settings.
**Files:** `ui/chat/voice/VoiceRecorder.kt` (new)

### 2.3 Voice Messages — Player
**iOS:** `VoicePlayer.swift` — single-instance, published state, 0.05s ticker, temp file cache.
**Android:** Use `MediaPlayer` + `ExoPlayer` with waveform visualization.
**Files:** `ui/chat/voice/VoicePlayer.kt` (new)

### 2.4 File Attachments
**iOS:** Generic file share with `fileName`, `fileMime`, `fileSizeBytes`.
**Android:** Document picker → encrypt → upload via MediaService.
**Files:** API models already have fields (add missing), UI picker needed

### 2.5 Location Sharing
**iOS:** `Message.kind.location` with lat/lng.
**Android:** New message type + map preview + Google Maps/deep link.
**Files:** Models, API, UI

### 2.6 Album / Multi-Photo
**iOS:** `albumID` groups multiple photos into an album.
**Android:** New field + album layout in chat UI.
**Files:** Models, UI

### 2.7 Premium Media
**iOS:** `Message.kind.premiumPhoto` / `premiumVideo` with token price.
**Android:** New message types + payment flow + unlock UI.
**Files:** Models, API, UI

---

## PHASE 3 — REAL-TIME & PUSH (1 week)

### 3.1 WebSocket — Full Event Model
**iOS has 47+ Event types in 12 categories:**

| Category | Events | Priority |
|----------|--------|----------|
| **Messaging** | newMessage, messageDeleted, messageDeletedForEveryone, messageReaction, messageRead, messageEdited, messageBounced, typingStarted, typingStopped | HIGH |
| **Presence** | presenceOnline, presenceAway, presenceDND, presenceInvisible, presenceOffline | HIGH |
| **Threads** | threadUpdated, threadDeleted | HIGH |
| **Groups** | groupUpdated, groupMemberJoined, groupMemberLeft, groupDeleted | MEDIUM |
| **Calls** | callOffer, callAnswer, callIceCandidate, callEnd, callUpgrade, callUpgradeAnswer | MEDIUM |
| **Audio Rooms** | audioRoomStarted, audioRoomPeerJoined, audioRoomPeerLeft, audioRoomEnded | MEDIUM |
| **Stories** | storyPosted, storyExpired, storyViewed | LOW |
| **Marketplace** | marketplaceListingCreated, marketplaceListingSold, marketplaceListingCancelled | LOW |
| **Games** | crashStarted, crashBetPlaced, crashTick, crashEnded, crashCashedOut, hiloCardRevealed, hiloRoundEnded, hiloCashedOut, limboResult | LOW |
| **AUCTION** | uinAuctionStarted, uinAuctionBid, uinAuctionEnded, uinAuctionOutbid | LOW |
| **Trades** | tradeReceived, tradeAccepted, tradeDeclined, tradeCancelled | LOW |
| **PETS** | huntEvent, petLevelUp, petHpChanged | LOW |
| **Nearby** | nearbyPeerAppeared, nearbyPeerDisappeared, nearbyBucketChanged | LOW |
| **Hood** | hoodMessage, hoodCount, hoodDelete, hoodReaction | LOW |
| **Radio** | (P2P, no WS events) | LOW |
| **SYSTEM** | jetonReact, ban, warning | LOW |

**Android needs:** Complete sealed class for all events, routing to repositories.

### 3.2 Push Notifications (FCM)
**iOS:** VoIP push via PushKit for calls + standard push for messages.
**Android:** FCM with data payloads, high-priority for calls.
**Files:** `firebase-messaging` dependency, `FCMService.kt`, push token registration (`POST /users/me/push-token`)

### 3.3 Incoming Call Handling
**iOS:** PushKit → CallKit → WebRTC.
**Android:** FCM data → foreground service → notification with full-screen intent → WebRTC.
**Files:** Call notification + WebRTC integration

---

## PHASE 4 — FEATURE MODULES (2-3 weeks)

### 4.1 Profile Editing
**iOS:** `PATCH /users/me` with partial fields, multipart avatar/cover upload.
**Missing on Android:** Cover photo, about/bio editing, visibility settings (lastSeen, online status, read receipts, typing indicator, profile photo, about), gender, interests, homepage, groupInvitePolicy, tradePolicy, callPolicy, age, city, country.
**Files:** `ui/profile/` (new), API models, User model

### 4.2 User Model — Full Alignment
**iOS `UserProfile` has 26 fields. Android `User` has ~15.**

Add: `identityKey`, `signingKey`, `signalIdentityKey`, `gender`, `statusMessage`, `lastSeenVisibility`, `genderVisibility`, `profileVisibility`, `groupInvitePolicy`, `tradePolicy`, `callPolicy`, `readReceiptsVisibility`, `reputation`, `reputationVisibility`, `interests`, `homepage`, `city`, `country`, `age`, `firstName`, `lastName`, `coverUrl`.

**Files:** `domain/model/User.kt`, data layers

### 4.3 Contact Model — Full Alignment
**iOS `Contact` has fields beyond Android:**
Add: `statusMessage`, `identityKey`, `signingKey`, `signalIdentityKey`, `gender`, `equippedPet`.
**Fix:** `id` should NOT be auto-generated. Use `userId` (UIN) as PK.
**Files:** `domain/model/Contact.kt`, `RCQDatabase.kt:ContactEntity`

### 4.4 Settings — Full Implementation
**iOS:** 6 settings groups managed by `SettingsService` + `NotificationPrefsService`.
**Missing on Android:**
- Notification preferences (message/group/call/story toggles, sound, vibration, preview)
- Privacy settings (lastSeen, online, readReceipts, typing, profilePhoto, about visibility)
- Appearance (theme dark/light, language, bubble style, font size)
- Storage (auto-download WiFi/cellular, save to gallery, quality preset)
- Proxy configuration (VLESS/REALITY via SingBoxTransport)
- Account management (delete account, forgot/reset password)
**Files:** `ui/settings/`, data models, API endpoints

### 4.5 Nearby (Geohash Geo-Discovery)
**iOS:** `POST /nearby/announce {lat, lng, bucket?}`, `GET /nearby/peers` → `[NearbyPeer]`, 30s auto-refresh, WS `nearbyPeerAppeared/Disappeared`, anonymous display names.
**Android:** API endpoints exist (`GET /nearby`) but no UI, no periodic announce, no WS events.
**Files:** `ui/nearby/` (new), background location service

### 4.6 Group Management
**iOS:** Full group CRUD with join/leave, member management, settings (entry price, post policy, members hidden), preview, search.
**Android:** Basic create/list only. Missing: getGroup, updateGroup, addMember, removeMember, join/leave, search, preview, settings.
**Files:** `GroupRepository.kt`, `ui/groups/`

### 4.7 Story Improvements
**iOS:** Story viewer with progress indicators, viewer list, reply to story, 24h expiry.
**Android:** Basic story list + create. Missing: viewer list UI, reply to story, progress indicator in viewer, story upload with encrypted media.
**Files:** `ui/stories/`

### 4.8 Audio Room Improvements
**iOS:** Mesh WebRTC (max 7 peers), host privileges, mute/raise-hand/invite, TURN credentials.
**Android:** API endpoints exist but audio rooms require WebRTC integration.
**Files:** WebRTC audio + mesh topology

### 4.9 WebRTC Calling
**iOS:** Full WebRTC with STUN/TURN, video upgrade, CallKit integration, VoIP push.
**Android:** API endpoints exist but no WebRTC implementation. Need `google-webrtc` dependency + signaling via WS + call notification UI.
**Files:** `data/webrtc/` (new), `ui/call/` (new)

### 4.10 Voice/Radio P2P
**iOS:** MultipeerConnectivity mesh (max 8 peers), AES-GCM encrypted, PTT voice, PBKDF2 room keys, Curve25519 1:1 ephemeral keys.
**Android:** Need Nearby Connections API or custom WebRTC mesh. Low priority.
**Files:** P2P networking layer (new)

### 4.11 Trades
**iOS:** Propose/accept/decline/cancel trades with items + UINs + tokens/scrolls. WS events.
**Android:** Missing entirely.
**Files:** API + repository + UI (new)

### 4.12 UIN Auction
**iOS:** Active auctions, bidding, owned UINs, release, recent winners. WS events with soft-close sound.
**Android:** Missing entirely.
**Files:** API + repository + UI (new)

### 4.13 Marketplace Improvements
**iOS:** Browse listings, buy, cancel, own listings, recent sales, WS events.
**Android:** API exists but UI stubs only. No WS events.
**Files:** UI layer + WS events

### 4.14 Games (Crash, HiLo, Limbo)
**iOS:** Real-time WS-driven games with bet/cashout, WS tick events for crash multiplier.
**Android:** API stubs exist. No WS events, no UI, no game logic.
**Files:** UI + WS events + game state management

### 4.15 Pet Hunting
**iOS:** Hunt system with zones, actions, XP/HP, feeding, healing, renaming, death mechanic.
**Android:** API endpoints exist but no UI, no hunt flow.
**Files:** `ui/pets/`, hunt state management

### 4.16 Polls
**iOS:** Create poll in group, vote, close, recovery by message ID.
**Android:** Missing entirely.
**Files:** API + repository + UI (new)

### 4.17 Hood Chat (Local Anonymous Chat)
**iOS:** Bucket-based anonymous chat, reply chain, reactions, banners (paid).
**Android:** Missing entirely.
**Files:** API + repository + UI (new)

### 4.18 Random Chat (Anonymous 1:1)
**iOS:** `MessageService+Random.swift` — anonymous matching + messaging.
**Android:** Missing entirely.
**Files:** API + repository + UI (new)

---

## PHASE 5 — SECURITY & INFRASTRUCTURE (1 week)

### 5.1 Signal Protocol — Full Implementation
**iOS:** libsignal with PQXDH (Kyber pre-keys), Double Ratchet, sealed sender.
**Android:** Use `signal-protocol-java` library or JNI bindings to `libsignal`.
**Components:**
- Identity key pair generation + storage
- Pre-key bundle generation + upload (`POST /keys/bundle`)
- Pre-key bundle fetch (`GET /keys/{uin}/bundle`)
- Session establishment + Double Ratchet
- Encryption/decryption of message payloads
- Session store (SQLite, shared with NSE equivalent)
- Trust-on-first-use (TOFU) identity verification

### 5.2 CryptoService — Full Implementation
**iOS:** Protocol with v1 (ECIES + ChaChaPoly) and v2 (Signal Double Ratchet in ECIES tunnel).
**Android:** Replace placeholder SHA-256 + AES-GCM with proper ECIES (Curve25519 ECDH + AES-GCM) + Signal Protocol.

### 5.3 Keychain / Secure Storage
**iOS:** KeychainStore with shared access group for NSE.
**Android:** Use `EncryptedSharedPreferences` or Android Keystore for: token, refresh token, identity private key, signing private key, PIN pepper.
**Files:** `data/auth/AuthManager.kt`

### 5.4 Proxy Support (VLESS/REALITY)
**iOS:** `SingBoxTransport.swift` — SOCKS5 proxy wrapped in VLESS + TLS REALITY to `35.238.53.96:443`.
**Android:** No proxy support. Need to integrate a SOCKS5 proxy or tunnel library.
**Files:** OkHttp proxy configuration

### 5.5 Panic PIN
**iOS:** 3-slot PIN vault (real/decoy/wipe), PBKDF2-SHA256 400k iterations, AES-GCM sealed slots.
**Android:** Implement with Android Keystore + Argon2id KDF + AES-GCM.
**Files:** `data/auth/PanicPinManager.kt` (new)

### 5.6 Biometric Unlock
**iOS:** `BiometricUnlock.swift` — Keychain-based with `SecAccessControl`.
**Android:** `BiometricPrompt` API + `EncryptedSharedPreferences`.
**Files:** `ui/auth/BiometricUnlock.kt` (new)

### 5.7 Recovery Phrase (BIP39)
**iOS:** Proper BIP39 mnemonic encoding from identity key seed.
**Android:** Current implementation is fake (random words, not BIP39). Replace with actual BIP39 (`bip39` library or implementation).
**Files:** `data/auth/RecoveryPhraseManager.kt`

### 5.8 Language / i18n
**iOS:** 15 languages, flat file + App Group handoff, version counter.
**Android:** Use Android resource strings (`values-*`). Need translations for all 15 languages.
**Files:** `res/values-*/strings.xml`

---

## PHASE 6 — UI & POLISH (ongoing)

### 6.1 Chat UI — Full Feature Parity
- Message bubbles with reactions picker
- Reply preview (quoted message above input)
- Forward message UI
- Voice message recorder/player inline
- File attachment picker
- Location preview
- Album/multi-photo layout
- Disappearing message timer
- Typing indicator
- Read receipts display (double-check marks)
- Delivery status (sending/sent/delivered/read)
- Message context menu (copy, edit, delete, reply, forward, report)
- Mention autocomplete (@username)
- Link preview

### 6.2 Contact List UI
- Online/away/dnd/invisible/offline status indicators (colored dots)
- Status message under nickname
- Last seen text
- Contact search with UIN + nickname
- Pending requests section with accept/decline
- Favorites section
- Blocked contacts list
- Contact profile sheet

### 6.3 Navigation — Missing Screens
Add navigation destinations for:
- Profile editing
- Nearby users
- Groups list + detail + create
- Voice call
- Video call
- Audio rooms
- Marketplace listings + detail
- Games (Crash, HiLo, Limbo) 3 screens
- Pets + hunt
- Trades (incoming/outgoing)
- UIN auctions
- Hood chat
- Random chat
- Radio rooms
- Polls
- Settings (all groups)
- Story viewer
- Group admin panel

### 6.4 Theme & Appearance
- Dark/light theme toggle (sync with server setting)
- Message bubble style selector (rounded/compact/classic)
- Font size slider
- Accent color customization
- Background/wallpaper per chat

---

## IMPLEMENTATION ORDER SUMMARY

| Phase | Focus | Effort | Impact |
|-------|-------|--------|--------|
| **0** | Critical bugs (contacts, status, WS) | 2-3 days | HIGH — app currently broken |
| **1** | Core messaging (E2EE, reactions, etc.) | 2 weeks | HIGH — messaging is primary feature |
| **2** | Media & voice | 1 week | HIGH — media sharing is core |
| **3** | Real-time & push | 1 week | HIGH — delivery reliability |
| **4** | Feature modules (14 sub-projects) | 3-4 weeks | MEDIUM — feature parity |
| **5** | Security & infra | 1 week | HIGH — E2EE is critical |
| **6** | UI polish | ongoing | MEDIUM — user experience |

**Total estimated effort: 8-10 weeks for full parity.**

---

## IMMEDIATE NEXT STEPS (Phase 0)

1. Fix `ContactEntity` primary key → use `userId` (UIN)
2. Add `DND` and `INVISIBLE` to `UserStatus`
3. Rewrite `WebSocketService.kt` with full event model + reconnection + presence
4. Fix `MessageEntity` — add reactions column + missing fields
5. Add `POST /presence/status` call on app foreground/background

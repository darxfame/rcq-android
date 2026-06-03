# iOS → Android Parity Audit

> Created: 2026-06-03. Source of truth: `reference/ios/RCQ/RCQ` plus `docs/RCQ_API_SPEC.md`.

## Rules

- Audit only active Android code under `app/src/main/java/com/rcq/messenger`.
- Ignore stale excluded Android code under `app/src/main/java/app/rcq/android`.
- Do not implement Games, Marketplace/UIN shop/items, or Pets even if iOS has leftover UI/assets; these are explicitly out of Android scope.
- For behavior conflicts, iOS reference wins unless `docs/RCQ_API_SPEC.md` says otherwise.

## Current Parity Map

| Domain | iOS Reference | Android Status | Next Action |
|---|---|---|---|
| Build/source set | Xcode app target | Partial | Android now excludes stale `app.rcq.android` source tree; continue deleting or quarantining stale code later. |
| API transport | `APIClient.swift` | Partial | Added shared `SERVER_TOKEN` key and `X-RCQ-Auth` header support; still audit timeout/proxy/reachability behavior. |
| WebSocket transport | `WebSocketService.swift` | Partial | Build WS URL from active `BuildConfig.API_BASE_URL` and send `X-RCQ-Auth`; next audit typed event coverage. |
| Auth/account | `AuthService.swift`, `AccountManager.swift`, `VisitStore.swift` | Partial | Compare register/session/account-switch/recovery semantics and persisted fields. |
| Crypto/Signal | `CryptoService.swift`, `SignalSession.swift`, `SignalProtocolStores.swift` | High risk | Verify ECIES wire format, libsignal bundle fields, session bootstrap, sealed/group envelope parity with tests. |
| Message service | `MessageService*.swift`, `MessageStore.swift`, `MessageDB.swift` | Partial | Compare send/edit/delete/reaction/read/bounce/visit/offline queue behavior and serverTime ordering. |
| Contacts/groups | `ContactService.swift`, `GroupService.swift`, group models/views | Partial | Verify endpoints, membership filtering, closed/public group behavior, invite/link cards. |
| Media | `MediaService.swift`, media bubble/components | Partial | Compare encrypted upload/download, progress, file/photo/video/GIF/voice/location payloads. |
| Calls/audio rooms | `CallService.swift`, `WebRTCManager.swift`, `AudioRoomService.swift` | Partial | Compare signaling event names, SDP/ICE payload fields, room roster/key rotation/mute events. |
| Stories | `StoryService.swift`, story views | Partial | Compare feed/post/delete/view/viewers/reply behavior. |
| Notifications/push | `NotificationService.swift`, notification extension | Missing/partial | Android has helper only; audit FCM token registration and decrypt/preview behavior. |
| Panic PIN/biometric | `PINVault.swift`, `PanicPINService.swift`, `BiometricUnlock.swift` | Partial | Compare vault layout, lockout, decoy/wipe mode semantics. |
| Stealth/sing-box | `SingBoxTransport.swift`, `RelayConfigStore.swift` | Partial | `libbox.aar` included; validate runtime routing on device with logcat and diagnostics. |
| Settings/privacy/sounds/language | multiple settings/services | Partial | Compare DataStore keys, server-backed prefs, local sounds/language, blocked/archive/favorites. |
| Nearby/radio/hood/random/news/polls | iOS services/views | Mostly missing from active Android | Decide scope order; implement only when iOS-backed and approved by current project phase. |

## Immediate Findings

- Android previously compiled a stale parallel `app.rcq.android` tree; production build now excludes it from `main` source roots.
- Android previously included both `rcqbox.aar` and `libbox.aar`, duplicating gomobile `go.*` classes; Gradle now includes only `libbox.aar`.
- Android WebSocket previously hardcoded `wss://api.rcq.app/ws`; it now derives scheme/host from `BuildConfig.API_BASE_URL`, matching iOS flavor/base-url behavior.
- Android HTTP/WS previously had no `X-RCQ-Auth` support; both now read `PreferencesKeys.SERVER_TOKEN`.

## UI Parity Findings

Source checked: iOS `RCQApp.swift`, `ContactListView.swift`, chat/settings/story/call views, and active Android Compose screens under `app/src/main/java/com/rcq/messenger/ui`.

| Surface | iOS Reference | Android Status | Gap |
|---|---|---|---|
| Root/app shell | `RootView` is AppState-driven: onboarding, boot, PIN/panic lock, privacy cover, call/audio overlays, app foreground reconnect/sync. | Auth gate plus bottom-nav scaffold starting at `Chats`. | Missing root PIN/privacy cover flow, call/audio full-screen overlay restoration, app foreground reconnect/offline queue UI handling. |
| Main navigation | iOS centers on `ContactListView` with action entry points for stories/news/nearby/random/radio/hood/QR/accounts/settings. | Bottom nav with Chats/Contacts/Rooms/Stories/Settings. | Navigation model intentionally redesigned, but many iOS entry points are missing or unreachable from Android. |
| Chats list | iOS has contact-first shell plus richer chat affordances. | Android has search and create/browse group actions. | Presence/status source and unread/mute semantics need verification; direct creation callback currently has a navigation TODO in `ChatsViewModel`. |
| Chat screen | iOS has message actions, unified media picker, albums, polls, link/location/voice/video/file bubbles, in-chat search, banners, media viewer. | Android has text, attachment menu, reply preview, basic media bubble. | Call/more buttons are stubs; sender names and typing names are hardcoded; many message types/actions are missing or partial. |
| Contacts/groups | iOS supports user info, group info/settings, QR, account switching, news/nearby/random/radio/hood entry points. | Android supports contacts, pending requests, add contact, group browse/create, profile. | Group info/settings and several iOS discovery/social entry points are missing. |
| Stories | iOS has composer, viewer, viewers, delete/reply behavior. | Android has Stories screen and viewer. | Add story, own story, download, and more actions are stubs; composer parity is missing. |
| Calls | iOS has call overlay, minimized call bar, signaling-backed state. | Android has `CallScreen`/`CallsScreen`. | Target user/nickname is hardcoded, call-back action is stubbed, minimized bar and restored active call UI are missing. |
| Audio rooms | iOS restores active room and presents room screen full-screen. | Android has `AudioRoomsScreen`. | Room click is still a join TODO; roster/restore/key rotation/mute UI parity needs audit. |
| Settings | iOS has privacy, notifications, sounds, blocked users, server picker, accounts, language, about/bug bounty flows. | Android has settings plus stealth/PIN/diagnostics. | Profile/privacy/cache/about/help rows are dead clicks; several iOS settings sheets are missing. |
| Auth/recovery UI | iOS onboarding and recovery are tied into boot/account state. | Android welcome/recovery phrase flow exists. | Recovery phrase copy action is TODO; account switching and onboarding details need parity audit. |

## Next Audit Order

1. UI/navigation parity backlog: remove dead clicks/stubs and prioritize iOS-backed entry points.
2. WebSocket typed event parity and downstream consumers.
3. Crypto/message envelope parity with focused JVM tests.
4. Auth/account/recovery persisted state parity.
5. Device validation: productionDebug install, login/register flow, WS connect, sing-box diagnostics.

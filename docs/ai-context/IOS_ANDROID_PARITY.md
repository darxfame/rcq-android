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

## Next Audit Order

1. WebSocket typed event parity and downstream consumers.
2. Crypto/message envelope parity with focused JVM tests.
3. Auth/account/recovery persisted state parity.
4. Device validation: productionDebug install, login/register flow, WS connect, sing-box diagnostics.

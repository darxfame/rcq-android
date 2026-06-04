# Current State — 2026-06-04

## Build
- **Production Debug:** ✅ Passing via `./gradlew assembleProductionDebug`
- **DB:** v17 · **Branch:** `ios-parity-transport-build`
- CI build fix: Gradle now includes only `app/libs/libbox.aar`; stale `app.rcq.android` code and `rcqbox.aar` are moved to `reference/android-legacy` instead of being hidden by a custom source-set.

## Working ✅
- Registration + JWT auth + ECIES key generation (iOS-compat)
- Outgoing messages (Android → iPad confirmed working)
- Incoming messages (with 🔒 placeholder if decrypt fails); `/messages/queue` is re-fetched on startup and every WS `CONNECTED`/opened/pong event, and undecryptable queue rows are stored + ACKed instead of silently growing the queue.
- Background message catch-up is scheduled through WorkManager every 15 minutes on connected networks; app foreground return republishes presence and reconnects WS after background sleep.
- Contact sync works; if `/contacts` returns an empty list and `.Dev` lookup fails, Android inserts local `.Dev` fallback at current Dev UIN `911` so Chats/Contacts are not empty. Legacy local `.Dev` fallback `84048` is removed on sync.
- Contact sync also updates existing direct chat nicknames/avatars so renamed contacts do not stay stale in Chats.
- Contact and group member identity fields now include `status_message`, `identity_key`, `signing_key`, and `signal_identity_key`; group sync caches member nicknames/keys into contacts for chat display and group fan-out.
- Group sync is route/backend-sensitive: current device run still saw `/groups` timeouts, but `GroupRepository` now seeds/repairs mandatory local `RCQ Beta` id `21` before/after network sync so the group never disappears from Chats/Groups while the server route is unavailable.
- Chat list sync: client-only `/chats` is not called; chat rows are derived from Room rows and `/messages/queue`
- Main chats screen now uses an `InboxUiState`/`InboxMapper` hub model that combines chats, contacts, and groups. Groups such as `RCQ Beta` and default contacts such as `.Dev` can appear even before they have messages.
- Chat rows now restore from local messages/contact cache after queue sync, update last-message previews on incoming/offline/outgoing messages, expose archived chat count, and clear notification badges when a chat is opened.
- Miranda/Telegram polish pass is applied at compile level: chat list rows use 44dp round avatars, status overlays, unread badges, and dividers; chat headers show avatar/status/member subtitle; text bubbles show rounded Telegram-style surfaces, inline timestamps, reaction chips, and delivery ticks; contacts default to Miranda-style Online/Away/Offline/Groups sections.
- Chat UI iOS-parity controls: call button routes to CallScreen, group header opens GroupInfo, More menu supports group info/search/mute/clear, reactions use an emoji picker, forwarding uses a target picker, in-chat search overlay is available, and group pinned text can render above messages.
- Contact info route now uses Android `ContactInfoScreen` parity UI with avatar/status/UIN copy, Message/Call actions, block, and remove-contact actions.
- Group info UI exists with members, rename, leave/delete actions, pinned text display, and owner/admin settings bottom sheet for post policy plus pinned announcement updates. Group browse has a Join action.
- Settings has an online/away/dnd/invisible status picker plus iOS-parity navigation for Privacy, Notifications, Blocked Users, About RCQ, and My QR Code.
- Nearby users UI exists under Contacts and calls server `GET /nearby` after fine-location permission is granted.
- Add Contact request parity: Android sends `POST /contacts/request` with iOS-compatible `{ "to_uin": ... }`, refreshes contacts after success/duplicate, and maps HTTP 409 to a duplicate-contact error.
- WebSocket: connect, send, receive, reconnect with backoff; Android WS URL builds `wss://` safely after OkHttp `HttpUrl` construction
- WebSocket backend parity pass: call events now use server fields (`media`, `sdp`, `candidate`, `reason`), `call_ice` parsing, renegotiate events, account burn, audio-room roster/member/speaking/mesh events, typed outgoing call/room payloads, and `GroupUpdated` sync/delete handling.
- Final cleanup pass from `CODEX_FIX_PLAN.md` v3 is applied: WebSocketService exposes renegotiate send helpers, CallScreen receives `targetUin` through navigation instead of using `0`, audio room leave sends WS immediately and attempts REST cleanup, group browse join shows loading, and Settings status values remain lowercase server keys with `Busy / DND` UI label.
- Connection diagnostics: client-only screens are reported locally, relay probes are capped, and OkHttp debug logging is BASIC to avoid large-body stalls
- WebSocket envelope parity: `system`, `visit`, and control envelope kinds (`read`, `reaction`, `edit`, `delete`, `bounce`) coming in `MessageNew` are now parsed from decrypted payload and applied (message state updates, delete/bounce, reactions, edits, system notices) instead of being treated as generic messages.
- WebSocket `message_reaction` is typed as a reaction map; ChatRepository no longer reads removed/raw reaction fields.
- Root `CODEX_FIXPLAN.md` pass is applied: Coil media/avatar loading, recovery phrase copy, ContactInfo UIN lookup, UIN-to-nickname refresh, group member count migration, lifecycle/WorkManager sync, call full-screen/mini-bar, runtime call/location permissions, deep links, direct status drawables, story rings, AuthViewModel singleton DataStore usage, REST message endpoint compile guards, local edit/delete handling, reply original sender names, and notification badge numbers.
- Dark mode (persisted DataStore, applied at launch)
- JIMM retro mode toggle (status-grouped contacts when ON)
- JIMM/QIP flat UI: compact 50dp NavBar, compact typography, status-dot rows
- Startup connection probe mirrors iOS: direct `GET /health` before auto-bypass, clears stale auto sing-box when direct works
- Stealth: ProxyManager AUTO/MANUAL/OFF, sing-box persistence across restarts
- Message send parity: `/messages/sealed` and `/messages/group-sealed` responses now mirror iOS (`delivered/queued`) status semantics in local message state, with safe handling of empty response bodies
- Group sealed send refreshes empty/stale group membership before send, builds Signal sessions for each recipient, caches fetched signal identities, and emits one encrypted payload per non-self member.
- Embedded bypass is dual-engine on Android: sing-box remains for compatible relays; Xray-core executable handles VLESS Reality xHTTP. Local priority relays now prefer `relay-uk-google-vision` (VLESS Reality TCP Vision via sing-box) before the xHTTP USA relay. If sing-box validation fails, fallback now passes only Xray-compatible xHTTP relays to Xray; device log confirms fallback to `relay-usa-amd-xhttp` starts successfully. HTTP body-read timeouts now count toward AUTO failover.
- Sing-box hysteria2 config attempts now include both current obfs JSON and legacy v1.6-1.8 `obfs` / `obfs-password` fallback attempts per relay, so a single unsupported obfs format no longer prevents relay rotation.
- PanicPIN / Biometric unlock
- Delivery states SENT/DELIVERED/READ, typing indicators, presence
- Edit/delete/reactions, pin/mute/archive, message search

## Broken / Partial 🔴
| Issue | Root Cause |
|-------|-----------|
| Call UI needs WebRTC SDP flow validation | WS event/payload contract is aligned, but `CallManager` still needs real SDP offer/answer creation from `CallService` before live calls can be considered functional |
| UI still needs broad visual device validation | 2026-06-04 ADB UI tree confirmed Chats renders `RCQ Beta` and `.Dev` (`911`); still verify search, emoji picker, attachment sheet, relay picker, contact groups, bubble ticks, custom VLESS entry, and group detail flows |
| Group sync currently times out on live route | 2026-06-04 ADB run showed `/groups` timing out even after AUTO starts Xray fallback. Mandatory `RCQ Beta` is locally seeded, but the server route still needs transport/backend validation |
| Входящие DM иногда показывают 🔒 | Signal-сессия не установлена для нового собеседника |
| JIMM mode влияет только на контакты | Другие экраны не читают LocalRetroMode |
| P5 Звуки / P7 Смайлы JIMM | Не реализованы (0%) |

## Removed 🗑️
- `WebSocketManager.kt` — дублирующий WS движок
- `GameRepository`, `GamesScreen`, `MarketplaceScreen` — удалены по scope
- `PetEntity`, `PetDao` — pets table (DB migration 11→12)

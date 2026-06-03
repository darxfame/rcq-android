# Next Steps

> Immediate tasks. Updated at end of each session.

## iOS Parity — Active

### Done ✅
- [x] Delete WebSocketManager + WebSocketEvent, migrate CallManager to WsEvent
- [x] Delete Games/Marketplace/Pets code and DB entities
- [x] Create docs structure
- [x] Write AI context files
- [x] Fix GitHub `assembleProductionDebug` duplicate AAR failure
- [x] Create `IOS_ANDROID_PARITY.md`
- [x] Align HTTP/WS transport base URL and `X-RCQ-Auth` support with iOS
- [x] Align startup direct probe with iOS `GET /health`; prevent false auto-bypass on live `api.rcq.app`
- [x] Stop Android chat list from calling absent `/chats`; align chat list with client-side Room/queue model
- [x] Make diagnostics treat client-only screens locally and avoid large response BODY logging stalls
- [x] Add inbox hub UI model and wire main chats screen to chats + contacts + groups
- [x] Add priority Britain VLESS Reality TCP Vision relay and make engine selection respect relay priority before choosing Xray
- [x] Fix Add Contact request backend parity: `POST /contacts/request` with `{ "to_uin": ... }`, refresh after success/duplicate, and 409 duplicate mapping

### This Session
- [x] Implement one current-milestone Add Contact backend parity fix
- [x] Add focused JVM contract test for Add Contact request body and duplicate mapping
- [x] Audit UI/navigation parity against iOS views for the main inbox/chat surface
- [x] Fix direct/group message send status handling to follow iOS `delivered/queued` and avoid `response.body()!!` crashes
- [x] Audit WebSocket typed event parity against iOS `WebSocketService.swift`
- [x] Handle iOS envelope control scenarios from `MessageNew` (`system`, `visit`, `read`, `reaction`, `edit`, `delete`, `bounce`) in chat repository
- [x] Execute `CODEX_FIX_PLAN.md` blocks 1-9 for WS/call/audio-room/group backend parity
- [x] Validate `./gradlew compileProductionDebugKotlin` after each fix-plan block
- [x] Validate final `./gradlew test`
- [ ] Remove dead UI clicks/stubs for currently visible Android screens
- [ ] Add focused tests for WebSocket event parsing
- [ ] Wire `CallManager` to real `CallService` SDP offer/answer and ICE send flow, then device-validate Android↔iOS calls
- [ ] Validate `assembleProductionDebug` after UI/device-facing parity fixes
- [x] Run online ADB validation on connected Android device for registration direct path
- [x] Run online ADB validation for chat-list startup requests and logcat crash regression
- [x] Add local priority VLESS Reality xhttp relay above signed remote/cache relay configs
- [x] Add local priority Britain VLESS Reality TCP Vision relay above xHTTP fallback
- [x] Add explicit built-in relay selection and custom `vless://` Reality URL input before/after login
- [x] Add and validate process-based Xray-core engine for VLESS Reality xHTTP relay
- [ ] Visually verify main inbox renders synced `RCQ Beta` and `.Dev`
- [ ] Fix/force direct transport or working relay for live ADB validation; current device routes Add Contact search through broken `relay-do-fra-yandex-hy2`
- [ ] Re-run ADB validation for Add Contact success, duplicate 409, empty search, and backend error states
- [ ] ADB-validate Britain relay (`relay-uk-google-vision`) with contacts/groups/messages queue
- [ ] ADB-validate explicit relay selection and custom VLESS add/select flow

### This Week
- [ ] Align root app shell with iOS boot/PIN/privacy/call/audio-room overlay behavior
- [ ] Implement iOS-backed settings screens: privacy, notifications, sounds, blocked users, server picker, accounts, about/help
- [ ] Implement chat UI parity for message actions, media picker, albums, polls, link/location/voice/video/file bubbles, in-chat search
- [ ] Crypto/message envelope parity audit and tests
- [ ] Auth/account/recovery parity audit
- [ ] Message ordering: use server envelope `serverTime` everywhere
- [ ] Keep sing-box relay routing validation for non-xHTTP relays; Xray handles xHTTP

### Next Week
- [ ] MVI refactor: ChatsViewModel + ChatViewModel
- [ ] Design tokens: SpacingTokens, ColorTokens, TypographyTokens
- [ ] GitHub Actions CI

## Codex Execution Queue (2026-06-03)

Execute `docs/ai-context/CODEX_FIX_PLAN.md` blocks in order:
- [x] BLOCK 1 — WsEvent model: add sdp/media/reason fields, room events, AccountBurned
- [x] BLOCK 2 — WsEvent parser: fix call_ice name, fix call_offer media field, room parsers
- [x] BLOCK 3 — WebSocketOutgoingPayloads: add all call/room signals
- [x] BLOCK 4 — RCQApiService: remove dead REST, fix SealedMessageResponse, add group endpoints
- [x] BLOCK 5 — CallRepository: remove REST, use WS-only architecture
- [x] BLOCK 6 — AudioRoomRepository: add WS room_enter after REST join; WS room_leave
- [x] BLOCK 7 — ChatRepository: fix GroupUpdated WS handler to trigger DB sync
- [x] BLOCK 8 — GroupRepository: add joinGroup, getGroupPreview, deleteGroup
- [x] BLOCK 9 — Fix all compile errors from changed WsEvent fields

## Priority Rule

1. iOS behavioral parity for active Android surfaces first
2. Security-sensitive parity: crypto, auth, WebSocket, message ordering
3. Device validation via ADB before broad feature expansion

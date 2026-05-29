# RCQ Android — Architectural Analysis & Roadmap

> **Generated:** 2026-05-29 from a full comparison of this repo against the
> iOS reference client (`/tmp/rcq-ios`) and the authoritative server
> (`/tmp/rcq-server-ref`, FastAPI). **For the next session:** read
> "Source of Truth" + "Critical Findings" before touching any networking,
> calls, or feature code. The Android API layer was partly built against an
> assumed contract that does **not** match the real server.
>
> **Scope decision (user, 2026-05-29):** 🚫 **Games are out of scope — we will
> NOT implement games at all, ever.** Remove all game code; do not re-add.

---

## Source of Truth (priority order)

1. **`/tmp/rcq-server-ref`** (FastAPI) — the REAL contract. If an endpoint or
   WS event isn't here, it does not exist. Routers live in
   `app/routers/*.py`, mounted in `app/main.py:63-87`.
2. **`/tmp/rcq-ios`** — the reference *implementation* of that contract
   (Swift). Use it to see HOW each endpoint is called, payload shapes, and
   crypto/sealed-sender flow. Key files: `Services/APIClient.swift`,
   `Services/MessageService.swift` (1328 lines), `Services/WebSocketService.swift`,
   `Services/CryptoService.swift`, `Services/CallService.swift`,
   `Services/AudioRoomService.swift`.
3. This Android repo — the thing we are fixing.

### Authoritative server routers (`app/main.py`)
`auth · users · contacts · groups · messages · keys · media · nearby ·
presence · random · audio_rooms · hood · hood_banners · reports · polls ·
news · admin · stories · migrate · uin_shop · referrals · public · ws`

There is **NO** `/chats`, **NO** REST `/calls`, **NO** `games`, **NO**
`marketplace`, **NO** `pets` on the server.

---

## Critical Findings (evidence-based)

### 🔴 F1 — Android ships 3 fictional feature areas that exist nowhere
`games`, `marketplace`, `pets` have **no server router and no iOS code**.
They are hallucinated scope. **Games are permanently out of scope per the
user — delete, never re-add.** Marketplace/pets: also delete (no backend).
Android currently carries:
- API: `games/{type}/state|bet|cashout`, `marketplace`, `marketplace/{id}/bid|buy`, `pets`, `pets/{id}/equip|unequip` in `data/api/RCQApiService.kt`
- WS events: `crash_bet_placed`, `crash_started`, `crash_tick`, `crash_point`, `crash_ended`, `hilo_card_revealed`, `hilo_cashed_out`, `hilo_round_ended` (casino games) in `data/websocket/WebSocketService.kt`
- Models: `domain/model/Game.kt`, `Marketplace.kt`, `Pet.kt`, `PetEntity.kt`, `PetDao.kt`, `data/repository/GameRepository.kt`
- UI: `ui/games/GamesScreen.kt`, `ui/market/MarketplaceScreen.kt`
**Action:** delete all of the above. Add a Room migration if `PetEntity` is dropped from the schema. Remove the games/market tabs from `ui/common/BottomNavBar.kt` and nav graph.

### 🔴 F2 — Calls cannot work: wrong transport + wrong event names
Android calls REST `/calls/initiate`, `/calls/{id}/accept|decline|end` — **these endpoints do not exist on the server.** Real design: **WebRTC signaling over the WebSocket** + TURN creds via `GET /users/me/turn-credentials`.
- Server WS call events (`app/routers/ws.py`): `call_offer`, `call_answer`, `call_ice`, `call_end`, `call_renegotiate`, `call_renegotiate_answer`, `call_renegotiate_decline`.
- Android currently emits/handles: `call_incoming`, `call_accepted`, `call_ended`, `call_ice_candidate`, `call_upgrade`, `call_upgrade_answer` — **names don't match**, so nothing connects.
- iOS reference: `Services/CallService.swift`, `Services/WebRTCManager.swift`, `Services/CallProvider.swift`.

### 🔴 F3 — Audio rooms event vocabulary mismatch
Server (`ws.py`): `room_offer`, `room_answer`, `room_ice`, `room_enter`, `room_leave`, `room_roster`, `room_member_entered`, `room_member_left`, `room_speaking`, `room_enter_rejected`. REST under `/audio_rooms` (`/{room_id}/membership|kick|owner_only|rotate_key`, `/{room_id}/members/{uin}/mute`).
Android uses `audio_room_started|peer_joined|peer_left|ended` and REST `rooms`, `rooms/{id}/join|leave|mute|raise-hand`. **Full mismatch** → audio rooms are non-functional. iOS ref: `Services/AudioRoomService.swift`, `Services/AudioRoomMeshManager.swift`.

### 🟠 F4 — Account recovery is NOT backend-pending — it already exists
`TODO`/memory says "BACKEND PENDING". Server already ships (`app/routers/auth.py`):
- `POST /auth/key-backup` (line 222) — store PBKDF2-SHA256 (600k iters) encrypted key blob
- `POST /auth/recover/challenge` (line 244) — issue nonce
- `POST /auth/recover/verify` (line 270) — Ed25519 challenge-response
Android already has `crypto/MnemonicHelper.kt` and `ui/auth/AccountRecoveryScreen.kt`. **This is unblocked — wire it to the real endpoints.** (The `account-recovery-backend.patch` in `docs/superpowers/plans/` is likely redundant — verify against the live server before proposing it upstream.)

### 🟠 F5 — Messaging is on the right track but verify queue/ack loop
Server messaging contract: `POST /messages/sealed`, `POST /messages/group-sealed`, `GET /messages/queue`, `POST /messages/queue/ack`, `POST /messages/{id}/react`. No `/chats`. Android already moved to `/messages/sealed` (good). **Confirm** the `/messages/queue` → process → `/messages/queue/ack` pull loop matches iOS `MessageService.swift` exactly (envelope fields, ack batching). Inbound decrypt field naming bug is tracked below.

### 🟡 F6 — Entire real feature families missing on Android
Present on server + iOS, absent on Android: **hood** (neighborhood geo-chat: `/hood/send`, `/hood/messages`, `/hood/banners`), **nearby** (`/nearby/checkin`, `/nearby/list`), **news** (`/news/feed`), **random chat** (`/random/queue|leave|skip`), **polls** (`/polls`, group polls), **presence** (`/presence/status|online|online-count`), **reports** (`/reports`), **uin_shop** (`/uin/...`), **referrals** (`/referrals`). These define Phase 4+.

---

## Roadmap

> Method: each phase below should be turned into a detailed plan via
> `superpowers:writing-plans` before execution, and built with
> `superpowers:test-driven-development`. Server is the contract oracle;
> iOS is the implementation reference. Commit messages in Russian.

### Phase 0 — Contract hygiene (do FIRST, unblocks everything)
- [ ] Cross-check every entry in `data/api/RCQApiService.kt` against
      `/tmp/rcq-server-ref/app/routers/*.py`. Produce a mapping table:
      keep / rename / delete.
- [ ] **Delete games entirely (user: never implement):** API `games/*` methods,
      `domain/model/Game.kt`, `data/repository/GameRepository.kt`,
      `ui/games/GamesScreen.kt`, casino WS events (`crash_*`, `hilo_*`) from
      `WebSocketService.kt`, and the Games tab in `ui/common/BottomNavBar.kt` + nav graph.
- [ ] Delete marketplace + pets (no backend): API `marketplace/*` + `pets/*`,
      `Marketplace.kt`, `Pet.kt`, `PetEntity.kt`, `PetDao.kt`, `ui/market/MarketplaceScreen.kt`.
      Add a Room migration if `PetEntity` leaves the schema.
- [ ] Replace `/chats` REST assumptions everywhere with the sealed/queue model.
- [ ] Align WS event-name enums in `domain/model/WebSocketEvent.kt` +
      `data/websocket/WebSocketService.kt` to the server's exact strings
      (see F2/F3 lists). This is the single highest-leverage fix.

### Phase 1 — Core messaging hardening (finish what's started)
- [ ] **BUG (inbound decrypt fields):** handler reads `ciphertext`/`signal_type`;
      server sends `payload`/`envelope_type`. Map `envelope_type`:
      `"prekey_message"`→PREKEY_TYPE(3), else→WHISPER_TYPE(2). File:
      `data/repository/ChatRepository.kt` (~:244) + WS `MessageNew` handler.
- [ ] **BUG (notification deep-link):** `service/NotificationHelper.kt` must put
      `chat_id`/sender UIN in the `PendingIntent`; `ui/MainActivity.kt` must
      implement `onNewIntent()` and route via `ui/RCQApp.kt`.
- [ ] Verify `/messages/queue` pull + `/messages/queue/ack` loop vs iOS
      `MessageService.swift`; ensure offline backlog drains on launch.
- [ ] Message reactions via `POST /messages/{id}/react` (server) — align with
      iOS `ReactionInboxStore.swift`; confirm Android path/payload.
- [ ] Forward-message chat selector UI (currently a stub).

### Phase 2 — Account recovery (UNBLOCKED — see F4)
- [ ] Wire `MnemonicHelper.kt` + `AccountRecoveryScreen.kt` to
      `POST /auth/key-backup` after registration (encrypt Signal identity +
      keys with mnemonic-derived key, PBKDF2-SHA256 600k iters — match server).
- [ ] Implement recover flow: `POST /auth/recover/challenge` → sign nonce with
      Ed25519 identity key → `POST /auth/recover/verify` → restore session.
- [ ] Reference iOS: search for `recover`/`keyBackup` in
      `Services/AuthService.swift` + `Services/Account*.swift`.
- [ ] Re-validate `docs/superpowers/plans/account-recovery-backend.patch`
      against the live server; it may already be implemented upstream.

### Phase 3 — Calls & Audio rooms (rebuild on correct transport — F2/F3)
- [ ] Move call signaling off REST onto WS: implement `call_offer`/`call_answer`/
      `call_ice`/`call_end`/`call_renegotiate*` in `call/CallManager.kt` +
      `service/CallService.kt`, mirroring iOS `WebRTCManager.swift` +
      `CallService.swift`.
- [ ] Fetch TURN via `GET /users/me/turn-credentials`.
- [ ] Incoming-call notification + full-screen intent; respect server's
      concurrent-call busy guard (`reason=busy`).
- [ ] Rebuild audio rooms against `room_*` WS events + `/audio_rooms/*` REST,
      including key rotation (`/{room_id}/rotate_key`) and speaking indicators.

### Phase 4 — Missing social/discovery features (server-backed, see F6)
Build in this order (lowest crypto risk first):
- [ ] **Presence** — `/presence/status`, `online`, `online-count`; WS `presence`,
      `status_message`, `typing`. (Improves chat UX immediately.)
- [ ] **Stories** — finish upload/reply/view against `/stories/*` (Android UI
      exists: `ui/stories/*`). Match iOS `StoryService.swift`.
- [ ] **Polls** — `/polls`, `/polls/{id}/close`, group polls. iOS `PollService.swift`.
- [ ] **Hood (geo neighborhood chat)** — `/hood/send|messages|banners`; WS
      `hood_message`, `hood_reaction`, `hood_count`, `hood_delete` (Android WS
      already lists some hood events — good starting point). iOS:
      `Services/HoodChatService.swift`, `HoodBannerService.swift`, `Utils/Geohash.swift`.
- [ ] **Nearby** — `/nearby/checkin|list`. iOS `NearbyService.swift`.
- [ ] **Random chat** — `/random/queue|leave|skip`. iOS `RandomChatService.swift`.
- [ ] **News** — `/news/feed`. iOS `NewsService.swift`.
- [ ] **UIN shop / referrals** — `/uin/*`, `/referrals/*` (monetization; lowest priority).

### Phase 5 — Security & privacy parity (iOS has, Android lacks)
- [ ] PIN lock + Panic PIN — iOS `PINVault.swift`, `PanicPINService.swift`.
- [ ] Biometric unlock — iOS `BiometricUnlock.swift` → Android BiometricPrompt.
- [ ] Reports/safety — `/reports`, block/report UI. iOS `ReportContactSheet.swift`.
- [ ] SingBox/proxy transport parity for censorship circumvention — Android has
      `service/SingBoxTransport.kt`; verify against iOS `SingBoxTransport.swift`
      + `RelayConfigStore.swift` and the auto-proxy `/health` probe logic in
      `APIClient.swift:177-221`.

### Phase 6 — Design parity & polish
- [ ] iOS-matched theme/colors/typography (`ui/theme/*` vs iOS `Utils/Theme.swift`).
- [ ] Emoticon reactions picker + in-bubble display (iOS `Utils/Emoticons.swift`,
      `Views/Components/EmoticonText*.swift`).
- [ ] Notification + system sounds parity (iOS `Services/SoundService.swift`).

### Phase 7 — Testing (currently ~none)
- [ ] CryptoService E2EE round-trip unit tests (encrypt → store → decrypt).
- [ ] iOS-compat vector test: decrypt a sealed message produced by iOS flow.
- [ ] Room migration tests across the v6→v10 chain.
- [ ] WS event (de)serialization tests against captured server frames.

---

## Quick reference — endpoint reconciliation

| Android currently calls | Real server | Verdict |
|---|---|---|
| `chats`, `chats/{id}/messages` | `/messages/sealed` + `/messages/queue` | **Replace** |
| `calls/initiate|accept|decline|end` | WS `call_*` + `/users/me/turn-credentials` | **Replace (F2)** |
| `rooms`, `rooms/{id}/join|leave|mute` | `/audio_rooms/*` + WS `room_*` | **Rename+rework (F3)** |
| `games/*` | — (none) | **Delete — out of scope forever (F1)** |
| `marketplace/*`, `pets/*` | — (none) | **Delete (F1)** |
| `keys/{uin}/bundle` | `/keys/{uin}/bundle` ✓ | Keep |
| `messages/sealed`, `messages/group-sealed` | same ✓ | Keep |
| `contacts*`, `groups*`, `stories*`, `presence/status` | match server ✓ | Keep, verify payloads |
| (missing) | `/auth/key-backup`, `/auth/recover/*` | **Add (F4)** |
| (missing) | `/hood/*`, `/nearby/*`, `/news/*`, `/random/*`, `/polls/*` | **Add (F6)** |

---

## Status snapshot (verified working, do not redo)
- ✅ libsignal E2EE integration; Signal Protocol stores (`crypto/*`).
- ✅ Build green; DB migrations v6→v10; Hilt DI for all DAOs.
- ✅ GitHub Actions APK release.
- ✅ Auth: register, recovery-phrase gen/display, token via DataStore + interceptor, logout.
- ✅ Contacts: list/search/add/accept/decline/remove/block/unblock/nickname/favorite.
- ✅ Chats list, send/edit/delete/reply, E2EE send.
- ✅ Media (Phase 2): photo/video/file/voice/location send+receive, all E2EE.
- ✅ Settings/profile screens; privacy & notification toggles.

## Known open bugs (carry-over, also in Phase 1)
- [ ] Inbound WS decrypt field mismatch (`payload`/`envelope_type`) — `ChatRepository.kt:~244`.
- [ ] Notification deep-link missing (`NotificationHelper.kt` + `MainActivity.onNewIntent`).
- [ ] Forward-message chat selector UI is a stub.
- [ ] Groups: create + group-chat screen + member mgmt not end-to-end.

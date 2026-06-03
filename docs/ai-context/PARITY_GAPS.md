# Android ↔ iOS ↔ Server Parity Gaps

> Generated: 2026-06-03. Source: live comparison of darxfame/rcq-android (ios-parity-transport-build), rcq-messenger/rcq-ios (main), rcq-messenger/rcq-server-ref (main).

## 🔴 CRITICAL — breaks current milestone flow

### GAP-01 · `POST /contacts` — dead endpoint (server: non-existent)
- **Android:** `@POST("contacts")` with `AddContactRequest(userId, nickname?)`
- **Server:** No `POST /contacts`. Only `POST /contacts/request` and `POST /contacts/respond`.
- **iOS:** Only `POST /contacts/request` with `Body(to_uin:)`
- **Fix:** Delete `addContact()`, `AddContactRequest` from `RCQApiService.kt`. Dead code.
- **File:** `data/api/RCQApiService.kt:33-35`

### GAP-02 · `PATCH /groups/{id}` — Android sends `PUT`
- **Android:** `@PUT("groups/{id}")` with full `Group` body
- **Server:** `PATCH /groups/{id}` with `GroupPatchIn` (partial fields only)
- **Fix:** Change to `@PATCH`, create `GroupPatchRequest` DTO with nullable fields.
- **File:** `data/api/RCQApiService.kt:96`

### GAP-03 · `POST /groups/{id}/members` — body field name mismatch
- **Android:** `AddMemberRequest(userId: Long)` → JSON `{"userId": 123}`
- **Server:** Expects `{"uin": 123}` (`AddMemberIn.uin`)
- **iOS:** Sends `{"uin": ...}`
- **Fix:** Rename field `userId` → `uin` in `AddMemberRequest`.
- **File:** `data/api/RCQApiService.kt:109`

### GAP-04 · `SealedMessageResponse` — field `id` vs `server_time`
- **Android:** `SealedMessageResponse(delivered, queued, id: String = "")`
- **Server:** `SendOut(delivered: bool, queued: bool, server_time: datetime)`
- **Fix:** Replace `id: String` with `@SerialName("server_time") val serverTime: String = ""`.
- **File:** `data/api/RCQApiService.kt` — `SealedMessageResponse` class
- **Impact:** Android loses the server timestamp needed for message ordering (serverTime used for dedup of WS+queue merge).

### GAP-05 · `GroupMemberApi` — missing `status` field (live presence)
- **Android:** No `status` field
- **Server:** `GroupMemberOut.status: str = "offline"` (live, manager-backed)
- **iOS:** Uses `status` to show online indicators in group member list
- **Fix:** Add `val status: String = "offline"` to `GroupMemberApi`.
- **File:** `data/api/RCQApiService.kt` — `GroupMemberApi` class

## 🟡 MEDIUM — missing endpoints/features

### GAP-06 · `GET /keys/me/status` — not implemented on Android
- **Server:** Returns `{has_bundle, one_time_prekey_count, target_count, signed_prekey_age_seconds}`
- **iOS:** `SignalIdentityBootstrap` calls this to decide when to replenish OPK pool
- **Fix:** Add `@GET("keys/me/status")` endpoint + call from `CryptoService` after bundle upload, and periodically.
- **Impact:** OPK pool eventually runs out; new Signal sessions degrade to signed-prekey-only (weaker forward secrecy).

### GAP-07 · `POST /groups/{id}/join` — not implemented on Android
- **Server:** Self-join for open groups
- **Fix:** Add `@POST("groups/{id}/join")` to API service and wire into group browse flow.

### GAP-08 · `GET /groups/{id}/preview` — not implemented on Android
- **Server:** `GroupPreviewOut(id, name, description, member_count, is_closed, owner_uin, owner_nickname, avatar_media_id, avatar_media_key)`
- **Fix:** Add `@GET("groups/{id}/preview")` + `GroupPreviewResponse` DTO.

### GAP-09 · `GroupApiResponse` — missing server fields
- **Android:** Missing `pinned_at`, `pinned_by`, `avatar_media_id`, `avatar_media_key`
- **Server:** All present in `GroupOut`
- **Fix:** Add the four fields (nullable) to `GroupApiResponse`.
- **File:** `data/api/RCQApiService.kt` — `GroupApiResponse` class

## 🔵 LOW — non-blocking for current milestone

### GAP-10 · Auth Registration — missing `inviter_uin` and `invite`
- **Android:** `RegisterRequest(nickname, identity_key, signing_key)` only
- **Server:** Also accepts `inviter_uin: int | None`, `invite: str | None`
- **Impact:** Cannot register on invite-only servers. api.rcq.app is currently open.

### GAP-11 · WsEvent `contact_removed` — verify handling
- **Server:** Sends `{"type": "contact_removed", "peer_uin": int}` when contact deleted
- **iOS:** Handles → `ContactService.removeLocal()` (immediate list update)
- **Android:** Verify `WsEvent` handles `contact_removed`; if missing, contact list is stale until restart.
- **File:** `data/websocket/WsEvent.kt` — check for `contact_removed` case

### GAP-12 · `ContactRow` response — `gender` and `last_seen` not mapped
- **Server:** `ContactRow` includes `gender: str | None`, `last_seen: datetime | None`
- **Android:** `User` model likely missing these fields
- **Impact:** No "last seen" display, no gender icon.

### GAP-13 · `/contacts/block` — Android ignores toggle response `{"blocked": bool}`
- **Server:** Returns `{"blocked": bool}` (it's a toggle, not a set)
- **Android:** `Response<Unit>` — does not update local cache with actual state
- **iOS:** Reads `out.blocked` and patches local contact list
- **Fix:** Change return type to capture blocked state, update local cache.

## Fix Priority (current milestone: Add Contact → chat → send → receive)

```
Priority 1 (this session):
  GAP-01  Delete dead @POST("contacts") + AddContactRequest class
  GAP-04  SealedMessageResponse: id → server_time
  GAP-05  GroupMemberApi: add status field
  GAP-03  AddMemberRequest: userId → uin

Priority 2 (next session):
  GAP-11  Verify/add WsEvent contact_removed handling
  GAP-06  Add GET /keys/me/status + OPK replenish trigger
  GAP-09  GroupApiResponse: add pinned_at, pinned_by, avatar_media_id, avatar_media_key
  GAP-07  Add POST /groups/{id}/join
  GAP-08  Add GET /groups/{id}/preview
  GAP-02  PUT /groups → PATCH /groups (only if updateGroup is called)
```

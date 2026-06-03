# Codex Fix Plan v2 — Groups, Messaging, Calls Full Parity
# (replaces v1 — more comprehensive after deeper re-analysis)

> Branch: `ios-parity-transport-build`
> Source of truth: server (rcq-messenger/rcq-server-ref) and iOS (rcq-messenger/rcq-ios).
> After EVERY block of changes: `./gradlew compileProductionDebugKotlin`. Fix errors before next block.
> Do NOT change business logic. Do NOT add new UI features. Fix only what is specified.

---

## BLOCK 1 — Fix WsEvent model (add missing fields, fix wrong fields)

**File:** `app/src/main/java/com/rcq/messenger/data/websocket/WebSocketService.kt`

The `WsEvent` sealed class is missing fields required for WebRTC call signaling and audio rooms.
Also has wrong field names (server sends `media` not `call_type`; server sends `reason` on call_end).

### 1a. Fix CallOffer — add sdp and media fields
```kotlin
// BEFORE:
data class CallOffer(val callId: String, val fromUin: Long, val callType: String) : WsEvent()

// AFTER:
data class CallOffer(
    val callId: String,
    val fromUin: Long,
    val media: String,     // "audio" or "video" — server field name is "media"
    val sdp: String        // WebRTC SDP offer
) : WsEvent()
```

### 1b. Fix CallAnswer — add sdp field
```kotlin
// BEFORE:
data class CallAnswer(val callId: String, val fromUin: Long) : WsEvent()

// AFTER:
data class CallAnswer(val callId: String, val fromUin: Long, val sdp: String) : WsEvent()
```

### 1c. Fix CallEnd — add fromUin and reason fields
```kotlin
// BEFORE:
data class CallEnd(val callId: String) : WsEvent()

// AFTER:
data class CallEnd(val callId: String, val fromUin: Long, val reason: String) : WsEvent()
```

### 1d. Fix CallIceCandidate — add candidate field (server field name "candidate")
```kotlin
// BEFORE:
data class CallIceCandidate(val callId: String, val raw: JsonObject) : WsEvent()

// AFTER:
data class CallIceCandidate(
    val callId: String,
    val fromUin: Long,
    val candidate: String  // JSON string of ICE candidate
) : WsEvent()
```

### 1e. Add call renegotiate events (server supports call_renegotiate/call_renegotiate_answer/call_renegotiate_decline)
```kotlin
data class CallRenegotiate(val callId: String, val fromUin: Long, val sdp: String) : WsEvent()
data class CallRenegotiateAnswer(val callId: String, val fromUin: Long, val sdp: String) : WsEvent()
data class CallRenegotiateDecline(val callId: String, val fromUin: Long) : WsEvent()
```

### 1f. Fix GroupUpdated — carry full group JSON (server sends entire group object)
```kotlin
// BEFORE:
data class GroupUpdated(val groupId: String) : WsEvent()

// AFTER:
data class GroupUpdated(val groupId: String, val groupJson: JsonObject?) : WsEvent()
```

### 1g. Add proper Audio Room events matching server event names
Replace the simplified audio room events with proper ones:
```kotlin
// REMOVE these:
// data class AudioRoomStarted(val roomId: String) : WsEvent()
// data class AudioRoomPeerJoined(val roomId: String, val userId: Long) : WsEvent()
// data class AudioRoomPeerLeft(val roomId: String, val userId: Long) : WsEvent()
// data class AudioRoomEnded(val roomId: String) : WsEvent()

// ADD these (matching server event payloads from ws.py):
data class RoomRoster(
    val roomId: Int,
    val members: List<RoomMember>,
    val ownerOnlySpeaking: Boolean
) : WsEvent()
data class RoomMemberEntered(
    val roomId: Int, val uin: Int, val nickname: String, val mutedByOwner: Boolean
) : WsEvent()
data class RoomMemberLeft(val roomId: Int, val uin: Int) : WsEvent()
data class RoomEnterRejected(val roomId: Int, val reason: String) : WsEvent()
data class RoomSpeaking(val roomId: Int, val uin: Int, val speaking: Boolean) : WsEvent()
data class RoomKicked(val roomId: Int, val reason: String) : WsEvent()
data class RoomDeleted(val roomId: Int) : WsEvent()
data class RoomMemberMuted(val roomId: Int, val uin: Int, val mutedByOwner: Boolean) : WsEvent()

// Keep for WebRTC mesh signaling:
data class RoomOffer(val roomId: Int, val fromUin: Int, val sdp: String) : WsEvent()
data class RoomAnswer(val roomId: Int, val fromUin: Int, val sdp: String) : WsEvent()
data class RoomIce(val roomId: Int, val fromUin: Int, val candidate: String) : WsEvent()

// Helper data class:
data class RoomMember(val uin: Int, val nickname: String, val mutedByOwner: Boolean)
```

### 1h. Add AccountBurned event
```kotlin
object AccountBurned : WsEvent()
```

---

## BLOCK 2 — Fix WsEvent parser (parseEvent function in WebSocketService.kt)

Update `parseEvent()` to match server's actual event names and field names.
Server WS event types from ws.py and WebSocketService.swift:

### 2a. Fix call_offer parser
```kotlin
// BEFORE:
"call_offer", "call_incoming" -> WsEvent.CallOffer(
    callId = obj["call_id"]?.jsonPrimitive?.contentOrNull ?: "",
    fromUin = obj["from_uin"]?.jsonPrimitive?.longOrNull ?: 0,
    callType = obj["call_type"]?.jsonPrimitive?.contentOrNull ?: "audio"
)

// AFTER:
"call_offer" -> WsEvent.CallOffer(
    callId = obj["call_id"]?.jsonPrimitive?.contentOrNull ?: "",
    fromUin = obj["from_uin"]?.jsonPrimitive?.longOrNull ?: 0,
    media = obj["media"]?.jsonPrimitive?.contentOrNull ?: "audio",
    sdp = obj["sdp"]?.jsonPrimitive?.contentOrNull ?: ""
)
```

### 2b. Fix call_answer parser
```kotlin
"call_answer" -> WsEvent.CallAnswer(
    callId = obj["call_id"]?.jsonPrimitive?.contentOrNull ?: "",
    fromUin = obj["from_uin"]?.jsonPrimitive?.longOrNull ?: 0,
    sdp = obj["sdp"]?.jsonPrimitive?.contentOrNull ?: ""
)
```

### 2c. Fix call_ice parser (server sends "call_ice" not "call_ice_candidate")
```kotlin
// BEFORE: "call_ice_candidate" -> ...
// AFTER:
"call_ice" -> WsEvent.CallIceCandidate(
    callId = obj["call_id"]?.jsonPrimitive?.contentOrNull ?: "",
    fromUin = obj["from_uin"]?.jsonPrimitive?.longOrNull ?: 0,
    candidate = obj["candidate"]?.jsonPrimitive?.contentOrNull ?: ""
)
```

### 2d. Fix call_end parser
```kotlin
"call_end" -> WsEvent.CallEnd(
    callId = obj["call_id"]?.jsonPrimitive?.contentOrNull ?: "",
    fromUin = obj["from_uin"]?.jsonPrimitive?.longOrNull ?: 0,
    reason = obj["reason"]?.jsonPrimitive?.contentOrNull ?: "ended"
)
```

### 2e. Add renegotiate parsers
```kotlin
"call_renegotiate" -> WsEvent.CallRenegotiate(
    callId = obj["call_id"]?.jsonPrimitive?.contentOrNull ?: "",
    fromUin = obj["from_uin"]?.jsonPrimitive?.longOrNull ?: 0,
    sdp = obj["sdp"]?.jsonPrimitive?.contentOrNull ?: ""
)
"call_renegotiate_answer" -> WsEvent.CallRenegotiateAnswer(
    callId = obj["call_id"]?.jsonPrimitive?.contentOrNull ?: "",
    fromUin = obj["from_uin"]?.jsonPrimitive?.longOrNull ?: 0,
    sdp = obj["sdp"]?.jsonPrimitive?.contentOrNull ?: ""
)
"call_renegotiate_decline" -> WsEvent.CallRenegotiateDecline(
    callId = obj["call_id"]?.jsonPrimitive?.contentOrNull ?: "",
    fromUin = obj["from_uin"]?.jsonPrimitive?.longOrNull ?: 0
)
```

### 2f. Fix group event parsers — pass full groupJson
```kotlin
"group_created", "group_membership_changed" -> WsEvent.GroupUpdated(
    groupId = obj["group"]?.jsonObject?.get("id")?.jsonPrimitive?.contentOrNull
        ?: obj["group_id"]?.jsonPrimitive?.contentOrNull ?: "",
    groupJson = obj["group"]?.jsonObject
)
"group_deleted" -> WsEvent.GroupDeleted(
    groupId = obj["group_id"]?.jsonPrimitive?.contentOrNull ?: ""
)
```

### 2g. Fix audio room parsers (use real server event names)
```kotlin
// Server sends room_roster after room_enter WS message from client
"room_roster" -> {
    val membersArray = obj["members"]?.jsonArray ?: JsonArray(emptyList())
    val members = membersArray.mapNotNull { elem ->
        val m = elem.jsonObject
        val uin = m["uin"]?.jsonPrimitive?.intOrNull ?: return@mapNotNull null
        WsEvent.RoomMember(
            uin = uin,
            nickname = m["nickname"]?.jsonPrimitive?.contentOrNull ?: uin.toString(),
            mutedByOwner = m["muted_by_owner"]?.jsonPrimitive?.booleanOrNull ?: false
        )
    }
    WsEvent.RoomRoster(
        roomId = obj["room_id"]?.jsonPrimitive?.intOrNull ?: 0,
        members = members,
        ownerOnlySpeaking = obj["owner_only_speaking"]?.jsonPrimitive?.booleanOrNull ?: false
    )
}
"room_member_entered" -> {
    val m = obj["member"]?.jsonObject ?: JsonObject(emptyMap())
    WsEvent.RoomMemberEntered(
        roomId = obj["room_id"]?.jsonPrimitive?.intOrNull ?: 0,
        uin = m["uin"]?.jsonPrimitive?.intOrNull ?: 0,
        nickname = m["nickname"]?.jsonPrimitive?.contentOrNull ?: "",
        mutedByOwner = m["muted_by_owner"]?.jsonPrimitive?.booleanOrNull ?: false
    )
}
"room_member_left" -> WsEvent.RoomMemberLeft(
    roomId = obj["room_id"]?.jsonPrimitive?.intOrNull ?: 0,
    uin = obj["uin"]?.jsonPrimitive?.intOrNull ?: 0
)
"room_enter_rejected" -> WsEvent.RoomEnterRejected(
    roomId = obj["room_id"]?.jsonPrimitive?.intOrNull ?: 0,
    reason = obj["reason"]?.jsonPrimitive?.contentOrNull ?: "rejected"
)
"room_speaking" -> WsEvent.RoomSpeaking(
    roomId = obj["room_id"]?.jsonPrimitive?.intOrNull ?: 0,
    uin = obj["uin"]?.jsonPrimitive?.intOrNull ?: 0,
    speaking = obj["speaking"]?.jsonPrimitive?.booleanOrNull ?: false
)
"audio_room_kicked" -> WsEvent.RoomKicked(
    roomId = obj["room_id"]?.jsonPrimitive?.intOrNull ?: 0,
    reason = obj["reason"]?.jsonPrimitive?.contentOrNull ?: "kicked"
)
"audio_room_deleted" -> WsEvent.RoomDeleted(
    roomId = obj["room_id"]?.jsonPrimitive?.intOrNull ?: 0
)
"audio_room_member_muted" -> WsEvent.RoomMemberMuted(
    roomId = obj["room_id"]?.jsonPrimitive?.intOrNull ?: 0,
    uin = obj["uin"]?.jsonPrimitive?.intOrNull ?: 0,
    mutedByOwner = obj["muted_by_owner"]?.jsonPrimitive?.booleanOrNull ?: false
)
"room_offer" -> WsEvent.RoomOffer(
    roomId = obj["room_id"]?.jsonPrimitive?.intOrNull ?: 0,
    fromUin = obj["from_uin"]?.jsonPrimitive?.intOrNull ?: 0,
    sdp = obj["sdp"]?.jsonPrimitive?.contentOrNull ?: ""
)
"room_answer" -> WsEvent.RoomAnswer(
    roomId = obj["room_id"]?.jsonPrimitive?.intOrNull ?: 0,
    fromUin = obj["from_uin"]?.jsonPrimitive?.intOrNull ?: 0,
    sdp = obj["sdp"]?.jsonPrimitive?.contentOrNull ?: ""
)
"room_ice" -> WsEvent.RoomIce(
    roomId = obj["room_id"]?.jsonPrimitive?.intOrNull ?: 0,
    fromUin = obj["from_uin"]?.jsonPrimitive?.intOrNull ?: 0,
    candidate = obj["candidate"]?.jsonPrimitive?.contentOrNull ?: ""
)
```

### 2h. Add account_burned parser
```kotlin
"account_burned" -> WsEvent.AccountBurned
```

### 2i. Fix typing parser (server sends from_uin + active, no chat_id, no stopping event)
```kotlin
// BEFORE:
"typing_started", "typing" -> WsEvent.TypingStarted(
    chatId = obj["chat_id"]?.jsonPrimitive?.contentOrNull ?: "",
    userId = obj["from_uin"]?.jsonPrimitive?.longOrNull ?: 0
)
"typing_stopped" -> WsEvent.TypingStopped(...)

// AFTER (server only sends "typing" with from_uin + active bool):
"typing" -> {
    val fromUin = obj["from_uin"]?.jsonPrimitive?.longOrNull ?: 0
    val active = obj["active"]?.jsonPrimitive?.booleanOrNull ?: true
    if (active) WsEvent.TypingStarted(chatId = "direct_$fromUin", userId = fromUin)
    else WsEvent.TypingStopped(chatId = "direct_$fromUin", userId = fromUin)
}
```

### 2j. Handle "pong" silently (don't emit as Unknown)
Add `"pong" -> WsEvent.Unknown("pong", obj)` OR just return a no-op:
```kotlin
"pong" -> return@try WsEvent.Unknown("pong", obj) // already handled by watchdog reset
```

---

## BLOCK 3 — Add all WebSocket outgoing payloads

**File:** `app/src/main/java/com/rcq/messenger/data/websocket/WebSocketOutgoingPayloads.kt`

Replace the entire file content with this (adds all missing outgoing message builders):

```kotlin
package com.rcq.messenger.data.websocket

import kotlinx.serialization.json.*

object WebSocketOutgoingPayloads {
    private val json = Json { encodeDefaults = true }

    private fun obj(vararg pairs: Pair<String, JsonElement>): String =
        JsonObject(mapOf(*pairs)).toString()

    // Heartbeat — server derives online state from last_seen refresh on each ping
    fun ping(): String = obj("type" to JsonPrimitive("ping"))

    // Typing indicator — server relays as {"type":"typing","from_uin":int,"active":bool}
    fun typing(toUin: Long, active: Boolean): String = obj(
        "type" to JsonPrimitive("typing"),
        "to_uin" to JsonPrimitive(toUin),
        "active" to JsonPrimitive(active)
    )

    // ── Call signaling (WebRTC, server is a dumb relay via ws.py) ──────────────

    fun callOffer(toUin: Long, callId: String, sdp: String, media: String = "audio"): String = obj(
        "type" to JsonPrimitive("call_offer"),
        "to_uin" to JsonPrimitive(toUin),
        "call_id" to JsonPrimitive(callId),
        "sdp" to JsonPrimitive(sdp),
        "media" to JsonPrimitive(media)
    )

    fun callAnswer(toUin: Long, callId: String, sdp: String): String = obj(
        "type" to JsonPrimitive("call_answer"),
        "to_uin" to JsonPrimitive(toUin),
        "call_id" to JsonPrimitive(callId),
        "sdp" to JsonPrimitive(sdp)
    )

    fun callIce(toUin: Long, callId: String, candidate: String): String = obj(
        "type" to JsonPrimitive("call_ice"),
        "to_uin" to JsonPrimitive(toUin),
        "call_id" to JsonPrimitive(callId),
        "candidate" to JsonPrimitive(candidate)
    )

    fun callEnd(toUin: Long, callId: String, reason: String = "user_ended"): String = obj(
        "type" to JsonPrimitive("call_end"),
        "to_uin" to JsonPrimitive(toUin),
        "call_id" to JsonPrimitive(callId),
        "reason" to JsonPrimitive(reason)
    )

    fun callRenegotiate(toUin: Long, callId: String, sdp: String): String = obj(
        "type" to JsonPrimitive("call_renegotiate"),
        "to_uin" to JsonPrimitive(toUin),
        "call_id" to JsonPrimitive(callId),
        "sdp" to JsonPrimitive(sdp)
    )

    fun callRenegotiateAnswer(toUin: Long, callId: String, sdp: String): String = obj(
        "type" to JsonPrimitive("call_renegotiate_answer"),
        "to_uin" to JsonPrimitive(toUin),
        "call_id" to JsonPrimitive(callId),
        "sdp" to JsonPrimitive(sdp)
    )

    fun callRenegotiateDecline(toUin: Long, callId: String): String = obj(
        "type" to JsonPrimitive("call_renegotiate_decline"),
        "to_uin" to JsonPrimitive(toUin),
        "call_id" to JsonPrimitive(callId)
    )

    // ── Audio Room signaling (server relay via ws.py) ──────────────────────────

    // Sent after REST POST /rooms/{id}/join to actually enter the real-time room
    fun roomEnter(roomId: Int): String = obj(
        "type" to JsonPrimitive("room_enter"),
        "room_id" to JsonPrimitive(roomId)
    )

    fun roomLeave(roomId: Int): String = obj(
        "type" to JsonPrimitive("room_leave"),
        "room_id" to JsonPrimitive(roomId)
    )

    fun roomOffer(roomId: Int, toUin: Int, sdp: String): String = obj(
        "type" to JsonPrimitive("room_offer"),
        "room_id" to JsonPrimitive(roomId),
        "to_uin" to JsonPrimitive(toUin),
        "sdp" to JsonPrimitive(sdp)
    )

    fun roomAnswer(roomId: Int, toUin: Int, sdp: String): String = obj(
        "type" to JsonPrimitive("room_answer"),
        "room_id" to JsonPrimitive(roomId),
        "to_uin" to JsonPrimitive(toUin),
        "sdp" to JsonPrimitive(sdp)
    )

    fun roomIce(roomId: Int, toUin: Int, candidate: String): String = obj(
        "type" to JsonPrimitive("room_ice"),
        "room_id" to JsonPrimitive(roomId),
        "to_uin" to JsonPrimitive(toUin),
        "candidate" to JsonPrimitive(candidate)
    )

    fun roomSpeaking(roomId: Int, speaking: Boolean): String = obj(
        "type" to JsonPrimitive("room_speaking"),
        "room_id" to JsonPrimitive(roomId),
        "speaking" to JsonPrimitive(speaking)
    )
}
```

Also add these helper methods to `WebSocketService`:
```kotlin
fun sendPing(): Boolean = sendMessage(WebSocketOutgoingPayloads.ping())
fun sendCallOffer(toUin: Long, callId: String, sdp: String, media: String = "audio") =
    sendMessage(WebSocketOutgoingPayloads.callOffer(toUin, callId, sdp, media))
fun sendCallAnswer(toUin: Long, callId: String, sdp: String) =
    sendMessage(WebSocketOutgoingPayloads.callAnswer(toUin, callId, sdp))
fun sendCallIce(toUin: Long, callId: String, candidate: String) =
    sendMessage(WebSocketOutgoingPayloads.callIce(toUin, callId, candidate))
fun sendCallEnd(toUin: Long, callId: String, reason: String = "user_ended") =
    sendMessage(WebSocketOutgoingPayloads.callEnd(toUin, callId, reason))
fun sendRoomEnter(roomId: Int) = sendMessage(WebSocketOutgoingPayloads.roomEnter(roomId))
fun sendRoomLeave(roomId: Int) = sendMessage(WebSocketOutgoingPayloads.roomLeave(roomId))
fun sendRoomOffer(roomId: Int, toUin: Int, sdp: String) =
    sendMessage(WebSocketOutgoingPayloads.roomOffer(roomId, toUin, sdp))
fun sendRoomAnswer(roomId: Int, toUin: Int, sdp: String) =
    sendMessage(WebSocketOutgoingPayloads.roomAnswer(roomId, toUin, sdp))
fun sendRoomIce(roomId: Int, toUin: Int, candidate: String) =
    sendMessage(WebSocketOutgoingPayloads.roomIce(roomId, toUin, candidate))
fun sendRoomSpeaking(roomId: Int, speaking: Boolean) =
    sendMessage(WebSocketOutgoingPayloads.roomSpeaking(roomId, speaking))
```

---

## BLOCK 4 — Fix RCQApiService: remove non-existent call REST endpoints, fix API service

**File:** `app/src/main/java/com/rcq/messenger/data/api/RCQApiService.kt`

### 4a. REMOVE these non-existent endpoints (server has no call REST API — calls are WS only):
```kotlin
// DELETE these completely:
@GET("calls")
suspend fun getCallHistory(): Response<CallLog>

@POST("calls/initiate")
suspend fun initiateCall(@Body request: InitiateCallRequest): Response<Call>

@POST("calls/{id}/accept")
suspend fun acceptCall(@Path("id") callId: String): Response<Call>

@POST("calls/{id}/decline")
suspend fun declineCall(@Path("id") callId: String): Response<Unit>

@POST("calls/{id}/end")
suspend fun endCall(@Path("id") callId: String): Response<Unit>
```

### 4b. Remove dead endpoint:
```kotlin
// DELETE: POST /contacts — server does not have this endpoint
@POST("contacts")
suspend fun addContact(@Body request: AddContactRequest): Response<Contact>
```

### 4c. Remove dead data classes:
```kotlin
// DELETE: AddContactRequest (no server endpoint uses it)
// DELETE: InitiateCallRequest (no server endpoint uses it)
// DELETE: CallLog — was used only by deleted getCallHistory()
```

### 4d. Fix SealedMessageResponse: id → server_time
```kotlin
// BEFORE:
data class SealedMessageResponse(
    val delivered: Boolean = false,
    val queued: Boolean = false,
    val id: String = ""
)

// AFTER:
data class SealedMessageResponse(
    val delivered: Boolean = false,
    val queued: Boolean = false,
    @kotlinx.serialization.SerialName("server_time") val serverTime: String = ""
)
```

### 4e. Fix GroupMemberApi — add status field
```kotlin
data class GroupMemberApi(
    val uin: Int,
    val nickname: String,
    val role: String = "member",
    val status: String = "offline",   // add this
    @kotlinx.serialization.SerialName("identity_key") val identityKey: String = "",
    @kotlinx.serialization.SerialName("signing_key") val signingKey: String = "",
    @kotlinx.serialization.SerialName("signal_identity_key") val signalIdentityKey: String? = null
)
```

### 4f. Fix AddMemberRequest — userId → uin
```kotlin
// BEFORE:
data class AddMemberRequest(val userId: Long)

// AFTER:
data class AddMemberRequest(val uin: Long)
```

### 4g. Add missing group endpoints
```kotlin
@GET("groups/{id}/preview")
suspend fun getGroupPreview(@Path("id") groupId: Int): Response<GroupPreviewResponse>

@POST("groups/{id}/join")
suspend fun joinGroup(@Path("id") groupId: Int): Response<GroupApiResponse>

@DELETE("groups/{id}")
suspend fun deleteGroup(@Path("id") groupId: String): Response<Unit>
```

### 4h. Add missing DTOs
```kotlin
@kotlinx.serialization.Serializable
data class GroupPreviewResponse(
    val id: Int,
    val name: String,
    val description: String? = null,
    @kotlinx.serialization.SerialName("member_count") val memberCount: Int,
    @kotlinx.serialization.SerialName("is_closed") val isClosed: Boolean = false,
    @kotlinx.serialization.SerialName("owner_uin") val ownerUin: Int,
    @kotlinx.serialization.SerialName("owner_nickname") val ownerNickname: String? = null,
    @kotlinx.serialization.SerialName("avatar_media_id") val avatarMediaId: String? = null,
    @kotlinx.serialization.SerialName("avatar_media_key") val avatarMediaKey: String? = null
)

@kotlinx.serialization.Serializable
data class KeyStatusResponse(
    @kotlinx.serialization.SerialName("has_bundle") val hasBundle: Boolean,
    @kotlinx.serialization.SerialName("one_time_prekey_count") val oneTimePreKeyCount: Int,
    @kotlinx.serialization.SerialName("target_count") val targetCount: Int,
    @kotlinx.serialization.SerialName("signed_prekey_age_seconds") val signedPreKeyAgeSeconds: Int? = null
)

@kotlinx.serialization.Serializable
data class GroupPatchRequest(
    val name: String? = null,
    val description: String? = null,
    @kotlinx.serialization.SerialName("post_policy") val postPolicy: String? = null,
    @kotlinx.serialization.SerialName("is_closed") val isClosed: Boolean? = null,
    @kotlinx.serialization.SerialName("members_hidden") val membersHidden: Boolean? = null,
    @kotlinx.serialization.SerialName("pinned_text") val pinnedText: String? = null,
    @kotlinx.serialization.SerialName("avatar_media_id") val avatarMediaId: String? = null,
    @kotlinx.serialization.SerialName("avatar_media_key") val avatarMediaKey: String? = null
)
```

### 4i. Add missing API endpoints
```kotlin
import retrofit2.http.PATCH

@GET("keys/me/status")
suspend fun getKeyStatus(): Response<KeyStatusResponse>

@PATCH("groups/{id}")
suspend fun patchGroup(@Path("id") groupId: String, @Body patch: GroupPatchRequest): Response<GroupApiResponse>
```

### 4j. Add missing GroupApiResponse fields
```kotlin
// Add to GroupApiResponse data class (all nullable, all default null):
@kotlinx.serialization.SerialName("pinned_at") val pinnedAt: String? = null,
@kotlinx.serialization.SerialName("pinned_by") val pinnedBy: Int? = null,
@kotlinx.serialization.SerialName("avatar_media_id") val avatarMediaId: String? = null,
@kotlinx.serialization.SerialName("avatar_media_key") val avatarMediaKey: String? = null,
```

---

## BLOCK 5 — Fix CallRepository (calls are WS-only, no REST)

**File:** `app/src/main/java/com/rcq/messenger/data/repository/GroupRepository.kt`
(CallRepository is in this file)

The server has NO REST endpoints for calls. Calls are signaled entirely through WebSocket.
Rewrite CallRepository to be WS-based:

```kotlin
@Singleton
class CallRepository @Inject constructor(
    private val callDao: CallDao
) {
    fun getCalls(limit: Int = 50): Flow<List<Call>> = callDao.getCalls(limit).map { entities ->
        entities.map { it.toDomain() }
    }

    fun getMissedCalls(): Flow<List<Call>> = callDao.getMissedCalls().map { entities ->
        entities.map { it.toDomain() }
    }

    // All call actions go through WebSocket — use WebSocketService.sendCallOffer() etc.
    // Call history is local-only (persisted when WS events arrive in CallManager).

    suspend fun recordCallStarted(callId: String, targetUin: Long, type: CallType): Unit {
        callDao.insertCall(CallEntity(
            id = callId,
            type = type.name,
            status = CallStatus.ONGOING.name,
            participantIds = listOf(targetUin),
            initiatorId = targetUin,
            startTime = System.currentTimeMillis(),
            endTime = null,
            duration = null,
            isGroupCall = false
        ))
    }

    suspend fun recordCallEnded(callId: String, reason: String): Unit {
        val now = System.currentTimeMillis()
        callDao.endCall(callId, now, reason)
    }
}
```

If `callDao.endCall()` doesn't exist, add it to `CallDao.kt`:
```kotlin
@Query("UPDATE calls SET status = 'ENDED', end_time = :endTime WHERE id = :callId")
suspend fun endCall(callId: String, endTime: Long, reason: String = "ended")
```

---

## BLOCK 6 — Fix AudioRoomRepository: add WS room_enter after REST join

**File:** `app/src/main/java/com/rcq/messenger/data/repository/GroupRepository.kt`
(AudioRoomRepository is in this file)

Audio rooms have TWO steps to join:
1. REST `POST /rooms/{id}/join` — creates DB membership (already exists)
2. WS `room_enter` — adds to Redis roster and gets back `room_roster` event

Also, `leave` must be a WS message, not REST.

Add `WebSocketService` injection and fix join/leave:

```kotlin
@Singleton
class AudioRoomRepository @Inject constructor(
    private val api: RCQApiService,
    private val webSocketService: WebSocketService  // ADD THIS INJECTION
) {
    suspend fun joinRoom(roomId: String): Result<AudioRoom> = runCatching {
        // Step 1: REST join (creates DB membership)
        val response = api.joinRoom(roomId)
        if (!response.isSuccessful) throw Exception("Failed to join room: ${response.code()}")
        val room = response.body()!!
        // Step 2: WS room_enter (adds to Redis roster, gets room_roster back)
        webSocketService.sendRoomEnter(roomId.toInt())
        room
    }

    suspend fun leaveRoom(roomId: String): Result<Unit> = runCatching {
        // WS room_leave (removes from Redis roster)
        webSocketService.sendRoomLeave(roomId.toInt())
        // Also REST leave to remove DB membership
        api.leaveRoom(roomId).let { if (!it.isSuccessful) throw Exception("REST leave failed") }
    }

    // Keep getAudioRooms, createRoom, getRoom, toggleMute, raiseHand as-is
}
```

---

## BLOCK 7 — Fix GroupRepository: update GroupUpdated WS handler

**File:** `app/src/main/java/com/rcq/messenger/data/repository/ChatRepository.kt`

In the `init { webSocketService.events.onEach { event -> when (event) { ... } } }` block,
fix the `GroupUpdated` handler to actually refresh the group from the bundled JSON:

```kotlin
is WsEvent.GroupUpdated -> {
    // Server includes the full group object in the WS event
    // Trigger a group sync so the local DB stays in sync
    scope.launch {
        try {
            val groupId = event.groupId
            if (groupId.isNotEmpty()) {
                val response = api.getGroup(groupId)   // GET /groups/{id}
                if (response.isSuccessful) {
                    val group = response.body()
                    if (group != null) {
                        groupDao.insertGroup(group.toGroupEntity())
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("ChatRepository", "GroupUpdated sync failed: ${e.message}")
        }
    }
}
```

Also add handler for `GroupDeleted`:
```kotlin
is WsEvent.GroupDeleted -> {
    scope.launch {
        try {
            groupDao.deleteGroup(event.groupId)
        } catch (e: Exception) {
            Log.e("ChatRepository", "GroupDeleted handler failed: ${e.message}")
        }
    }
}
```

---

## BLOCK 8 — Fix GroupRepository: add missing methods

**File:** `app/src/main/java/com/rcq/messenger/data/repository/GroupRepository.kt`

### 8a. Add joinGroup (POST /groups/{id}/join)
```kotlin
suspend fun joinGroup(groupId: Int): Result<Group> = runCatching {
    val response = api.joinGroup(groupId)
    if (response.isSuccessful) {
        val group = response.body()!!
        groupDao.insertGroup(group.toGroupEntity())
        group.toGroupEntity().toDomain()
    } else throw Exception("joinGroup failed: ${response.code()}")
}
```

### 8b. Add getGroupPreview (GET /groups/{id}/preview)
```kotlin
suspend fun getGroupPreview(groupId: Int): Result<GroupPreviewResponse> = runCatching {
    val response = api.getGroupPreview(groupId)
    if (response.isSuccessful) response.body()!!
    else throw Exception("getGroupPreview failed: ${response.code()}")
}
```

### 8c. Add deleteGroup (DELETE /groups/{id})
```kotlin
suspend fun deleteGroup(groupId: String): Result<Unit> = runCatching {
    val response = api.deleteGroup(groupId)
    if (response.isSuccessful) {
        groupDao.deleteGroup(groupId)
    } else throw Exception("deleteGroup failed: ${response.code()}")
}
```

### 8d. Fix addMember call site to use uin= instead of userId=
```kotlin
// BEFORE:
api.addMember(groupId, AddMemberRequest(uin = userId))
// This is already correct IF AddMemberRequest was fixed in BLOCK 4f
```

---

## BLOCK 9 — Fix compile errors from changed WsEvent fields

After changing WsEvent data classes in BLOCK 1, find ALL uses of old fields and fix them:

1. `WsEvent.CallOffer`: any code reading `.callType` → change to `.media`
   Also add `.sdp` where needed (e.g., in `CallManager.kt`)

2. `WsEvent.CallAnswer`: add `.sdp` extraction where needed

3. `WsEvent.CallEnd`: any code reading just `.callId` needs to also handle `.fromUin` and `.reason`

4. `WsEvent.CallIceCandidate`: any code reading `.raw["candidate"]` → change to `.candidate`

5. `WsEvent.GroupUpdated`: code that just reads `.groupId` is OK, but now `.groupJson` is also available

6. Audio room events: code using old `AudioRoomStarted/PeerJoined/PeerLeft/Ended` → update to new event names

Search for all usages: `grep -r "WsEvent\." app/src/main/`

---

## FINAL VERIFICATION

```bash
./gradlew compileProductionDebugKotlin
./gradlew test
```

Then commit:
```bash
git add -u
git commit -m "Android: полный паритет WS-событий, звонков и групп — fix call/room signals, WsEvent fields, outgoing payloads, dead REST endpoints"
git push origin ios-parity-transport-build
```

---

## Summary of what this fixes

| Area | Problem | Fix |
|------|---------|-----|
| Calls | REST endpoints don't exist on server | Remove dead REST; calls are WS-only |
| Calls | Missing SDP in WsEvent.CallOffer/Answer | Add sdp field to both |
| Calls | call_ice_candidate → server sends call_ice | Fix event name in parser |
| Calls | Missing fromUin/reason in CallEnd | Add fields |
| Calls | All call signals missing from outgoing payloads | Add to WebSocketOutgoingPayloads |
| Calls | call_type → server sends "media" field | Fix field name |
| Audio Rooms | room_enter not sent after REST join | Add WS room_enter in joinRoom() |
| Audio Rooms | room_leave via REST → wrong | Use WS room_leave |
| Audio Rooms | room_roster/entered events not parsed | Add proper parsers |
| Audio Rooms | Room signaling (offer/answer/ice) missing | Add to outgoing payloads |
| Groups | GroupUpdated only carries ID, not data | Carry groupJson + trigger sync |
| Groups | join/preview/delete endpoints missing | Add to API service and repository |
| Groups | AddMemberRequest.userId → uin | Fix field name |
| Groups | PUT /groups → PATCH | Already fixed; verify in repo |
| Messaging | SealedMessageResponse.id → server_time | Fix field |
| Messaging | GroupMemberApi missing status | Add field |
| Misc | Dead POST /contacts endpoint | Remove |
| Misc | Dead call REST endpoints | Remove |
| WS | account_burned not handled | Add WsEvent.AccountBurned |

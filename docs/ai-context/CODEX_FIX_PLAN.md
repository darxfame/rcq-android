# Codex Fix Plan v3 — Remaining P1/P2 Issues
# (v1+v2 are 95% done by Codex — this is the final cleanup pass)

> Branch: `ios-parity-transport-build`
> After EVERY block: `./gradlew compileProductionDebugKotlin`

---

## STATUS: What's DONE (don't re-do)
- WsEvent model: all call/room/group fields correct ✅
- WsEvent parser: all event types correct ✅
- WebSocketOutgoingPayloads: all call/room signals ✅
- RCQApiService: dead REST removed, DTOs fixed ✅
- GroupRepository: joinGroup/deleteGroup/getGroupPreview ✅
- CallRepository: WS-only ✅
- AudioRoomRepository: REST join + WS room_enter ✅
- GroupInfoScreen + GroupInfoViewModel ✅
- ChatScreen: onCall/onGroupInfo/More menu/ReactionPicker/ForwardPicker/InChatSearch/PinnedBanner ✅
- GroupInfoScreen registered in NavHost ✅
- Assets: 52 ICQ emoticons, RCQ sounds, status icons, icq_logo icon ✅

---

## BLOCK A — Add WS send helper methods to WebSocketService (P1)

**File:** `app/src/main/java/com/rcq/messenger/data/websocket/WebSocketService.kt`

CallManager and other callers need to send WS signals through WebSocketService, not via raw `sendMessage()`. Add these public helper methods to `WebSocketService` class if not present:

```kotlin
fun sendPing(): Boolean = sendMessage(WebSocketOutgoingPayloads.ping())
fun sendCallOffer(toUin: Long, callId: String, sdp: String, media: String = "audio"): Boolean =
    sendMessage(WebSocketOutgoingPayloads.callOffer(toUin, callId, sdp, media))
fun sendCallAnswer(toUin: Long, callId: String, sdp: String): Boolean =
    sendMessage(WebSocketOutgoingPayloads.callAnswer(toUin, callId, sdp))
fun sendCallIce(toUin: Long, callId: String, candidate: String): Boolean =
    sendMessage(WebSocketOutgoingPayloads.callIce(toUin, callId, candidate))
fun sendCallEnd(toUin: Long, callId: String, reason: String = "user_ended"): Boolean =
    sendMessage(WebSocketOutgoingPayloads.callEnd(toUin, callId, reason))
fun sendCallRenegotiate(toUin: Long, callId: String, sdp: String): Boolean =
    sendMessage(WebSocketOutgoingPayloads.callRenegotiate(toUin, callId, sdp))
fun sendCallRenegotiateAnswer(toUin: Long, callId: String, sdp: String): Boolean =
    sendMessage(WebSocketOutgoingPayloads.callRenegotiateAnswer(toUin, callId, sdp))
fun sendCallRenegotiateDecline(toUin: Long, callId: String): Boolean =
    sendMessage(WebSocketOutgoingPayloads.callRenegotiateDecline(toUin, callId))
fun sendRoomEnter(roomId: Int): Boolean =
    sendMessage(WebSocketOutgoingPayloads.roomEnter(roomId))
fun sendRoomLeave(roomId: Int): Boolean =
    sendMessage(WebSocketOutgoingPayloads.roomLeave(roomId))
fun sendRoomOffer(roomId: Int, toUin: Int, sdp: String): Boolean =
    sendMessage(WebSocketOutgoingPayloads.roomOffer(roomId, toUin, sdp))
fun sendRoomAnswer(roomId: Int, toUin: Int, sdp: String): Boolean =
    sendMessage(WebSocketOutgoingPayloads.roomAnswer(roomId, toUin, sdp))
fun sendRoomIce(roomId: Int, toUin: Int, candidate: String): Boolean =
    sendMessage(WebSocketOutgoingPayloads.roomIce(roomId, toUin, candidate))
fun sendRoomSpeaking(roomId: Int, speaking: Boolean): Boolean =
    sendMessage(WebSocketOutgoingPayloads.roomSpeaking(roomId, speaking))
```

---

## BLOCK B — Fix CallScreen: pass targetUin via navigation (P1)

**Files:**
- `app/src/main/java/com/rcq/messenger/ui/calls/CallScreen.kt`
- `app/src/main/java/com/rcq/messenger/ui/RCQApp.kt`

### B1. Update Routes to carry targetUin
```kotlin
// In RCQApp.kt Routes object, add:
const val CALL = "call/{chatId}/{targetUin}"
fun call(chatId: String, targetUin: Long) = "call/$chatId/$targetUin"
```

### B2. Update ChatScreen to pass targetUin to call
```kotlin
// In ChatScreen, the onCall lambda should pass chatId and targetUin:
// Add targetUin to ChatViewModel:
val targetUin: StateFlow<Long> = // = peerUin from chat (direct_XXXXXX → parse XXXXXX)
// In ChatViewModel:
fun getPeerUin(): Long = _chatId.value.removePrefix("direct_").toLongOrNull() ?: 0L
```

### B3. Update NavHost to extract targetUin
```kotlin
composable(
    route = Routes.CALL,
    arguments = listOf(
        navArgument("chatId") { type = NavType.StringType },
        navArgument("targetUin") { type = NavType.LongType; defaultValue = 0L }
    )
) { backStackEntry ->
    val chatId = backStackEntry.arguments?.getString("chatId") ?: ""
    val targetUin = backStackEntry.arguments?.getLong("targetUin") ?: 0L
    CallScreen(
        chatId = chatId,
        targetUin = targetUin,
        onBack = { navController.popBackStack() }
    )
}
```

### B4. Update CallScreen composable signature
```kotlin
@Composable
fun CallScreen(
    chatId: String,
    targetUin: Long,   // ADD THIS
    onBack: () -> Unit,
    viewModel: CallViewModel = hiltViewModel()
) {
    LaunchedEffect(targetUin) {
        if (targetUin > 0L) viewModel.setTargetUin(targetUin)
    }
    // ... rest of screen
}
```

### B5. Add setTargetUin to CallViewModel
```kotlin
// In CallViewModel:
private val _targetUin = MutableStateFlow(0L)
val targetUin: StateFlow<Long> = _targetUin.asStateFlow()

fun setTargetUin(uin: Long) { _targetUin.value = uin }

// Initiating a call uses webSocketService.sendCallOffer(targetUin, ...)
```

---

## BLOCK C — Verify ChatRepository GroupUpdated handler (P1)

**File:** `app/src/main/java/com/rcq/messenger/data/repository/ChatRepository.kt`

Find the `webSocketService.events.onEach { event -> when(event) { ... } }` block.

Verify this handler exists for `WsEvent.GroupUpdated`:
```kotlin
is WsEvent.GroupUpdated -> {
    scope.launch {
        val groupId = event.groupId
        if (groupId.isNotEmpty()) {
            val response = api.getGroup(groupId)
            if (response.isSuccessful) {
                response.body()?.let { group ->
                    groupDao.insertGroup(group.toGroupEntity())
                }
            }
        }
    }
}
```

And for `WsEvent.GroupDeleted`:
```kotlin
is WsEvent.GroupDeleted -> {
    scope.launch {
        groupDao.deleteGroup(event.groupId)
    }
}
```

If either handler is missing, ADD it. If GroupRepository is not injected into ChatRepository, add it.

---

## BLOCK D — Verify GroupBrowseViewModel.joinGroup() calls repository (P2)

**File:** `app/src/main/java/com/rcq/messenger/ui/contacts/GroupBrowseViewModel.kt`

Ensure `joinGroup()` calls the repository and isn't a stub:
```kotlin
fun joinGroup(groupId: String, onSuccess: (String) -> Unit) {
    viewModelScope.launch {
        _isLoading.value = true
        groupRepository.joinGroup(groupId.toIntOrNull() ?: 0)
            .onSuccess { onSuccess(groupId) }
            .onFailure { e -> _error.value = "Failed to join: ${e.message}" }
        _isLoading.value = false
    }
}
```

Also ensure GroupBrowseScreen passes `onGroupJoined` to NavHost correctly, and NavHost navigates to chat:
```kotlin
// In RCQApp.kt:
composable("groups") {
    GroupBrowseScreen(
        onBack = { navController.popBackStack() },
        onGroupJoined = { groupId -> navController.navigate(Routes.chat(groupId)) }
    )
}
```

---

## BLOCK E — Fix AudioRoomRepository.leaveRoom() REST cleanup (P2)

**File:** `app/src/main/java/com/rcq/messenger/data/repository/GroupRepository.kt` (AudioRoomRepository)

Verify `leaveRoom()` does BOTH WS leave AND REST leave:
```kotlin
suspend fun leaveRoom(roomId: String): Result<Unit> = runCatching {
    // Step 1: WS leave (removes from Redis roster immediately)
    webSocketService.sendRoomLeave(roomId.toIntOrNull() ?: 0)
    // Step 2: REST leave (removes DB membership)
    val response = api.leaveRoom(roomId)
    if (!response.isSuccessful) {
        android.util.Log.w("AudioRoomRepository", "REST leaveRoom failed: ${response.code()} — WS leave already sent")
        // Don't throw — WS leave is what matters for real-time, REST is cleanup
    }
}
```

---

## BLOCK F — Verify SettingsScreen status values match server (P2)

**File:** `app/src/main/java/com/rcq/messenger/ui/settings/SettingsScreen.kt`

The status picker must use EXACTLY these string values (server expects lowercase):
```kotlin
listOf(
    "online" to "Online",
    "away" to "Away",
    "dnd" to "Busy / DND",
    "invisible" to "Invisible"
)
// NOT: "Online", "Away", "DND", "Invisible" (wrong case)
// NOT: "busy", "unavailable" (wrong values)
```

Also ensure `onSetStatus` calls `userRepository.updatePresence(status)` which calls `POST /presence/status` with `{"status": "online"}` (lowercase).

---

## FINAL BUILD + COMMIT

```bash
./gradlew compileProductionDebugKotlin
./gradlew test

git add -u
git commit -m "Android: финальный cleanup — WS helpers, CallScreen targetUin, GroupBrowse join, AudioRoom REST cleanup, статус picker"
git push origin ios-parity-transport-build
```

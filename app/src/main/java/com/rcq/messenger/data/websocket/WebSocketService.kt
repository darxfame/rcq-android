package com.rcq.messenger.data.websocket

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.rcq.messenger.di.PreferencesKeys
import com.rcq.messenger.service.ProxyManager
import com.rcq.messenger.service.RcqProxySelector
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.json.*
import okhttp3.*
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton

// ---- Event Model (sealed class with all event types) ----

sealed class WsEvent {
    // Contact events
    data class ContactRequest(
        val requestId: Long,
        val fromUin: Long,
        val fromNickname: String,
        val avatarUrl: String? = null
    ) : WsEvent()

    data class ContactResponse(
        val requestId: Long,
        val accepted: Boolean,
        val peerUin: Long
    ) : WsEvent()

    data class ContactRemoved(val peerUin: Long) : WsEvent()
    data class ContactBlocked(val peerUin: Long) : WsEvent()
    data class ContactUnblocked(val peerUin: Long) : WsEvent()

    // Message events
    data class MessageNew(val chatId: String, val raw: JsonObject) : WsEvent()
    data class MessageEdited(val chatId: String, val messageId: String, val content: String) : WsEvent()
    data class MessageDeleted(val chatId: String, val messageId: String) : WsEvent()
    data class MessageDeletedForEveryone(val chatId: String, val messageId: String) : WsEvent()
    data class MessageReaction(val chatId: String, val messageId: String, val raw: JsonObject) : WsEvent()
    data class MessageRead(val chatId: String, val messageId: String, val userId: Long) : WsEvent()
    data class MessageDelivered(val chatId: String, val messageId: String) : WsEvent()
    data class MessageBounced(val chatId: String, val messageId: String) : WsEvent()

    // Typing events
    data class TypingStarted(val chatId: String, val userId: Long) : WsEvent()
    data class TypingStopped(val chatId: String, val userId: Long) : WsEvent()

    // Presence events
    data class PresenceOnline(val uin: Long) : WsEvent()
    data class PresenceAway(val uin: Long) : WsEvent()
    data class PresenceDnd(val uin: Long) : WsEvent()
    data class PresenceInvisible(val uin: Long) : WsEvent()
    data class PresenceOffline(val uin: Long) : WsEvent()

    // Group events
    data class GroupUpdated(val groupId: String) : WsEvent()
    data class GroupMemberJoined(val groupId: String, val userId: Long) : WsEvent()
    data class GroupMemberLeft(val groupId: String, val userId: Long) : WsEvent()
    data class GroupDeleted(val groupId: String) : WsEvent()

    // Call events
    data class CallOffer(val callId: String, val fromUin: Long, val callType: String) : WsEvent()
    data class CallAnswer(val callId: String, val fromUin: Long) : WsEvent()
    data class CallIceCandidate(val callId: String, val raw: JsonObject) : WsEvent()
    data class CallEnd(val callId: String) : WsEvent()
    data class CallUpgrade(val callId: String, val toType: String) : WsEvent()
    data class CallUpgradeAnswer(val callId: String, val accepted: Boolean) : WsEvent()

    // Audio Room events
    data class AudioRoomStarted(val roomId: String) : WsEvent()
    data class AudioRoomPeerJoined(val roomId: String, val userId: Long) : WsEvent()
    data class AudioRoomPeerLeft(val roomId: String, val userId: Long) : WsEvent()
    data class AudioRoomEnded(val roomId: String) : WsEvent()

    // Story events
    data class StoryPosted(val storyId: String, val userId: Long) : WsEvent()
    data class StoryExpired(val storyId: String) : WsEvent()
    data class StoryViewed(val storyId: String, val userId: Long) : WsEvent()

    // System events
    data class Ban(val reason: String) : WsEvent()
    data class Warning(val message: String) : WsEvent()

    // Fallback
    data class Unknown(val type: String, val raw: JsonObject) : WsEvent()
}

enum class ConnectionState {
    DISCONNECTED, CONNECTING, CONNECTED, RECONNECTING, ERROR
}

class ReconnectStrategy(
    private val maxDelayMs: Long = 30_000L
) {
    private val baseDelayMs = 1_000L
    private val multiplier = 2.0
    private val attempt = AtomicInteger(0)

    fun nextDelay(): Long {
        val exp = attempt.getAndIncrement()
        val delay = (baseDelayMs * Math.pow(multiplier, exp.toDouble())).toLong()
        return minOf(delay, maxDelayMs)
    }

    fun reset() {
        attempt.set(0)
    }
}

@Singleton
class WebSocketService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dataStore: DataStore<Preferences>,
    private val proxyManager: ProxyManager
) {
    companion object {
        private const val TAG = "WebSocketService"
        private const val WS_BASE_URL = "wss://api.rcq.app/ws"
        private const val PING_INTERVAL_SECONDS = 25L
        private const val STALE_WATCHDOG_MS = 90_000L
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    @Volatile
    private var webSocket: WebSocket? = null
    private var reconnectJob: Job? = null
    private var watchdogJob: Job? = null
    @Volatile
    private var intentionalDisconnect = false
    private val reconnectStrategy = ReconnectStrategy()

    // Shared OkHttpClient — reused across reconnects to avoid thread pool leaks
    private val okHttpClient = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .pingInterval(PING_INTERVAL_SECONDS, TimeUnit.SECONDS)
        .proxySelector(RcqProxySelector(proxyManager))
        .build()

    // Reconnect immediately when network becomes available
    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            Log.d(TAG, "Network available — triggering reconnect")
            if (!intentionalDisconnect &&
                _connectionState.value != ConnectionState.CONNECTED &&
                _connectionState.value != ConnectionState.CONNECTING
            ) {
                reconnectJob?.cancel()
                reconnectStrategy.reset()
                scope.launch {
                    _connectionState.value = ConnectionState.CONNECTING
                    createWebSocket()
                }
            }
        }
    }

    init {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val req = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        cm.registerNetworkCallback(req, networkCallback)
    }

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    /** Stream of all parsed WebSocket events */
    private val _events = MutableSharedFlow<WsEvent>(extraBufferCapacity = 64)
    val events: SharedFlow<WsEvent> = _events.asSharedFlow()

    /** Backward-compatible contact request events for PendingRequestsViewModel */
    private val _contactRequests = MutableSharedFlow<WebSocketContactRequest>(extraBufferCapacity = 16)
    val contactRequests: SharedFlow<WebSocketContactRequest> = _contactRequests.asSharedFlow()

    /** Backward-compatible contact response events */
    private val _contactResponses = MutableSharedFlow<WebSocketContactResponse>(extraBufferCapacity = 16)
    val contactResponses: SharedFlow<WebSocketContactResponse> = _contactResponses.asSharedFlow()

    /** Stream of presence events specifically */
    private val _presenceEvents = MutableSharedFlow<WsEvent>(extraBufferCapacity = 16)
    val presenceEvents: SharedFlow<WsEvent> = _presenceEvents.asSharedFlow()

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    // ---- Public API ----

    fun connect() {
        intentionalDisconnect = false
        val current = _connectionState.value
        if (current == ConnectionState.CONNECTED || current == ConnectionState.CONNECTING) return
        _connectionState.value = ConnectionState.CONNECTING
        createWebSocket()
    }

    fun disconnect() {
        intentionalDisconnect = true
        reconnectJob?.cancel()
        reconnectJob = null
        watchdogJob?.cancel()
        watchdogJob = null
        webSocket?.close(1000, "User disconnected")
        webSocket = null
        _connectionState.value = ConnectionState.DISCONNECTED
        reconnectStrategy.reset()
    }

    fun sendMessage(message: String) {
        webSocket?.send(message)
    }

    // ---- Internal ----

    private fun createWebSocket() {
        val (token, uin) = runBlocking {
            val prefs = dataStore.data.first()
            Pair(prefs[PreferencesKeys.AUTH_TOKEN], prefs[PreferencesKeys.USER_UIN])
        }

        if (token == null || uin == null) {
            Log.e(TAG, "No token or UIN available for WebSocket")
            _connectionState.value = ConnectionState.ERROR
            scheduleReconnect()
            return
        }

        val wsUrl = "$WS_BASE_URL/$uin?token=$token"
        Log.d(TAG, "Connecting WebSocket: $wsUrl")

        val request = Request.Builder()
            .url(wsUrl)
            .build()

        webSocket = okHttpClient.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d(TAG, "WebSocket connected")
                _connectionState.value = ConnectionState.CONNECTED
                reconnectStrategy.reset()
                proxyManager.reportSuccess()
                startWatchdog()
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                watchdogJob?.cancel()
                startWatchdog()
                handleMessage(text)
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "WebSocket closing: $code $reason")
                webSocket.close(1000, null)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "WebSocket closed: $code $reason")
                _connectionState.value = ConnectionState.DISCONNECTED
                watchdogJob?.cancel()
                scheduleReconnect()
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e(TAG, "WebSocket failure: ${t.message}")
                _connectionState.value = ConnectionState.ERROR
                watchdogJob?.cancel()
                proxyManager.reportFailure()
                scheduleReconnect()
            }
        })
    }

    private fun handleMessage(text: String) {
        try {
            val element = json.parseToJsonElement(text)
            val obj = element.jsonObject
            val type = obj["type"]?.jsonPrimitive?.contentOrNull ?: return

            val event = parseEvent(type, obj)
            scope.launch {
                _events.emit(event)

                        when (event) {
                            is WsEvent.ContactRequest -> {
                                _contactRequests.emit(
                                    WebSocketContactRequest(
                                        id = event.requestId,
                                        fromUin = event.fromUin,
                                        nickname = event.fromNickname
                                    )
                                )
                            }
                            is WsEvent.ContactResponse -> {
                                _contactResponses.emit(
                                    WebSocketContactResponse(
                                        requestId = event.requestId,
                                        accepted = event.accepted,
                                        peerUin = event.peerUin
                                    )
                                )
                            }
                            is WsEvent.PresenceOnline,
                            is WsEvent.PresenceAway,
                            is WsEvent.PresenceDnd,
                            is WsEvent.PresenceInvisible,
                            is WsEvent.PresenceOffline -> {
                                _presenceEvents.emit(event)
                            }
                            else -> { /* routed via _events for downstream consumers */ }
                        }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing WebSocket message: ${e.message}")
        }
    }

    @Suppress("UNUSED_PARAMETER")
    private fun parseEvent(type: String, obj: JsonObject): WsEvent {
        return try {
            when (type) {
                // Contact events
                "contact_request" -> WsEvent.ContactRequest(
                    requestId = obj["request_id"]?.jsonPrimitive?.longOrNull ?: 0,
                    fromUin = obj["from_uin"]?.jsonPrimitive?.longOrNull ?: 0,
                    fromNickname = obj["from_nickname"]?.jsonPrimitive?.contentOrNull ?: "",
                    avatarUrl = obj["avatar_url"]?.jsonPrimitive?.contentOrNull
                )
                "contact_response" -> WsEvent.ContactResponse(
                    requestId = obj["request_id"]?.jsonPrimitive?.longOrNull ?: 0,
                    accepted = obj["accepted"]?.jsonPrimitive?.booleanOrNull ?: false,
                    peerUin = obj["to_uin"]?.jsonPrimitive?.longOrNull
                        ?: obj["from_uin"]?.jsonPrimitive?.longOrNull ?: 0
                )
                "contact_removed" -> WsEvent.ContactRemoved(
                    peerUin = obj["peer_uin"]?.jsonPrimitive?.longOrNull ?: 0
                )
                "contact_blocked" -> WsEvent.ContactBlocked(
                    peerUin = obj["peer_uin"]?.jsonPrimitive?.longOrNull ?: 0
                )
                "contact_unblocked" -> WsEvent.ContactUnblocked(
                    peerUin = obj["peer_uin"]?.jsonPrimitive?.longOrNull ?: 0
                )

                // Message events — server sends envelope_type directly as "type" (sealed sender)
                "message", "prekey_message", "reaction", "delete", "edit", "read", "bounce",
                "message_new", "new_message" -> WsEvent.MessageNew(
                    chatId = obj["chat_id"]?.jsonPrimitive?.contentOrNull ?: "",
                    raw = obj
                )
                "message_edited", "message_edit" -> WsEvent.MessageEdited(
                    chatId = obj["chat_id"]?.jsonPrimitive?.contentOrNull ?: "",
                    messageId = obj["message_id"]?.jsonPrimitive?.contentOrNull ?: "",
                    content = obj["content"]?.jsonPrimitive?.contentOrNull ?: ""
                )
                "message_deleted", "message_delete" -> WsEvent.MessageDeleted(
                    chatId = obj["chat_id"]?.jsonPrimitive?.contentOrNull ?: "",
                    messageId = obj["message_id"]?.jsonPrimitive?.contentOrNull ?: ""
                )
                "message_deleted_for_everyone" -> WsEvent.MessageDeletedForEveryone(
                    chatId = obj["chat_id"]?.jsonPrimitive?.contentOrNull ?: "",
                    messageId = obj["message_id"]?.jsonPrimitive?.contentOrNull ?: ""
                )
                "message_reaction" -> WsEvent.MessageReaction(
                    chatId = obj["chat_id"]?.jsonPrimitive?.contentOrNull ?: "",
                    messageId = obj["message_id"]?.jsonPrimitive?.contentOrNull ?: "",
                    raw = obj
                )
                "message_read" -> WsEvent.MessageRead(
                    chatId = obj["chat_id"]?.jsonPrimitive?.contentOrNull ?: "",
                    messageId = obj["message_id"]?.jsonPrimitive?.contentOrNull ?: "",
                    userId = obj["user_id"]?.jsonPrimitive?.longOrNull ?: 0
                )
                "message_delivered", "message_deliver" -> WsEvent.MessageDelivered(
                    chatId = obj["chat_id"]?.jsonPrimitive?.contentOrNull ?: "",
                    messageId = obj["message_id"]?.jsonPrimitive?.contentOrNull ?: ""
                )
                "message_bounced" -> WsEvent.MessageBounced(
                    chatId = obj["chat_id"]?.jsonPrimitive?.contentOrNull ?: "",
                    messageId = obj["message_id"]?.jsonPrimitive?.contentOrNull ?: ""
                )

                // Typing events — server sends from_uin (not user_id)
                "typing_started", "typing" -> WsEvent.TypingStarted(
                    chatId = obj["chat_id"]?.jsonPrimitive?.contentOrNull ?: "",
                    userId = obj["from_uin"]?.jsonPrimitive?.longOrNull
                        ?: obj["user_id"]?.jsonPrimitive?.longOrNull ?: 0
                )
                "typing_stopped" -> WsEvent.TypingStopped(
                    chatId = obj["chat_id"]?.jsonPrimitive?.contentOrNull ?: "",
                    userId = obj["from_uin"]?.jsonPrimitive?.longOrNull
                        ?: obj["user_id"]?.jsonPrimitive?.longOrNull ?: 0
                )

                // Presence events
                "presence_online", "online" -> WsEvent.PresenceOnline(
                    uin = obj["uin"]?.jsonPrimitive?.longOrNull
                        ?: obj["user_id"]?.jsonPrimitive?.longOrNull ?: 0
                )
                "presence_away", "away" -> WsEvent.PresenceAway(
                    uin = obj["uin"]?.jsonPrimitive?.longOrNull
                        ?: obj["user_id"]?.jsonPrimitive?.longOrNull ?: 0
                )
                "presence_dnd", "dnd" -> WsEvent.PresenceDnd(
                    uin = obj["uin"]?.jsonPrimitive?.longOrNull
                        ?: obj["user_id"]?.jsonPrimitive?.longOrNull ?: 0
                )
                "presence_invisible", "invisible" -> WsEvent.PresenceInvisible(
                    uin = obj["uin"]?.jsonPrimitive?.longOrNull
                        ?: obj["user_id"]?.jsonPrimitive?.longOrNull ?: 0
                )
                "presence_offline", "offline" -> WsEvent.PresenceOffline(
                    uin = obj["uin"]?.jsonPrimitive?.longOrNull
                        ?: obj["user_id"]?.jsonPrimitive?.longOrNull ?: 0
                )

                // Group events — server sends group_created/group_membership_changed/group_deleted
                "group_created", "group_membership_changed", "group_updated" -> WsEvent.GroupUpdated(
                    groupId = obj["group"]?.jsonObject?.get("id")?.jsonPrimitive?.contentOrNull
                        ?: obj["group_id"]?.jsonPrimitive?.contentOrNull ?: ""
                )
                "group_member_joined", "group_member_added" -> WsEvent.GroupMemberJoined(
                    groupId = obj["group_id"]?.jsonPrimitive?.contentOrNull ?: "",
                    userId = obj["user_id"]?.jsonPrimitive?.longOrNull ?: 0
                )
                "group_member_left", "group_member_removed" -> WsEvent.GroupMemberLeft(
                    groupId = obj["group_id"]?.jsonPrimitive?.contentOrNull ?: "",
                    userId = obj["user_id"]?.jsonPrimitive?.longOrNull ?: 0
                )
                "group_deleted" -> WsEvent.GroupDeleted(
                    groupId = obj["group_id"]?.jsonPrimitive?.contentOrNull ?: ""
                )

                // Call events
                "call_offer", "call_incoming" -> WsEvent.CallOffer(
                    callId = obj["call_id"]?.jsonPrimitive?.contentOrNull ?: "",
                    fromUin = obj["from_uin"]?.jsonPrimitive?.longOrNull ?: 0,
                    callType = obj["call_type"]?.jsonPrimitive?.contentOrNull ?: "audio"
                )
                "call_answer", "call_accepted" -> WsEvent.CallAnswer(
                    callId = obj["call_id"]?.jsonPrimitive?.contentOrNull ?: "",
                    fromUin = obj["from_uin"]?.jsonPrimitive?.longOrNull ?: 0
                )
                "call_ice_candidate" -> WsEvent.CallIceCandidate(
                    callId = obj["call_id"]?.jsonPrimitive?.contentOrNull ?: "",
                    raw = obj
                )
                "call_end", "call_ended" -> WsEvent.CallEnd(
                    callId = obj["call_id"]?.jsonPrimitive?.contentOrNull ?: ""
                )
                "call_upgrade" -> WsEvent.CallUpgrade(
                    callId = obj["call_id"]?.jsonPrimitive?.contentOrNull ?: "",
                    toType = obj["to_type"]?.jsonPrimitive?.contentOrNull ?: "video"
                )
                "call_upgrade_answer" -> WsEvent.CallUpgradeAnswer(
                    callId = obj["call_id"]?.jsonPrimitive?.contentOrNull ?: "",
                    accepted = obj["accepted"]?.jsonPrimitive?.booleanOrNull ?: false
                )

                // Audio Room events
                "room_created", "audio_room_started" -> WsEvent.AudioRoomStarted(
                    roomId = obj["room_id"]?.jsonPrimitive?.contentOrNull
                        ?: obj["id"]?.jsonPrimitive?.contentOrNull ?: ""
                )
                "room_member_joined", "audio_room_peer_joined" -> WsEvent.AudioRoomPeerJoined(
                    roomId = obj["room_id"]?.jsonPrimitive?.contentOrNull ?: "",
                    userId = obj["user_id"]?.jsonPrimitive?.longOrNull ?: 0
                )
                "room_member_left", "audio_room_peer_left" -> WsEvent.AudioRoomPeerLeft(
                    roomId = obj["room_id"]?.jsonPrimitive?.contentOrNull ?: "",
                    userId = obj["user_id"]?.jsonPrimitive?.longOrNull ?: 0
                )
                "room_deleted", "audio_room_ended" -> WsEvent.AudioRoomEnded(
                    roomId = obj["room_id"]?.jsonPrimitive?.contentOrNull ?: ""
                )

                // Story events
                "story_created", "story_posted" -> WsEvent.StoryPosted(
                    storyId = obj["story_id"]?.jsonPrimitive?.contentOrNull
                        ?: obj["id"]?.jsonPrimitive?.contentOrNull ?: "",
                    userId = obj["user_id"]?.jsonPrimitive?.longOrNull ?: 0
                )
                "story_deleted", "story_expired" -> WsEvent.StoryExpired(
                    storyId = obj["story_id"]?.jsonPrimitive?.contentOrNull ?: ""
                )
                "story_viewed" -> WsEvent.StoryViewed(
                    storyId = obj["story_id"]?.jsonPrimitive?.contentOrNull ?: "",
                    userId = obj["user_id"]?.jsonPrimitive?.longOrNull ?: 0
                )

                // System events
                "ban" -> WsEvent.Ban(
                    reason = obj["reason"]?.jsonPrimitive?.contentOrNull ?: "Banned"
                )
                "warning" -> WsEvent.Warning(
                    message = obj["message"]?.jsonPrimitive?.contentOrNull ?: ""
                )

                else -> {
                    Log.w(TAG, "Unknown WS event type: $type — raw: ${obj.toString().take(200)}")
                    WsEvent.Unknown(type, obj)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing event $type: ${e.message}")
            WsEvent.Unknown(type, obj)
        }
    }

    private fun scheduleReconnect() {
        if (intentionalDisconnect) return

        val delay = reconnectStrategy.nextDelay()
        Log.d(TAG, "Scheduling reconnect in ${delay}ms")
        _connectionState.value = ConnectionState.RECONNECTING

        reconnectJob?.cancel()
        reconnectJob = scope.launch {
            delay(delay)
            if (!intentionalDisconnect) {
                _connectionState.value = ConnectionState.CONNECTING
                createWebSocket()
            }
        }
    }

    private fun startWatchdog() {
        watchdogJob?.cancel()
        watchdogJob = scope.launch {
            delay(STALE_WATCHDOG_MS)
            Log.w(TAG, "Stale watchdog triggered — no messages for ${STALE_WATCHDOG_MS}ms, reconnecting...")
            webSocket?.close(1000, "Stale connection")
            webSocket = null
            _connectionState.value = ConnectionState.ERROR
            scheduleReconnect()
        }
    }
}

// Backward-compatible data classes for existing consumers (PendingRequestsViewModel)
data class WebSocketContactRequest(
    val id: Long,
    val fromUin: Long,
    val nickname: String
)

data class WebSocketContactResponse(
    val requestId: Long,
    val accepted: Boolean,
    val peerUin: Long
)

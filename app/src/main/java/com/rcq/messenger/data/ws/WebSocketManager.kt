package com.rcq.messenger.data.ws

import android.util.Log
import com.rcq.messenger.domain.model.WebSocketEvent
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.json.Json
import okhttp3.*
import okio.ByteString
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WebSocketManager @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val json: Json
) {
    companion object {
        private const val TAG = "WebSocketManager"
        private const val RECONNECT_INTERVAL = 5000L
        private const val MAX_RECONNECT_ATTEMPTS = 5
        private const val PING_INTERVAL = 30000L
    }

    private var webSocket: WebSocket? = null
    private var reconnectAttempts = 0
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _eventFlow = MutableSharedFlow<WebSocketEvent>()
    val eventFlow: SharedFlow<WebSocketEvent> = _eventFlow.asSharedFlow()

    private var pingJob: Job? = null
    private var reconnectJob: Job? = null

    fun connect(url: String) {
        disconnect()

        val request = Request.Builder()
            .url(url)
            .build()

        webSocket = okHttpClient.newWebSocket(request, webSocketListener)
        _connectionState.value = ConnectionState.CONNECTING
    }

    fun disconnect() {
        pingJob?.cancel()
        reconnectJob?.cancel()
        webSocket?.close(1000, "Normal closure")
        webSocket = null
        _connectionState.value = ConnectionState.DISCONNECTED
    }

    fun sendMessage(event: WebSocketEvent) {
        try {
            val message = json.encodeToString(WebSocketEvent.serializer(), event)
            webSocket?.send(message)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send message", e)
        }
    }

    private val webSocketListener = object : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            Log.d(TAG, "WebSocket connected")
            _connectionState.value = ConnectionState.CONNECTED
            reconnectAttempts = 0
            startPing()
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            try {
                val event = json.decodeFromString(WebSocketEvent.serializer(), text)
                scope.launch {
                    _eventFlow.emit(event)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to parse message: $text", e)
            }
        }

        override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
            onMessage(webSocket, bytes.utf8())
        }

        override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
            Log.d(TAG, "WebSocket closing: $code $reason")
            _connectionState.value = ConnectionState.DISCONNECTING
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            Log.d(TAG, "WebSocket closed: $code $reason")
            _connectionState.value = ConnectionState.DISCONNECTED
            stopPing()
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            Log.e(TAG, "WebSocket error", t)
            _connectionState.value = ConnectionState.DISCONNECTED
            stopPing()
            scheduleReconnect()
        }
    }

    private fun startPing() {
        pingJob = scope.launch {
            while (isActive && _connectionState.value == ConnectionState.CONNECTED) {
                delay(PING_INTERVAL)
                webSocket?.send("ping")
            }
        }
    }

    private fun stopPing() {
        pingJob?.cancel()
        pingJob = null
    }

    private fun scheduleReconnect() {
        if (reconnectAttempts >= MAX_RECONNECT_ATTEMPTS) {
            Log.w(TAG, "Max reconnect attempts reached")
            return
        }

        reconnectJob = scope.launch {
            delay(RECONNECT_INTERVAL * (reconnectAttempts + 1))
            reconnectAttempts++
            Log.d(TAG, "Reconnecting... attempt $reconnectAttempts")
            // Note: reconnect logic would need the original URL
            // This is a simplified version
        }
    }

    enum class ConnectionState {
        DISCONNECTED,
        CONNECTING,
        CONNECTED,
        DISCONNECTING
    }
}
package com.rcq.messenger.service

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import okhttp3.*
import org.json.JSONObject
import javax.inject.Inject

@AndroidEntryPoint
class WebSocketService : Service() {

    private val binder = LocalBinder()
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var webSocket: WebSocket? = null
    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _messages = MutableSharedFlow<MessageEvent>()
    val messages: SharedFlow<MessageEvent> = _messages.asSharedFlow()

    inner class LocalBinder : Binder() {
        fun getService(): WebSocketService = this@WebSocketService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        connect()
    }

    override fun onDestroy() {
        super.onDestroy()
        disconnect()
        serviceScope.cancel()
    }

    private fun connect() {
        val client = OkHttpClient()
        val request = Request.Builder()
            .url("wss://ws.rcq.messenger/")
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                _connectionState.value = ConnectionState.CONNECTED
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                handleMessage(text)
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                _connectionState.value = ConnectionState.DISCONNECTING
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                _connectionState.value = ConnectionState.DISCONNECTED
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                _connectionState.value = ConnectionState.ERROR
                serviceScope.launch {
                    delay(5000)
                    connect()
                }
            }
        })
    }

    private fun disconnect() {
        webSocket?.close(1000, "Service stopping")
        webSocket = null
    }

    private fun handleMessage(text: String) {
        try {
            val json = JSONObject(text)
            val type = json.getString("type")
            val data = json.optJSONObject("data")

            serviceScope.launch {
                _messages.emit(MessageEvent(type, data))
            }
        } catch (e: Exception) {
            // Log error
        }
    }

    fun send(type: String, data: Map<String, Any>) {
        val json = JSONObject().apply {
            put("type", type)
            put("data", JSONObject(data))
        }
        webSocket?.send(json.toString())
    }
}

enum class ConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    DISCONNECTING,
    ERROR
}

data class MessageEvent(
    val type: String,
    val data: JSONObject?
)
package app.rcq.android.net

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.TimeUnit
import kotlin.math.min

/**
 * WebSocket channel to the backend (rcq-spec 7). Connects to
 * `wss://<host>/ws/<uin>?token=<jwt>` and surfaces every inbound event as
 * a parsed (type, json) pair; the caller branches on `type`
 * (message-family vs contact_* etc). Auto-reconnects with exponential
 * backoff while it's supposed to stay connected.
 */
class RcqSocket(private val baseWsUrl: String = DEFAULT_WS_URL) {

    private val client = OkHttpClient.Builder()
        .pingInterval(20, TimeUnit.SECONDS)
        .build()

    private var ws: WebSocket? = null
    private var shouldStayConnected = false
    private var attempt = 0

    private var uin: Int = 0
    private var token: String = ""
    private var onEvent: (type: String, obj: JsonObject) -> Unit = { _, _ -> }
    private var onState: (Boolean) -> Unit = {}

    fun connect(
        uin: Int,
        token: String,
        onEvent: (type: String, obj: JsonObject) -> Unit,
        onState: (connected: Boolean) -> Unit = {},
    ) {
        this.uin = uin
        this.token = token
        this.onEvent = onEvent
        this.onState = onState
        shouldStayConnected = true
        open()
    }

    private fun open() {
        ws?.cancel()
        val request = Request.Builder().url("$baseWsUrl/ws/$uin?token=$token").build()
        ws = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                attempt = 0
                onState(true)
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                runCatching {
                    val obj = JsonParser.parseString(text).asJsonObject
                    val type = obj.get("type")?.asString ?: return
                    onEvent(type, obj)
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                onState(false)
                scheduleReconnect()
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                onState(false)
                scheduleReconnect()
            }
        })
    }

    private fun scheduleReconnect() {
        if (!shouldStayConnected) return
        attempt += 1
        val delayMs = min(30_000L, (1000L shl min(attempt - 1, 5)))
        Thread {
            Thread.sleep(delayMs)
            if (shouldStayConnected) open()
        }.apply { isDaemon = true }.start()
    }

    fun disconnect() {
        shouldStayConnected = false
        ws?.close(1000, null)
        ws = null
    }

    companion object {
        const val DEFAULT_WS_URL = "wss://api.rcq.app"
    }
}

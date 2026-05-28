package app.rcq.android.net

import com.google.gson.JsonParser
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.TimeUnit

/**
 * WebSocket channel to the backend (rcq-spec 7). Connects to
 * `wss://<host>/ws/<uin>?token=<jwt>` and surfaces the base64 `payload` of
 * every inbound envelope event to the caller, who decrypts it. Protocol
 * keepalive is handled by OkHttp's ping; the app-level ping/reconnect
 * watchdog from iOS is a later refinement.
 */
class RcqSocket(private val baseWsUrl: String = DEFAULT_WS_URL) {

    private val client = OkHttpClient.Builder()
        .pingInterval(20, TimeUnit.SECONDS)
        .build()

    private var ws: WebSocket? = null

    fun connect(
        uin: Int,
        token: String,
        onPayload: (String) -> Unit,
        onState: (connected: Boolean) -> Unit = {},
    ) {
        val request = Request.Builder()
            .url("$baseWsUrl/ws/$uin?token=$token")
            .build()
        ws = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) = onState(true)

            override fun onMessage(webSocket: WebSocket, text: String) {
                runCatching {
                    val obj = JsonParser.parseString(text).asJsonObject
                    obj.get("payload")?.asString?.let(onPayload)
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) =
                onState(false)

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) =
                onState(false)
        })
    }

    fun disconnect() {
        ws?.close(1000, null)
        ws = null
    }

    companion object {
        const val DEFAULT_WS_URL = "wss://api.rcq.app"
    }
}

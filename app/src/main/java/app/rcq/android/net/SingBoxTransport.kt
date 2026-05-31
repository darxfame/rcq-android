package app.rcq.android.net

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.net.InetSocketAddress
import java.net.Proxy

/**
 * Embedded censorship-circumvention transport. Runs sing-box in-process (via
 * the gomobile-bound [rcqbox] wrapper — same Go core the iOS client uses),
 * exposing a local SOCKS/mixed proxy on 127.0.0.1:[LOCAL_PORT]. RcqApi /
 * RcqSocket route their OkHttp traffic through [proxy] when the transport is
 * active, so a VLESS+Reality / Hysteria2 relay carries the API + WebSocket to
 * the backend — defeating DPI that blocks `api.rcq.app` directly.
 *
 * Opt-in (a Settings toggle persisted in prefs); off by default = zero effect,
 * the app connects directly exactly as before. Engaged at boot in
 * [app.rcq.android.Session.start] BEFORE the API/socket are built, so they pick
 * up the proxy. The relay list is bundled here for now; a fetched + Ed25519
 * verified remote config (iOS RelayConfigStore parity) is the next phase.
 */
object SingBoxTransport {
    const val LOCAL_PORT = 1089
    private const val PREFS = "rcq_singbox"
    private const val KEY_ENABLED = "enabled"

    @Volatile
    var isActive = false
        private set

    private var box: rcqbox.BoxService? = null

    data class Relay(
        val tag: String,
        val proto: String,            // "vless" | "hysteria2"
        val server: String,
        val port: Int,
        val sni: String,
        val uuid: String? = null,
        val publicKey: String? = null,
        val shortId: String? = null,
        val flow: String? = null,
        val password: String? = null,
        val obfsPassword: String? = null,
    )

    /** Bundled relay pool — mirrors the live signed config (v9). A fetched +
     *  verified remote list (so rotations need no app update) is the next
     *  phase; until then a fresh install always has a working pool. */
    private val bundledRelays = listOf(
        Relay("relay-do-fra-yandex-hy2", "hysteria2", "165.22.90.214", 443, "www.yandex.ru", password = "JN0qzA4LJfhHPKKN3QHj4eN8", obfsPassword = "jXfGkLToOkTihpeJzDiNf8Bb"),
        Relay("relay-do-fra-yandex", "vless", "165.22.90.214", 443, "www.yandex.ru", uuid = "2081b3c4-faaa-4cce-a0ab-607197b28237", publicKey = "n33TZTLNrc6X7jTGrKWex_sk8aIQ6Qqz-eC8lqYMii8", shortId = "aa5d483441e59ac7", flow = "xtls-rprx-vision"),
        Relay("relay-oracle-il-hy2", "hysteria2", "129.159.143.135", 443, "www.microsoft.com", password = "bvuvu74CVsiXdcJazcYphnO5", obfsPassword = "PaEHrZABTk36orhfFON7Jure"),
        Relay("relay-oracle-il", "vless", "129.159.143.135", 443, "www.microsoft.com", uuid = "ff005e0c-175e-4475-a166-eeac88f514e2", publicKey = "_Hhc-2pjkvR914mddMdmuoOVaT74vWR8Gby7KmJp9F8", shortId = "318567678ac9878e", flow = "xtls-rprx-vision"),
        Relay("relay-gcp-hy2", "hysteria2", "35.238.53.96", 443, "www.apple.com", password = "QaY3uT8EmfZxfON65jaT5bSu", obfsPassword = "fLpJ2c211xjnZcP9VNcNpbZP"),
        Relay("relay-gcp", "vless", "35.238.53.96", 443, "www.apple.com", uuid = "8e3b35d3-18a6-406d-9ac6-c5558a806663", publicKey = "mQZ8CJeMWyf7oYGWJG8oOI52or2kx4yTthl6AGZkSTw", shortId = "b5b8979af1f27aab", flow = "xtls-rprx-vision"),
        Relay("relay-aws-sg-hy2", "hysteria2", "47.129.249.170", 443, "www.amazon.com", password = "IjO9NlfvuXuP8w4tZNXHZwGL", obfsPassword = "yBlwN4J7IMzQi3VCMo0oKZHh"),
        Relay("relay-aws-sg", "vless", "47.129.249.170", 443, "www.amazon.com", uuid = "2b0a3318-7bfc-4ff2-83ae-2f322cb91ef8", publicKey = "xxasGveo2BtMx4doxftb-AJcvIXL-9LpymZcV9tIRxo", shortId = "533142a04b016a00", flow = "xtls-rprx-vision"),
    )

    private fun relays(): List<Relay> = bundledRelays

    /** SOCKS proxy pointing at the local sing-box inbound, or null when the
     *  transport is off (OkHttp treats null as a direct connection). */
    fun proxy(): Proxy? =
        if (isActive) Proxy(Proxy.Type.SOCKS, InetSocketAddress("127.0.0.1", LOCAL_PORT)) else null

    fun isEnabled(ctx: Context): Boolean =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_ENABLED, false)

    fun setEnabled(ctx: Context, on: Boolean) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(KEY_ENABLED, on).apply()
    }

    /** Start the in-process sing-box. Blocking (call off the main thread).
     *  Returns true once the local proxy is listening (idempotent). */
    @Synchronized
    fun start(): Boolean {
        if (isActive) return true
        return runCatching {
            val svc = rcqbox.Rcqbox.newBoxService()
            svc.start(buildConfig())
            box = svc
            isActive = true
            android.util.Log.i("RCQsingbox", "started — local proxy 127.0.0.1:$LOCAL_PORT")
            true
        }.getOrElse {
            android.util.Log.w("RCQsingbox", "start failed: ${it.message}")
            isActive = false
            box = null
            false
        }
    }

    @Synchronized
    fun stop() {
        isActive = false
        box?.let { runCatching { it.stop() } }
        box = null
    }

    /** sing-box config: a local mixed inbound + one outbound per relay, behind
     *  a `urltest` selector that probes /health through each and picks the
     *  fastest (re-evaluated every 5 min). The urltest outbound is first =
     *  the default route. Format mirrors the iOS buildConfig exactly. */
    private fun buildConfig(): String {
        val rs = relays()
        val outbounds = JSONArray()
        outbounds.put(JSONObject().apply {
            put("type", "urltest")
            put("tag", "out")
            put("outbounds", JSONArray(rs.map { it.tag }))
            put("url", "https://api.rcq.app/health")
            put("interval", "5m")
            put("tolerance", 50)
        })
        rs.forEach { outbounds.put(if (it.proto == "hysteria2") hysteria2Outbound(it) else vlessOutbound(it)) }

        val inbound = JSONObject().apply {
            put("type", "mixed")
            put("tag", "in")
            put("listen", "127.0.0.1")
            put("listen_port", LOCAL_PORT)
        }
        return JSONObject().apply {
            put("log", JSONObject().put("level", "warn"))
            put("inbounds", JSONArray().put(inbound))
            put("outbounds", outbounds)
        }.toString()
    }

    private fun vlessOutbound(r: Relay): JSONObject = JSONObject().apply {
        put("type", "vless")
        put("tag", r.tag)
        put("server", r.server)
        put("server_port", r.port)
        put("uuid", r.uuid ?: "")
        put("flow", r.flow ?: "xtls-rprx-vision")
        put("tls", JSONObject().apply {
            put("enabled", true)
            put("server_name", r.sni)
            put("utls", JSONObject().apply { put("enabled", true); put("fingerprint", "chrome") })
            put("reality", JSONObject().apply {
                put("enabled", true)
                put("public_key", r.publicKey ?: "")
                put("short_id", r.shortId ?: "")
            })
        })
    }

    /** Hysteria2 outbound: UDP + Salamander obfs (every QUIC packet XOR-wrapped
     *  so DPI can't fingerprint the handshake). insecure=true — the relay has a
     *  self-signed cert; auth is the user + obfs password, not PKI. */
    private fun hysteria2Outbound(r: Relay): JSONObject = JSONObject().apply {
        put("type", "hysteria2")
        put("tag", r.tag)
        put("server", r.server)
        put("server_port", r.port)
        put("password", r.password ?: "")
        put("tls", JSONObject().apply {
            put("enabled", true)
            put("server_name", r.sni)
            put("insecure", true)
        })
        r.obfsPassword?.takeIf { it.isNotEmpty() }?.let {
            put("obfs", JSONObject().apply { put("type", "salamander"); put("password", it) })
        }
    }
}

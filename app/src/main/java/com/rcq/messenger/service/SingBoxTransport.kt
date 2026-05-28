package com.rcq.messenger.service

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.json.JSONArray
import org.json.JSONObject
import java.net.InetSocketAddress
import java.net.Socket
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class RelayConfig(
    val version: Int = 0,
    val relays: List<RelayEntry> = emptyList(),
    val sig: String = ""
)

@Serializable
data class RelayEntry(
    val tag: String,
    val proto: String,
    val server: String,
    val port: Int,
    val sni: String,
    val password: String? = null,
    val obfs_password: String? = null,
    val uuid: String? = null,
    val public_key: String? = null,
    val short_id: String? = null,
    val flow: String? = null,
    val priority: Int = 0
)

// Mirrors iOS SingBoxTransport.swift: same relay-config.json, same JSON format
// for sing-box, same SOCKS5 local port 1089.
//
// ACTIVATE: download libbox.aar from https://github.com/SagerNet/sing-box/releases
// → place at app/libs/libbox.aar
// → add to app/build.gradle.kts: implementation(files("libs/libbox.aar"))
// → uncomment the LibboxBoxService lines below

@Singleton
class SingBoxTransport @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "SingBoxTransport"
        const val LOCAL_PORT = 1089
        private const val PREF_ENABLED = "rcq.singbox.enabled"
        private const val PREF_LAST_RELAY = "rcq.singbox.lastGoodRelayTag"
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences("singbox", Context.MODE_PRIVATE)
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    // private var boxService: io.nekohasekai.libbox.BoxService? = null

    var isActive = false
        private set

    val isEnabled: Boolean get() = prefs.getBoolean(PREF_ENABLED, false)

    fun proxyAddress(): InetSocketAddress? =
        if (isActive) InetSocketAddress("127.0.0.1", LOCAL_PORT) else null

    suspend fun start() {
        if (isActive) return
        val config = buildSingBoxConfig() ?: run {
            Log.e(TAG, "Failed to build sing-box config"); return
        }
        // withContext(Dispatchers.IO) {
        //     boxService = io.nekohasekai.libbox.BoxService()
        //     boxService!!.start(config)
        // }
        // isActive = true
        // scope.launch { probeRelaysInBackground() }
        Log.i(TAG, "Config ready (add libbox.aar to activate):\n${config.take(200)}")
    }

    fun stop() {
        // boxService?.stop(); boxService = null
        isActive = false
    }

    suspend fun setEnabled(on: Boolean) {
        prefs.edit().putBoolean(PREF_ENABLED, on).apply()
        if (on) start() else stop()
    }

    private fun buildSingBoxConfig(): String? {
        val relays = loadRelays() ?: return null
        val ordered = orderedRelays(relays)
        val outbounds = JSONArray().apply {
            put(JSONObject().apply {
                put("type", "urltest"); put("tag", "out")
                put("outbounds", JSONArray(ordered.map { it.tag }))
                put("url", "https://api.rcq.app/health")
                put("interval", "5m"); put("tolerance", 50)
            })
            for (r in ordered) put(if (r.proto == "vless") vlessOutbound(r) else hysteria2Outbound(r))
        }
        return JSONObject().apply {
            put("log", JSONObject().put("level", "warn"))
            put("inbounds", JSONArray().put(JSONObject().apply {
                put("type", "mixed"); put("tag", "in")
                put("listen", "127.0.0.1"); put("listen_port", LOCAL_PORT)
            }))
            put("outbounds", outbounds)
        }.toString()
    }

    private fun vlessOutbound(r: RelayEntry) = JSONObject().apply {
        put("type", "vless"); put("tag", r.tag)
        put("server", r.server); put("server_port", r.port)
        put("uuid", r.uuid ?: ""); put("flow", r.flow ?: "xtls-rprx-vision")
        put("tls", JSONObject().apply {
            put("enabled", true); put("server_name", r.sni)
            put("utls", JSONObject().put("enabled", true).put("fingerprint", "chrome"))
            put("reality", JSONObject().apply {
                put("enabled", true)
                put("public_key", r.public_key ?: "")
                put("short_id", r.short_id ?: "")
            })
        })
    }

    private fun hysteria2Outbound(r: RelayEntry) = JSONObject().apply {
        put("type", "hysteria2"); put("tag", r.tag)
        put("server", r.server); put("server_port", r.port)
        put("password", r.password ?: "")
        put("tls", JSONObject().apply {
            put("enabled", true); put("server_name", r.sni); put("insecure", true)
        })
        r.obfs_password?.takeIf { it.isNotEmpty() }?.let {
            put("obfs", JSONObject().put("type", "salamander").put("password", it))
        }
    }

    private fun loadRelays(): List<RelayEntry>? = runCatching {
        val raw = context.assets.open("relay-config.json").bufferedReader().readText()
        json.decodeFromString<RelayConfig>(raw).relays.sortedBy { it.priority }
    }.onFailure { Log.e(TAG, "relay-config load failed", it) }.getOrNull()

    private fun orderedRelays(base: List<RelayEntry>): List<RelayEntry> {
        val last = prefs.getString(PREF_LAST_RELAY, null) ?: return base
        val idx = base.indexOfFirst { it.tag == last }.takeIf { it > 0 } ?: return base
        return base.toMutableList().also { it.add(0, it.removeAt(idx)) }
    }

    private suspend fun probeRelaysInBackground() {
        val winner = loadRelays()?.firstOrNull { probeTcp(it.server, it.port) } ?: return
        prefs.edit().putString(PREF_LAST_RELAY, winner.tag).apply()
        Log.i(TAG, "Fastest relay: ${winner.tag}")
    }

    private suspend fun probeTcp(host: String, port: Int): Boolean =
        withContext(Dispatchers.IO) {
            runCatching { Socket().use { it.connect(InetSocketAddress(host, port), 4000); true } }
                .getOrDefault(false)
        }
}

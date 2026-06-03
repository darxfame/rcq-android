package com.rcq.messenger.service

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import org.bouncycastle.crypto.params.Ed25519PublicKeyParameters
import org.bouncycastle.crypto.signers.Ed25519Signer
import org.json.JSONArray
import org.json.JSONObject
import timber.log.Timber
import java.net.URL
import java.util.Base64
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RelayConfigRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        // Ed25519 public key for relay-config.json signature verification
        private const val SIGNING_KEY_B64 = "TY834OFcBvtUqHcnVw/QrPBOaEAZo7a1GAmABMhjkT8="
        private val ENDPOINTS = listOf(
            "https://raw.githubusercontent.com/rcq-messenger/rcq-ios/main/relay-config.json",
            "https://relay.rcq.app/v1/config"
        )
        private const val CACHE_FILE = "relay-config.json"
        private const val PREFS = "rcq_relays"
        private const val KEY_CUSTOM_RELAYS = "custom_relays"
        private const val KEY_SELECTED_RELAY_TAG = "selected_relay_tag"

        private val LOCAL_PRIORITY_RELAYS = listOf(
            RelayEntry(
                tag = "relay-uk-google-vision", proto = "vless",
                server = "uk.e0f.network", port = 443, sni = "www.google.com",
                uuid = "834703d2-abf1-471a-83b5-ee87e0b6cd8e",
                public_key = "nPcUKydxoRI66O3tT9O4QCZpLjOBvkGsiXG7pDX1BBw",
                short_id = "6ba85179e30d4fc2",
                flow = "xtls-rprx-vision",
                fingerprint = "chrome",
                allow_insecure = false,
                transport_type = "tcp",
                priority = -120
            ),
            RelayEntry(
                tag = "relay-usa-amd-xhttp", proto = "vless",
                server = "80.209.243.23", port = 443, sni = "amd.com",
                uuid = "63bbedb0-2e27-4a15-9aca-b0856d5f9b3a",
                public_key = "YQL5CMcuLgjJwH-2f10LlWx79ZDMnRzl8oZAFPPUqmk",
                short_id = "2a3f5c8d",
                fingerprint = "random",
                allow_insecure = false,
                transport_type = "xhttp",
                transport_path = "/telemetry",
                xhttp_mode = "auto",
                priority = -100
            )
        )

        // Mirrors iOS RelayConfigStore.bundledFallback — все 8 relay из relay-config.json v9
        private val BUNDLED_FALLBACK = listOf(
            RelayEntry(
                tag = "relay-do-fra-yandex-hy2", proto = "hysteria2",
                server = "165.22.90.214", port = 443, sni = "www.yandex.ru",
                password = "JN0qzA4LJfhHPKKN3QHj4eN8",
                obfs_password = "jXfGkLToOkTihpeJzDiNf8Bb", priority = 0
            ),
            RelayEntry(
                tag = "relay-do-fra-yandex", proto = "vless",
                server = "165.22.90.214", port = 443, sni = "www.yandex.ru",
                uuid = "2081b3c4-faaa-4cce-a0ab-607197b28237",
                public_key = "n33TZTLNrc6X7jTGrKWex_sk8aIQ6Qqz-eC8lqYMii8",
                short_id = "aa5d483441e59ac7", flow = "xtls-rprx-vision", priority = 1
            ),
            RelayEntry(
                tag = "relay-oracle-il-hy2", proto = "hysteria2",
                server = "129.159.143.135", port = 443, sni = "www.microsoft.com",
                password = "bvuvu74CVsiXdcJazcYphnO5",
                obfs_password = "PaEHrZABTk36orhfFON7Jure", priority = 2
            ),
            RelayEntry(
                tag = "relay-oracle-il", proto = "vless",
                server = "129.159.143.135", port = 443, sni = "www.microsoft.com",
                uuid = "ff005e0c-175e-4475-a166-eeac88f514e2",
                public_key = "_Hhc-2pjkvR914mddMdmuoOVaT74vWR8Gby7KmJp9F8",
                short_id = "318567678ac9878e", flow = "xtls-rprx-vision", priority = 3
            ),
            RelayEntry(
                tag = "relay-gcp-hy2", proto = "hysteria2",
                server = "35.238.53.96", port = 443, sni = "www.apple.com",
                password = "QaY3uT8EmfZxfON65jaT5bSu",
                obfs_password = "fLpJ2c211xjnZcP9VNcNpbZP", priority = 4
            ),
            RelayEntry(
                tag = "relay-gcp", proto = "vless",
                server = "35.238.53.96", port = 443, sni = "www.apple.com",
                uuid = "8e3b35d3-18a6-406d-9ac6-c5558a806663",
                public_key = "mQZ8CJeMWyf7oYGWJG8oOI52or2kx4yTthl6AGZkSTw",
                short_id = "b5b8979af1f27aab", flow = "xtls-rprx-vision", priority = 5
            ),
            RelayEntry(
                tag = "relay-aws-sg-hy2", proto = "hysteria2",
                server = "47.129.249.170", port = 443, sni = "www.amazon.com",
                password = "IjO9NlfvuXuP8w4tZNXHZwGL",
                obfs_password = "yBlwN4J7IMzQi3VCMo0oKZHh", priority = 6
            ),
            RelayEntry(
                tag = "relay-aws-sg", proto = "vless",
                server = "47.129.249.170", port = 443, sni = "www.amazon.com",
                uuid = "2b0a3318-7bfc-4ff2-83ae-2f322cb91ef8",
                public_key = "xxasGveo2BtMx4doxftb-AJcvIXL-9LpymZcV9tIRxo",
                short_id = "533142a04b016a00", flow = "xtls-rprx-vision", priority = 7
            )
        )
    }

    private val jsonCodec = Json { ignoreUnknownKeys = true; isLenient = true }
    private val cacheFile get() = context.filesDir.resolve(CACHE_FILE)
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private var cached: List<RelayEntry>? = null

    var selectedRelayTag: String
        get() = prefs.getString(KEY_SELECTED_RELAY_TAG, "") ?: ""
        set(value) {
            prefs.edit().putString(KEY_SELECTED_RELAY_TAG, value.trim()).apply()
        }

    fun currentRelays(): List<RelayEntry> {
        cached?.takeIf { it.isNotEmpty() }?.let { return it }
        loadFromDisk()?.let { cached = withLocalPriorityRelays(it); return cached!! }
        return withLocalPriorityRelays(BUNDLED_FALLBACK)
    }

    fun selectedOrCurrentRelays(): List<RelayEntry> =
        RelayPreferencePolicy.applySelection(currentRelays(), selectedRelayTag)

    fun addCustomVless(url: String): Result<RelayEntry> = runCatching {
        val relay = VlessRelayParser.parse(url).getOrThrow()
        val relays = customRelays()
            .filterNot { it.tag == relay.tag || (it.uuid == relay.uuid && it.server == relay.server) } + relay
        prefs.edit().putString(KEY_CUSTOM_RELAYS, jsonCodec.encodeToString(relays)).apply()
        cached = null
        selectedRelayTag = relay.tag
        relay
    }

    fun customRelays(): List<RelayEntry> = runCatching {
        val raw = prefs.getString(KEY_CUSTOM_RELAYS, "")?.takeIf { it.isNotBlank() } ?: return emptyList()
        jsonCodec.decodeFromString<List<RelayEntry>>(raw)
    }.onFailure { Timber.w("RelayConfig: custom relays decode failed: ${it.message}") }.getOrDefault(emptyList())

    fun selectRelay(tag: String) {
        val exists = currentRelays().any { it.tag == tag }
        selectedRelayTag = if (exists) tag else ""
    }

    fun clearRelaySelection() {
        selectedRelayTag = ""
    }

    suspend fun refreshInBackground() = withContext(Dispatchers.IO) {
        for (url in ENDPOINTS) {
            runCatching {
                val data = URL(url).readBytes()
                val relays = verifyAndDecode(data) ?: return@runCatching
                cached = withLocalPriorityRelays(relays)
                cacheFile.writeBytes(data)
                Timber.d("RelayConfig: refreshed from $url — ${cached?.size ?: 0} relays")
                return@withContext
            }.onFailure { Timber.w("RelayConfig: fetch from $url failed: ${it.message}") }
        }
        Timber.d("RelayConfig: all endpoints failed, using ${currentRelays().size} cached/bundled relays")
    }

    private fun loadFromDisk(): List<RelayEntry>? = runCatching {
        if (!cacheFile.exists()) return null
        verifyAndDecode(cacheFile.readBytes())
    }.onFailure { Timber.w("RelayConfig: disk load failed: ${it.message}") }.getOrNull()

    fun verifyAndDecode(data: ByteArray): List<RelayEntry>? = runCatching {
        val raw = JSONObject(String(data, Charsets.UTF_8))
        val sigB64 = raw.optString("sig", "").takeIf { it.isNotEmpty() } ?: run {
            Timber.w("RelayConfig: missing sig field"); return null
        }
        val sigBytes = Base64.getDecoder().decode(sigB64)

        // Canonical JSON = all fields except "sig", sorted keys (mirrors iOS .sortedKeys)
        val withoutSig = JSONObject(String(data, Charsets.UTF_8)).apply { remove("sig") }
        val canonical = canonicalJson(withoutSig).toByteArray(Charsets.UTF_8)

        val pubBytes = Base64.getDecoder().decode(SIGNING_KEY_B64)
        val verifier = Ed25519Signer()
        verifier.init(false, Ed25519PublicKeyParameters(pubBytes))
        verifier.update(canonical, 0, canonical.size)
        if (!verifier.verifySignature(sigBytes)) {
            Timber.w("RelayConfig: Ed25519 signature invalid — rejecting config")
            return null
        }

        jsonCodec.decodeFromString<RelayConfig>(String(data, Charsets.UTF_8))
            .relays.sortedBy { it.priority }
    }.onFailure { Timber.w("RelayConfig: verify/decode failed: ${it.message}") }.getOrNull()

    private fun withLocalPriorityRelays(relays: List<RelayEntry>): List<RelayEntry> {
        val localAndCustom = LOCAL_PRIORITY_RELAYS + customRelays()
        val localTags = localAndCustom.mapTo(mutableSetOf()) { it.tag }
        return (localAndCustom + relays.filterNot { it.tag in localTags })
            .sortedBy { it.priority }
    }

    // Matches iOS JSONSerialization options [.sortedKeys, .withoutEscapingSlashes]
    private fun canonicalJson(obj: JSONObject): String = buildString {
        append('{')
        var first = true
        obj.keys().asSequence().sorted().forEach { key ->
            if (!first) append(',')
            first = false
            append(quoteString(key)); append(':'); append(canonicalValue(obj[key]))
        }
        append('}')
    }

    private fun canonicalValue(v: Any): String = when (v) {
        is JSONObject -> canonicalJson(v)
        is JSONArray -> buildString {
            append('[')
            for (i in 0 until v.length()) { if (i > 0) append(','); append(canonicalValue(v[i])) }
            append(']')
        }
        is String -> quoteString(v)
        is Boolean -> if (v) "true" else "false"
        is Number -> v.toString()
        JSONObject.NULL -> "null"
        else -> quoteString(v.toString())
    }

    // No forward-slash escaping — matches iOS .withoutEscapingSlashes
    private fun quoteString(s: String): String = buildString {
        append('"')
        for (c in s) when (c) {
            '"'  -> append("\\\"")
            '\\' -> append("\\\\")
            '\n' -> append("\\n")
            '\r' -> append("\\r")
            '\t' -> append("\\t")
            else -> if (c.code < 0x20) append("\\u%04x".format(c.code)) else append(c)
        }
        append('"')
    }
}

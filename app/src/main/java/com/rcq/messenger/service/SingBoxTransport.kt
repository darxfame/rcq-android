package com.rcq.messenger.service

import android.content.Context
import android.content.SharedPreferences
import com.rcq.messenger.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import timber.log.Timber
import java.net.HttpURLConnection
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.Socket
import java.net.URL
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
    val fingerprint: String? = null,
    val allow_insecure: Boolean = false,
    val transport_type: String? = null,
    val transport_path: String? = null,
    val xhttp_mode: String? = null,
    val priority: Int = 0
)

data class EmbeddedRelaySelection(
    val engine: String,
    val relays: List<RelayEntry>
)

object RelaySelectionPolicy {
    fun orderForAndroid(
        base: List<RelayEntry>,
        lastGoodTag: String?,
        supportsXhttp: Boolean = false
    ): List<RelayEntry> {
        val supported = base.filter { it.isSupportedByEngine(supportsXhttp) }
        val priorityRelays = supported
            .filter { it.priority < 0 }
            .sortedBy { it.priority }
        val regularRelays = supported
            .filter { it.priority >= 0 }
            .sortedBy { it.priority }

        val tcpRelays = regularRelays.filter { it.isTcpRelay }
        val udpRelays = regularRelays.filterNot { it.isTcpRelay }
        return priorityRelays + promoteLastGood(tcpRelays, lastGoodTag) + udpRelays
    }

    fun tcpProbeCandidates(base: List<RelayEntry>, supportsXhttp: Boolean = false): List<RelayEntry> =
        orderForAndroid(base, lastGoodTag = null, supportsXhttp = supportsXhttp)
            .filter { it.isTcpRelay }

    fun selectForEmbeddedTransport(
        base: List<RelayEntry>,
        lastGoodTag: String?,
        xrayAvailable: Boolean
    ): EmbeddedRelaySelection {
        val orderedWithXhttp = orderForAndroid(base, lastGoodTag = lastGoodTag, supportsXhttp = xrayAvailable)
        val first = orderedWithXhttp.firstOrNull()
        if (xrayAvailable && first?.isXhttpRelay == true) {
            val xrayRelays = orderedWithXhttp.filter { it.isXhttpRelay }
            return EmbeddedRelaySelection("xray", xrayRelays)
        }
        val xrayRelays = base
            .filter { it.isXhttpRelay }
            .sortedBy { it.priority }
        if (xrayAvailable && first == null && xrayRelays.isNotEmpty()) {
            return EmbeddedRelaySelection("xray", promoteLastGood(xrayRelays, lastGoodTag))
        }
        return EmbeddedRelaySelection(
            "sing-box",
            orderForAndroid(base, lastGoodTag = lastGoodTag, supportsXhttp = false)
        )
    }

    fun selectForEngine(
        engine: String,
        base: List<RelayEntry>,
        lastGoodTag: String?
    ): EmbeddedRelaySelection {
        return when (engine) {
            "xray" -> EmbeddedRelaySelection(
                engine = "xray",
                relays = promoteLastGood(
                    base.filter { it.isXhttpRelay }.sortedBy { it.priority },
                    lastGoodTag
                )
            )
            else -> EmbeddedRelaySelection(
                engine = "sing-box",
                relays = orderForAndroid(base, lastGoodTag = lastGoodTag, supportsXhttp = false)
            )
        }
    }

    private fun promoteLastGood(relays: List<RelayEntry>, lastGoodTag: String?): List<RelayEntry> {
        if (lastGoodTag.isNullOrBlank()) return relays
        val idx = relays.indexOfFirst { it.tag == lastGoodTag }
        if (idx <= 0) return relays
        return relays.toMutableList().also { it.add(0, it.removeAt(idx)) }
    }

    private val RelayEntry.isTcpRelay: Boolean
        get() = proto.equals("vless", ignoreCase = true)

    private val RelayEntry.isXhttpRelay: Boolean
        get() = transport_type.equals("xhttp", ignoreCase = true)

    private fun RelayEntry.isSupportedByEngine(supportsXhttp: Boolean): Boolean {
        val transport = transport_type?.trim()?.lowercase()
        return transport != "xhttp" || supportsXhttp
    }
}

internal object SingBoxConfigJsonBuilder {
    fun build(ordered: List<RelayEntry>, localPort: Int): String =
        build(ordered, localPort, legacyHysteria2Obfs = false)

    fun buildLegacy(relay: RelayEntry, localPort: Int): String =
        build(listOf(relay), localPort, legacyHysteria2Obfs = true)

    private fun build(
        ordered: List<RelayEntry>,
        localPort: Int,
        legacyHysteria2Obfs: Boolean
    ): String {
        val healthUrl = BuildConfig.API_BASE_URL.trimEnd('/') + "/health"
        return buildJsonObject {
            putJsonObject("log") { put("level", "warn") }
            putJsonObject("dns") {
                putJsonArray("servers") {
                    add(buildJsonObject {
                        put("tag", "cloudflare")
                        put("address", "1.1.1.1")
                        put("detour", "direct")
                    })
                    add(buildJsonObject {
                        put("tag", "google")
                        put("address", "8.8.8.8")
                        put("detour", "direct")
                    })
                }
                put("strategy", "ipv4_only")
            }
            putJsonArray("inbounds") {
                add(buildJsonObject {
                    put("type", "mixed"); put("tag", "in")
                    put("listen", "127.0.0.1"); put("listen_port", localPort)
                })
            }
            putJsonArray("outbounds") {
                add(buildJsonObject {
                    put("type", "urltest")
                    put("tag", "out")
                    putJsonArray("outbounds") { ordered.forEach { add(it.tag) } }
                    put("url", healthUrl)
                    put("interval", "5m")
                    put("tolerance", 50)
                })
                ordered.forEach {
                    add(
                        if (it.proto == "vless") {
                            vlessOutbound(it)
                        } else {
                            hysteria2Outbound(it, legacyHysteria2Obfs)
                        }
                    )
                }
            }
        }.toString()
    }

    private fun vlessOutbound(r: RelayEntry) = buildJsonObject {
        put("type", "vless"); put("tag", r.tag)
        put("server", r.server); put("server_port", r.port)
        put("uuid", r.uuid ?: "")
        r.flow?.takeIf { it.isNotBlank() }?.let { put("flow", it) }
        putJsonObject("tls") {
            put("enabled", true); put("server_name", r.sni)
            put("insecure", r.allow_insecure)
            putJsonObject("utls") {
                put("enabled", true)
                put("fingerprint", r.fingerprint?.takeIf { it.isNotBlank() } ?: "chrome")
            }
            putJsonObject("reality") {
                put("enabled", true)
                put("public_key", r.public_key ?: "")
                put("short_id", r.short_id ?: "")
            }
        }
        r.transport_type
            ?.takeIf { it.isNotBlank() && !it.equals("tcp", ignoreCase = true) }
            ?.let { type ->
                putJsonObject("transport") {
                    put("type", type)
                    r.transport_path?.takeIf { it.isNotBlank() }?.let { put("path", it) }
                    if (type == "xhttp") {
                        r.xhttp_mode?.takeIf { it.isNotBlank() }?.let { put("mode", it) }
                    }
                }
            }
    }

    private fun hysteria2Outbound(r: RelayEntry, legacyObfs: Boolean) = buildJsonObject {
        put("type", "hysteria2"); put("tag", r.tag)
        put("server", r.server); put("server_port", r.port)
        put("password", r.password ?: "")
        putJsonObject("tls") {
            put("enabled", true); put("server_name", r.sni); put("insecure", true)
        }
        r.obfs_password?.takeIf { it.isNotEmpty() }?.let { obfsPwd ->
            // SingBox ≥1.8: obfs must be an object {type, password}.
            // The legacy flat-string form ("obfs":"salamander") is rejected:
            // "cannot unmarshal string into Go value of type option.Hysteria2Obfs".
            putJsonObject("obfs") {
                put("type", "salamander")
                put("password", obfsPwd)
            }
        }
    }
}

internal object RelayStartPlan {
    fun attempts(ordered: List<RelayEntry>): List<List<RelayEntry>> =
        ordered.map { first -> listOf(first) + ordered.filterNot { it.tag == first.tag } }
}

// Mirrors iOS SingBoxTransport.swift: same relay-config.json, same sing-box JSON format,
// same SOCKS5 local port 1089.
//
// ACTIVATE sing-box: download libbox.aar from https://github.com/SagerNet/sing-box/releases
// → place at app/libs/libbox.aar
// → add to app/build.gradle.kts: implementation(files("libs/libbox.aar"))
// → uncomment the boxService lines below

@Singleton
class SingBoxTransport @Inject constructor(
    @ApplicationContext private val context: Context,
    private val relayConfigRepository: RelayConfigRepository
) {
    companion object {
        private const val TAG = "SingBoxTransport"
        const val LOCAL_PORT = 1089
        private const val PREF_ENABLED = "rcq.singbox.enabled"
        private const val PREF_LAST_RELAY = "rcq.singbox.lastGoodRelayTag"
        private const val PREF_LAST_ERROR = "rcq.singbox.lastError"
        private const val BUNDLED_ENGINE_SUPPORTS_XHTTP = false
        private const val ROUTE_PROBE_ATTEMPTS = 3
        private const val ROUTE_PROBE_RETRY_MS = 350L
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences("singbox", Context.MODE_PRIVATE)

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /**
     * Держим ссылку на нативный сервис через Any?, чтобы файл компилировался
     * и без libbox.aar на classpath. Реальный тип — io.nekohasekai.libbox.BoxService.
     */
    private var boxService: Any? = null

    var isActive = false
        private set

    /**
     * true только если нативный движок libbox реально присутствует на classpath
     * (app/libs/libbox.aar добавлен). Проверяется один раз через reflection,
     * чтобы UI мог честно показать «движок не установлен» вместо ложного «активен».
     */
    val isEngineAvailable: Boolean by lazy {
        runCatching { Class.forName("io.nekohasekai.libbox.BoxService") }.isSuccess
    }

    val isEnabled: Boolean get() = prefs.getBoolean(PREF_ENABLED, false)

    val lastStartError: String? get() = prefs.getString(PREF_LAST_ERROR, null)
        ?.takeIf { it.isNotEmpty() }

    fun proxyAddress(): InetSocketAddress? =
        if (isActive) InetSocketAddress("127.0.0.1", LOCAL_PORT) else null

    suspend fun start(): Boolean = withContext(Dispatchers.IO) {
        if (isActive) return@withContext true
        val attempts = buildSingBoxConfigAttempts() ?: run {
            Timber.e("$TAG: Failed to build sing-box config")
            return@withContext false
        }
        if (!isEngineAvailable) {
            // Честно фиксируем причину: нативный движок не собран в APK.
            val msg = "Движок sing-box не установлен: добавьте app/libs/libbox.aar и пересоберите."
            prefs.edit().putString(PREF_LAST_ERROR, msg).apply()
            Timber.w("$TAG: engine unavailable — bypass NOT active. $msg")
            return@withContext false
        }
        var lastError: Throwable? = null
        for ((relayTag, config) in attempts) {
            var service: Any? = null
            runCatching {
                Timber.d("$TAG: trying relay $relayTag")
                service = startNativeEngine(config)
                if (!validateProxyRoute()) {
                    Timber.w("$TAG: relay $relayTag started, route check failed — keeping active (iOS parity)")
                }
                boxService = service
                isActive = true
                prefs.edit()
                    .remove(PREF_LAST_ERROR)
                    .putString(PREF_LAST_RELAY, relayTag)
                    .apply()
                Timber.i("$TAG: sing-box started on 127.0.0.1:$LOCAL_PORT via $relayTag")
                scope.launch { probeRelaysInBackground() }
                return@withContext true
            }.onFailure { e ->
                lastError = e
                service?.let { stopService(it) }
                boxService = null
                isActive = false
                // Traverse cause chain — InvocationTargetException wraps the real Go panic
                val causeChain = generateSequence(e) { it.cause }
                    .joinToString(" ← ") { "${it.javaClass.simpleName}: ${it.message}" }
                Timber.w("$TAG: relay $relayTag failed: $causeChain")
            }
        }
        // Extract deepest non-null message so the real Go panic is surfaced in UI
        val realMsg = lastError?.let {
            generateSequence(it) { c -> c.cause }
                .mapNotNull { c -> c.message?.takeIf { m -> m.isNotBlank() } }
                .firstOrNull()?.take(400)
        } ?: "Ни один встроенный relay не прошел проверку (${attempts.size} попыток)"
        prefs.edit().putString(PREF_LAST_ERROR, realMsg).apply()
        Timber.e(lastError, "$TAG: start failed: $realMsg")
        false
    }

    private suspend fun validateProxyRoute(): Boolean {
        repeat(ROUTE_PROBE_ATTEMPTS) { attempt ->
            val ok = runCatching {
                val proxy = Proxy(Proxy.Type.SOCKS, InetSocketAddress("127.0.0.1", LOCAL_PORT))
                val url = URL(BuildConfig.API_BASE_URL.trimEnd('/') + "/health")
                val conn = (url.openConnection(proxy) as HttpURLConnection).apply {
                    connectTimeout = 4_000
                    readTimeout = 4_000
                    requestMethod = "GET"
                    setRequestProperty("Accept", "application/json")
                }
                try {
                    val code = conn.responseCode
                    code in 200..499
                } finally {
                    conn.disconnect()
                }
            }.onFailure {
                Timber.w(it, "$TAG: proxy route validation failed (attempt ${attempt + 1}/$ROUTE_PROBE_ATTEMPTS)")
            }.getOrDefault(false)
            if (ok) return true
            if (attempt < ROUTE_PROBE_ATTEMPTS - 1) {
                delay(ROUTE_PROBE_RETRY_MS)
            }
        }
        return false
    }

    /**
     * Запускает нативное ядро через reflection — без статической зависимости от libbox,
     * чтобы модуль компилировался и без .aar.
     *
     * Реальное API libbox.aar (проверено `javap` против classes.jar):
     *   - `Libbox.setup(SetupOptions)` — ОБЯЗАТЕЛЬНО до newService(); задаёт writable пути
     *     (basePath/workingPath/tempPath) и fixAndroidStack=true. Без этого ядро пишет
     *     `cache.db` в CWD ("/" на Android — read-only) и падает.
     *   - `SetupOptions()` — пустой конструктор + сеттеры
     *   - `Libbox.newService(String config, PlatformInterface platform)` → BoxService
     *   - `boxService.start()` — без аргументов
     */
    private fun startNativeEngine(config: String): Any {
        val libboxClass = Class.forName("io.nekohasekai.libbox.Libbox")
        val setupOptsClass = Class.forName("io.nekohasekai.libbox.SetupOptions")
        val platformIface = Class.forName("io.nekohasekai.libbox.PlatformInterface")

        // 1) Writable директории для Go-runtime
        val baseDir = context.filesDir.absolutePath
        val tempDir = context.cacheDir.absolutePath

        // 2) Libbox.setup(SetupOptions) — фиксирует basePath/workingPath до старта Go-runtime,
        //    чтобы cache.db писался в filesDir, а не в "/" (read-only на Android)
        val opts = setupOptsClass.getConstructor().newInstance()
        setupOptsClass.getMethod("setBasePath", String::class.java).invoke(opts, baseDir)
        setupOptsClass.getMethod("setWorkingPath", String::class.java).invoke(opts, baseDir)
        setupOptsClass.getMethod("setTempPath", String::class.java).invoke(opts, tempDir)
        runCatching {
            setupOptsClass.getMethod("setFixAndroidStack", Boolean::class.javaPrimitiveType)
                .invoke(opts, true)
        }
        libboxClass.getMethod("setup", setupOptsClass).invoke(null, opts)
        Timber.d("$TAG: Libbox.setup() basePath=$baseDir tempPath=$tempDir")

        // 3) PlatformInterface stub
        val platformStub = java.lang.reflect.Proxy.newProxyInstance(
            platformIface.classLoader,
            arrayOf(platformIface),
            EmptyPlatformHandler
        )

        // 4) newService → start
        val svc = libboxClass
            .getMethod("newService", String::class.java, platformIface)
            .invoke(null, config, platformStub)
            ?: error("Libbox.newService вернул null")
        svc.javaClass.getMethod("start").invoke(svc)
        Timber.d("$TAG: engine started via Libbox.newService + EmptyPlatformHandler")
        return svc
    }

    fun stop() {
        val svc = boxService
        boxService = null
        isActive = false
        if (svc != null) stopService(svc)
        Timber.d("$TAG: stopped")
    }

    private fun stopService(svc: Any) {
        // Метод закрытия отличается между сборками: close() (SagerNet) или stop() (iOS-style)
        runCatching { svc.javaClass.getMethod("close").invoke(svc) }
            .recoverCatching { svc.javaClass.getMethod("stop").invoke(svc) }
            .onFailure { Timber.w(it, "$TAG: stop failed") }
    }

    suspend fun setEnabled(on: Boolean) {
        prefs.edit().putBoolean(PREF_ENABLED, on).apply()
        if (on) start() else stop()
    }

    private fun buildSingBoxConfigAttempts(): List<Pair<String, String>>? {
        val relays = relayConfigRepository.selectedOrCurrentRelays()
        if (relays.isEmpty()) { Timber.w("$TAG: no relays available"); return null }
        val ordered = orderedRelays(relays)
        if (ordered.isEmpty()) {
            Timber.w("$TAG: no relays supported by bundled sing-box engine")
            return null
        }
        // Each attempt uses ONE relay only — avoids a single bad relay breaking the whole config.
        // urltest with all relays means a parse error in ANY outbound kills the entire attempt.
        val normalAttempts = ordered.map { relay ->
            relay.tag to SingBoxConfigJsonBuilder.build(listOf(relay), LOCAL_PORT)
        }
        val legacyAttempts = ordered
            .filter { it.proto.equals("hysteria2", ignoreCase = true) && !it.obfs_password.isNullOrBlank() }
            .map { relay ->
                "${relay.tag}-legacy" to SingBoxConfigJsonBuilder.buildLegacy(relay, LOCAL_PORT)
            }
        return normalAttempts + legacyAttempts
    }

    private fun orderedRelays(base: List<RelayEntry>): List<RelayEntry> {
        val last = prefs.getString(PREF_LAST_RELAY, null)
        return RelaySelectionPolicy.orderForAndroid(
            base,
            last,
            supportsXhttp = BUNDLED_ENGINE_SUPPORTS_XHTTP
        )
    }

    private suspend fun probeRelaysInBackground() {
        val relays = relayConfigRepository.selectedOrCurrentRelays()
        val winner = RelaySelectionPolicy.tcpProbeCandidates(
            relays,
            supportsXhttp = BUNDLED_ENGINE_SUPPORTS_XHTTP
        )
            .firstOrNull { probeTcp(it.server, it.port) } ?: return
        prefs.edit().putString(PREF_LAST_RELAY, winner.tag).apply()
        Timber.i("$TAG: Fastest relay: ${winner.tag}")
    }

    private suspend fun probeTcp(host: String, port: Int): Boolean =
        withContext(Dispatchers.IO) {
            runCatching { Socket().use { it.connect(InetSocketAddress(host, port), 4000); true } }
                .getOrDefault(false)
        }
}

/**
 * Полноценная реализация PlatformInterface через reflection-прокси.
 * В режиме mixed/SOCKS5 (без tun-inbound) ядро sing-box не вызывает TUN-методы,
 * но ТРЕБУЕТ не-null возврат для systemCertificates(), getInterfaces(), readWIFIState(),
 * localDNSTransport(). Без этого Go-код паникует ещё до start().
 *
 * Для каждого метода возвращаем минимально-валидный объект:
 * - StringIterator / NetworkInterfaceIterator → прокси с hasNext()=false
 * - LocalDNSTransport → прокси с raw()=false, exchange/lookup = no-op
 * - WIFIState → new WIFIState("", "") через reflection
 * - int → 0 (openTun, findConnectionOwner, uidByPackageName)
 * - boolean → false
 * - String → ""
 * - void → null
 */
private object EmptyPlatformHandler : java.lang.reflect.InvocationHandler {
    override fun invoke(proxy: Any?, method: java.lang.reflect.Method, args: Array<out Any?>?): Any? {
        val name = method.name
        val ret = method.returnType

        // Методы, возвращающие итераторы — создаём пустой прокси
        if (name == "systemCertificates" || name == "getInterfaces") {
            return emptyIteratorProxy(ret)
        }
        // localDNSTransport — прокси с raw()=false
        if (name == "localDNSTransport") {
            return java.lang.reflect.Proxy.newProxyInstance(
                ret.classLoader, arrayOf(ret)
            ) { _, m, _ ->
                when (m.returnType) {
                    Boolean::class.javaPrimitiveType -> false
                    else -> null
                }
            }
        }
        // readWIFIState — new WIFIState("", "")
        if (name == "readWIFIState") {
            return runCatching {
                ret.getConstructor(String::class.java, String::class.java)
                    .newInstance("", "")
            }.getOrNull()
        }
        // writeLog — просто логируем
        if (name == "writeLog") {
            val msg = args?.firstOrNull() as? String
            if (msg != null) Timber.d("libbox: $msg")
            return null
        }

        // Дефолты по типу возврата
        return when (ret) {
            Boolean::class.javaPrimitiveType -> false
            Int::class.javaPrimitiveType -> 0
            Long::class.javaPrimitiveType -> 0L
            java.lang.String::class.java -> ""
            Void.TYPE -> null
            else -> null
        }
    }

    private fun emptyIteratorProxy(iface: Class<*>): Any {
        return java.lang.reflect.Proxy.newProxyInstance(
            iface.classLoader, arrayOf(iface)
        ) { _, m, _ ->
            when (m.name) {
                "hasNext" -> false
                "len" -> 0
                "next" -> null
                else -> null
            }
        }
    }
}

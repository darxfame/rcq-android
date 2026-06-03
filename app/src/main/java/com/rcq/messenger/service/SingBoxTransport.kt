package com.rcq.messenger.service

import android.content.Context
import android.content.SharedPreferences
import com.rcq.messenger.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import org.json.JSONArray
import org.json.JSONObject
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

    suspend fun start() = withContext(Dispatchers.IO) {
        if (isActive) return@withContext
        val config = buildSingBoxConfig() ?: run {
            Timber.e("$TAG: Failed to build sing-box config"); return@withContext
        }
        if (!isEngineAvailable) {
            // Честно фиксируем причину: нативный движок не собран в APK.
            val msg = "Движок sing-box не установлен: добавьте app/libs/libbox.aar и пересоберите."
            prefs.edit().putString(PREF_LAST_ERROR, msg).apply()
            Timber.w("$TAG: engine unavailable — bypass NOT active. $msg")
            return@withContext
        }
        runCatching {
            val service = startNativeEngine(config)
            boxService = service
            if (!validateProxyRoute()) {
                stopService(service)
                boxService = null
                error("Встроенный relay запустился, но api.rcq.app через него недоступен")
            }
            isActive = true
            prefs.edit().remove(PREF_LAST_ERROR).apply()
            Timber.i("$TAG: sing-box started on 127.0.0.1:$LOCAL_PORT")
            scope.launch { probeRelaysInBackground() }
        }.onFailure { e ->
            isActive = false
            boxService = null
            prefs.edit().putString(PREF_LAST_ERROR, e.message?.take(400) ?: "unknown").apply()
            Timber.e(e, "$TAG: start failed: ${e.message}")
        }
        Unit
    }

    private fun validateProxyRoute(): Boolean {
        val proxy = Proxy(Proxy.Type.SOCKS, InetSocketAddress("127.0.0.1", LOCAL_PORT))
        val url = URL(BuildConfig.API_BASE_URL.trimEnd('/') + "/health")
        return runCatching {
            val conn = (url.openConnection(proxy) as HttpURLConnection).apply {
                connectTimeout = 6_000
                readTimeout = 6_000
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
            Timber.w(it, "$TAG: proxy route validation failed")
        }.getOrDefault(false)
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

    private fun buildSingBoxConfig(): String? {
        val relays = relayConfigRepository.selectedOrCurrentRelays()
        if (relays.isEmpty()) { Timber.w("$TAG: no relays available"); return null }
        val ordered = orderedRelays(relays)
        if (ordered.isEmpty()) {
            Timber.w("$TAG: no relays supported by bundled sing-box engine")
            return null
        }
        val outbounds = JSONArray().apply {
            // selector вместо urltest: не требует cache.db для хранения истории проб.
            // Выбор быстрейшего relay делаем сами через probeRelaysInBackground() →
            // PREF_LAST_RELAY → orderedRelays() ставит победителя первым при следующем старте.
            put(JSONObject().apply {
                put("type", "selector"); put("tag", "out")
                put("outbounds", JSONArray(ordered.map { it.tag }))
                put("default", ordered.first().tag)
            })
            for (r in ordered) put(if (r.proto == "vless") vlessOutbound(r) else hysteria2Outbound(r))
        }
        // Sing-box по дефолту пишет cache.db в CWD ("/" на Android — read-only).
        // Защита в два слоя:
        //   1) Libbox.setup() в startNativeEngine() переключает CWD на filesDir
        //   2) selector вместо urltest — даже без cache.db работает корректно
        //   3) experimental.cache_file.enabled=false — на случай если ядро всё-таки
        //      попытается открыть cache.db, оно его пропустит
        return JSONObject().apply {
            put("log", JSONObject().put("level", "warn"))
            put("inbounds", JSONArray().put(JSONObject().apply {
                put("type", "mixed"); put("tag", "in")
                put("listen", "127.0.0.1"); put("listen_port", LOCAL_PORT)
            }))
            put("outbounds", outbounds)
            put("experimental", JSONObject().put("cache_file", JSONObject().put("enabled", false)))
        }.toString()
    }

    private fun vlessOutbound(r: RelayEntry) = JSONObject().apply {
        put("type", "vless"); put("tag", r.tag)
        put("server", r.server); put("server_port", r.port)
        put("uuid", r.uuid ?: "")
        r.flow?.takeIf { it.isNotBlank() }?.let { put("flow", it) }
        put("tls", JSONObject().apply {
            put("enabled", true); put("server_name", r.sni)
            put("insecure", r.allow_insecure)
            put("utls", JSONObject()
                .put("enabled", true)
                .put("fingerprint", r.fingerprint?.takeIf { it.isNotBlank() } ?: "chrome")
            )
            put("reality", JSONObject().apply {
                put("enabled", true)
                put("public_key", r.public_key ?: "")
                put("short_id", r.short_id ?: "")
            })
        })
        r.transport_type
            ?.takeIf { it.isNotBlank() && !it.equals("tcp", ignoreCase = true) }
            ?.let { type ->
            put("transport", JSONObject().apply {
                put("type", type)
                r.transport_path?.takeIf { it.isNotBlank() }?.let { put("path", it) }
                if (type == "xhttp") {
                    r.xhttp_mode?.takeIf { it.isNotBlank() }?.let { put("mode", it) }
                }
            })
        }
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

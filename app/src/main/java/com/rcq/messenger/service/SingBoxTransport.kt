package com.rcq.messenger.service

import android.content.Context
import android.content.SharedPreferences
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
            boxService = startNativeEngine(config)
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

    /**
     * Запускает нативное ядро через reflection — без статической зависимости от libbox,
     * чтобы модуль компилировался и без .aar. Зеркалит iOS (service.start(configJSON)).
     *
     * Пробуем два варианта API libbox:
     *  1) iOS-style: `BoxService(config).start()` или `BoxService().start(config)` (конструктор).
     *  2) SagerNet-style: `Libbox.newService(config, platform).start()` с no-op PlatformInterface.
     * Возвращает живой объект BoxService (для последующего .close()/.stop()).
     */
    private fun startNativeEngine(config: String): Any {
        val boxServiceClass = Class.forName("io.nekohasekai.libbox.BoxService")

        // Вариант 1: конструктор BoxService(String) + start()
        runCatching {
            val ctor = boxServiceClass.getConstructor(String::class.java)
            val svc = ctor.newInstance(config)
            boxServiceClass.getMethod("start").invoke(svc)
            Timber.d("$TAG: engine via BoxService(config).start()")
            return svc
        }
        // Вариант 1b: пустой конструктор + start(String)
        runCatching {
            val svc = boxServiceClass.getDeclaredConstructor().newInstance()
            boxServiceClass.getMethod("start", String::class.java).invoke(svc, config)
            Timber.d("$TAG: engine via BoxService().start(config)")
            return svc
        }
        // Вариант 2: Libbox.newService(config, PlatformInterface) + start()
        val libboxClass = Class.forName("io.nekohasekai.libbox.Libbox")
        val platformIface = Class.forName("io.nekohasekai.libbox.PlatformInterface")
        val platformStub = java.lang.reflect.Proxy.newProxyInstance(
            platformIface.classLoader,
            arrayOf(platformIface),
            NoopPlatformHandler
        )
        val svc = libboxClass
            .getMethod("newService", String::class.java, platformIface)
            .invoke(null, config, platformStub)
            ?: error("Libbox.newService вернул null")
        svc.javaClass.getMethod("start").invoke(svc)
        Timber.d("$TAG: engine via Libbox.newService(config, stub).start()")
        return svc
    }

    fun stop() {
        val svc = boxService
        boxService = null
        isActive = false
        if (svc != null) {
            // Метод закрытия отличается между сборками: close() (SagerNet) или stop() (iOS-style)
            runCatching { svc.javaClass.getMethod("close").invoke(svc) }
                .recoverCatching { svc.javaClass.getMethod("stop").invoke(svc) }
                .onFailure { Timber.w(it, "$TAG: stop failed") }
        }
        Timber.d("$TAG: stopped")
    }

    suspend fun setEnabled(on: Boolean) {
        prefs.edit().putBoolean(PREF_ENABLED, on).apply()
        if (on) start() else stop()
    }

    private fun buildSingBoxConfig(): String? {
        val relays = relayConfigRepository.currentRelays()
        if (relays.isEmpty()) { Timber.w("$TAG: no relays available"); return null }
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

    private fun orderedRelays(base: List<RelayEntry>): List<RelayEntry> {
        val last = prefs.getString(PREF_LAST_RELAY, null) ?: return base
        val idx = base.indexOfFirst { it.tag == last }.takeIf { it > 0 } ?: return base
        return base.toMutableList().also { it.add(0, it.removeAt(idx)) }
    }

    private suspend fun probeRelaysInBackground() {
        val relays = relayConfigRepository.currentRelays()
        val winner = relays.firstOrNull { probeTcp(it.server, it.port) } ?: return
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
 * No-op реализация libbox PlatformInterface через reflection-прокси.
 * В режиме mixed/SOCKS5 (без tun-inbound) ядро sing-box не вызывает методы
 * туннелирования, поэтому безопасно вернуть нейтральные значения по типу возврата.
 * Используется только когда .aar требует Libbox.newService(config, platform).
 */
private object NoopPlatformHandler : java.lang.reflect.InvocationHandler {
    override fun invoke(proxy: Any?, method: java.lang.reflect.Method, args: Array<out Any?>?): Any? {
        return when (method.returnType) {
            Boolean::class.javaPrimitiveType -> false
            Int::class.javaPrimitiveType -> 0
            Long::class.javaPrimitiveType -> 0L
            java.lang.String::class.java -> ""
            Void.TYPE -> null
            else -> null
        }
    }
}

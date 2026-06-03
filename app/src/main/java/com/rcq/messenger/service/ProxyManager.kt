package com.rcq.messenger.service

import android.content.Context
import android.content.SharedPreferences
import com.rcq.messenger.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import java.util.concurrent.TimeUnit
import timber.log.Timber
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.ProxySelector
import java.net.SocketAddress
import java.net.URI
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton

enum class BypassMode { OFF, AUTO, MANUAL, BUILT_IN }

object AutoBypassPolicy {
    fun shouldRestoreEmbeddedTransport(
        bypassModeIsAuto: Boolean,
        embeddedTransportWasActive: Boolean,
        embeddedTransportExplicitlyEnabled: Boolean
    ): Boolean =
        bypassModeIsAuto && embeddedTransportWasActive && embeddedTransportExplicitlyEnabled

    fun shouldPersistEmbeddedTransportForMode(mode: String): Boolean =
        mode == BypassMode.BUILT_IN.name
}

@Singleton
class ProxyManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val singBoxTransport: SingBoxTransport,
    private val xrayTransport: XrayTransport,
    private val relayConfigRepository: RelayConfigRepository
) {
    companion object {
        private const val PREFS = "rcq_proxy"
        private const val KEY_MANUAL_URL = "manual_proxy_url"
        private const val KEY_BYPASS_MODE = "bypass_mode"
        private const val KEY_SINGBOX_WAS_ACTIVE = "singbox_was_active"
        private const val AUTO_THRESHOLD = 3
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private val scope = CoroutineScope(Dispatchers.IO)
    private val failureCount = AtomicInteger(0)

    private val _statusLabel = MutableStateFlow(computeLabel())
    val statusLabel: StateFlow<String> = _statusLabel

    init {
        if (bypassMode == BypassMode.BUILT_IN) {
            scope.launch {
                Timber.i("ProxyManager: starting explicit built-in embedded transport")
                startEmbeddedTransport()
                _statusLabel.value = computeLabel()
            }
        }
        // Mirrors iOS boot policy: restore only an explicit embedded-transport opt-in.
        // Auto fallback is a transient route and must be revalidated before reuse.
        else if (AutoBypassPolicy.shouldRestoreEmbeddedTransport(
                bypassModeIsAuto = bypassMode == BypassMode.AUTO,
                embeddedTransportWasActive = prefs.getBoolean(KEY_SINGBOX_WAS_ACTIVE, false),
                embeddedTransportExplicitlyEnabled = singBoxTransport.isEnabled
            )
        ) {
            scope.launch {
                Timber.i("ProxyManager: restoring embedded transport from last session")
                startEmbeddedTransport()
                _statusLabel.value = computeLabel()
            }
        } else {
            prefs.edit().putBoolean(KEY_SINGBOX_WAS_ACTIVE, false).apply()
        }
    }

    var bypassMode: BypassMode
        get() = BypassMode.entries.getOrElse(
            prefs.getInt(KEY_BYPASS_MODE, BypassMode.AUTO.ordinal)
        ) { BypassMode.AUTO }
        set(v) {
            prefs.edit().putInt(KEY_BYPASS_MODE, v.ordinal).apply()
            failureCount.set(0)
            _statusLabel.value = computeLabel()
            Timber.d("ProxyManager: bypass mode → $v")
        }

    var manualProxyUrl: String
        get() = prefs.getString(KEY_MANUAL_URL, "") ?: ""
        set(v) {
            prefs.edit().putString(KEY_MANUAL_URL, v.trim()).apply()
            _statusLabel.value = computeLabel()
        }

    // Совместимость со старым кодом StealthViewModel
    var singboxAutoDisabled: Boolean
        get() = bypassMode == BypassMode.OFF
        set(v) { if (v) bypassMode = BypassMode.OFF }

    fun currentProxy(): Proxy = when (bypassMode) {
        BypassMode.OFF -> Proxy.NO_PROXY
        BypassMode.MANUAL -> parseProxyUrl(manualProxyUrl) ?: Proxy.NO_PROXY
        BypassMode.BUILT_IN -> {
            xrayTransport.proxyAddress()?.let { return Proxy(Proxy.Type.SOCKS, it) }
            singBoxTransport.proxyAddress()?.let { return Proxy(Proxy.Type.SOCKS, it) }
            Proxy.NO_PROXY
        }
        BypassMode.AUTO -> {
            xrayTransport.proxyAddress()?.let { return Proxy(Proxy.Type.SOCKS, it) }
            singBoxTransport.proxyAddress()?.let { return Proxy(Proxy.Type.SOCKS, it) }
            Proxy.NO_PROXY
        }
    }

    /** Вызывается при успешном соединении — сбрасывает счётчик ошибок */
    fun reportSuccess() {
        if (failureCount.getAndSet(0) > 0) {
            _statusLabel.value = computeLabel()
        }
    }

    /** Вызывается при неудачном соединении — в AUTO-режиме запускает sing-box после порога */
    fun reportFailure() {
        if (bypassMode != BypassMode.AUTO) return
        val count = failureCount.incrementAndGet()
        Timber.w("ProxyManager: connection failure #$count")
        if (count >= AUTO_THRESHOLD && !isEmbeddedTransportActive()) {
            Timber.i("ProxyManager: failure threshold reached → starting embedded transport")
            scope.launch {
                if (startEmbeddedTransport()) {
                    prefs.edit().putBoolean(KEY_SINGBOX_WAS_ACTIVE, true).apply()
                }
                _statusLabel.value = computeLabel()
            }
        }
    }

    /**
     * Пробует прямое подключение к серверу.
     * Если недоступен — активирует bypass (AUTO-режим → sing-box или MANUAL-прокси).
     * Возвращает true если прямое соединение работает.
     */
    suspend fun probeAndAutoEnable(
        onStatus: (String) -> Unit = {}
    ): Boolean = withContext(Dispatchers.IO) {
        onStatus("Проверяю соединение…")
        val probeUrl = BuildConfig.API_BASE_URL.trimEnd('/') + "/health"
        val directOk = withTimeoutOrNull(6_000) {
            runCatching {
                val client = OkHttpClient.Builder()
                    .proxy(Proxy.NO_PROXY)
                    .protocols(listOf(Protocol.HTTP_1_1))
                    .connectTimeout(4, TimeUnit.SECONDS)
                    .readTimeout(6, TimeUnit.SECONDS)
                    .build()
                client.newCall(Request.Builder().url(probeUrl).get().build()).execute().use { resp ->
                    resp.isSuccessful || resp.code in 401..403
                }
            }.getOrDefault(false)
        } ?: false

        if (directOk) {
            if (bypassMode == BypassMode.AUTO && isEmbeddedTransportActive() && !singBoxTransport.isEnabled) {
                Timber.i("ProxyManager: direct probe OK — stopping stale auto embedded transport")
                stopEmbeddedTransports()
                prefs.edit().putBoolean(KEY_SINGBOX_WAS_ACTIVE, false).apply()
            }
            failureCount.set(0)
            _statusLabel.value = computeLabel()
            onStatus("Прямое подключение ✓")
            Timber.i("ProxyManager: direct probe OK — no bypass needed")
        } else {
            onStatus("Прямое соединение недоступно, включаю обход…")
            Timber.i("ProxyManager: direct probe FAILED — enabling bypass")
            if (bypassMode == BypassMode.OFF) bypassMode = BypassMode.AUTO
            if (bypassMode == BypassMode.AUTO && !isEmbeddedTransportActive()) {
                if (startEmbeddedTransport()) {
                    prefs.edit().putBoolean(KEY_SINGBOX_WAS_ACTIVE, true).apply()
                }
                _statusLabel.value = computeLabel()
            }
            onStatus("Обход включён")
        }
        directOk
    }

    /**
     * Немедленно включает обход вручную, не дожидаясь порога ошибок.
     * В OFF-режиме переключает в AUTO, затем запускает sing-box сразу.
     */
    fun forceEnableNow() {
        scope.launch {
            if (bypassMode == BypassMode.OFF) bypassMode = BypassMode.AUTO
            if (!isEmbeddedTransportActive()) {
                if (startEmbeddedTransport()) {
                    prefs.edit().putBoolean(KEY_SINGBOX_WAS_ACTIVE, true).apply()
                }
            }
            failureCount.set(AUTO_THRESHOLD)
            _statusLabel.value = computeLabel()
            Timber.i("ProxyManager: bypass force-enabled by user")
        }
    }

    /** Явная остановка sing-box пользователем — сбрасывает персистентный флаг */
    fun stopSingBox() {
        stopEmbeddedTransports()
        prefs.edit().putBoolean(KEY_SINGBOX_WAS_ACTIVE, false).apply()
        failureCount.set(0)
        _statusLabel.value = computeLabel()
    }

    fun stealthStatusLabel(): String = computeLabel()

    fun isAutoSingBoxActive(): Boolean =
        bypassMode == BypassMode.AUTO && isEmbeddedTransportActive()

    private suspend fun startEmbeddedTransport(): Boolean {
        val relays = relayConfigRepository.selectedOrCurrentRelays()
        val selection = RelaySelectionPolicy.selectForEmbeddedTransport(
            base = relays,
            lastGoodTag = null,
            xrayAvailable = xrayTransport.isEngineAvailable
        )
        return when (selection.engine) {
            "xray" -> {
                singBoxTransport.stop()
                xrayTransport.start(selection.relays)
            }
            else -> {
                xrayTransport.stop()
                singBoxTransport.start()
                singBoxTransport.isActive
            }
        }.also { ok ->
            Timber.i("ProxyManager: embedded ${selection.engine} start result=$ok")
        }
    }

    private fun stopEmbeddedTransports() {
        singBoxTransport.stop()
        xrayTransport.stop()
    }

    private fun isEmbeddedTransportActive(): Boolean =
        singBoxTransport.isActive || xrayTransport.isActive

    private fun computeLabel(): String = when (bypassMode) {
        BypassMode.OFF -> "Выключено"
        BypassMode.MANUAL -> manualProxyUrl.ifBlank { "Ручной (не задан)" }
        BypassMode.BUILT_IN -> when {
            xrayTransport.isActive -> "Встроенный relay: ${selectedRelayLabel()} · Xray"
            singBoxTransport.isActive -> "Встроенный relay: ${selectedRelayLabel()}"
            !xrayTransport.isEngineAvailable && !singBoxTransport.isEngineAvailable ->
                "Встроенный relay: движок не установлен"
            else -> "Встроенный relay: ожидает запуска"
        }
        BypassMode.AUTO -> when {
            xrayTransport.isActive -> "Авто: ${selectedRelayLabel()} · Xray"
            singBoxTransport.isActive -> "Авто: ${selectedRelayLabel()}"
            // Движок не собран в APK — честно сообщаем, а не «подключаю прокси…» без конца
            !xrayTransport.isEngineAvailable && !singBoxTransport.isEngineAvailable && failureCount.get() > 0 ->
                "Авто: движки обхода не установлены"
            failureCount.get() > 0 -> "Авто: ${failureCount.get()} ошибок, подключаю прокси…"
            else -> "Авто: прямое подключение"
        }
    }

    private fun selectedRelayLabel(): String =
        relayConfigRepository.selectedRelayTag
            .takeIf { it.isNotBlank() }
            ?: "auto"

    private fun parseProxyUrl(url: String): Proxy? = runCatching {
        val uri = URI(if (url.contains("://")) url else "socks5://$url")
        val host = uri.host ?: return null
        val port = if (uri.port > 0) uri.port else 1080
        val type = when (uri.scheme?.lowercase()) {
            "http", "https" -> Proxy.Type.HTTP
            else -> Proxy.Type.SOCKS
        }
        Timber.d("ProxyManager: using $type proxy $host:$port")
        Proxy(type, InetSocketAddress(host, port))
    }.onFailure { Timber.w("ProxyManager: bad proxy URL '$url': ${it.message}") }.getOrNull()
}

class RcqProxySelector(private val proxyManager: ProxyManager) : ProxySelector() {
    override fun select(uri: URI?): List<Proxy> {
        val rcqProxy = proxyManager.currentProxy()
        if (rcqProxy.type() != Proxy.Type.DIRECT) return listOf(rcqProxy)

        val systemSelector = getDefault()
        if (systemSelector != null && systemSelector !is RcqProxySelector) {
            val systemProxies = runCatching { systemSelector.select(uri) }.getOrNull()
            if (!systemProxies.isNullOrEmpty()) return systemProxies
        }
        return listOf(Proxy.NO_PROXY)
    }

    override fun connectFailed(uri: URI?, sa: SocketAddress?, ioe: IOException?) {
        Timber.w("ProxySelector: connect failed for $uri via $sa: ${ioe?.message}")
        proxyManager.reportFailure()
    }
}

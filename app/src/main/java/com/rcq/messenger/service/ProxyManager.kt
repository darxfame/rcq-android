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

enum class BypassMode { OFF, AUTO, MANUAL }

@Singleton
class ProxyManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val singBoxTransport: SingBoxTransport
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
        // Restore sing-box if it was active when the process was last killed
        if (bypassMode == BypassMode.AUTO && prefs.getBoolean(KEY_SINGBOX_WAS_ACTIVE, false)) {
            scope.launch {
                Timber.i("ProxyManager: restoring sing-box from last session")
                singBoxTransport.start()
                _statusLabel.value = computeLabel()
            }
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
        BypassMode.AUTO -> {
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
        if (count >= AUTO_THRESHOLD && !singBoxTransport.isActive) {
            Timber.i("ProxyManager: failure threshold reached → starting sing-box")
            scope.launch {
                singBoxTransport.start()
                prefs.edit().putBoolean(KEY_SINGBOX_WAS_ACTIVE, true).apply()
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
        val directOk = withTimeoutOrNull(5_000) {
            runCatching {
                val client = OkHttpClient.Builder()
                    .connectTimeout(4, TimeUnit.SECONDS)
                    .readTimeout(4, TimeUnit.SECONDS)
                    .build()
                val resp = client.newCall(Request.Builder().url(probeUrl).head().build()).execute()
                resp.close()
                resp.isSuccessful || resp.code in 401..403
            }.getOrDefault(false)
        } ?: false

        if (directOk) {
            onStatus("Прямое подключение ✓")
            Timber.i("ProxyManager: direct probe OK — no bypass needed")
        } else {
            onStatus("Прямое соединение недоступно, включаю обход…")
            Timber.i("ProxyManager: direct probe FAILED — enabling bypass")
            if (bypassMode == BypassMode.OFF) bypassMode = BypassMode.AUTO
            if (bypassMode == BypassMode.AUTO && !singBoxTransport.isActive) {
                singBoxTransport.start()
                prefs.edit().putBoolean(KEY_SINGBOX_WAS_ACTIVE, true).apply()
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
            if (!singBoxTransport.isActive) {
                singBoxTransport.start()
                prefs.edit().putBoolean(KEY_SINGBOX_WAS_ACTIVE, true).apply()
            }
            failureCount.set(AUTO_THRESHOLD)
            _statusLabel.value = computeLabel()
            Timber.i("ProxyManager: bypass force-enabled by user")
        }
    }

    /** Явная остановка sing-box пользователем — сбрасывает персистентный флаг */
    fun stopSingBox() {
        singBoxTransport.stop()
        prefs.edit().putBoolean(KEY_SINGBOX_WAS_ACTIVE, false).apply()
        failureCount.set(0)
        _statusLabel.value = computeLabel()
    }

    fun stealthStatusLabel(): String = computeLabel()

    private fun computeLabel(): String = when (bypassMode) {
        BypassMode.OFF -> "Выключено"
        BypassMode.MANUAL -> manualProxyUrl.ifBlank { "Ручной (не задан)" }
        BypassMode.AUTO -> when {
            singBoxTransport.isActive -> "Авто: sing-box активен"
            failureCount.get() > 0 -> "Авто: ${failureCount.get()} ошибок, подключаю прокси…"
            else -> "Авто: прямое подключение"
        }
    }

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
    override fun select(uri: URI?): List<Proxy> = listOf(proxyManager.currentProxy())
    override fun connectFailed(uri: URI?, sa: SocketAddress?, ioe: IOException?) {
        Timber.w("ProxySelector: connect failed for $uri via $sa: ${ioe?.message}")
        proxyManager.reportFailure()
    }
}

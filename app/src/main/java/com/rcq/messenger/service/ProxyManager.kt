package com.rcq.messenger.service

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
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
        // Сколько подряд неудачных соединений → автозапуск sing-box
        private const val AUTO_THRESHOLD = 3
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private val scope = CoroutineScope(Dispatchers.IO)
    private val failureCount = AtomicInteger(0)

    private val _statusLabel = MutableStateFlow(computeLabel())
    val statusLabel: StateFlow<String> = _statusLabel

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
                _statusLabel.value = computeLabel()
            }
        }
    }

    fun stealthStatusLabel(): String = computeLabel()

    private fun computeLabel(): String = when (bypassMode) {
        BypassMode.OFF -> "Выключено"
        BypassMode.MANUAL -> manualProxyUrl.ifBlank { "Ручной (не задан)" }
        BypassMode.AUTO -> when {
            singBoxTransport.isActive -> "Авто (sing-box активен)"
            failureCount.get() > 0 -> "Авто (${failureCount.get()} ошибок…)"
            else -> "Авто (прямое подключение)"
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

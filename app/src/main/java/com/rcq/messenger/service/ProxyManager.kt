package com.rcq.messenger.service

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.ProxySelector
import java.net.SocketAddress
import java.net.URI
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProxyManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val singBoxTransport: SingBoxTransport
) {
    companion object {
        private const val PREFS = "rcq_proxy"
        private const val KEY_MANUAL_URL = "manual_proxy_url"
        private const val KEY_AUTO_DISABLED = "singbox_auto_disabled"
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    var manualProxyUrl: String
        get() = prefs.getString(KEY_MANUAL_URL, "") ?: ""
        set(v) { prefs.edit().putString(KEY_MANUAL_URL, v.trim()).apply() }

    var singboxAutoDisabled: Boolean
        get() = prefs.getBoolean(KEY_AUTO_DISABLED, false)
        set(v) { prefs.edit().putBoolean(KEY_AUTO_DISABLED, v).apply() }

    fun currentProxy(): Proxy {
        val manual = manualProxyUrl
        if (manual.isNotBlank()) {
            parseProxyUrl(manual)?.let { return it }
        }
        singBoxTransport.proxyAddress()?.let { addr ->
            return Proxy(Proxy.Type.SOCKS, addr)
        }
        return Proxy.NO_PROXY
    }

    fun stealthStatusLabel(): String {
        val manual = manualProxyUrl
        if (manual.isNotBlank()) return manual
        if (singBoxTransport.isActive) return "Авто (sing-box)"
        if (singboxAutoDisabled) return "Выключено"
        return "Прямое подключение"
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
    }
}

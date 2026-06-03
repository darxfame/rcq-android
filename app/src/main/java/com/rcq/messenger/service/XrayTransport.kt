package com.rcq.messenger.service

import android.content.Context
import android.content.SharedPreferences
import com.rcq.messenger.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.net.Socket
import timber.log.Timber
import java.io.File
import java.net.HttpURLConnection
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

object XrayConfigBuilder {
    fun build(relay: RelayEntry, socksPort: Int): String {
        val transport = relay.transport_type?.takeIf { it.isNotBlank() } ?: "tcp"
        val xhttpSettings = if (relay.transport_type.equals("xhttp", ignoreCase = true)) {
            ""","xhttpSettings":{"path":${jsonString(relay.transport_path?.takeIf { it.isNotBlank() } ?: "/")},"mode":${jsonString(relay.xhttp_mode?.takeIf { it.isNotBlank() } ?: "auto")}}"""
        } else {
            ""
        }
        val flow = relay.flow?.takeIf { it.isNotBlank() }
            ?.let { ""","flow":${jsonString(it)}""" }
            ?: ""
        return """
            {
              "log":{"loglevel":"warning"},
              "inbounds":[{
                "listen":"127.0.0.1",
                "port":$socksPort,
                "protocol":"socks",
                "settings":{"auth":"noauth","udp":false}
              }],
              "outbounds":[{
                "protocol":"vless",
                "settings":{
                  "vnext":[{
                    "address":${jsonString(relay.server)},
                    "port":${relay.port},
                    "users":[{
                      "id":${jsonString(relay.uuid ?: "")},
                      "encryption":"none"$flow
                    }]
                  }]
                },
                "streamSettings":{
                  "network":${jsonString(transport)},
                  "security":"reality",
                  "realitySettings":{
                    "serverName":${jsonString(relay.sni)},
                    "fingerprint":${jsonString(relay.fingerprint?.takeIf { it.isNotBlank() } ?: "chrome")},
                    "publicKey":${jsonString(relay.public_key ?: "")},
                    "shortId":${jsonString(relay.short_id ?: "")}
                  }$xhttpSettings
                }
              }]
            }
        """.trimIndent().replace("\n", "").replace(Regex("\\s{2,}"), "")
    }

    private fun jsonString(value: String): String = buildString {
        append('"')
        for (ch in value) {
            when (ch) {
                '"' -> append("\\\"")
                '\\' -> append("\\\\")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> append(ch)
            }
        }
        append('"')
    }
}

@Singleton
class XrayTransport @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "XrayTransport"
        private const val PREFS = "xray"
        private const val PREF_LAST_ERROR = "rcq.xray.lastError"
        private const val ROUTE_PROBE_ATTEMPTS = 3
        private const val ROUTE_PROBE_RETRY_MS = 350L
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private var process: Process? = null

    val isEngineAvailable: Boolean
        get() = executableFile().canExecute() || executableFile().exists()

    var isActive = false
        private set

    val lastStartError: String? get() = prefs.getString(PREF_LAST_ERROR, null)
        ?.takeIf { it.isNotEmpty() }

    fun proxyAddress(): InetSocketAddress? =
        if (isActive) InetSocketAddress("127.0.0.1", SingBoxTransport.LOCAL_PORT) else null

    suspend fun start(relays: List<RelayEntry>): Boolean = withContext(Dispatchers.IO) {
        if (isActive) return@withContext true
        val relay = relays.firstOrNull { it.transport_type.equals("xhttp", ignoreCase = true) }
            ?: relays.firstOrNull()
            ?: return@withContext fail("Нет relay для Xray")

        val executable = executableFile()
        if (!executable.exists()) return@withContext fail("Движок Xray не установлен в APK")
        executable.setExecutable(true, false)

        stop()
        val workDir = context.filesDir.resolve("xray").apply { mkdirs() }
        val configFile = workDir.resolve("config.json")
        configFile.writeText(
            XrayConfigBuilder.build(relay, socksPort = SingBoxTransport.LOCAL_PORT),
            Charsets.UTF_8
        )

        runCatching {
            val pb = ProcessBuilder(executable.absolutePath, "run", "-config", configFile.absolutePath)
                .directory(workDir)
                .redirectErrorStream(true)
            pb.environment()["XRAY_LOCATION_ASSET"] = workDir.absolutePath
            pb.environment()["xray.location.asset"] = workDir.absolutePath
            val started = pb.start()
            process = started
            delay(900) // non-blocking wait for Xray to init its SOCKS listener
            if (!validateProxyRoute()) {
                Timber.w("$TAG: started ${relay.tag}, but health route check failed")
                throw IllegalStateException("Health route check failed")
            }
            isActive = true
            prefs.edit().remove(PREF_LAST_ERROR).apply()
            Timber.i("$TAG: started ${relay.tag} on 127.0.0.1:${SingBoxTransport.LOCAL_PORT}")
            true
        }.getOrElse { e ->
            stop()
            fail(e.message?.take(400) ?: "unknown")
        }
    }

    fun stop() {
        process?.destroy()
        runCatching { process?.waitFor() }
        process = null
        isActive = false
        Timber.d("$TAG: stopped")
    }

    private fun executableFile(): File =
        File(context.applicationInfo.nativeLibraryDir, "libxray_exec.so")

    private fun fail(message: String): Boolean {
        prefs.edit().putString(PREF_LAST_ERROR, message).apply()
        Timber.e("$TAG: start failed: $message")
        return false
    }

    private suspend fun validateProxyRoute(): Boolean {
        repeat(ROUTE_PROBE_ATTEMPTS) { attempt ->
            val ok = runCatching {
                val proxy = Proxy(
                    Proxy.Type.SOCKS,
                    InetSocketAddress("127.0.0.1", SingBoxTransport.LOCAL_PORT)
                )
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
}

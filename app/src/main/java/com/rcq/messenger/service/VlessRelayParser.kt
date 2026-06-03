package com.rcq.messenger.service

import java.net.URI
import java.net.URLDecoder
import java.util.Locale

object VlessRelayParser {
    fun parse(rawUrl: String, priority: Int = -200): Result<RelayEntry> = runCatching {
        val trimmed = rawUrl.trim()
        require(trimmed.startsWith("vless://", ignoreCase = true)) { "Only vless:// URLs are supported" }

        val uri = URI(trimmed)
        require(uri.scheme.equals("vless", ignoreCase = true)) { "Only vless:// URLs are supported" }

        val uuid = uri.userInfo?.takeIf { it.isNotBlank() }
            ?: error("VLESS UUID is missing")
        val host = uri.host?.takeIf { it.isNotBlank() }
            ?: error("VLESS host is missing")
        val port = uri.port.takeIf { it > 0 } ?: 443
        val query = parseQuery(uri.rawQuery ?: "")

        require(query["security"].equals("reality", ignoreCase = true)) {
            "Only VLESS Reality URLs are supported"
        }

        val publicKey = query["pbk"]?.takeIf { it.isNotBlank() }
            ?: error("Reality public key (pbk) is missing")
        val sni = (query["sni"] ?: query["host"])?.takeIf { it.isNotBlank() }
            ?: error("SNI is missing")
        val transportType = query["type"]?.takeIf { it.isNotBlank() } ?: "tcp"
        val label = decode(uri.rawFragment ?: "").ifBlank { host }

        RelayEntry(
            tag = "custom-${slug(label)}",
            proto = "vless",
            server = host,
            port = port,
            sni = sni,
            uuid = uuid,
            public_key = publicKey,
            short_id = query["sid"]?.takeIf { it.isNotBlank() },
            flow = query["flow"]?.takeIf { it.isNotBlank() },
            fingerprint = query["fp"]?.takeIf { it.isNotBlank() } ?: "chrome",
            allow_insecure = query["allowInsecure"].equals("true", ignoreCase = true),
            transport_type = transportType,
            transport_path = query["path"]?.takeIf { it.isNotBlank() },
            xhttp_mode = query["xhttpMode"]?.takeIf { it.isNotBlank() },
            priority = priority
        )
    }

    private fun parseQuery(rawQuery: String): Map<String, String> {
        if (rawQuery.isBlank()) return emptyMap()
        return rawQuery.split('&')
            .mapNotNull { part ->
                val idx = part.indexOf('=')
                if (idx < 0) return@mapNotNull decode(part) to ""
                decode(part.substring(0, idx)) to decode(part.substring(idx + 1))
            }
            .toMap()
    }

    private fun decode(value: String): String =
        URLDecoder.decode(value, Charsets.UTF_8.name())

    private fun slug(value: String): String {
        val ascii = value
            .lowercase(Locale.US)
            .replace(Regex("[^a-z0-9]+"), "-")
            .trim('-')
        return ascii.ifBlank { "vless-${System.currentTimeMillis()}" }
    }
}

object RelayPreferencePolicy {
    fun applySelection(relays: List<RelayEntry>, selectedTag: String?): List<RelayEntry> {
        val tag = selectedTag?.takeIf { it.isNotBlank() } ?: return relays
        val selected = relays.firstOrNull { it.tag == tag } ?: return relays
        return listOf(selected)
    }
}

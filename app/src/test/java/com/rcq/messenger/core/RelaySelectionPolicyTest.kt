package com.rcq.messenger.core

import com.rcq.messenger.service.RelayEntry
import com.rcq.messenger.service.RelaySelectionPolicy
import org.junit.Assert.assertEquals
import org.junit.Test

class RelaySelectionPolicyTest {

    @Test
    fun `android keeps local priority xhttp relay before hysteria2 last-good relay when engine supports xhttp`() {
        val relays = listOf(
            relay(tag = "relay-hy2", proto = "hysteria2", priority = 0),
            relay(tag = "relay-vless", proto = "vless", priority = 1),
            relay(
                tag = "relay-usa-amd-xhttp",
                proto = "vless",
                priority = -100,
                transportType = "xhttp"
            )
        )

        val ordered = RelaySelectionPolicy.orderForAndroid(
            relays,
            lastGoodTag = "relay-hy2",
            supportsXhttp = true
        )

        assertEquals("relay-usa-amd-xhttp", ordered.first().tag)
    }

    @Test
    fun `android excludes xhttp relay when bundled engine does not support xhttp`() {
        val relays = listOf(
            relay(
                tag = "relay-usa-amd-xhttp",
                proto = "vless",
                priority = -100,
                transportType = "xhttp"
            ),
            relay(tag = "relay-vless", proto = "vless", priority = 1),
            relay(tag = "relay-hy2", proto = "hysteria2", priority = 2)
        )

        val ordered = RelaySelectionPolicy.orderForAndroid(
            relays,
            lastGoodTag = null,
            supportsXhttp = false
        )

        assertEquals(listOf("relay-vless", "relay-hy2"), ordered.map { it.tag })
    }

    @Test
    fun `android does not promote hysteria2 from tcp-only probe ahead of vless relays`() {
        val relays = listOf(
            relay(tag = "relay-hy2", proto = "hysteria2", priority = 0),
            relay(tag = "relay-vless", proto = "vless", priority = 1)
        )

        val ordered = RelaySelectionPolicy.orderForAndroid(relays, lastGoodTag = "relay-hy2")

        assertEquals("relay-vless", ordered.first().tag)
    }

    @Test
    fun `android can promote last-good vless within non-priority relays`() {
        val relays = listOf(
            relay(tag = "relay-vless-a", proto = "vless", priority = 1),
            relay(tag = "relay-vless-b", proto = "vless", priority = 2)
        )

        val ordered = RelaySelectionPolicy.orderForAndroid(relays, lastGoodTag = "relay-vless-b")

        assertEquals("relay-vless-b", ordered.first().tag)
    }

    private fun relay(
        tag: String,
        proto: String,
        priority: Int,
        transportType: String? = null
    ) = RelayEntry(
        tag = tag,
        proto = proto,
        server = "127.0.0.1",
        port = 443,
        sni = "example.com",
        uuid = if (proto == "vless") "uuid" else null,
        public_key = if (proto == "vless") "public-key" else null,
        short_id = if (proto == "vless") "short-id" else null,
        password = if (proto == "hysteria2") "password" else null,
        obfs_password = if (proto == "hysteria2") "obfs" else null,
        transport_type = transportType,
        priority = priority
    )
}

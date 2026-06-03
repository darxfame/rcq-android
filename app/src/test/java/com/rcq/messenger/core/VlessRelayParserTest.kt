package com.rcq.messenger.core

import com.rcq.messenger.service.RelayPreferencePolicy
import com.rcq.messenger.service.VlessRelayParser
import com.rcq.messenger.service.RelayEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VlessRelayParserTest {

    @Test
    fun `parses vless reality tcp vision url`() {
        val relay = VlessRelayParser.parse(
            "vless://834703d2-abf1-471a-83b5-ee87e0b6cd8e@uk.e0f.network:443" +
                "?security=reality&sni=www.google.com&fp=chrome" +
                "&pbk=nPcUKydxoRI66O3tT9O4QCZpLjOBvkGsiXG7pDX1BBw" +
                "&sid=6ba85179e30d4fc2&type=tcp&flow=xtls-rprx-vision&encryption=none" +
                "#%F0%9F%87%AC%F0%9F%87%A7%20Britain%20AM%20WL%20Mobile%20Vless"
        ).getOrThrow()

        assertEquals("vless", relay.proto)
        assertEquals("uk.e0f.network", relay.server)
        assertEquals(443, relay.port)
        assertEquals("www.google.com", relay.sni)
        assertEquals("834703d2-abf1-471a-83b5-ee87e0b6cd8e", relay.uuid)
        assertEquals("nPcUKydxoRI66O3tT9O4QCZpLjOBvkGsiXG7pDX1BBw", relay.public_key)
        assertEquals("6ba85179e30d4fc2", relay.short_id)
        assertEquals("xtls-rprx-vision", relay.flow)
        assertEquals("chrome", relay.fingerprint)
        assertEquals("tcp", relay.transport_type)
        assertEquals("custom-britain-am-wl-mobile-vless", relay.tag)
    }

    @Test
    fun `rejects non reality vless url`() {
        val result = VlessRelayParser.parse("vless://uuid@example.com:443?security=tls&type=tcp")

        assertEquals(true, result.isFailure)
    }

    @Test
    fun `selected relay narrows list when tag exists`() {
        val relays = listOf(
            relay("relay-a"),
            relay("relay-b"),
        )

        val selected = RelayPreferencePolicy.applySelection(relays, selectedTag = "relay-b")

        assertEquals(listOf("relay-b"), selected.map { it.tag })
    }

    @Test
    fun `missing selected relay falls back to full list`() {
        val relays = listOf(relay("relay-a"))

        val selected = RelayPreferencePolicy.applySelection(relays, selectedTag = "missing")

        assertEquals(listOf("relay-a"), selected.map { it.tag })
    }

    @Test
    fun `blank selected relay falls back to full list`() {
        val relays = listOf(relay("relay-a"))

        val selected = RelayPreferencePolicy.applySelection(relays, selectedTag = "")

        assertEquals(listOf("relay-a"), selected.map { it.tag })
    }

    private fun relay(tag: String) = RelayEntry(
        tag = tag,
        proto = "vless",
        server = "example.com",
        port = 443,
        sni = "example.com",
        uuid = "uuid",
        public_key = "pbk",
        short_id = "sid"
    )
}

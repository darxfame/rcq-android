package com.rcq.messenger.core

import com.rcq.messenger.service.RelayEntry
import com.rcq.messenger.service.RelaySelectionPolicy
import org.junit.Assert.assertEquals
import org.junit.Test

class RelayEngineSelectionPolicyTest {

    @Test
    fun `xhttp relay is assigned to xray when xray is available`() {
        val selected = RelaySelectionPolicy.selectForEmbeddedTransport(
            base = listOf(xhttpRelay()),
            lastGoodTag = null,
            xrayAvailable = true
        )

        assertEquals("xray", selected.engine)
        assertEquals("relay-usa-amd-xhttp", selected.relays.first().tag)
    }

    @Test
    fun `xhttp relay is not assigned to sing-box when xray is unavailable`() {
        val selected = RelaySelectionPolicy.selectForEmbeddedTransport(
            base = listOf(xhttpRelay(), vlessRelay()),
            lastGoodTag = null,
            xrayAvailable = false
        )

        assertEquals("sing-box", selected.engine)
        assertEquals("relay-vless", selected.relays.first().tag)
    }

    private fun xhttpRelay() = RelayEntry(
        tag = "relay-usa-amd-xhttp",
        proto = "vless",
        server = "80.209.243.23",
        port = 443,
        sni = "amd.com",
        uuid = "63bbedb0-2e27-4a15-9aca-b0856d5f9b3a",
        public_key = "YQL5CMcuLgjJwH-2f10LlWx79ZDMnRzl8oZAFPPUqmk",
        short_id = "2a3f5c8d",
        transport_type = "xhttp",
        transport_path = "/telemetry",
        xhttp_mode = "auto",
        priority = -100
    )

    private fun vlessRelay() = RelayEntry(
        tag = "relay-vless",
        proto = "vless",
        server = "165.22.90.214",
        port = 443,
        sni = "www.yandex.ru",
        uuid = "uuid",
        public_key = "public",
        short_id = "sid",
        priority = 1
    )
}

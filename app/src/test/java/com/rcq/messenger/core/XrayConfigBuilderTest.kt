package com.rcq.messenger.core

import com.rcq.messenger.service.RelayEntry
import com.rcq.messenger.service.XrayConfigBuilder
import org.junit.Assert.assertTrue
import org.junit.Test

class XrayConfigBuilderTest {

    @Test
    fun `builds vless reality xhttp config with local socks inbound`() {
        val relay = RelayEntry(
            tag = "relay-usa-amd-xhttp",
            proto = "vless",
            server = "80.209.243.23",
            port = 443,
            sni = "amd.com",
            uuid = "63bbedb0-2e27-4a15-9aca-b0856d5f9b3a",
            public_key = "YQL5CMcuLgjJwH-2f10LlWx79ZDMnRzl8oZAFPPUqmk",
            short_id = "2a3f5c8d",
            fingerprint = "random",
            transport_type = "xhttp",
            transport_path = "/telemetry",
            xhttp_mode = "auto",
            priority = -100
        )

        val config = XrayConfigBuilder.build(relay, socksPort = 1089)

        assertTrue(config.contains("\"protocol\":\"socks\""))
        assertTrue(config.contains("\"port\":1089"))
        assertTrue(config.contains("\"protocol\":\"vless\""))
        assertTrue(config.contains("\"address\":\"80.209.243.23\""))
        assertTrue(config.contains("\"network\":\"xhttp\""))
        assertTrue(config.contains("\"security\":\"reality\""))
        assertTrue(config.contains("\"serverName\":\"amd.com\""))
        assertTrue(config.contains("\"publicKey\":\"YQL5CMcuLgjJwH-2f10LlWx79ZDMnRzl8oZAFPPUqmk\""))
        assertTrue(config.contains("\"shortId\":\"2a3f5c8d\""))
        assertTrue(config.contains("\"path\":\"/telemetry\""))
        assertTrue(config.contains("\"mode\":\"auto\""))
    }
}

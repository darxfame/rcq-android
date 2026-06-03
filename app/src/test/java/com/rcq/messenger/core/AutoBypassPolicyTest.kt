package com.rcq.messenger.core

import com.rcq.messenger.service.AutoBypassPolicy
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AutoBypassPolicyTest {

    @Test
    fun `auto fallback does not restore embedded sing-box on next launch`() {
        assertFalse(
            AutoBypassPolicy.shouldRestoreEmbeddedTransport(
                bypassModeIsAuto = true,
                embeddedTransportWasActive = true,
                embeddedTransportExplicitlyEnabled = false
            )
        )
    }

    @Test
    fun `explicit embedded transport opt-in restores on next launch`() {
        assertTrue(
            AutoBypassPolicy.shouldRestoreEmbeddedTransport(
                bypassModeIsAuto = true,
                embeddedTransportWasActive = true,
                embeddedTransportExplicitlyEnabled = true
            )
        )
    }

    @Test
    fun `automatic mode is not explicit embedded relay opt-in`() {
        assertFalse(AutoBypassPolicy.shouldPersistEmbeddedTransportForMode("AUTO"))
        assertTrue(AutoBypassPolicy.shouldPersistEmbeddedTransportForMode("BUILT_IN"))
    }
}

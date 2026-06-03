package app.rcq.android.crypto

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * On-device tests for the panic-PIN vault. Uses the real EncryptedSharedPreferences
 * (for the pepper) + the real PBKDF2; cleans the vault up after each test.
 */
@RunWith(AndroidJUnit4::class)
class PinVaultTest {
    private val ctx = InstrumentationRegistry.getInstrumentation().targetContext

    @After
    fun cleanup() = PinVault.destroy(ctx)

    @Test
    fun createUnlockRoundTrip() {
        val unlock = PinVault.createWithRealPin(ctx, "1234")
        assertEquals(PinVault.MODE_REAL, unlock.payload.mode)
        val dataKey = PinVault.dataKeyBytes(unlock.payload)
        assertNotNull(dataKey)
        assertEquals(32, dataKey!!.size)
        assertTrue(PinVault.isConfigured(ctx))

        // Right PIN unlocks and yields the same dataKey.
        val u2 = PinVault.unlock(ctx, "1234")
        assertNotNull(u2)
        assertArrayEquals(dataKey, PinVault.dataKeyBytes(u2!!.payload))

        // Wrong PIN opens no slot.
        assertNull(PinVault.unlock(ctx, "9999"))
    }

    @Test
    fun changePinKeepsDataKey() {
        val unlock = PinVault.createWithRealPin(ctx, "1234")
        val dataKey = PinVault.dataKeyBytes(unlock.payload)!!
        val realSlot = unlock.payload.layout!!.realSlot
        PinVault.reSealSlot(ctx, realSlot, unlock.payload, "5678")
        // Old PIN no longer opens; new PIN opens with the SAME dataKey
        // (so the message DB never needs re-encrypting on a PIN change).
        assertNull(PinVault.unlock(ctx, "1234"))
        val u = PinVault.unlock(ctx, "5678")
        assertNotNull(u)
        assertArrayEquals(dataKey, PinVault.dataKeyBytes(u!!.payload))
    }

    @Test
    fun destroyClearsVault() {
        PinVault.createWithRealPin(ctx, "1234")
        assertTrue(PinVault.isConfigured(ctx))
        PinVault.destroy(ctx)
        assertFalse(PinVault.isConfigured(ctx))
        assertNull(PinVault.unlock(ctx, "1234"))
    }

    @Test
    fun lockoutEscalates() {
        assertEquals(0L, PinVault.lockoutMillis(4))
        assertEquals(30_000L, PinVault.lockoutMillis(5))
        assertEquals(60_000L, PinVault.lockoutMillis(6))
        assertEquals(300_000L, PinVault.lockoutMillis(7))
        assertEquals(3_600_000L, PinVault.lockoutMillis(20))
    }
}

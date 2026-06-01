package app.rcq.android.crypto

import androidx.test.ext.junit.runners.AndroidJUnit4
import app.rcq.android.model.RadioMessage
import app.rcq.android.nearby.RadioFrame
import app.rcq.android.nearby.RadioPayload
import app.rcq.android.nearby.RadioWire
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith

/**
 * On-device verification of the Radio crypto + wire codec — the one Radio
 * phase an emulator *can* prove (no BLE/Wi-Fi-Direct radio needed). Confirms:
 *   - X25519 ECDH derives a symmetric session key on both sides,
 *   - AES-GCM seal/open round-trips and rejects a wrong key,
 *   - room keys are deterministic, room-scoped, and password-gated,
 *   - a full message survives encode → seal → frame → decode → open → decode.
 * Runs against the real BouncyCastle + Android JCA providers.
 */
@RunWith(AndroidJUnit4::class)
class RadioCryptoTest {

    @Test
    fun sessionKeyIsSymmetric() {
        val a = RadioCrypto.makeEphemeralKeys()
        val b = RadioCrypto.makeEphemeralKeys()
        val keyAB = RadioCrypto.deriveSessionKey(a.priv, b.pub)
        val keyBA = RadioCrypto.deriveSessionKey(b.priv, a.pub)
        assertArrayEquals("both sides must derive the same 1:1 key", keyAB, keyBA)
        assertEquals(32, keyAB.size)
    }

    @Test
    fun sealOpenRoundTripsAndRejectsWrongKey() {
        val key = RadioCrypto.roomKey("room-xyz", null)
        val plain = "привет radio · hello".toByteArray(Charsets.UTF_8)
        val sealed = RadioCrypto.seal(plain, key)
        assertArrayEquals(plain, RadioCrypto.open(sealed, key))

        val wrong = RadioCrypto.roomKey("other-room", null)
        try {
            RadioCrypto.open(sealed, wrong)
            fail("opening with the wrong key must throw (GCM tag mismatch)")
        } catch (_: Exception) {
            // expected
        }
    }

    @Test
    fun roomKeyOpenIsDeterministicAndScoped() {
        val k1 = RadioCrypto.roomKey("alpha", null)
        val k2 = RadioCrypto.roomKey("alpha", null)
        val other = RadioCrypto.roomKey("beta", null)
        assertArrayEquals("same open room id → same key", k1, k2)
        assertFalse("different room ids → different keys", k1.contentEquals(other))
    }

    @Test
    fun roomKeyPasswordIsGated() {
        val open = RadioCrypto.roomKey("gamma", null)
        val pwd = RadioCrypto.roomKey("gamma", "hunter2")
        val pwdAgain = RadioCrypto.roomKey("gamma", "hunter2")
        val wrongPwd = RadioCrypto.roomKey("gamma", "hunter3")
        assertArrayEquals("same password → same key", pwd, pwdAgain)
        assertFalse("password key differs from open key", pwd.contentEquals(open))
        assertFalse("wrong password → different key", pwd.contentEquals(wrongPwd))
        // Trim parity: surrounding whitespace must not change the key.
        assertArrayEquals(pwd, RadioCrypto.roomKey("gamma", "  hunter2  "))
    }

    @Test
    fun fullMessageSurvivesEncodeSealFrameDecodeOpenDecode() {
        val key = RadioCrypto.roomKey("trip", "pw")
        val original = RadioMessage(
            id = "m-1",
            senderDisplayName = "Wandering Stranger #4242",
            isFromMe = true,
            text = "edge :smile: case",
            timestampMs = 1_700_000_000_000L,
            replyToId = "m-0",
            replyToSender = "Quiet Nomad #1001",
            replyToBody = "previous line",
            reactions = mapOf("Quiet Nomad #1001" to "heart"),
        )

        // sender side: payload → seal → frame → bytes
        val payloadBytes = RadioWire.encodePayload(RadioPayload.Message(original))
        val sealed = RadioCrypto.seal(payloadBytes, key)
        val frameBytes = RadioWire.encodeFrame(RadioFrame.Sealed(sealed))

        // receiver side: bytes → frame → open → payload
        val frame = RadioWire.decodeFrame(frameBytes)
        assertTrue(frame is RadioFrame.Sealed)
        val opened = RadioCrypto.open((frame as RadioFrame.Sealed).combined, key)
        val payload = RadioWire.decodePayload(opened)
        assertTrue(payload is RadioPayload.Message)
        val got = (payload as RadioPayload.Message).message

        assertEquals(original.id, got.id)
        assertEquals(original.senderDisplayName, got.senderDisplayName)
        assertEquals(original.text, got.text)
        assertEquals(original.timestampMs, got.timestampMs)
        assertEquals(original.replyToId, got.replyToId)
        assertEquals(original.replyToSender, got.replyToSender)
        assertEquals(original.replyToBody, got.replyToBody)
        assertEquals(original.reactions, got.reactions)
        assertFalse("isFromMe must never be trusted off the wire", got.isFromMe)
    }

    @Test
    fun handshakeFrameRoundTrips() {
        val keys = RadioCrypto.makeEphemeralKeys()
        val frame = RadioWire.decodeFrame(RadioWire.encodeFrame(RadioFrame.Handshake(keys.pub)))
        assertTrue(frame is RadioFrame.Handshake)
        assertArrayEquals(keys.pub, (frame as RadioFrame.Handshake).pub)
    }

    @Test
    fun decodeRejectsGarbage() {
        assertNull(RadioWire.decodeFrame("not json".toByteArray()))
        assertNull(RadioWire.decodePayload("{\"p\":\"???\"}".toByteArray()))
    }
}

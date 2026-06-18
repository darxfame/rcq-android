package app.rcq.android.crypto

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EnvelopeForwardTest {
    @Test
    fun forwardedNameRoundTripsOnTextPhotoAndVideo() {
        val envs = listOf(
            Envelope.text("hello", forwardedFromName = "Alice"),
            Envelope.photo("mid", "key", "cap", forwardedFromName = "Alice"),
            Envelope.video("mid", "key", "thumb", 3.0, "cap", forwardedFromName = "Alice"),
        )

        envs.forEach { env ->
            val decoded = Envelope.fromJsonBytes(env.toJsonBytes())
            val name = when (decoded) {
                is Envelope.Text -> decoded.forwardedFromName
                is Envelope.Photo -> decoded.forwardedFromName
                is Envelope.Video -> decoded.forwardedFromName
                else -> null
            }
            assertTrue(decoded::class == env::class)
            assertEquals("Alice", name)
        }
    }
}

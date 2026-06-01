package app.rcq.android.nearby

import android.util.Base64
import app.rcq.android.model.RadioMessage
import com.google.gson.JsonObject
import com.google.gson.JsonParser

/**
 * Outer Radio wire frame, sent length-prefixed over the Wi-Fi Direct TCP
 * socket (reliable) or as a single UDP datagram (unreliable voice). Android
 * port of the iOS `RadioFrame`. Tagging lets the 1:1 key-swap interleave with
 * encrypted traffic on the same channel — the inviter can't derive the AES key
 * until the peer's public key comes back in a [Handshake].
 */
sealed class RadioFrame {
    /** Raw 32-byte Curve25519 public key for the 1:1 ECDH handshake. */
    class Handshake(val pub: ByteArray) : RadioFrame()

    /** AES-GCM combined ciphertext over a JSON-encoded [RadioPayload]. */
    class Sealed(val combined: ByteArray) : RadioFrame()
}

/**
 * Decrypted Radio application event — the plaintext inside a [RadioFrame.Sealed].
 * Android port of the iOS `RadioPayload`.
 */
sealed class RadioPayload {
    class Message(val message: RadioMessage) : RadioPayload()
    class Reaction(val messageId: String, val reactor: String, val asset: String?) : RadioPayload()
    /** Self-introduce on join, mapping endpoint -> chosen display name. */
    class Roster(val displayName: String) : RadioPayload()
    /** Reliably sent before the first voice frame so receivers preallocate. */
    class VoiceTalkStart(val speaker: String) : RadioPayload()
    /** Reliably sent on PTT release — tears down the receiver track. */
    class VoiceTalkStop(val speaker: String) : RadioPayload()
    /** PTT audio packet (raw 16 kHz mono PCM16 chunk), sent UDP for latency. */
    class VoiceFrame(val speaker: String, val seq: Long, val data: ByteArray) : RadioPayload()
}

/**
 * JSON codec for the Radio wire. Self-contained and Android-only (Radio never
 * talks to iOS), so the keys are terse and the shape is ours to choose. Frames
 * are compact JSON; binary fields are base64. Returns null on any malformed
 * input so the receiver simply drops a bad frame.
 */
object RadioWire {

    // ── frames ────────────────────────────────────────────────────────
    fun encodeFrame(frame: RadioFrame): ByteArray {
        val o = JsonObject()
        when (frame) {
            is RadioFrame.Handshake -> {
                o.addProperty("f", "hs")
                o.addProperty("pub", b64(frame.pub))
            }
            is RadioFrame.Sealed -> {
                o.addProperty("f", "s")
                o.addProperty("d", b64(frame.combined))
            }
        }
        return o.toString().toByteArray(Charsets.UTF_8)
    }

    fun decodeFrame(bytes: ByteArray): RadioFrame? = runCatching {
        val o = JsonParser.parseString(String(bytes, Charsets.UTF_8)).asJsonObject
        when (o.get("f")?.asString) {
            "hs" -> RadioFrame.Handshake(unb64(o.get("pub").asString))
            "s" -> RadioFrame.Sealed(unb64(o.get("d").asString))
            else -> null
        }
    }.getOrNull()

    // ── payloads ──────────────────────────────────────────────────────
    fun encodePayload(p: RadioPayload): ByteArray {
        val o = JsonObject()
        when (p) {
            is RadioPayload.Message -> {
                o.addProperty("p", "msg")
                o.add("m", encodeMessage(p.message))
            }
            is RadioPayload.Reaction -> {
                o.addProperty("p", "rx")
                o.addProperty("mid", p.messageId)
                o.addProperty("by", p.reactor)
                if (p.asset != null) o.addProperty("a", p.asset)
            }
            is RadioPayload.Roster -> {
                o.addProperty("p", "ros")
                o.addProperty("dn", p.displayName)
            }
            is RadioPayload.VoiceTalkStart -> {
                o.addProperty("p", "vts")
                o.addProperty("sp", p.speaker)
            }
            is RadioPayload.VoiceTalkStop -> {
                o.addProperty("p", "vte")
                o.addProperty("sp", p.speaker)
            }
            is RadioPayload.VoiceFrame -> {
                o.addProperty("p", "vf")
                o.addProperty("sp", p.speaker)
                o.addProperty("seq", p.seq)
                o.addProperty("d", b64(p.data))
            }
        }
        return o.toString().toByteArray(Charsets.UTF_8)
    }

    fun decodePayload(bytes: ByteArray): RadioPayload? = runCatching {
        val o = JsonParser.parseString(String(bytes, Charsets.UTF_8)).asJsonObject
        when (o.get("p")?.asString) {
            "msg" -> RadioPayload.Message(decodeMessage(o.getAsJsonObject("m")))
            "rx" -> RadioPayload.Reaction(
                o.get("mid").asString,
                o.get("by").asString,
                o.get("a")?.asString,
            )
            "ros" -> RadioPayload.Roster(o.get("dn").asString)
            "vts" -> RadioPayload.VoiceTalkStart(o.get("sp").asString)
            "vte" -> RadioPayload.VoiceTalkStop(o.get("sp").asString)
            "vf" -> RadioPayload.VoiceFrame(
                o.get("sp").asString,
                o.get("seq").asLong,
                unb64(o.get("d").asString),
            )
            else -> null
        }
    }.getOrNull()

    // ── message (de)serialization ─────────────────────────────────────
    private fun encodeMessage(m: RadioMessage): JsonObject {
        val o = JsonObject()
        o.addProperty("id", m.id)
        o.addProperty("sn", m.senderDisplayName)
        o.addProperty("t", m.text)
        o.addProperty("ts", m.timestampMs)
        m.replyToId?.let { o.addProperty("rid", it) }
        m.replyToSender?.let { o.addProperty("rsn", it) }
        m.replyToBody?.let { o.addProperty("rbd", it) }
        if (m.reactions.isNotEmpty()) {
            val r = JsonObject()
            m.reactions.forEach { (k, v) -> r.addProperty(k, v) }
            o.add("rx", r)
        }
        return o
    }

    private fun decodeMessage(o: JsonObject): RadioMessage {
        val reactions = mutableMapOf<String, String>()
        o.getAsJsonObject("rx")?.entrySet()?.forEach { (k, v) -> reactions[k] = v.asString }
        return RadioMessage(
            id = o.get("id").asString,
            senderDisplayName = o.get("sn").asString,
            isFromMe = false, // set by the receiver; never trusted off the wire
            text = o.get("t").asString,
            timestampMs = o.get("ts").asLong,
            replyToId = o.get("rid")?.asString,
            replyToSender = o.get("rsn")?.asString,
            replyToBody = o.get("rbd")?.asString,
            reactions = reactions,
        )
    }

    private fun b64(bytes: ByteArray): String = Base64.encodeToString(bytes, Base64.NO_WRAP)
    private fun unb64(s: String): ByteArray = Base64.decode(s, Base64.NO_WRAP)
}

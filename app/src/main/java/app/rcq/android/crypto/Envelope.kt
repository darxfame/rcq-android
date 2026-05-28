package app.rcq.android.crypto

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.util.UUID

/**
 * Message envelope — the plaintext payload that lives inside a sealed
 * envelope. Mirrors the iOS `Envelope` Codable (CryptoService.swift). The
 * MVP only handles text; other kinds (media, reactions, system, …) decode
 * to [Unknown] and are ignored for now.
 *
 * Wire JSON for a text message (must match iOS byte-for-byte enough for
 * its JSONDecoder to parse, and vice versa):
 *   {"kind":"text","id":"<UUID>","text":"<string>"}
 * iOS uses uppercase UUID strings; we emit the same. Optional fields
 * (ttl, fwdName, reply) are omitted, matching iOS `encodeIfPresent`.
 */
sealed interface Envelope {
    data class Text(val id: String, val text: String) : Envelope
    data class Unknown(val kind: String) : Envelope

    /** Serialize to the exact JSON bytes that get signed and shipped. */
    fun toJsonBytes(): ByteArray = when (this) {
        is Text -> JsonObject().apply {
            addProperty("kind", "text")
            addProperty("id", id)
            addProperty("text", text)
        }.toString().toByteArray(Charsets.UTF_8)
        is Unknown -> JsonObject().apply { addProperty("kind", kind) }
            .toString().toByteArray(Charsets.UTF_8)
    }

    companion object {
        fun text(body: String): Text =
            Text(id = UUID.randomUUID().toString().uppercase(), text = body)

        fun fromJsonBytes(bytes: ByteArray): Envelope {
            val obj = JsonParser.parseString(String(bytes, Charsets.UTF_8)).asJsonObject
            return when (val kind = obj.get("kind")?.asString) {
                "text" -> Text(
                    id = obj.get("id")?.asString ?: UUID.randomUUID().toString(),
                    text = obj.get("text")?.asString.orEmpty(),
                )
                else -> Unknown(kind ?: "unknown")
            }
        }
    }
}

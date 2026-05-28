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
    /** Photo. `mediaId`/`mediaKey` point at the out-of-band encrypted
     *  blob (rcq-spec 9). caption may be empty. */
    data class Photo(val id: String, val mediaId: String, val mediaKey: String, val caption: String?) : Envelope
    data class Unknown(val kind: String) : Envelope

    /** Serialize to the exact JSON bytes that get signed and shipped.
     *  Field names match the iOS Envelope CodingKeys. */
    fun toJsonBytes(): ByteArray = when (this) {
        is Text -> JsonObject().apply {
            addProperty("kind", "text")
            addProperty("id", id)
            addProperty("text", text)
        }.toString().toByteArray(Charsets.UTF_8)
        is Photo -> JsonObject().apply {
            addProperty("kind", "photo")
            addProperty("id", id)
            addProperty("mediaID", mediaId)
            addProperty("mediaKey", mediaKey)
            if (!caption.isNullOrEmpty()) addProperty("caption", caption)
        }.toString().toByteArray(Charsets.UTF_8)
        is Unknown -> JsonObject().apply { addProperty("kind", kind) }
            .toString().toByteArray(Charsets.UTF_8)
    }

    companion object {
        fun text(body: String): Text =
            Text(id = UUID.randomUUID().toString().uppercase(), text = body)

        fun photo(mediaId: String, mediaKey: String, caption: String?): Photo =
            Photo(UUID.randomUUID().toString().uppercase(), mediaId, mediaKey, caption)

        fun fromJsonBytes(bytes: ByteArray): Envelope {
            val obj = JsonParser.parseString(String(bytes, Charsets.UTF_8)).asJsonObject
            val id = obj.get("id")?.asString ?: UUID.randomUUID().toString()
            return when (val kind = obj.get("kind")?.asString) {
                "text" -> Text(id, obj.get("text")?.asString.orEmpty())
                "photo" -> Photo(
                    id = id,
                    mediaId = obj.get("mediaID")?.asString.orEmpty(),
                    mediaKey = obj.get("mediaKey")?.asString.orEmpty(),
                    caption = obj.get("caption")?.asString,
                )
                else -> Unknown(kind ?: "unknown")
            }
        }
    }
}

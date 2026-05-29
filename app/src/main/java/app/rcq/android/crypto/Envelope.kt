package app.rcq.android.crypto

import com.google.gson.JsonArray
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
/** Quoted-message context, matching the iOS ReplyContext Codable
 *  ({id, snippet, authorName}), carried under the "reply" key. */
data class Reply(val id: String, val snippet: String, val authorName: String)

sealed interface Envelope {
    data class Text(val id: String, val text: String, val replyTo: Reply? = null) : Envelope
    /** Photo. `mediaId`/`mediaKey` point at the out-of-band encrypted
     *  blob (rcq-spec 9). caption may be empty. */
    data class Photo(val id: String, val mediaId: String, val mediaKey: String, val caption: String?) : Envelope
    /** A reaction to another message (iOS kind "reaction"). Carries no own
     *  message id; [targetId] is the reacted message's UUID, [asset] the
     *  emoji (null clears, currently treated as a no-op on receipt). */
    data class Reaction(val targetId: String, val asset: String?) : Envelope
    /** Delete-for-everyone (iOS kind "delete"): the author retracts the
     *  message [targetId] for all recipients. */
    data class Delete(val targetId: String) : Envelope
    /** Edit (iOS kind "edit"): the author replaces the body of message
     *  [targetId] with [text]. */
    data class Edit(val targetId: String, val text: String) : Envelope
    /** Read receipt (iOS kind "read"): the recipient acknowledges seeing
     *  the messages [targetIds]. The original sender flips those bubbles
     *  to READ. */
    data class ReadReceipt(val targetIds: List<String>) : Envelope
    /** File attachment (iOS kind "file"). Like [Photo] the bytes live in an
     *  out-of-band encrypted blob; [fileName]/[mime]/[sizeBytes] describe it
     *  for the bubble. */
    data class File(
        val id: String,
        val mediaId: String,
        val mediaKey: String,
        val fileName: String,
        val mime: String,
        val sizeBytes: Long,
        val caption: String?,
    ) : Envelope
    /** Voice note (iOS kind "voice"). Audio bytes live in an encrypted
     *  blob; [durationSec] drives the bubble timer. */
    data class Voice(
        val id: String,
        val mediaId: String,
        val mediaKey: String,
        val durationSec: Double,
    ) : Envelope
    data class Unknown(val kind: String) : Envelope

    /** Serialize to the exact JSON bytes that get signed and shipped.
     *  Field names match the iOS Envelope CodingKeys. */
    fun toJsonBytes(): ByteArray = when (this) {
        is Text -> JsonObject().apply {
            addProperty("kind", "text")
            addProperty("id", id)
            addProperty("text", text)
            replyTo?.let {
                add("reply", JsonObject().apply {
                    addProperty("id", it.id)
                    addProperty("snippet", it.snippet)
                    addProperty("authorName", it.authorName)
                })
            }
        }.toString().toByteArray(Charsets.UTF_8)
        is Photo -> JsonObject().apply {
            addProperty("kind", "photo")
            addProperty("id", id)
            addProperty("mediaID", mediaId)
            addProperty("mediaKey", mediaKey)
            if (!caption.isNullOrEmpty()) addProperty("caption", caption)
        }.toString().toByteArray(Charsets.UTF_8)
        is Reaction -> JsonObject().apply {
            addProperty("kind", "reaction")
            addProperty("targetID", targetId)
            if (asset != null) addProperty("asset", asset)
        }.toString().toByteArray(Charsets.UTF_8)
        is Delete -> JsonObject().apply {
            addProperty("kind", "delete")
            addProperty("targetID", targetId)
        }.toString().toByteArray(Charsets.UTF_8)
        is Edit -> JsonObject().apply {
            addProperty("kind", "edit")
            addProperty("targetID", targetId)
            addProperty("text", text)
        }.toString().toByteArray(Charsets.UTF_8)
        is ReadReceipt -> JsonObject().apply {
            addProperty("kind", "read")
            add("targetIDs", JsonArray().apply { targetIds.forEach { add(it) } })
        }.toString().toByteArray(Charsets.UTF_8)
        is File -> JsonObject().apply {
            addProperty("kind", "file")
            addProperty("id", id)
            addProperty("mediaID", mediaId)
            addProperty("mediaKey", mediaKey)
            addProperty("fname", fileName)
            addProperty("mime", mime)
            addProperty("size", sizeBytes)
            if (!caption.isNullOrEmpty()) addProperty("caption", caption)
        }.toString().toByteArray(Charsets.UTF_8)
        is Voice -> JsonObject().apply {
            addProperty("kind", "voice")
            addProperty("id", id)
            addProperty("mediaID", mediaId)
            addProperty("mediaKey", mediaKey)
            addProperty("durationSec", durationSec)
        }.toString().toByteArray(Charsets.UTF_8)
        is Unknown -> JsonObject().apply { addProperty("kind", kind) }
            .toString().toByteArray(Charsets.UTF_8)
    }

    companion object {
        fun text(body: String, replyTo: Reply? = null): Text =
            Text(id = UUID.randomUUID().toString().uppercase(), text = body, replyTo = replyTo)

        fun photo(mediaId: String, mediaKey: String, caption: String?): Photo =
            Photo(UUID.randomUUID().toString().uppercase(), mediaId, mediaKey, caption)

        fun reaction(targetId: String, asset: String?): Reaction = Reaction(targetId, asset)

        fun delete(targetId: String): Delete = Delete(targetId)

        fun edit(targetId: String, text: String): Edit = Edit(targetId, text)

        fun readReceipt(targetIds: List<String>): ReadReceipt = ReadReceipt(targetIds)

        fun file(mediaId: String, mediaKey: String, fileName: String, mime: String, sizeBytes: Long, caption: String?): File =
            File(UUID.randomUUID().toString().uppercase(), mediaId, mediaKey, fileName, mime, sizeBytes, caption)

        fun voice(mediaId: String, mediaKey: String, durationSec: Double): Voice =
            Voice(UUID.randomUUID().toString().uppercase(), mediaId, mediaKey, durationSec)

        fun fromJsonBytes(bytes: ByteArray): Envelope {
            val obj = JsonParser.parseString(String(bytes, Charsets.UTF_8)).asJsonObject
            val id = obj.get("id")?.asString ?: UUID.randomUUID().toString()
            val reply = obj.getAsJsonObject("reply")?.let {
                Reply(
                    id = it.get("id")?.asString.orEmpty(),
                    snippet = it.get("snippet")?.asString.orEmpty(),
                    authorName = it.get("authorName")?.asString.orEmpty(),
                )
            }
            return when (val kind = obj.get("kind")?.asString) {
                "text" -> Text(id, obj.get("text")?.asString.orEmpty(), reply)
                "photo" -> Photo(
                    id = id,
                    mediaId = obj.get("mediaID")?.asString.orEmpty(),
                    mediaKey = obj.get("mediaKey")?.asString.orEmpty(),
                    caption = obj.get("caption")?.asString,
                )
                "reaction" -> Reaction(
                    targetId = obj.get("targetID")?.asString.orEmpty(),
                    asset = obj.get("asset")?.asString,
                )
                "delete" -> Delete(obj.get("targetID")?.asString.orEmpty())
                "edit" -> Edit(
                    targetId = obj.get("targetID")?.asString.orEmpty(),
                    text = obj.get("text")?.asString.orEmpty(),
                )
                "read" -> ReadReceipt(
                    obj.getAsJsonArray("targetIDs")?.mapNotNull { it.asString } ?: emptyList(),
                )
                "file" -> File(
                    id = id,
                    mediaId = obj.get("mediaID")?.asString.orEmpty(),
                    mediaKey = obj.get("mediaKey")?.asString.orEmpty(),
                    fileName = obj.get("fname")?.asString ?: "file",
                    mime = obj.get("mime")?.asString ?: "application/octet-stream",
                    sizeBytes = obj.get("size")?.asLong ?: 0L,
                    caption = obj.get("caption")?.asString,
                )
                "voice" -> Voice(
                    id = id,
                    mediaId = obj.get("mediaID")?.asString.orEmpty(),
                    mediaKey = obj.get("mediaKey")?.asString.orEmpty(),
                    durationSec = obj.get("durationSec")?.asDouble ?: 0.0,
                )
                else -> Unknown(kind ?: "unknown")
            }
        }
    }
}

package app.rcq.android.model

/** A mutual contact, as returned by GET /contacts (server-authoritative). */
data class Contact(
    val uin: Int,
    val nickname: String,
    val identityKey: String,        // base64 raw X25519 public
    val signingKey: String?,        // base64 raw Ed25519 public
    val status: String? = null,     // online|away|dnd|offline
    val statusMessage: String? = null,
    val blocked: Boolean = false,
    val gender: String? = null,     // "m" | "f" | null (visibility-gated)
    val lastSeen: Long? = null,     // epoch millis, null when hidden/online
) {
    /** Presence as a typed enum (server never sends `invisible` for peers). */
    val presence: UserStatus get() = UserStatus.from(status)
}

/** An inbound contact request awaiting the user's accept/decline. */
data class PendingRequest(
    val requestId: Int,
    val fromUin: Int,
    val fromNickname: String,
)

/** Delivery state of an outgoing message. Incoming messages are always
 *  DELIVERED. Mirrors the iOS DeliveryState minus read receipts. */
enum class DeliveryState { SENDING, SENT, DELIVERED, FAILED }

/** A single chat message, persisted locally (the server doesn't keep
 *  messages after delivery). `id` is the envelope UUID — the dedup key
 *  that prevents a message arriving via both WebSocket and the queue
 *  drain from showing twice (rcq-spec 6.3.1). */
data class ChatMessage(
    val id: String,
    val peerUin: Int,
    val fromMe: Boolean,
    val body: String,             // text, or caption for media
    val sentAt: Long,
    val state: DeliveryState = DeliveryState.DELIVERED,
    val kind: String = "text",    // "text" | "photo"
    val mediaId: String? = null,
    val mediaKey: String? = null,
    val replyToSnippet: String? = null,
    val replyToAuthor: String? = null,
)

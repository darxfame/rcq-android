package app.rcq.android.model

/** A mutual contact, as returned by GET /contacts (server-authoritative). */
data class Contact(
    val uin: Int,
    val nickname: String,
    val identityKey: String,      // base64 raw X25519 public
    val signingKey: String?,      // base64 raw Ed25519 public
    val status: String? = null,   // online|away|dnd|offline
)

/** An inbound contact request awaiting the user's accept/decline. */
data class PendingRequest(
    val requestId: Int,
    val fromUin: Int,
    val fromNickname: String,
)

/** A single chat message, persisted locally (the server doesn't keep
 *  messages after delivery). `id` is the envelope UUID — the dedup key
 *  that prevents a message arriving via both WebSocket and the queue
 *  drain from showing twice (rcq-spec 6.3.1). */
data class ChatMessage(
    val id: String,
    val peerUin: Int,
    val fromMe: Boolean,
    val body: String,
    val sentAt: Long,
)

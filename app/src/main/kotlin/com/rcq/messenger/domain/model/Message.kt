package com.rcq.messenger.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Message delivery state tracking.
 * 
 * Shows progression:
 * SENDING → SENT → DELIVERED → READ
 */
@Serializable
enum class DeliveryState {
    @SerialName("sending")
    SENDING,      // Message being uploaded to server
    
    @SerialName("sent")
    SENT,         // Server accepted message
    
    @SerialName("delivered")
    DELIVERED,    // Recipient's device received message
    
    @SerialName("read")
    READ          // Recipient opened message/chat
}

/**
 * Emoji reaction with list of users who reacted.
 */
@Serializable
data class Reaction(
    val emoji: String,
    val userUins: List<Long> = emptyList()
) {
    fun hasUser(uin: Long): Boolean = userUins.contains(uin)
    
    fun withUser(uin: Long): Reaction {
        return if (hasUser(uin)) {
            copy(userUins = userUins - uin)
        } else {
            copy(userUins = userUins + uin)
        }
    }
}

/**
 * Full Message model with all fields for Phase 1.
 * 
 * CRITICAL (Phase 1.1):
 * - Added ciphertext/nonce/ephemeralKey for E2EE
 * - Added signal_type for Signal Protocol version
 * 
 * FEATURES (Phase 1.2-1.7):
 * - Reactions (Phase 1.2)
 * - Edit history (Phase 1.3)
 * - Delivery state (Phase 1.4)
 * - Reply to message (Phase 1.5)
 * - Forward (Phase 1.6)
 * - Disappearing messages / TTL (Phase 1.7)
 * 
 * FUTURE (Phase 2+):
 * - Media fields (mediaUrl, mediaType, mediaSize)
 * - Voice messages
 * - File attachments
 */
@Serializable
data class Message(
    val id: Long,
    val chatId: Long,
    
    @SerialName("from_uin")
    val fromUin: Long,
    
    val text: String,
    
    @SerialName("created_at")
    val createdAt: Long,
    
    // ==================== E2EE (Phase 1.1) ====================
    
    val ciphertext: String? = null,
    val nonce: String? = null,
    
    @SerialName("ephemeral_key")
    val ephemeralKey: String? = null,
    
    @SerialName("signal_type")
    val signalType: String? = null,
    
    // ==================== REACTIONS (Phase 1.2) ====================
    
    val reactions: List<Reaction> = emptyList(),
    
    // ==================== EDIT (Phase 1.3) ====================
    
    @SerialName("edited_at")
    val editedAt: Long? = null,
    
    @SerialName("edit_history")
    val editHistory: List<String> = emptyList(),
    
    // ==================== DELIVERY STATE (Phase 1.4) ====================
    
    @SerialName("delivery_state")
    val deliveryState: DeliveryState = DeliveryState.SENDING,
    
    // ==================== REPLY (Phase 1.5) ====================
    
    @SerialName("reply_to_id")
    val replyToId: Long? = null,
    
    @SerialName("reply_to_text")
    val replyToText: String? = null,
    
    @SerialName("reply_to_author_uin")
    val replyToAuthorUin: Long? = null,
    
    // ==================== FORWARD (Phase 1.6) ====================
    
    @SerialName("forwarded_from_uin")
    val forwardedFromUin: Long? = null,
    
    @SerialName("forwarded_from_name")
    val forwardedFromName: String? = null,
    
    // ==================== TTL (Phase 1.7) ====================
    
    @SerialName("ttl_seconds")
    val ttlSeconds: Int? = null,
    
    @SerialName("expires_at")
    val expiresAt: Long? = null,
    
    // ==================== MEDIA (Phase 2) ====================
    
    @SerialName("media_url")
    val mediaUrl: String? = null,
    
    @SerialName("media_type")
    val mediaType: String? = null,
    
    @SerialName("media_size")
    val mediaSize: Long? = null
) {
    fun toEntity(): MessageEntity = MessageEntity(
        id = id,
        chatId = chatId,
        fromUin = fromUin,
        text = text,
        createdAt = createdAt,
        ciphertext = ciphertext,
        nonce = nonce,
        ephemeralKey = ephemeralKey,
        signalType = signalType,
        reactions = reactionsToJson(),
        editedAt = editedAt,
        editHistory = editHistory.joinToString("|"),
        deliveryState = deliveryState.name,
        replyToId = replyToId,
        replyToText = replyToText,
        replyToAuthorUin = replyToAuthorUin,
        forwardedFromUin = forwardedFromUin,
        forwardedFromName = forwardedFromName,
        ttlSeconds = ttlSeconds,
        expiresAt = expiresAt,
        mediaUrl = mediaUrl,
        mediaType = mediaType,
        mediaSize = mediaSize
    )
    
    private fun reactionsToJson(): String {
        return reactions.joinToString(",") { "${it.emoji}:${it.userUins.joinToString(":")}" }
    }
    
    companion object {
        fun fromEntity(entity: MessageEntity): Message = Message(
            id = entity.id,
            chatId = entity.chatId,
            fromUin = entity.fromUin,
            text = entity.text,
            createdAt = entity.createdAt,
            ciphertext = entity.ciphertext,
            nonce = entity.nonce,
            ephemeralKey = entity.ephemeralKey,
            signalType = entity.signalType,
            reactions = entity.reactions?.parseReactions() ?: emptyList(),
            editedAt = entity.editedAt,
            editHistory = entity.editHistory?.split("|") ?: emptyList(),
            deliveryState = try {
                DeliveryState.valueOf(entity.deliveryState)
            } catch (e: Exception) {
                DeliveryState.SENDING
            },
            replyToId = entity.replyToId,
            replyToText = entity.replyToText,
            replyToAuthorUin = entity.replyToAuthorUin,
            forwardedFromUin = entity.forwardedFromUin,
            forwardedFromName = entity.forwardedFromName,
            ttlSeconds = entity.ttlSeconds,
            expiresAt = entity.expiresAt,
            mediaUrl = entity.mediaUrl,
            mediaType = entity.mediaType,
            mediaSize = entity.mediaSize
        )
        
        private fun String.parseReactions(): List<Reaction> {
            return split(",").mapNotNull { part ->
                val (emoji, uinsStr) = part.split(":").takeIf { it.size >= 2 } ?: return@mapNotNull null
                val uins = uinsStr.split(":").mapNotNull { it.toLongOrNull() }
                Reaction(emoji, uins)
            }
        }
    }
}

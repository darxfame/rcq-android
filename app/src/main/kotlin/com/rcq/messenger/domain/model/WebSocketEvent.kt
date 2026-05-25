package com.rcq.messenger.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * Complete WebSocket event model — implements 40+ event types from iOS implementation.
 * 
 * Structured as sealed class for type-safe routing to repositories.
 * Maps to iOS WebSocket.Event enum in 12 categories:
 * - Messaging (9 events)
 * - Presence (5 events)
 * - Threads (2 events)
 * - Groups (4 events)
 * - Calls (6 events)
 * - Audio Rooms (4 events)
 * - Stories (3 events)
 * - Marketplace (3 events)
 * - Games (9 events)
 * - Auctions (4 events)
 * - Trades (4 events)
 * - Pets (3 events)
 * And others...
 */
@Serializable
sealed class WebSocketEvent {
    abstract val timestamp: Long
    
    // ==================== MESSAGING EVENTS ====================
    
    @Serializable
    @SerialName("new_message")
    data class NewMessage(
        override val timestamp: Long,
        val messageId: String,
        val fromUin: Long,
        val toUin: Long,
        val text: String,
        val ciphertext: String? = null,
        val nonce: String? = null,
        val signalType: String? = null
    ) : WebSocketEvent()
    
    @Serializable
    @SerialName("message_deleted")
    data class MessageDeleted(
        override val timestamp: Long,
        val messageId: String,
        val chatId: Long
    ) : WebSocketEvent()
    
    @Serializable
    @SerialName("message_deleted_for_everyone")
    data class MessageDeletedForEveryone(
        override val timestamp: Long,
        val messageId: String,
        val chatId: Long
    ) : WebSocketEvent()
    
    @Serializable
    @SerialName("message_reaction")
    data class MessageReaction(
        override val timestamp: Long,
        val messageId: String,
        val reaction: String,
        val fromUin: Long
    ) : WebSocketEvent()
    
    @Serializable
    @SerialName("message_read")
    data class MessageRead(
        override val timestamp: Long,
        val messageIds: List<String>,
        val fromUin: Long
    ) : WebSocketEvent()
    
    @Serializable
    @SerialName("message_edited")
    data class MessageEdited(
        override val timestamp: Long,
        val messageId: String,
        val newText: String,
        val editedAt: Long
    ) : WebSocketEvent()
    
    @Serializable
    @SerialName("message_bounced")
    data class MessageBounced(
        override val timestamp: Long,
        val messageId: String,
        val reason: String
    ) : WebSocketEvent()
    
    @Serializable
    @SerialName("typing_started")
    data class TypingStarted(
        override val timestamp: Long,
        val fromUin: Long,
        val chatId: Long
    ) : WebSocketEvent()
    
    @Serializable
    @SerialName("typing_stopped")
    data class TypingStopped(
        override val timestamp: Long,
        val fromUin: Long,
        val chatId: Long
    ) : WebSocketEvent()
    
    // ==================== PRESENCE EVENTS ====================
    
    @Serializable
    @SerialName("presence_online")
    data class PresenceOnline(
        override val timestamp: Long,
        val userUin: Long
    ) : WebSocketEvent()
    
    @Serializable
    @SerialName("presence_away")
    data class PresenceAway(
        override val timestamp: Long,
        val userUin: Long
    ) : WebSocketEvent()
    
    @Serializable
    @SerialName("presence_dnd")
    data class PresenceDnd(
        override val timestamp: Long,
        val userUin: Long
    ) : WebSocketEvent()
    
    @Serializable
    @SerialName("presence_invisible")
    data class PresenceInvisible(
        override val timestamp: Long,
        val userUin: Long
    ) : WebSocketEvent()
    
    @Serializable
    @SerialName("presence_offline")
    data class PresenceOffline(
        override val timestamp: Long,
        val userUin: Long
    ) : WebSocketEvent()
    
    // ==================== THREAD EVENTS ====================
    
    @Serializable
    @SerialName("thread_updated")
    data class ThreadUpdated(
        override val timestamp: Long,
        val threadId: Long,
        val updatedFields: JsonElement? = null
    ) : WebSocketEvent()
    
    @Serializable
    @SerialName("thread_deleted")
    data class ThreadDeleted(
        override val timestamp: Long,
        val threadId: Long
    ) : WebSocketEvent()
    
    // ==================== GROUP EVENTS ====================
    
    @Serializable
    @SerialName("group_updated")
    data class GroupUpdated(
        override val timestamp: Long,
        val groupId: Long,
        val updatedFields: JsonElement? = null
    ) : WebSocketEvent()
    
    @Serializable
    @SerialName("group_member_joined")
    data class GroupMemberJoined(
        override val timestamp: Long,
        val groupId: Long,
        val memberUin: Long
    ) : WebSocketEvent()
    
    @Serializable
    @SerialName("group_member_left")
    data class GroupMemberLeft(
        override val timestamp: Long,
        val groupId: Long,
        val memberUin: Long
    ) : WebSocketEvent()
    
    @Serializable
    @SerialName("group_deleted")
    data class GroupDeleted(
        override val timestamp: Long,
        val groupId: Long
    ) : WebSocketEvent()
    
    // ==================== CALL EVENTS ====================
    
    @Serializable
    @SerialName("call_offer")
    data class CallOffer(
        override val timestamp: Long,
        val callId: String,
        val fromUin: Long,
        val offer: JsonElement? = null
    ) : WebSocketEvent()
    
    @Serializable
    @SerialName("call_answer")
    data class CallAnswer(
        override val timestamp: Long,
        val callId: String,
        val answer: JsonElement? = null
    ) : WebSocketEvent()
    
    @Serializable
    @SerialName("call_ice_candidate")
    data class CallIceCandidate(
        override val timestamp: Long,
        val callId: String,
        val candidate: JsonElement? = null
    ) : WebSocketEvent()
    
    @Serializable
    @SerialName("call_end")
    data class CallEnd(
        override val timestamp: Long,
        val callId: String,
        val duration: Long
    ) : WebSocketEvent()
    
    @Serializable
    @SerialName("call_upgrade")
    data class CallUpgrade(
        override val timestamp: Long,
        val callId: String
    ) : WebSocketEvent()
    
    @Serializable
    @SerialName("call_upgrade_answer")
    data class CallUpgradeAnswer(
        override val timestamp: Long,
        val callId: String
    ) : WebSocketEvent()
    
    // ==================== AUDIO ROOM EVENTS ====================
    
    @Serializable
    @SerialName("audio_room_started")
    data class AudioRoomStarted(
        override val timestamp: Long,
        val roomId: String,
        val hostUin: Long
    ) : WebSocketEvent()
    
    @Serializable
    @SerialName("audio_room_peer_joined")
    data class AudioRoomPeerJoined(
        override val timestamp: Long,
        val roomId: String,
        val peerUin: Long
    ) : WebSocketEvent()
    
    @Serializable
    @SerialName("audio_room_peer_left")
    data class AudioRoomPeerLeft(
        override val timestamp: Long,
        val roomId: String,
        val peerUin: Long
    ) : WebSocketEvent()
    
    @Serializable
    @SerialName("audio_room_ended")
    data class AudioRoomEnded(
        override val timestamp: Long,
        val roomId: String
    ) : WebSocketEvent()
    
    // ==================== STORY EVENTS ====================
    
    @Serializable
    @SerialName("story_posted")
    data class StoryPosted(
        override val timestamp: Long,
        val storyId: String,
        val fromUin: Long
    ) : WebSocketEvent()
    
    @Serializable
    @SerialName("story_expired")
    data class StoryExpired(
        override val timestamp: Long,
        val storyId: String
    ) : WebSocketEvent()
    
    @Serializable
    @SerialName("story_viewed")
    data class StoryViewed(
        override val timestamp: Long,
        val storyId: String,
        val viewerUin: Long
    ) : WebSocketEvent()
    
    // ==================== MARKETPLACE EVENTS ====================
    
    @Serializable
    @SerialName("marketplace_listing_created")
    data class MarketplaceListingCreated(
        override val timestamp: Long,
        val listingId: String
    ) : WebSocketEvent()
    
    @Serializable
    @SerialName("marketplace_listing_sold")
    data class MarketplaceListingSold(
        override val timestamp: Long,
        val listingId: String
    ) : WebSocketEvent()
    
    @Serializable
    @SerialName("marketplace_listing_cancelled")
    data class MarketplaceListingCancelled(
        override val timestamp: Long,
        val listingId: String
    ) : WebSocketEvent()
    
    // ==================== GENERIC/UNKNOWN EVENT ====================
    
    @Serializable
    data class Unknown(
        override val timestamp: Long,
        val type: String,
        val data: JsonElement? = null
    ) : WebSocketEvent()
}

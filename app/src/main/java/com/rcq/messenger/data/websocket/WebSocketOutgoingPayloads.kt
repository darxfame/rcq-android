package com.rcq.messenger.data.websocket

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

object WebSocketOutgoingPayloads {
    private fun obj(vararg pairs: Pair<String, JsonElement>): String =
        JsonObject(mapOf(*pairs)).toString()

    // Heartbeat: server derives online state from last_seen refresh on each ping.
    fun ping(): String = obj("type" to JsonPrimitive("ping"))

    // Typing indicator: server relays as {"type":"typing","from_uin":int,"active":bool}.
    fun typing(toUin: Long, active: Boolean): String = obj(
        "type" to JsonPrimitive("typing"),
        "to_uin" to JsonPrimitive(toUin),
        "active" to JsonPrimitive(active)
    )

    // Call signaling (WebRTC, server relay via ws.py).
    fun callOffer(toUin: Long, callId: String, sdp: String, media: String = "audio"): String = obj(
        "type" to JsonPrimitive("call_offer"),
        "to_uin" to JsonPrimitive(toUin),
        "call_id" to JsonPrimitive(callId),
        "sdp" to JsonPrimitive(sdp),
        "media" to JsonPrimitive(media)
    )

    fun callAnswer(toUin: Long, callId: String, sdp: String): String = obj(
        "type" to JsonPrimitive("call_answer"),
        "to_uin" to JsonPrimitive(toUin),
        "call_id" to JsonPrimitive(callId),
        "sdp" to JsonPrimitive(sdp)
    )

    fun callIce(toUin: Long, callId: String, candidate: String): String = obj(
        "type" to JsonPrimitive("call_ice"),
        "to_uin" to JsonPrimitive(toUin),
        "call_id" to JsonPrimitive(callId),
        "candidate" to JsonPrimitive(candidate)
    )

    fun callEnd(toUin: Long, callId: String, reason: String = "user_ended"): String = obj(
        "type" to JsonPrimitive("call_end"),
        "to_uin" to JsonPrimitive(toUin),
        "call_id" to JsonPrimitive(callId),
        "reason" to JsonPrimitive(reason)
    )

    fun callRenegotiate(toUin: Long, callId: String, sdp: String): String = obj(
        "type" to JsonPrimitive("call_renegotiate"),
        "to_uin" to JsonPrimitive(toUin),
        "call_id" to JsonPrimitive(callId),
        "sdp" to JsonPrimitive(sdp)
    )

    fun callRenegotiateAnswer(toUin: Long, callId: String, sdp: String): String = obj(
        "type" to JsonPrimitive("call_renegotiate_answer"),
        "to_uin" to JsonPrimitive(toUin),
        "call_id" to JsonPrimitive(callId),
        "sdp" to JsonPrimitive(sdp)
    )

    fun callRenegotiateDecline(toUin: Long, callId: String): String = obj(
        "type" to JsonPrimitive("call_renegotiate_decline"),
        "to_uin" to JsonPrimitive(toUin),
        "call_id" to JsonPrimitive(callId)
    )

    // Audio room signaling (server relay via ws.py).
    fun roomEnter(roomId: Int): String = obj(
        "type" to JsonPrimitive("room_enter"),
        "room_id" to JsonPrimitive(roomId)
    )

    fun roomLeave(roomId: Int): String = obj(
        "type" to JsonPrimitive("room_leave"),
        "room_id" to JsonPrimitive(roomId)
    )

    fun roomOffer(roomId: Int, toUin: Int, sdp: String): String = obj(
        "type" to JsonPrimitive("room_offer"),
        "room_id" to JsonPrimitive(roomId),
        "to_uin" to JsonPrimitive(toUin),
        "sdp" to JsonPrimitive(sdp)
    )

    fun roomAnswer(roomId: Int, toUin: Int, sdp: String): String = obj(
        "type" to JsonPrimitive("room_answer"),
        "room_id" to JsonPrimitive(roomId),
        "to_uin" to JsonPrimitive(toUin),
        "sdp" to JsonPrimitive(sdp)
    )

    fun roomIce(roomId: Int, toUin: Int, candidate: String): String = obj(
        "type" to JsonPrimitive("room_ice"),
        "room_id" to JsonPrimitive(roomId),
        "to_uin" to JsonPrimitive(toUin),
        "candidate" to JsonPrimitive(candidate)
    )

    fun roomSpeaking(roomId: Int, speaking: Boolean): String = obj(
        "type" to JsonPrimitive("room_speaking"),
        "room_id" to JsonPrimitive(roomId),
        "speaking" to JsonPrimitive(speaking)
    )
}

package com.rcq.messenger.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class WebSocketEvent(
    val type: String,
    val data: String? = null,
    val timestamp: Long = System.currentTimeMillis()
) {
    companion object {
        const val TYPE_MESSAGE = "message"
        const val TYPE_TYPING = "typing"
        const val TYPE_CALL_OFFER = "call_offer"
        const val TYPE_CALL_ANSWER = "call_answer"
        const val TYPE_CALL_ICE_CANDIDATE = "call_ice_candidate"
        const val TYPE_CALL_END = "call_end"
        const val TYPE_USER_STATUS = "user_status"
        const val TYPE_PING = "ping"
        const val TYPE_PONG = "pong"
    }
}
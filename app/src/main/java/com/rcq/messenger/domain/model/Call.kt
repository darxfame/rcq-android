package com.rcq.messenger.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Call(
    val id: String,
    val type: CallType,
    val targetId: Long,
    val targetNickname: String,
    val targetAvatar: String? = null,
    val initiatorId: Long,
    val status: CallStatus = CallStatus.PENDING,
    val startedAt: Long? = null,
    val endedAt: Long? = null,
    val duration: Long = 0
)

@Serializable
enum class CallType {
    AUDIO,
    VIDEO
}

@Serializable
enum class CallStatus {
    PENDING,
    RINGING,
    CONNECTING,
    CONNECTED,
    ENDED,
    MISSED,
    DECLINED,
    FAILED
}

@Serializable
data class CallLog(
    val calls: List<Call>,
    val totalMissed: Int = 0,
    val lastCall: Call? = null
)
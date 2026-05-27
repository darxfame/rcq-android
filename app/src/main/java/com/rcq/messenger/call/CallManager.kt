package com.rcq.messenger.call

import android.content.Context
import android.content.Intent
import com.rcq.messenger.data.ws.WebSocketManager
import com.rcq.messenger.domain.model.WebSocketEvent
import com.rcq.messenger.service.CallService
import com.rcq.messenger.service.CallState
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.webrtc.IceCandidate
import org.webrtc.SessionDescription
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CallManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val webSocketManager: WebSocketManager,
    private val json: Json
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _currentCall = MutableStateFlow<CallInfo?>(null)
    val currentCall: StateFlow<CallInfo?> = _currentCall.asStateFlow()

    private val _incomingCall = MutableStateFlow<IncomingCallInfo?>(null)
    val incomingCall: StateFlow<IncomingCallInfo?> = _incomingCall.asStateFlow()

    private var callService: CallService? = null

    init {
        observeWebSocketEvents()
    }

    private fun observeWebSocketEvents() {
        scope.launch {
            webSocketManager.eventFlow.collect { event ->
                when (event.type) {
                    WebSocketEvent.TYPE_CALL_OFFER -> handleCallOffer(event)
                    WebSocketEvent.TYPE_CALL_ANSWER -> handleCallAnswer(event)
                    WebSocketEvent.TYPE_CALL_ICE_CANDIDATE -> handleIceCandidate(event)
                    WebSocketEvent.TYPE_CALL_END -> handleCallEnd(event)
                    else -> {}
                }
            }
        }
    }

    fun startCall(targetUin: Long, isVideoCall: Boolean = false) {
        val callInfo = CallInfo(
            callId = generateCallId(),
            targetUin = targetUin,
            isVideoCall = isVideoCall,
            isOutgoing = true,
            state = CallState.CONNECTING
        )
        _currentCall.value = callInfo

        // Start CallService
        val intent = Intent(context, CallService::class.java)
        context.startService(intent)

        // Send call offer via WebSocket
        val offer = CallOfferData(
            callId = callInfo.callId,
            targetUin = targetUin,
            isVideoCall = isVideoCall,
            sdp = null // Will be set by WebRTC
        )

        sendWebSocketMessage("call_offer", offer)
    }

    fun acceptCall(callId: String) {
        val incoming = _incomingCall.value
        if (incoming?.callId == callId) {
            val callInfo = CallInfo(
                callId = callId,
                targetUin = incoming.callerUin,
                isVideoCall = incoming.isVideoCall,
                isOutgoing = false,
                state = CallState.CONNECTING
            )
            _currentCall.value = callInfo
            _incomingCall.value = null

            // Start CallService
            val intent = Intent(context, CallService::class.java)
            context.startService(intent)

            // Send answer via WebSocket
            val answer = CallAnswerData(
                callId = callId,
                accepted = true,
                sdp = null // Will be set by WebRTC
            )

            sendWebSocketMessage("call_answer", answer)
        }
    }

    fun declineCall(callId: String) {
        _incomingCall.value = null

        val answer = CallAnswerData(
            callId = callId,
            accepted = false,
            sdp = null
        )

        sendWebSocketMessage("call_answer", answer)
    }

    fun endCall() {
        val currentCall = _currentCall.value
        if (currentCall != null) {
            // Send end call message
            val endCall = CallEndData(
                callId = currentCall.callId,
                reason = "user_hangup"
            )

            sendWebSocketMessage("call_end", endCall)

            // Stop CallService
            val intent = Intent(context, CallService::class.java)
            context.stopService(intent)

            _currentCall.value = null
        }
    }

    private fun handleCallOffer(event: WebSocketEvent) {
        try {
            val data = json.decodeFromString<Map<String, String>>(event.data ?: "{}")
            val incomingCall = IncomingCallInfo(
                callId = data["callId"] ?: "",
                callerUin = data["fromUin"]?.toLongOrNull() ?: 0L,
                callerName = "User ${data["fromUin"]}", // TODO: получить имя из UserRepository
                isVideoCall = data["callType"] == "video",
                timestamp = System.currentTimeMillis()
            )
            _incomingCall.value = incomingCall
        } catch (e: Exception) {
            // Log error parsing call offer
        }
    }

    private fun handleCallAnswer(event: WebSocketEvent) {
        try {
            val data = json.decodeFromString<Map<String, String>>(event.data ?: "{}")
            val currentCall = _currentCall.value
            val callId = data["callId"]
            if (currentCall?.callId == callId) {
                val accepted = data["accepted"]?.toBoolean() ?: false
                if (accepted) {
                    _currentCall.value = currentCall?.copy(state = CallState.CONNECTED)
                } else {
                    _currentCall.value = null
                    // Stop CallService
                    val intent = Intent(context, CallService::class.java)
                    context.stopService(intent)
                }
            }
        } catch (e: Exception) {
            // Log error parsing call answer
        }
    }

    private fun handleIceCandidate(event: WebSocketEvent) {
        try {
            val data = json.decodeFromString<Map<String, String>>(event.data ?: "{}")
            val sdpMid = data["sdpMid"]
            val sdpMLineIndex = data["sdpMLineIndex"]?.toIntOrNull() ?: 0
            val candidate = data["candidate"]

            if (candidate != null) {
                callService?.addIceCandidate(
                    IceCandidate(sdpMid, sdpMLineIndex, candidate)
                )
            }
        } catch (e: Exception) {
            // Log error parsing ICE candidate
        }
    }

    private fun handleCallEnd(event: WebSocketEvent) {
        try {
            val data = json.decodeFromString<Map<String, String>>(event.data ?: "{}")
            val callId = data["callId"]
            val currentCall = _currentCall.value
            if (currentCall?.callId == callId) {
                _currentCall.value = null

                // Stop CallService
                val intent = Intent(context, CallService::class.java)
                context.stopService(intent)
            }

            // Clear incoming call if it matches
            val incomingCall = _incomingCall.value
            if (incomingCall?.callId == callId) {
                _incomingCall.value = null
            }
        } catch (e: Exception) {
            // Log error parsing call end
        }
    }

    private fun sendWebSocketMessage(type: String, data: Any) {
        scope.launch {
            try {
                val dataJson = json.encodeToString(data)
                val event = WebSocketEvent(
                    type = type,
                    data = dataJson
                )
                webSocketManager.sendMessage(event)
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    private fun generateCallId(): String {
        return "call_${System.currentTimeMillis()}_${(1000..9999).random()}"
    }

    fun bindCallService(service: CallService) {
        callService = service
    }

    fun unbindCallService() {
        callService = null
    }
}

@Serializable
data class CallInfo(
    val callId: String,
    val targetUin: Long,
    val isVideoCall: Boolean,
    val isOutgoing: Boolean,
    val state: CallState,
    val startTime: Long = System.currentTimeMillis()
)

@Serializable
data class IncomingCallInfo(
    val callId: String,
    val callerUin: Long,
    val callerName: String,
    val isVideoCall: Boolean,
    val timestamp: Long
)

@Serializable
data class CallOfferData(
    val callId: String,
    val targetUin: Long,
    val isVideoCall: Boolean,
    val sdp: String?
)

@Serializable
data class CallAnswerData(
    val callId: String,
    val accepted: Boolean,
    val sdp: String?
)

@Serializable
data class CallEndData(
    val callId: String,
    val reason: String
)

@Serializable
data class IceCandidateData(
    val callId: String,
    val candidate: String,
    val sdpMid: String,
    val sdpMLineIndex: Int
)
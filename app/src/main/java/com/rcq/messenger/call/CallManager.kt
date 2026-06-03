package com.rcq.messenger.call

import android.content.Context
import android.content.Intent
import com.rcq.messenger.data.websocket.WebSocketService
import com.rcq.messenger.data.websocket.WsEvent
import com.rcq.messenger.service.CallService
import com.rcq.messenger.service.CallState
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.webrtc.IceCandidate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CallManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val webSocketService: WebSocketService,
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
            webSocketService.events.collect { event ->
                when (event) {
                    is WsEvent.CallOffer -> handleCallOffer(event)
                    is WsEvent.CallAnswer -> handleCallAnswer(event)
                    is WsEvent.CallIceCandidate -> handleIceCandidate(event)
                    is WsEvent.CallEnd -> handleCallEnd(event)
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
        context.startService(Intent(context, CallService::class.java))
        webSocketService.sendCallOffer(
            toUin = targetUin,
            callId = callInfo.callId,
            sdp = "",
            media = if (isVideoCall) "video" else "audio"
        )
    }

    fun acceptCall(callId: String) {
        val incoming = _incomingCall.value ?: return
        if (incoming.callId != callId) return
        val callInfo = CallInfo(
            callId = callId,
            targetUin = incoming.callerUin,
            isVideoCall = incoming.isVideoCall,
            isOutgoing = false,
            state = CallState.CONNECTING
        )
        _currentCall.value = callInfo
        _incomingCall.value = null
        context.startService(Intent(context, CallService::class.java))
        webSocketService.sendCallAnswer(
            toUin = incoming.callerUin,
            callId = callId,
            sdp = ""
        )
    }

    fun declineCall(callId: String) {
        val incoming = _incomingCall.value
        _incomingCall.value = null
        if (incoming?.callId == callId) {
            webSocketService.sendCallEnd(
                toUin = incoming.callerUin,
                callId = callId,
                reason = "declined"
            )
        }
    }

    fun endCall() {
        val call = _currentCall.value ?: return
        webSocketService.sendCallEnd(
            toUin = call.targetUin,
            callId = call.callId,
            reason = "user_hangup"
        )
        context.stopService(Intent(context, CallService::class.java))
        _currentCall.value = null
    }

    private fun handleCallOffer(event: WsEvent.CallOffer) {
        _incomingCall.value = IncomingCallInfo(
            callId = event.callId,
            callerUin = event.fromUin,
            callerName = "User ${event.fromUin}",
            isVideoCall = event.media == "video",
            timestamp = System.currentTimeMillis()
        )
    }

    private fun handleCallAnswer(event: WsEvent.CallAnswer) {
        val call = _currentCall.value ?: return
        if (call.callId != event.callId) return
        _currentCall.value = call.copy(state = CallState.CONNECTED)
    }

    private fun handleIceCandidate(event: WsEvent.CallIceCandidate) {
        val candidateJson = runCatching { json.parseToJsonElement(event.candidate).jsonObject }.getOrNull()
        val sdpMid = candidateJson?.get("sdp_mid")?.jsonPrimitive?.contentOrNull
        val sdpMLineIndex = candidateJson?.get("sdp_mline_index")?.jsonPrimitive?.intOrNull ?: 0
        val candidate = candidateJson?.get("candidate")?.jsonPrimitive?.contentOrNull ?: event.candidate
        callService?.addIceCandidate(IceCandidate(sdpMid, sdpMLineIndex, candidate))
    }

    private fun handleCallEnd(event: WsEvent.CallEnd) {
        if (_currentCall.value?.callId == event.callId) {
            context.stopService(Intent(context, CallService::class.java))
            _currentCall.value = null
        }
        if (_incomingCall.value?.callId == event.callId) {
            _incomingCall.value = null
        }
    }

    private fun generateCallId() = "call_${System.currentTimeMillis()}_${(1000..9999).random()}"

    fun bindCallService(service: CallService) { callService = service }
    fun unbindCallService() { callService = null }
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

package com.rcq.messenger.ui.calls

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rcq.messenger.call.CallManager
import com.rcq.messenger.data.repository.ChatRepository
import com.rcq.messenger.service.CallService
import com.rcq.messenger.service.CallState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CallViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val callManager: CallManager
) : ViewModel() {

    private val _callState = MutableStateFlow(CallState.IDLE)
    val callState: StateFlow<CallState> = _callState.asStateFlow()

    private val _isMuted = MutableStateFlow(false)
    val isMuted: StateFlow<Boolean> = _isMuted.asStateFlow()

    private val _isSpeakerOn = MutableStateFlow(false)
    val isSpeakerOn: StateFlow<Boolean> = _isSpeakerOn.asStateFlow()

    private val _callDuration = MutableStateFlow(0L)
    val callDuration: StateFlow<Long> = _callDuration.asStateFlow()

    private val _targetNickname = MutableStateFlow("User")
    val targetNickname: StateFlow<String> = _targetNickname.asStateFlow()

    private var callService: CallService? = null

    init {
        // Observe CallManager state
        viewModelScope.launch {
            callManager.currentCall.collect { callInfo ->
                callInfo?.let {
                    _callState.value = it.state
                }
            }
        }
    }

    fun startCall(chatId: String, targetUin: Long) {
        _callState.value = CallState.CONNECTING
        callManager.startCall(targetUin, isVideoCall = false)
    }

    fun startVideoCall(chatId: String, targetUin: Long) {
        _callState.value = CallState.CONNECTING
        callManager.startCall(targetUin, isVideoCall = true)
    }

    fun acceptIncomingCall(callId: String) {
        callManager.acceptCall(callId)
    }

    fun declineCall(callId: String) {
        callManager.declineCall(callId)
    }

    fun endCall() {
        callManager.endCall()
        _callState.value = CallState.ENDED
    }

    fun toggleMute() {
        val newMuteState = !_isMuted.value
        _isMuted.value = newMuteState
        callService?.setMicrophoneEnabled(!newMuteState)
    }

    fun toggleSpeaker() {
        val newSpeakerState = !_isSpeakerOn.value
        _isSpeakerOn.value = newSpeakerState
        callService?.setSpeakerphoneOn(newSpeakerState)
    }

    fun bindCallService(service: CallService) {
        callService = service

        // Observe service state
        viewModelScope.launch {
            service.callState.collect { state ->
                _callState.value = state
            }
        }

        viewModelScope.launch {
            service.isMuted.collect { muted ->
                _isMuted.value = muted
            }
        }

        viewModelScope.launch {
            service.isSpeakerOn.collect { speakerOn ->
                _isSpeakerOn.value = speakerOn
            }
        }
    }

    fun unbindCallService() {
        callService = null
    }
}
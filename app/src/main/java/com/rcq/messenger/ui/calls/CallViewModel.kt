package com.rcq.messenger.ui.calls

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rcq.messenger.call.CallManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CallViewModel @Inject constructor(
    private val callManager: CallManager
) : ViewModel() {

    private val _callState = MutableStateFlow(CallState.IDLE)
    val callState: StateFlow<CallState> = _callState.asStateFlow()

    private val _isMuted = MutableStateFlow(false)
    val isMuted: StateFlow<Boolean> = _isMuted.asStateFlow()

    private val _isSpeakerOn = MutableStateFlow(false)
    val isSpeakerOn: StateFlow<Boolean> = _isSpeakerOn.asStateFlow()

    fun startCall(chatId: String, targetUin: Long) {
        viewModelScope.launch {
            _callState.value = CallState.CONNECTING
            try {
                callManager.startCall(targetUin, isVideoCall = false)
                _callState.value = CallState.CONNECTED
            } catch (e: Exception) {
                _callState.value = CallState.FAILED
            }
        }
    }

    fun endCall() {
        viewModelScope.launch {
            callManager.endCall()
            _callState.value = CallState.IDLE
        }
    }

    fun toggleMute() {
        _isMuted.value = !_isMuted.value
        callManager.setMuted(_isMuted.value)
    }

    fun toggleSpeaker() {
        _isSpeakerOn.value = !_isSpeakerOn.value
        callManager.setSpeakerEnabled(_isSpeakerOn.value)
    }
}

enum class CallState {
    IDLE,
    CONNECTING,
    CONNECTED,
    FAILED
}
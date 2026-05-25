package com.rcq.messenger.service

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.webrtc.*
import javax.inject.Inject

@AndroidEntryPoint
class CallService : Service() {

    private val binder = LocalBinder()
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var peerConnectionFactory: PeerConnectionFactory? = null
    private var peerConnection: PeerConnection? = null
    private var localVideoTrack: VideoTrack? = null
    private var localAudioTrack: AudioTrack? = null
    private var remoteVideoTrack: VideoTrack? = null

    private val _callState = MutableStateFlow(CallState.IDLE)
    val callState: StateFlow<CallState> = _callState.asStateFlow()

    inner class LocalBinder : Binder() {
        fun getService(): CallService = this@CallService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        initializeWebRTC()
    }

    override fun onDestroy() {
        super.onDestroy()
        cleanup()
        serviceScope.cancel()
    }

    private fun initializeWebRTC() {
        val options = PeerConnectionFactory.InitializationOptions.builder(this)
            .setEnableInternalTracer(true)
            .createInitializationOptions()
        PeerConnectionFactory.initialize(options)

        val encoderFactory = DefaultVideoEncoderFactory(
            EglBase.create().eglBaseContext,
            true,
            true
        )
        val decoderFactory = DefaultVideoDecoderFactory(EglBase.create().eglBaseContext)

        peerConnectionFactory = PeerConnectionFactory.builder()
            .setVideoEncoderFactory(encoderFactory)
            .setVideoDecoderFactory(decoderFactory)
            .createPeerConnectionFactory()
    }

    fun startCall(isVideoCall: Boolean) {
        _callState.value = CallState.CONNECTING

        // Create local tracks
        val audioConstraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("googEchoCancellation", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("googNoiseSuppression", "true"))
        }

        val audioSource = peerConnectionFactory?.createAudioSource(audioConstraints)
        localAudioTrack = peerConnectionFactory?.createAudioTrack("audio_track", audioSource)

        if (isVideoCall) {
            val surfaceTextureHelper = SurfaceTextureHelper.create("CaptureThread", EglBase.create().eglBaseContext)
            val videoSource = peerConnectionFactory?.createVideoSource(false)
            localVideoTrack = peerConnectionFactory?.createVideoTrack("video_track", videoSource)
        }

        // Create peer connection
        val rtcConfig = PeerConnection.RTCConfiguration(emptyList()).apply {
            sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
        }

        peerConnection = peerConnectionFactory?.createPeerConnection(
            rtcConfig,
            object : PeerConnection.Observer {
                override fun onSignalingChange(state: PeerConnection.SignalingState?) {}
                override fun onIceConnectionChange(state: PeerConnection.IceConnectionState?) {
                    when (state) {
                        PeerConnection.IceConnectionState.CONNECTED -> _callState.value = CallState.CONNECTED
                        PeerConnection.IceConnectionState.DISCONNECTED -> _callState.value = CallState.DISCONNECTED
                        PeerConnection.IceConnectionState.FAILED -> _callState.value = CallState.ERROR
                        else -> {}
                    }
                }
                override fun onIceConnectionReceivingChange(receiving: Boolean) {}
                override fun onIceGatheringChange(state: PeerConnection.IceGatheringState?) {}
                override fun onIceCandidate(candidate: IceCandidate?) {}
                override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>?) {}
                override fun onAddStream(stream: MediaStream?) {
                    remoteVideoTrack = stream?.videoTracks?.firstOrNull()
                }
                override fun onRemoveStream(stream: MediaStream?) {}
                override fun onDataChannel(channel: DataChannel?) {}
                override fun onRenegotiationNeeded() {}
                override fun onAddTrack(receiver: RtpReceiver?, streams: Array<out MediaStream>?) {
                    remoteVideoTrack = receiver?.track() as? VideoTrack
                }
            }
        )

        // Add tracks
        localAudioTrack?.let { peerConnection?.addTrack(it, listOf("stream")) }
        localVideoTrack?.let { peerConnection?.addTrack(it, listOf("stream")) }

        _callState.value = CallState.RINGING
    }

    fun endCall() {
        cleanup()
        _callState.value = CallState.ENDED
    }

    fun setMicrophoneEnabled(enabled: Boolean) {
        localAudioTrack?.setEnabled(enabled)
    }

    fun setSpeakerphoneOn(on: Boolean) {
        // Enable/disable speakerphone
    }

    private fun cleanup() {
        peerConnection?.close()
        peerConnection = null
        localAudioTrack?.setEnabled(false)
        localAudioTrack = null
        localVideoTrack?.setEnabled(false)
        localVideoTrack = null
        remoteVideoTrack = null
    }
}

enum class CallState {
    IDLE,
    CONNECTING,
    RINGING,
    CONNECTED,
    DISCONNECTED,
    ENDED,
    ERROR
}
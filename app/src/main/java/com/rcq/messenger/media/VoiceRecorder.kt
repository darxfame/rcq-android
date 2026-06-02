package com.rcq.messenger.media

import android.content.Context
import android.media.MediaRecorder
import android.media.MediaPlayer
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VoiceRecorder @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var mediaRecorder: MediaRecorder? = null
    private var mediaPlayer: MediaPlayer? = null
    private var currentRecordingFile: File? = null

    private val _recordingState = MutableStateFlow(RecordingState.IDLE)
    val recordingState: StateFlow<RecordingState> = _recordingState.asStateFlow()

    private val _playbackState = MutableStateFlow(PlaybackState.IDLE)
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    private val _recordingDuration = MutableStateFlow(0L)
    val recordingDuration: StateFlow<Long> = _recordingDuration.asStateFlow()

    private val voiceDir = File(context.filesDir, "voice")

    init {
        if (!voiceDir.exists()) {
            voiceDir.mkdirs()
        }
    }

    fun startRecording(): Result<File> {
        return try {
            if (_recordingState.value != RecordingState.IDLE) {
                return Result.failure(Exception("Already recording"))
            }

            val outputFile = File(voiceDir, "voice_${System.currentTimeMillis()}.m4a")
            currentRecordingFile = outputFile

            mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(128000)
                setAudioSamplingRate(44100)
                setOutputFile(outputFile.absolutePath)

                prepare()
                start()
            }

            _recordingState.value = RecordingState.RECORDING
            Result.success(outputFile)
        } catch (e: IOException) {
            _recordingState.value = RecordingState.ERROR
            Result.failure(e)
        }
    }

    fun stopRecording(): Result<File> {
        return try {
            if (_recordingState.value != RecordingState.RECORDING) {
                return Result.failure(Exception("Not recording"))
            }

            mediaRecorder?.apply {
                stop()
                release()
            }
            mediaRecorder = null

            _recordingState.value = RecordingState.IDLE

            currentRecordingFile?.let { file ->
                if (file.exists() && file.length() > 0) {
                    Result.success(file)
                } else {
                    Result.failure(Exception("Recording file is empty"))
                }
            } ?: Result.failure(Exception("No recording file"))
        } catch (e: Exception) {
            _recordingState.value = RecordingState.ERROR
            Result.failure(e)
        }
    }

    fun cancelRecording() {
        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
            mediaRecorder = null

            currentRecordingFile?.delete()
            currentRecordingFile = null

            _recordingState.value = RecordingState.IDLE
        } catch (e: Exception) {
            _recordingState.value = RecordingState.ERROR
        }
    }

    fun playVoiceMessage(file: File): Result<Unit> {
        return try {
            if (_playbackState.value == PlaybackState.PLAYING) {
                stopPlayback()
            }

            mediaPlayer = MediaPlayer().apply {
                setDataSource(file.absolutePath)
                setOnCompletionListener {
                    _playbackState.value = PlaybackState.IDLE
                }
                setOnErrorListener { _, _, _ ->
                    _playbackState.value = PlaybackState.ERROR
                    true
                }
                prepare()
                start()
            }

            _playbackState.value = PlaybackState.PLAYING
            Result.success(Unit)
        } catch (e: Exception) {
            _playbackState.value = PlaybackState.ERROR
            Result.failure(e)
        }
    }

    fun stopPlayback() {
        try {
            mediaPlayer?.apply {
                if (isPlaying) {
                    stop()
                }
                release()
            }
            mediaPlayer = null
            _playbackState.value = PlaybackState.IDLE
        } catch (e: Exception) {
            _playbackState.value = PlaybackState.ERROR
        }
    }

    fun pausePlayback() {
        try {
            mediaPlayer?.apply {
                if (isPlaying) {
                    pause()
                    _playbackState.value = PlaybackState.PAUSED
                }
            }
        } catch (e: Exception) {
            _playbackState.value = PlaybackState.ERROR
        }
    }

    fun resumePlayback() {
        try {
            mediaPlayer?.apply {
                if (!isPlaying && _playbackState.value == PlaybackState.PAUSED) {
                    start()
                    _playbackState.value = PlaybackState.PLAYING
                }
            }
        } catch (e: Exception) {
            _playbackState.value = PlaybackState.ERROR
        }
    }

    fun getVoiceDuration(file: File): Long {
        return try {
            val player = MediaPlayer().apply {
                setDataSource(file.absolutePath)
                prepare()
            }
            val duration = player.duration.toLong()
            player.release()
            duration
        } catch (e: Exception) {
            0L
        }
    }

    fun cleanup() {
        cancelRecording()
        stopPlayback()
    }
}

enum class RecordingState {
    IDLE,
    RECORDING,
    ERROR
}

enum class PlaybackState {
    IDLE,
    PLAYING,
    PAUSED,
    ERROR
}
package app.rcq.android.nearby

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.util.Log
import java.util.concurrent.ConcurrentHashMap
import kotlin.concurrent.thread

/**
 * Push-to-talk voice for Radio — Android port of the iOS `RadioVoiceEngine`.
 * Capture: a single [AudioRecord] (VOICE_COMMUNICATION source → hardware AEC/NS
 * where available) at 16 kHz mono PCM16, chunked into ~40 ms frames handed to
 * the caller for sealing + broadcast. Playback: one [AudioTrack] per remote
 * speaker (the system mixes them), fed as frames arrive.
 *
 * v1 sends RAW PCM — over a dedicated Wi-Fi Direct LAN 256 kbps is nothing, and
 * it sidesteps the MediaCodec AAC dance. (iOS uses AAC-LC, but Radio never
 * crosses platforms so the wire is ours.) Output is forced to the loudspeaker
 * (walkie-talkie feel), restored on [stop].
 *
 * ⚠ COMPILE-VERIFIED ONLY — real capture/playback needs a physical device.
 */
class RadioVoiceEngine(private val appContext: Context) {

    private val audioManager by lazy {
        appContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }

    @Volatile private var record: AudioRecord? = null
    @Volatile private var captureThread: Thread? = null
    @Volatile private var capturing = false
    private var seq = 0L

    /** speaker display name -> their playback track. */
    private val tracks = ConcurrentHashMap<String, AudioTrack>()

    // Saved audio state to restore on stop.
    private var savedMode = AudioManager.MODE_NORMAL
    private var savedSpeaker = false
    @Volatile private var audioPrepared = false

    fun hasMicPermission(): Boolean =
        appContext.checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

    // ── capture (PTT) ─────────────────────────────────────────────────
    @SuppressLint("MissingPermission")
    fun startCapture(onFrame: (seq: Long, pcm: ByteArray) -> Unit): Boolean {
        if (capturing) return true
        if (!hasMicPermission()) return false
        val minBuf = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_IN, ENCODING)
        if (minBuf <= 0) return false
        val rec = runCatching {
            AudioRecord(
                MediaRecorder.AudioSource.VOICE_COMMUNICATION,
                SAMPLE_RATE, CHANNEL_IN, ENCODING,
                maxOf(minBuf, FRAME_BYTES * 4),
            )
        }.getOrNull() ?: return false
        if (rec.state != AudioRecord.STATE_INITIALIZED) { rec.release(); return false }

        prepareAudio()
        record = rec
        capturing = true
        rec.startRecording()
        captureThread = thread(name = "radio-ptt-capture", isDaemon = true) {
            val buf = ByteArray(FRAME_BYTES)
            while (capturing) {
                var off = 0
                while (off < FRAME_BYTES && capturing) {
                    val n = rec.read(buf, off, FRAME_BYTES - off)
                    if (n <= 0) break
                    off += n
                }
                if (off == FRAME_BYTES) onFrame(seq++, buf.copyOf())
            }
        }
        return true
    }

    fun stopCapture() {
        capturing = false
        captureThread = null
        record?.let { rec ->
            runCatching { if (rec.recordingState == AudioRecord.RECORDSTATE_RECORDING) rec.stop() }
            runCatching { rec.release() }
        }
        record = null
        if (tracks.isEmpty()) releaseAudio()
    }

    // ── playback ──────────────────────────────────────────────────────
    fun feedFrame(speaker: String, pcm: ByteArray) {
        val track = tracks.getOrPut(speaker) { makeTrack() } ?: return
        runCatching { track.write(pcm, 0, pcm.size) }
            .onFailure { Log.w(TAG, "track write failed for $speaker") }
    }

    fun dropSpeaker(speaker: String) {
        tracks.remove(speaker)?.let { t ->
            runCatching { t.pause(); t.flush(); t.stop() }
            runCatching { t.release() }
        }
        if (tracks.isEmpty() && !capturing) releaseAudio()
    }

    fun stop() {
        stopCapture()
        tracks.keys.toList().forEach { dropSpeaker(it) }
        releaseAudio()
    }

    private fun makeTrack(): AudioTrack? {
        prepareAudio()
        val minOut = AudioTrack.getMinBufferSize(SAMPLE_RATE, CHANNEL_OUT, ENCODING)
        if (minOut <= 0) return null
        return runCatching {
            AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build(),
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(ENCODING)
                        .setSampleRate(SAMPLE_RATE)
                        .setChannelMask(CHANNEL_OUT)
                        .build(),
                )
                .setBufferSizeInBytes(maxOf(minOut, FRAME_BYTES * 4))
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()
                .also { it.play() }
        }.getOrNull()
    }

    // ── audio routing (loudspeaker, walkie-talkie style) ───────────────
    @Suppress("DEPRECATION")
    private fun prepareAudio() {
        if (audioPrepared) return
        savedMode = audioManager.mode
        savedSpeaker = audioManager.isSpeakerphoneOn
        runCatching {
            audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
            audioManager.isSpeakerphoneOn = true
        }
        audioPrepared = true
    }

    @Suppress("DEPRECATION")
    private fun releaseAudio() {
        if (!audioPrepared) return
        runCatching {
            audioManager.isSpeakerphoneOn = savedSpeaker
            audioManager.mode = savedMode
        }
        audioPrepared = false
    }

    companion object {
        private const val TAG = "RadioVoice"
        private const val SAMPLE_RATE = 16_000
        private const val CHANNEL_IN = AudioFormat.CHANNEL_IN_MONO
        private const val CHANNEL_OUT = AudioFormat.CHANNEL_OUT_MONO
        private const val ENCODING = AudioFormat.ENCODING_PCM_16BIT
        private const val FRAME_SAMPLES = 640          // 40 ms @ 16 kHz
        private const val FRAME_BYTES = FRAME_SAMPLES * 2
    }
}

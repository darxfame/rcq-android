package app.rcq.android.media

import android.media.MediaRecorder
import java.io.File

/**
 * Records a short voice note to an AAC track in an MPEG-4 (.m4a) container —
 * playable by both Android's MediaPlayer and iOS's AVAudioPlayer, so a clip
 * recorded here decodes on the iOS client and vice versa. Tap-to-start /
 * tap-to-stop (no hold gesture, which is fiddly to drive + test).
 */
class VoiceRecorder(private val cacheDir: File) {
    private var recorder: MediaRecorder? = null
    private var outFile: File? = null
    private var startedAt: Long = 0L

    val isRecording: Boolean get() = recorder != null

    /** Begin capturing. Throws if the mic is unavailable (caller catches). */
    fun start() {
        stopQuietly()
        val f = File(cacheDir, "voice-out.m4a")
        @Suppress("DEPRECATION")
        val r = MediaRecorder()
        r.setAudioSource(MediaRecorder.AudioSource.MIC)
        r.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
        r.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
        r.setAudioSamplingRate(44_100)
        r.setAudioEncodingBitRate(64_000)
        r.setOutputFile(f.absolutePath)
        r.prepare()
        r.start()
        recorder = r
        outFile = f
        startedAt = System.currentTimeMillis()
    }

    /** Stop and return (bytes, durationSec ≥ 1), or null if nothing usable. */
    fun stop(): Pair<ByteArray, Int>? {
        val r = recorder ?: return null
        recorder = null
        val dur = ((System.currentTimeMillis() - startedAt) / 1000L).toInt().coerceAtLeast(1)
        return runCatching {
            r.stop()
            r.release()
            val bytes = outFile?.readBytes() ?: return null
            bytes to dur
        }.getOrElse {
            runCatching { r.release() }
            null
        }
    }

    /** Abort without producing a message. */
    fun cancel() {
        stopQuietly()
        outFile?.let { runCatching { it.delete() } }
    }

    private fun stopQuietly() {
        recorder?.let {
            runCatching { it.stop() }
            runCatching { it.release() }
        }
        recorder = null
    }
}

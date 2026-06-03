package app.rcq.android.call

import android.content.Context
import android.media.AudioManager
import android.media.Ringtone
import android.media.RingtoneManager
import android.media.ToneGenerator
import android.os.VibrationEffect
import android.os.Vibrator

/**
 * Call ringing audio. Incoming → the device ringtone on a loop + vibration;
 * outgoing → a ringback tone. Android has no CallKit, so we drive both
 * ourselves. Idempotent [stop].
 */
class Ringer(private val context: Context) {
    private var ringtone: Ringtone? = null
    private var ringback: ToneGenerator? = null
    private val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator

    fun startIncoming() {
        stop()
        runCatching {
            val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            ringtone = RingtoneManager.getRingtone(context, uri)?.apply {
                isLooping = true
                play()
            }
        }
        runCatching {
            vibrator?.vibrate(
                VibrationEffect.createWaveform(longArrayOf(0, 800, 1000), 0),
            )
        }
    }

    fun startRingback() {
        stop()
        runCatching {
            ringback = ToneGenerator(AudioManager.STREAM_VOICE_CALL, 70).apply {
                startTone(ToneGenerator.TONE_SUP_RINGTONE)
            }
        }
    }

    fun stop() {
        runCatching { ringtone?.stop() }
        ringtone = null
        runCatching { ringback?.stopTone(); ringback?.release() }
        ringback = null
        runCatching { vibrator?.cancel() }
    }
}

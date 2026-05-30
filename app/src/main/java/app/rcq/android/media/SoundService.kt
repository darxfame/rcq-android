package app.rcq.android.media

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import app.rcq.android.R
import app.rcq.android.data.LocalStores

/**
 * Plays short notification tones for message + presence events, gated by
 * the [LocalStores] sound toggles. The Android analogue of the iOS
 * SoundService. Bundled tones live in res/raw (snd_message / snd_online /
 * snd_offline). Loading is async; [SoundPool.play] on a not-yet-loaded id
 * is a silent no-op, so no readiness gate is needed.
 *
 * Call [init] once from MainActivity.onCreate (after LocalStores.init).
 */
object SoundService {
    private var pool: SoundPool? = null
    private var msg = 0
    private var online = 0
    private var offline = 0

    fun init(context: Context) {
        if (pool != null) return
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        val p = SoundPool.Builder().setMaxStreams(4).setAudioAttributes(attrs).build()
        val app = context.applicationContext
        msg = p.load(app, R.raw.snd_message, 1)
        online = p.load(app, R.raw.snd_online, 1)
        offline = p.load(app, R.raw.snd_offline, 1)
        pool = p
    }

    private fun play(id: Int) {
        pool?.play(id, 0.7f, 0.7f, 1, 0, 1f)
    }

    /** Inbound message to a non-active, non-muted thread. */
    fun message() { if (LocalStores.soundMessagesOn()) play(msg) }

    /** A contact transitioned to online. */
    fun contactOnline() { if (LocalStores.soundPresenceOn()) play(online) }

    /** A contact transitioned to offline. */
    fun contactOffline() { if (LocalStores.soundPresenceOn()) play(offline) }
}

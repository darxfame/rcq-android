package com.rcq.messenger.service

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import com.rcq.messenger.R

enum class RcqSound {
    MESSAGE_INCOMING,
    MESSAGE_SENT,
    CONTACT_ONLINE,
    CONTACT_OFFLINE,
    NUDGE,
    APP_STARTUP,
    SUCCESS,
    FAIL,
    CELEBRATORY,
    JOIN_ALL,
    JOIN_ME,
}

@Singleton
class SoundManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val attrs = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_NOTIFICATION)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()

    private val pool = SoundPool.Builder()
        .setMaxStreams(4)
        .setAudioAttributes(attrs)
        .build()

    private val sounds = mutableMapOf<RcqSound, Int>()
    private var ringPlayer: MediaPlayer? = null
    private var enabled = true

    init { preload() }

    private fun preload() {
        sounds[RcqSound.MESSAGE_INCOMING] = pool.load(context, R.raw.message_incoming, 1)
        sounds[RcqSound.MESSAGE_SENT]     = pool.load(context, R.raw.message_sent, 1)
        sounds[RcqSound.CONTACT_ONLINE]   = pool.load(context, R.raw.contact_online, 1)
        sounds[RcqSound.CONTACT_OFFLINE]  = pool.load(context, R.raw.contact_offline, 1)
        sounds[RcqSound.NUDGE]            = pool.load(context, R.raw.nudge, 1)
        sounds[RcqSound.APP_STARTUP]      = pool.load(context, R.raw.app_startup, 1)
        sounds[RcqSound.SUCCESS]          = pool.load(context, R.raw.success, 1)
        sounds[RcqSound.FAIL]             = pool.load(context, R.raw.fail_tone, 1)
        sounds[RcqSound.CELEBRATORY]      = pool.load(context, R.raw.celebratory_chime, 1)
        sounds[RcqSound.JOIN_ALL]         = pool.load(context, R.raw.join_all, 1)
        sounds[RcqSound.JOIN_ME]          = pool.load(context, R.raw.join_me, 1)
    }

    fun play(sound: RcqSound) {
        if (!enabled) return
        val id = sounds[sound] ?: return
        pool.play(id, 1f, 1f, 1, 0, 1f)
    }

    fun startRinging() {
        if (!enabled) return
        stopRinging()
        ringPlayer = MediaPlayer.create(context, R.raw.rolling)?.apply {
            isLooping = true
            start()
        }
    }

    fun stopRinging() {
        ringPlayer?.stop()
        ringPlayer?.release()
        ringPlayer = null
    }

    fun setEnabled(on: Boolean) { enabled = on }
}

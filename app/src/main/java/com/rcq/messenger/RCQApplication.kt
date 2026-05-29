package com.rcq.messenger

import android.app.Application
import com.rcq.messenger.data.repository.OutboxProcessor
import com.rcq.messenger.service.NotificationHelper
import timber.log.Timber
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class RCQApplication : Application() {

    @Inject lateinit var notificationHelper: NotificationHelper
    // Inject eagerly so WebSocket event handler in init{} runs at startup,
    // not lazily when first screen is shown
    @Inject lateinit var chatRepository: com.rcq.messenger.data.repository.ChatRepository
    @Inject lateinit var outboxProcessor: OutboxProcessor

    override fun onCreate() {
        super.onCreate()
        instance = this
        if (com.rcq.messenger.BuildConfig.DEBUG) Timber.plant(Timber.DebugTree())
        notificationHelper.createNotificationChannels()
        // chatRepository injection forces its init{} block to run,
        // wiring up the WebSocket event listener before any screen loads
    }

    companion object {
        lateinit var instance: RCQApplication
            private set
    }
}
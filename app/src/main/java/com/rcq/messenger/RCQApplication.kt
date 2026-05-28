package com.rcq.messenger

import android.app.Application
import com.rcq.messenger.service.NotificationHelper
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class RCQApplication : Application() {

    @Inject lateinit var notificationHelper: NotificationHelper

    override fun onCreate() {
        super.onCreate()
        instance = this
        notificationHelper.createNotificationChannels()
    }

    companion object {
        lateinit var instance: RCQApplication
            private set
    }
}
package com.rcq.messenger.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

object PreferencesKeys {
    val AUTH_TOKEN = stringPreferencesKey("token")
    val USER_UIN = longPreferencesKey("uin")
    val SERVER_TOKEN = stringPreferencesKey("server_token")
    val RETRO_MODE = booleanPreferencesKey("pref_retro_mode_enabled")
    val DARK_THEME = booleanPreferencesKey("pref_dark_theme")
    val AMOLED_THEME = booleanPreferencesKey("pref_amoled_theme")
    val HIGH_CONTRAST = booleanPreferencesKey("pref_high_contrast")
    val COMPACT_MODE = booleanPreferencesKey("pref_compact_mode")
    val NOTIFICATIONS_ENABLED = booleanPreferencesKey("pref_notifications_enabled")
    val GROUP_NOTIFICATIONS = booleanPreferencesKey("group_notifications")
    val CONTACT_ONLINE_NOTIFICATIONS = booleanPreferencesKey("contact_online_notifications")
    val MESSAGE_PREVIEW = booleanPreferencesKey("pref_message_preview")
    val SOUND_ENABLED = booleanPreferencesKey("pref_sound_enabled")
    val VIBRATION_ENABLED = booleanPreferencesKey("pref_vibration_enabled")
    val NOTIFICATION_LED = booleanPreferencesKey("notification_led")
    val READ_RECEIPTS = booleanPreferencesKey("pref_read_receipts")
    val TYPING_INDICATOR = booleanPreferencesKey("typing_indicator")
    val LAST_SEEN_VISIBLE = booleanPreferencesKey("pref_last_seen_visible")
    val ONLINE_VISIBLE = booleanPreferencesKey("pref_online_visible")
    val LAST_SEEN_VISIBILITY = stringPreferencesKey("last_seen_visibility")
    val ONLINE_VISIBILITY = stringPreferencesKey("online_visibility")
    val PROFILE_PHOTO_VISIBILITY = stringPreferencesKey("profile_photo_visibility")
    val GROUP_INVITE_POLICY = stringPreferencesKey("group_invite_policy")
    val GENDER_VISIBILITY = stringPreferencesKey("gender_visibility")
}

@Singleton
class AuthInterceptor @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val token = runBlocking {
            try {
                dataStore.data.first()[PreferencesKeys.AUTH_TOKEN]
            } catch (e: Exception) {
                null
            }
        }
        val serverToken = runBlocking {
            try {
                dataStore.data.first()[PreferencesKeys.SERVER_TOKEN]
            } catch (e: Exception) {
                null
            }
        }

        val request = chain.request().newBuilder().apply {
            addHeader("Content-Type", "application/json")
            addHeader("Accept", "application/json")
            token?.let {
                addHeader("Authorization", "Bearer $it")
            }
            serverToken?.let {
                addHeader("X-RCQ-Auth", it)
            }
        }.build()

        return chain.proceed(request)
    }
}

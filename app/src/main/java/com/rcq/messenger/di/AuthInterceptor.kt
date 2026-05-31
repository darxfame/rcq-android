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
    val RETRO_MODE = booleanPreferencesKey("pref_retro_mode_enabled")
    val DARK_THEME = booleanPreferencesKey("pref_dark_theme")
    val AMOLED_THEME = booleanPreferencesKey("pref_amoled_theme")
    val HIGH_CONTRAST = booleanPreferencesKey("pref_high_contrast")
    val COMPACT_MODE = booleanPreferencesKey("pref_compact_mode")
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

        val request = chain.request().newBuilder().apply {
            addHeader("Content-Type", "application/json")
            addHeader("Accept", "application/json")
            token?.let {
                addHeader("Authorization", "Bearer $it")
            }
        }.build()

        return chain.proceed(request)
    }
}


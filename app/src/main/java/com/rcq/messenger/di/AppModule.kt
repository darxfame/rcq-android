package com.rcq.messenger.di

import android.content.Context
import com.rcq.messenger.BuildConfig
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room
import com.rcq.messenger.data.api.RCQApiService
import com.rcq.messenger.data.db.*
import com.rcq.messenger.data.ws.WebSocketManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "rcq_prefs")

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        encodeDefaults = true
    }

    @Provides
    @Singleton
    fun provideAuthInterceptor(dataStore: DataStore<Preferences>): AuthInterceptor {
        return AuthInterceptor(dataStore)
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(authInterceptor: AuthInterceptor): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .addInterceptor(authInterceptor)
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(client: OkHttpClient, json: Json): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
    }

    @Provides
    @Singleton
    fun provideApiService(retrofit: Retrofit): RCQApiService {
        return retrofit.create(RCQApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): RCQDatabase {
        return Room.databaseBuilder(
            context,
            RCQDatabase::class.java,
            "rcq_database"
        ).fallbackToDestructiveMigration().build()
    }

    @Provides
    fun provideUserDao(database: RCQDatabase): UserDao = database.userDao()

    @Provides
    fun provideContactDao(database: RCQDatabase): ContactDao = database.contactDao()

    @Provides
    fun provideChatDao(database: RCQDatabase): ChatDao = database.chatDao()

    @Provides
    fun provideMessageDao(database: RCQDatabase): MessageDao = database.messageDao()

    @Provides
    fun provideGroupDao(database: RCQDatabase): GroupDao = database.groupDao()

    @Provides
    fun provideStoryDao(database: RCQDatabase): StoryDao = database.storyDao()

    @Provides
    fun provideCallDao(database: RCQDatabase): CallDao = database.callDao()

    @Provides
    fun providePetDao(database: RCQDatabase): PetDao = database.petDao()

    @Provides
    @Singleton
    fun provideWebSocketManager(okHttpClient: OkHttpClient, json: Json): WebSocketManager {
        return WebSocketManager(okHttpClient, json)
    }

    @Provides
    @Singleton
    fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> {
        return context.dataStore
    }
}
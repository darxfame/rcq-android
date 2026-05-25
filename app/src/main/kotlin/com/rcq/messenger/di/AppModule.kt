package com.rcq.messenger.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room
import com.rcq.messenger.BuildConfig
import com.rcq.messenger.data.api.RCQApiService
import com.rcq.messenger.data.db.RCQDatabase
import com.rcq.messenger.data.interceptor.AuthInterceptor
import com.rcq.messenger.data.ws.WebSocketManager
import com.squareup.okhttp3.OkHttpClient
import com.squareup.okhttp3.logging.HttpLoggingInterceptor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import okhttp3.MediaType.Companion.toMediaType
import javax.inject.Singleton

private const val PREFERENCES_NAME = "rcq_preferences"
private const val DATABASE_NAME = "rcq_database"

val Context.preferencesDataStore: DataStore<Preferences> by preferencesDataStore(
    name = PREFERENCES_NAME
)

/**
 * Dependency Injection Module — provides all singleton dependencies.
 * 
 * CRITICAL FIXES (0.1, 0.3):
 * - Provides WebSocketManager for real-time communication
 * - Provides RCQApiService with correct endpoint types
 * - Provides AuthInterceptor for token management
 * - Configures OkHttpClient with logging and auth
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    
    // ==================== PREFERENCES ====================
    
    @Singleton
    @Provides
    fun providePreferencesDataStore(
        @ApplicationContext context: Context
    ): DataStore<Preferences> = context.preferencesDataStore
    
    // ==================== DATABASE ====================
    
    @Singleton
    @Provides
    fun provideRCQDatabase(
        @ApplicationContext context: Context
    ): RCQDatabase = Room.databaseBuilder(
        context,
        RCQDatabase::class.java,
        DATABASE_NAME
    )
        .fallbackToDestructiveMigration()
        .build()
    
    // ==================== HTTP CLIENT ====================
    
    @Singleton
    @Provides
    fun provideAuthInterceptor(
        preferencesDataStore: DataStore<Preferences>
    ): AuthInterceptor = AuthInterceptor(preferencesDataStore)
    
    @Singleton
    @Provides
    fun provideHttpLoggingInterceptor(): HttpLoggingInterceptor {
        val interceptor = HttpLoggingInterceptor()
        interceptor.level = if (BuildConfig.DEBUG) {
            HttpLoggingInterceptor.Level.BODY
        } else {
            HttpLoggingInterceptor.Level.NONE
        }
        return interceptor
    }
    
    @Singleton
    @Provides
    fun provideOkHttpClient(
        authInterceptor: AuthInterceptor,
        loggingInterceptor: HttpLoggingInterceptor
    ): OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .addInterceptor(loggingInterceptor)
        .connectTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .build()
    
    // ==================== API SERVICE ====================
    
    @Singleton
    @Provides
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }
    
    @Singleton
    @Provides
    fun provideRCQApiService(
        okHttpClient: OkHttpClient,
        json: Json
    ): RCQApiService = Retrofit.Builder()
        .baseUrl(BuildConfig.API_BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
        .create(RCQApiService::class.java)
    
    // ==================== WEBSOCKET ====================
    
    @Singleton
    @Provides
    fun provideWebSocketManager(
        okHttpClient: OkHttpClient
    ): WebSocketManager = WebSocketManager(okHttpClient)
}

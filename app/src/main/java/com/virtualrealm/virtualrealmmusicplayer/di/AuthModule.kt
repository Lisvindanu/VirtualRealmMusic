// app/src/main/java/com/virtualrealm/virtualrealmmusicplayer/di/AuthModule.kt

package com.virtualrealm.virtualrealmmusicplayer.di

import android.content.Context
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.virtualrealm.virtualrealmmusicplayer.data.auth.SpotifyAuthHandler
import com.virtualrealm.virtualrealmmusicplayer.domain.model.Music
import com.virtualrealm.virtualrealmmusicplayer.util.RuntimeTypeAdapterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AuthModule {

    @Provides
    @Singleton
    fun provideGson(): Gson {
        // Daftarkan adapter untuk sealed class Music
        val musicAdapterFactory = RuntimeTypeAdapterFactory
            .of(Music::class.java, "music_type") // Gunakan nama field yang unik
            .registerSubtype(Music.SpotifyTrack::class.java, "spotify")
            .registerSubtype(Music.YoutubeVideo::class.java, "youtube")

        return GsonBuilder()
            .registerTypeAdapterFactory(musicAdapterFactory)
            .create()
    }

    @Provides
    @Singleton
    fun provideAuthOkHttpClient(): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideSpotifyAuthHandler(
        @ApplicationContext context: Context,
        okHttpClient: OkHttpClient,
        gson: Gson
    ): SpotifyAuthHandler {
        return SpotifyAuthHandler(context, okHttpClient, gson)
    }
}
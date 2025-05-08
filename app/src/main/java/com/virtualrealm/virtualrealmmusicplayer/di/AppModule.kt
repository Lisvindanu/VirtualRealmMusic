// app/src/main/java/com/virtualrealm/virtualrealmmusicplayer/di/AppModule.kt

package com.virtualrealm.virtualrealmmusicplayer.di

import android.app.Application
import android.content.Context
import com.virtualrealm.virtualrealmmusicplayer.service.YoutubeAudioHelper
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideApplicationContext(application: Application): Context {
        return application.applicationContext
    }

    @Provides
    @Singleton
    fun provideYoutubeAudioHelper(): YoutubeAudioHelper {
        return YoutubeAudioHelper()
    }
}
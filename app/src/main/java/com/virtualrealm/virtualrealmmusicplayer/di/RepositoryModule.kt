// di/RepositoryModule.kt
package com.virtualrealm.virtualrealmmusicplayer.di

import com.virtualrealm.virtualrealmmusicplayer.data.repository.AuthRepositoryImpl
import com.virtualrealm.virtualrealmmusicplayer.data.repository.MusicRepositoryImpl
import com.virtualrealm.virtualrealmmusicplayer.domain.repository.AuthRepository
import com.virtualrealm.virtualrealmmusicplayer.domain.repository.MusicRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindMusicRepository(
        musicRepositoryImpl: MusicRepositoryImpl
    ): MusicRepository

    @Binds
    @Singleton
    abstract fun bindAuthRepository(
        authRepositoryImpl: AuthRepositoryImpl
    ): AuthRepository
}
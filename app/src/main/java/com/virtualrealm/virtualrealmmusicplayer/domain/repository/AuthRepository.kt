// domain/repository/AuthRepository.kt
package com.virtualrealm.virtualrealmmusicplayer.domain.repository

import android.content.Intent
import com.virtualrealm.virtualrealmmusicplayer.domain.model.AuthState
import com.virtualrealm.virtualrealmmusicplayer.domain.model.Resource
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    suspend fun getAuthState(): Flow<AuthState>
    suspend fun saveAuthState(authState: AuthState)
    suspend fun clearAuthState()
    suspend fun refreshSpotifyToken(): Flow<Resource<AuthState>>
    suspend fun exchangeSpotifyCode(code: String): Flow<Resource<AuthState>>
    suspend fun exchangeSpotifyCode(intent: Intent): Flow<Resource<AuthState>>  // New method
    fun getSpotifyAuthUrl(): String
    fun startSpotifyAuthFlow()  // New method
}
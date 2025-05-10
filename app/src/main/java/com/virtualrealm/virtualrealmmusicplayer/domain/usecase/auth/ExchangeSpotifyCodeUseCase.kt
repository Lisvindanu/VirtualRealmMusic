package com.virtualrealm.virtualrealmmusicplayer.domain.usecase.auth

import android.content.Intent
import com.virtualrealm.virtualrealmmusicplayer.domain.model.AuthState
import com.virtualrealm.virtualrealmmusicplayer.domain.model.Resource
import com.virtualrealm.virtualrealmmusicplayer.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ExchangeSpotifyCodeUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    // Keep original method for backward compatibility
    suspend operator fun invoke(code: String): Flow<Resource<AuthState>> {
        return authRepository.exchangeSpotifyCode(code)
    }

    // Add new method for handling intents
    suspend operator fun invoke(intent: Intent): Flow<Resource<AuthState>> {
        return authRepository.exchangeSpotifyCode(intent.toString())
    }
}

// domain/usecase/auth/ExchangeSpotifyCodeUseCase.kt
package com.virtualrealm.virtualrealmmusicplayer.domain.usecase.auth

import com.virtualrealm.virtualrealmmusicplayer.domain.model.AuthState
import com.virtualrealm.virtualrealmmusicplayer.domain.model.Resource
import com.virtualrealm.virtualrealmmusicplayer.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ExchangeSpotifyCodeUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(code: String): Flow<Resource<AuthState>> {
        return authRepository.exchangeSpotifyCode(code)
    }
}

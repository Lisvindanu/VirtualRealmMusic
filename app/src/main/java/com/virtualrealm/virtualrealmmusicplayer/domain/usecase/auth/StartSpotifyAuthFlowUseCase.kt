package com.virtualrealm.virtualrealmmusicplayer.domain.usecase.auth

import com.virtualrealm.virtualrealmmusicplayer.domain.repository.AuthRepository
import javax.inject.Inject

class StartSpotifyAuthFlowUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    operator fun invoke() {
        authRepository.startSpotifyAuthFlow()
    }
}

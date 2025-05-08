// domain/usecase/auth/LogoutUseCase.kt
package com.virtualrealm.virtualrealmmusicplayer.domain.usecase.auth

import com.virtualrealm.virtualrealmmusicplayer.domain.repository.AuthRepository
import javax.inject.Inject

class LogoutUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke() {
        authRepository.clearAuthState()
    }
}

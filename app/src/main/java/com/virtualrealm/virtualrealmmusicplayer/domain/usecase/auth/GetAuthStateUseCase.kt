// app/src/main/java/com/virtualrealm/virtualrealmmusicplayer/domain/usecase/auth/GetAuthStateUseCase.kt
package com.virtualrealm.virtualrealmmusicplayer.domain.usecase.auth

import com.virtualrealm.virtualrealmmusicplayer.domain.model.AuthState
import com.virtualrealm.virtualrealmmusicplayer.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetAuthStateUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    // Hapus keyword suspend
    suspend operator fun invoke(): Flow<AuthState> {
        return authRepository.getAuthState()
    }
}
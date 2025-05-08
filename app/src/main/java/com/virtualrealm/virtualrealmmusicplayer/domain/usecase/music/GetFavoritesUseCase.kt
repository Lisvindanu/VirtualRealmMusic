// domain/usecase/music/GetFavoritesUseCase.kt
package com.virtualrealm.virtualrealmmusicplayer.domain.usecase.music

import com.virtualrealm.virtualrealmmusicplayer.domain.model.Music
import com.virtualrealm.virtualrealmmusicplayer.domain.repository.MusicRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetFavoritesUseCase @Inject constructor(
    private val musicRepository: MusicRepository
) {
    suspend operator fun invoke(): Flow<List<Music>> {
        return musicRepository.getFavorites()
    }
}
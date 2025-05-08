// domain/usecase/music/ToggleFavoriteUseCase.kt
package com.virtualrealm.virtualrealmmusicplayer.domain.usecase.music

import com.virtualrealm.virtualrealmmusicplayer.domain.model.Music
import com.virtualrealm.virtualrealmmusicplayer.domain.repository.MusicRepository
import javax.inject.Inject

class ToggleFavoriteUseCase @Inject constructor(
    private val musicRepository: MusicRepository
) {
    suspend operator fun invoke(music: Music) {
        val isInFavorites = musicRepository.isInFavorites(music.id)
        if (isInFavorites) {
            musicRepository.removeFromFavorites(music)
        } else {
            musicRepository.addToFavorites(music)
        }
    }
}
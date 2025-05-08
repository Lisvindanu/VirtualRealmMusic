// app/src/main/java/com/virtualrealm/virtualrealmmusicplayer/ui/player/PlayerViewModel.kt
package com.virtualrealm.virtualrealmmusicplayer.ui.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.virtualrealm.virtualrealmmusicplayer.domain.model.Music
import com.virtualrealm.virtualrealmmusicplayer.domain.model.Resource
import com.virtualrealm.virtualrealmmusicplayer.domain.repository.MusicRepository
import com.virtualrealm.virtualrealmmusicplayer.domain.usecase.music.ToggleFavoriteUseCase
import com.virtualrealm.virtualrealmmusicplayer.util.Constants
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase,
    private val musicRepository: MusicRepository
) : ViewModel() {

    private val _music = MutableStateFlow<Music?>(null)
    val music: StateFlow<Music?> = _music.asStateFlow()

    private val _isFavorite = MutableStateFlow(false)
    val isFavorite: StateFlow<Boolean> = _isFavorite.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun loadMusic(musicId: String, musicType: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                when (musicType) {
                    Constants.MUSIC_TYPE_SPOTIFY -> {
                        // Get the Spotify track from the database
                        // For a real app, you might want to fetch it from the Spotify API if not in DB
                        val musicFromDb = musicRepository.getFavorites().collect { favorites ->
                            val track = favorites.find {
                                it.id == musicId && it is Music.SpotifyTrack
                            }

                            if (track != null) {
                                _music.value = track
                                checkFavoriteStatus(musicId)
                            }
                        }
                    }
                    Constants.MUSIC_TYPE_YOUTUBE -> {
                        // Get the YouTube video from the database
                        // For a real app, you might want to fetch it from the YouTube API if not in DB
                        val musicFromDb = musicRepository.getFavorites().collect { favorites ->
                            val video = favorites.find {
                                it.id == musicId && it is Music.YoutubeVideo
                            }

                            if (video != null) {
                                _music.value = video
                                checkFavoriteStatus(musicId)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                _error.value = "Error loading music: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun checkFavoriteStatus(musicId: String) {
        viewModelScope.launch {
            _isFavorite.value = musicRepository.isInFavorites(musicId)
        }
    }

    fun toggleFavorite() {
        val currentMusic = _music.value ?: return

        viewModelScope.launch {
            _isLoading.value = true
            toggleFavoriteUseCase(currentMusic)
            checkFavoriteStatus(currentMusic.id)
            _isLoading.value = false
        }
    }
}

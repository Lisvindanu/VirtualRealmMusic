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

    // Update the loadMusic function in PlayerViewModel.kt
    fun loadMusic(musicId: String, musicType: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                when (musicType) {
                    Constants.MUSIC_TYPE_SPOTIFY -> {
                        // Create a simple Spotify track
                        val track = Music.SpotifyTrack(
                            id = musicId,
                            title = "Spotify Track",
                            artists = "Artist",
                            thumbnailUrl = "",
                            albumName = "Album",
                            uri = "spotify:track:$musicId",
                            durationMs = 0
                        )
                        _music.value = track
                    }
                    Constants.MUSIC_TYPE_YOUTUBE -> {
                        // Create a simple YouTube video
                        val video = Music.YoutubeVideo(
                            id = musicId,
                            title = "YouTube Video",
                            artists = "Channel",
                            thumbnailUrl = "",
                            channelTitle = "Channel"
                        )
                        _music.value = video
                    }
                }
                // Check favorite status
                _isFavorite.value = musicRepository.isInFavorites(musicId)
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

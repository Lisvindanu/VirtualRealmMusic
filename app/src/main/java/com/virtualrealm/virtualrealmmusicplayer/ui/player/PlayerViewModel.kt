// app/src/main/java/com/virtualrealm/virtualrealmmusicplayer/ui/player/PlayerViewModel.kt
package com.virtualrealm.virtualrealmmusicplayer.ui.player

import android.util.Log
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

            Log.d("PlayerViewModel", "Loading music ID: $musicId, type: $musicType")

            try {
                // Periksa apakah ada data musik di cache lokal
                val localMusic = musicRepository.getLocalMusicById(musicId)

                if (localMusic != null) {
                    Log.d("PlayerViewModel", "Found music in local cache: ${localMusic.title}")
                    _music.value = localMusic
                    _isFavorite.value = musicRepository.isInFavorites(musicId)
                    _isLoading.value = false
                    return@launch
                }

                // Jika tidak ada di cache, buat placeholder sambil memuat data lengkap
                when (musicType) {
                    Constants.MUSIC_TYPE_YOUTUBE -> {
                        // Buat objek YouTube placeholder
                        val tempVideo = Music.YoutubeVideo(
                            id = musicId,
                            title = "Loading...",
                            artists = "Loading...",
                            thumbnailUrl = "",
                            channelTitle = "YouTube"
                        )
                        _music.value = tempVideo

                        // Coba muat data lengkapnya secara asinkron
                        try {
                            val details = musicRepository.getYoutubeVideoDetails(musicId)
                            if (details != null) {
                                Log.d("PlayerViewModel", "Successfully loaded video details: ${details.title}")
                                _music.value = details
                            }
                        } catch (e: Exception) {
                            Log.e("PlayerViewModel", "Error loading YouTube details: ${e.message}", e)
                            // Tetap gunakan tempVideo jika gagal
                        }
                    }

                    Constants.MUSIC_TYPE_SPOTIFY -> {
                        // Logika untuk Spotify...
                    }
                }

                // Check favorite status
                _isFavorite.value = musicRepository.isInFavorites(musicId)

            } catch (e: Exception) {
                Log.e("PlayerViewModel", "Error loading music: ${e.message}", e)
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

// ui/player/PlayerViewModel.kt
package com.virtualrealm.virtualrealmmusicplayer.ui.player

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.virtualrealm.virtualrealmmusicplayer.domain.model.Music
import com.virtualrealm.virtualrealmmusicplayer.domain.repository.MusicRepository
import com.virtualrealm.virtualrealmmusicplayer.domain.usecase.music.ToggleFavoriteUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase,
    private val musicRepository: MusicRepository
) : ViewModel() {

    private val _isFavorite = MutableLiveData<Boolean>()
    val isFavorite: LiveData<Boolean> = _isFavorite

    fun checkFavoriteStatus(musicId: String) {
        viewModelScope.launch {
            val status = musicRepository.isInFavorites(musicId)
            _isFavorite.value = status
        }
    }

    fun toggleFavorite(music: Music) {
        viewModelScope.launch {
            toggleFavoriteUseCase(music)
            // Update UI after toggling
            checkFavoriteStatus(music.id)
        }
    }
}
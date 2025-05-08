// File: app/src/main/java/com/virtualrealm/virtualrealmmusicplayer/ui/home/HomeViewModel.kt
package com.virtualrealm.virtualrealmmusicplayer.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.virtualrealm.virtualrealmmusicplayer.domain.model.AuthState
import com.virtualrealm.virtualrealmmusicplayer.domain.model.Music
import com.virtualrealm.virtualrealmmusicplayer.domain.usecase.auth.GetAuthStateUseCase
import com.virtualrealm.virtualrealmmusicplayer.domain.usecase.auth.LogoutUseCase
import com.virtualrealm.virtualrealmmusicplayer.domain.usecase.music.GetFavoritesUseCase
import com.virtualrealm.virtualrealmmusicplayer.domain.usecase.music.ToggleFavoriteUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getFavoritesUseCase: GetFavoritesUseCase,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase,
    private val getAuthStateUseCase: GetAuthStateUseCase,
    private val logoutUseCase: LogoutUseCase
) : ViewModel() {

    private val _favorites = MutableStateFlow<List<Music>>(emptyList())
    val favorites: StateFlow<List<Music>> = _favorites.asStateFlow()

    private val _authState = MutableStateFlow<AuthState?>(null)
    val authState: StateFlow<AuthState?> = _authState.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        viewModelScope.launch {
            getFavoritesUseCase().collect {
                _favorites.value = it
            }
        }

        viewModelScope.launch {
            getAuthStateUseCase().collect {
                _authState.value = it
            }
        }
    }

    fun toggleFavorite(music: Music) {
        viewModelScope.launch {
            _isLoading.value = true
            toggleFavoriteUseCase(music)
            _isLoading.value = false
        }
    }

    fun logout() {
        viewModelScope.launch {
            logoutUseCase()
        }
    }
}
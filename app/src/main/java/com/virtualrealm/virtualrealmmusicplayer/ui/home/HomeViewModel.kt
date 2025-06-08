// app/src/main/java/com/virtualrealm/virtualrealmmusicplayer/ui/home/HomeViewModel.kt

package com.virtualrealm.virtualrealmmusicplayer.ui.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.virtualrealm.virtualrealmmusicplayer.domain.model.AuthState
import com.virtualrealm.virtualrealmmusicplayer.domain.model.Music
import com.virtualrealm.virtualrealmmusicplayer.domain.usecase.auth.GetAuthStateUseCase
import com.virtualrealm.virtualrealmmusicplayer.domain.usecase.auth.LogoutUseCase
import com.virtualrealm.virtualrealmmusicplayer.domain.usecase.music.GetFavoritesUseCase
import com.virtualrealm.virtualrealmmusicplayer.domain.usecase.music.ToggleFavoriteUseCase
import com.virtualrealm.virtualrealmmusicplayer.util.PlaylistManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getFavoritesUseCase: GetFavoritesUseCase,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase,
    private val getAuthStateUseCase: GetAuthStateUseCase,
    private val logoutUseCase: LogoutUseCase,
    private val playlistManager: PlaylistManager
) : ViewModel() {

    private val _favorites = MutableStateFlow<List<Music>>(emptyList())
    val favorites: StateFlow<List<Music>> = _favorites.asStateFlow()

    private val _authState = MutableStateFlow<AuthState?>(null)
    val authState: StateFlow<AuthState?> = _authState.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        viewModelScope.launch {
            getFavoritesUseCase()
                .catch { e ->
                    Log.e("HomeViewModel", "Error loading favorites: ${e.message}", e)
                    _errorMessage.value = "Error loading favorites: ${e.message}"
                }
                .collect {
                    _favorites.value = it
                }
        }

        viewModelScope.launch {
            getAuthStateUseCase()
                .catch { e ->
                    Log.e("HomeViewModel", "Error loading auth state: ${e.message}", e)
                    _errorMessage.value = "Error loading auth state: ${e.message}"
                }
                .collect {
                    _authState.value = it
                }
        }
    }

    fun toggleFavorite(music: Music) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                toggleFavoriteUseCase(music)
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Error toggling favorite: ${e.message}", e)
                _errorMessage.value = "Error updating favorites: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            try {
                logoutUseCase()
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Error during logout: ${e.message}", e)
                _errorMessage.value = "Error during logout: ${e.message}"
            }
        }
    }

    /**
     * Clear corrupted playlist data if there are JSON parsing errors
     */
    fun clearCorruptedData() {
        viewModelScope.launch {
            try {
                playlistManager.clearCorruptedData()
                _errorMessage.value = null
                Log.d("HomeViewModel", "Cleared corrupted playlist data")
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Error clearing corrupted data: ${e.message}", e)
                _errorMessage.value = "Error clearing corrupted data: ${e.message}"
            }
        }
    }

    /**
     * Clear error message
     */
    fun clearError() {
        _errorMessage.value = null
    }
}
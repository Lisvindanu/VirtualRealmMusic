// app/src/main/java/com/virtualrealm/virtualrealmmusicplayer/ui/main/MainViewModel.kt

package com.virtualrealm.virtualrealmmusicplayer.ui.main

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.virtualrealm.virtualrealmmusicplayer.domain.model.AuthState
import com.virtualrealm.virtualrealmmusicplayer.domain.model.Music
import com.virtualrealm.virtualrealmmusicplayer.domain.usecase.auth.GetAuthStateUseCase
import com.virtualrealm.virtualrealmmusicplayer.util.PlaylistManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val getAuthStateUseCase: GetAuthStateUseCase,
    private val playlistManager: PlaylistManager
) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState?>(null)
    val authState: StateFlow<AuthState?> = _authState.asStateFlow()

    private val _savedPlaylists = MutableStateFlow<List<String>>(emptyList())
    val savedPlaylists: StateFlow<List<String>> = _savedPlaylists.asStateFlow()

    init {
        viewModelScope.launch {
            getAuthStateUseCase().collectLatest {
                _authState.value = it
            }
        }

        // Load saved playlist names
        loadSavedPlaylists()
    }

    private fun loadSavedPlaylists() {
        viewModelScope.launch {
            playlistManager.getSavedPlaylistNames().collect { names ->
                _savedPlaylists.value = names
            }
        }
    }

    // Save current playlist with a name
    fun saveCurrentPlaylist(name: String, playlist: List<Music>) {
        viewModelScope.launch {
            try {
                playlistManager.saveNamedPlaylist(name, playlist)
                // Refresh the list of saved playlists
                loadSavedPlaylists()
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error saving playlist: ${e.message}", e)
            }
        }
    }

    // Get a saved playlist by name
    suspend fun getSavedPlaylist(name: String): List<Music>? {
        return playlistManager.loadNamedPlaylist(name)
    }

    // Delete a saved playlist
    fun deletePlaylist(name: String) {
        viewModelScope.launch {
            playlistManager.deleteNamedPlaylist(name)
            // Refresh the list of saved playlists
            loadSavedPlaylists()
        }
    }
}
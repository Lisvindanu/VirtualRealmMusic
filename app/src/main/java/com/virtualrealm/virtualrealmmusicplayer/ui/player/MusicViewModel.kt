// app/src/main/java/com/virtualrealm/virtualrealmmusicplayer/ui/player/MusicViewModel.kt

package com.virtualrealm.virtualrealmmusicplayer.ui.player

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.virtualrealm.virtualrealmmusicplayer.domain.model.Music
import com.virtualrealm.virtualrealmmusicplayer.service.MusicService
import com.virtualrealm.virtualrealmmusicplayer.ui.main.MainViewModel
import com.virtualrealm.virtualrealmmusicplayer.util.PlaylistManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MusicViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val playlistManager: PlaylistManager
) : ViewModel() {

    @SuppressLint("StaticFieldLeak")
    private var musicService: MusicService? = null
    private var bound = false

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentTrack = MutableStateFlow<Music?>(null)
    val currentTrack: StateFlow<Music?> = _currentTrack.asStateFlow()

    private val _playlist = MutableStateFlow<List<Music>>(emptyList())
    val playlist: StateFlow<List<Music>> = _playlist.asStateFlow()

    private val _currentIndex = MutableStateFlow(0)
    val currentIndex: StateFlow<Int> = _currentIndex.asStateFlow()

    // Track playlist modifications
    private val _playlistModified = MutableStateFlow(false)
    val playlistModified: StateFlow<Boolean> = _playlistModified.asStateFlow()

    // Store the last operation for undo functionality
    private val _lastPlaylistOperation = MutableStateFlow<PlaylistOperation?>(null)
    val lastPlaylistOperation: StateFlow<PlaylistOperation?> = _lastPlaylistOperation.asStateFlow()

    // Track for undo functionality
    private var lastRemovedTrack: Pair<Int, Music>? = null

    sealed class PlaylistOperation {
        data class ADD(val music: Music) : PlaylistOperation()
        data class REMOVE(val index: Int, val music: Music) : PlaylistOperation()
        data class MOVE(val fromIndex: Int, val toIndex: Int) : PlaylistOperation()
        object CLEAR : PlaylistOperation()
    }

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            val binder = service as MusicService.MusicBinder
            musicService = binder.getService()
            bound = true

            // Start observing service state
            observeServiceState()

            // Load saved playlist
            loadPersistedPlaylist()
        }

        override fun onServiceDisconnected(className: ComponentName) {
            musicService = null
            bound = false
        }
    }

    init {
        bindService()
    }

    private fun bindService() {
        Intent(context, MusicService::class.java).also { intent ->
            context.bindService(intent, connection, Context.BIND_AUTO_CREATE)
        }
    }

    private fun observeServiceState() {
        viewModelScope.launch {
            musicService?.playbackState?.collect { state ->
                _isPlaying.value = state == MusicService.PlaybackState.PLAYING
            }
        }

        viewModelScope.launch {
            musicService?.currentTrack?.collect { track ->
                _currentTrack.value = track
            }
        }

        viewModelScope.launch {
            musicService?.playlist?.collect { list ->
                _playlist.value = list
                // Save playlist to persistent storage
                persistPlaylist()
            }
        }

        viewModelScope.launch {
            musicService?.currentIndex?.collect { index ->
                _currentIndex.value = index
            }
        }
    }

    // Save playlist to persistent storage
    private fun persistPlaylist() {
        viewModelScope.launch {
            playlistManager.saveCurrentPlaylist(
                playlist = _playlist.value,
                currentIndex = _currentIndex.value,
                position = getCurrentPosition()
            )
        }
    }

    // Load saved playlist
    private fun loadPersistedPlaylist() {
        viewModelScope.launch {
            playlistManager.getCurrentPlaylist().collect { playlistState ->
                if (playlistState.playlist.isNotEmpty()) {
                    // Only restore if there's no active playlist
                    if (_playlist.value.isEmpty()) {
                        setPlaylist(
                            playlistState.playlist,
                            playlistState.currentIndex
                        )

                        // Seek to saved position
                        if (playlistState.positionMs > 0) {
                            seekTo(playlistState.positionMs)
                        }
                    }
                }
            }
        }
    }

    fun playMusic(music: Music) {
        musicService?.playMusic(music)
    }

    fun setPlaylist(playlist: List<Music>, startIndex: Int = 0) {
        musicService?.setPlaylist(playlist, startIndex)
    }

    fun addToPlaylist(music: Music) {
        musicService?.addToPlaylist(music)

        // Record this operation
        _lastPlaylistOperation.value = PlaylistOperation.ADD(music)
        _playlistModified.value = true
    }

    fun removeFromPlaylist(index: Int) {
        // Save for undo
        val track = _playlist.value.getOrNull(index)
        if (track != null) {
            lastRemovedTrack = index to track
            _lastPlaylistOperation.value = PlaylistOperation.REMOVE(index, track)
            _playlistModified.value = true
        }

        musicService?.removeFromPlaylist(index)
    }

    fun undoLastPlaylistOperation() {
        when (val operation = _lastPlaylistOperation.value) {
            is PlaylistOperation.REMOVE -> {
                // Re-add the removed track
                val (index, track) = lastRemovedTrack ?: return
                val currentList = _playlist.value.toMutableList()
                if (index <= currentList.size) {
                    currentList.add(index, track)
                    setPlaylist(currentList, _currentIndex.value)
                }
            }
            is PlaylistOperation.ADD -> {
                // Remove the last added track
                val track = operation.music
                val index = _playlist.value.indexOfFirst { it.id == track.id }
                if (index >= 0) {
                    removeFromPlaylist(index)
                }
            }
            else -> {} // Other operations don't have undo yet
        }

        // Clear the last operation
        _lastPlaylistOperation.value = null
    }

    fun acknowledgePlaylistModification() {
        _playlistModified.value = false
        _lastPlaylistOperation.value = null
    }

    fun play() {
        musicService?.play()
    }

    fun pause() {
        musicService?.pause()
    }

    fun stop() {
        musicService?.stop()
    }

    fun skipToNext() {
        musicService?.skipToNext()
    }

    fun skipToPrevious() {
        musicService?.skipToPrevious()
    }

    // Modify togglePlayPause to better handle different source types
    fun togglePlayPause() {
        if (_isPlaying.value) {
            pause()
        } else {
            play()
        }

        // Update UI state immediately without waiting for service callback
        _isPlaying.value = !_isPlaying.value
    }

    // Check if a track is in the playlist
    fun isTrackInPlaylist(music: Music): Boolean {
        return _playlist.value.any { it.id == music.id }
    }

    // Get position of a track in the playlist by ID
    fun getPlaylistPosition(musicId: String): Int? {
        val index = _playlist.value.indexOfFirst { it.id == musicId }
        return if (index >= 0) index else null
    }

    // Functions for player position control
    fun getCurrentPosition(): Long {
        return musicService?.getCurrentPosition() ?: 0
    }

    fun getDuration(): Long {
        return musicService?.getDuration() ?: 0
    }

    fun seekTo(position: Long) {
        musicService?.seekTo(position)
    }

    fun playTrackAndUpdatePlaylist(music: Music) {
        viewModelScope.launch {
            // First check if track is already in playlist
            val currentList = _playlist.value
            val index = currentList.indexOfFirst { it.id == music.id }

            if (index >= 0) {
                // Track exists in playlist - play it at its position
                _currentIndex.value = index
                playMusic(music)
            } else {
                // Track not in playlist - add and play
                val newList = currentList.toMutableList()
                newList.add(music)
                _playlist.value = newList
                _currentIndex.value = newList.size - 1
                playMusic(music)

                // Update the last operation
                _lastPlaylistOperation.value = PlaylistOperation.ADD(music)
                _playlistModified.value = true
            }
        }
    }

    // Add this method to load a playlist by name
    fun loadPlaylist(playlistName: String, mainViewModel: MainViewModel, startIndex: Int = 0) {
        viewModelScope.launch {
            val playlist = mainViewModel.getSavedPlaylist(playlistName)
            if (playlist != null && playlist.isNotEmpty()) {
                setPlaylist(playlist, startIndex)
            }
        }
    }

    // Add this method to handle playlist continuity
    fun handlePlaybackCompletion() {
        viewModelScope.launch {
            // When track completes, play the next one
            skipToNext()

            // Also save playlist state
            persistPlaylist()
        }
    }

    // Add this method to check if a track is in any playlists
    fun findPlaylistsContainingTrack(mainViewModel: MainViewModel, trackId: String): List<String> {
        val result = mutableListOf<String>()

        viewModelScope.launch {
            val playlists = mainViewModel.savedPlaylists.value

            // For each playlist, check if it contains the track
            for (playlistName in playlists) {
                val playlist = mainViewModel.getSavedPlaylist(playlistName)
                if (playlist?.any { it.id == trackId } == true) {
                    result.add(playlistName)
                }
            }
        }

        return result
    }

    // Add this method to create a new playlist with the current track
    fun createNewPlaylist(name: String, music: Music, mainViewModel: MainViewModel) {
        viewModelScope.launch {
            // Create a new playlist with just this track
            val playlist = listOf(music)
            mainViewModel.saveCurrentPlaylist(name, playlist)

            // Set as current playlist
            setPlaylist(playlist, 0)

            // Record this operation
            _lastPlaylistOperation.value = PlaylistOperation.ADD(music)
            _playlistModified.value = true
        }
    }



    override fun onCleared() {
        if (bound) {
            // Save playlist before unbinding
            persistPlaylist()

            context.unbindService(connection)
            bound = false
        }
        super.onCleared()
    }
}
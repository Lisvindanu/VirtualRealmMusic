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
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MusicViewModel @Inject constructor(
    @ApplicationContext private val context: Context
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

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            val binder = service as MusicService.MusicBinder
            musicService = binder.getService()
            bound = true

            // Start observing service state
            observeServiceState()
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
            }
        }

        viewModelScope.launch {
            musicService?.currentIndex?.collect { index ->
                _currentIndex.value = index
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
    }

    fun removeFromPlaylist(index: Int) {
        musicService?.removeFromPlaylist(index)
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

    fun togglePlayPause() {
        if (_isPlaying.value) {
            pause()
        } else {
            play()
        }
    }

    override fun onCleared() {
        if (bound) {
            context.unbindService(connection)
            bound = false
        }
        super.onCleared()
    }
}
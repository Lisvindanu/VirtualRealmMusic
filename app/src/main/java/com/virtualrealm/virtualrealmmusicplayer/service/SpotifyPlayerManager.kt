// app/src/main/java/com/virtualrealm/virtualrealmmusicplayer/service/SpotifyPlayerManager.kt
package com.virtualrealm.virtualrealmmusicplayer.service

import android.content.Context
import android.util.Log
import com.spotify.android.appremote.api.ConnectionParams
import com.spotify.android.appremote.api.Connector
import com.spotify.android.appremote.api.SpotifyAppRemote
import com.spotify.protocol.client.Subscription
import com.spotify.protocol.types.PlayerState
import com.virtualrealm.virtualrealmmusicplayer.util.ApiCredentials
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SpotifyPlayerManager @Inject constructor(
    private val context: Context
) {
    private var spotifyAppRemote: SpotifyAppRemote? = null
    private var connectionParams: ConnectionParams = ConnectionParams.Builder(ApiCredentials.SPOTIFY_CLIENT_ID)
        .setRedirectUri(ApiCredentials.SPOTIFY_REDIRECT_URI)
        .showAuthView(true)
        .build()

    private var playerStateSubscription: Subscription<PlayerState>? = null

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected

    private val _currentPositionMs = MutableStateFlow(0L)
    val currentPositionMs: StateFlow<Long> = _currentPositionMs

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying

    private val _currentTrackDuration = MutableStateFlow(0L)
    val currentTrackDuration: StateFlow<Long> = _currentTrackDuration

    fun connect(onConnected: () -> Unit = {}, onFailure: (Throwable) -> Unit = {}) {
        if (spotifyAppRemote?.isConnected == true) {
            _isConnected.value = true
            onConnected()
            return
        }

        SpotifyAppRemote.connect(context, connectionParams,
            object : Connector.ConnectionListener {
                override fun onConnected(appRemote: SpotifyAppRemote) {
                    spotifyAppRemote = appRemote
                    _isConnected.value = true
                    Log.d("SpotifyPlayerManager", "Connected to Spotify!")

                    // Subscribe to player state
                    subscribeToPlayerState()

                    onConnected()
                }

                override fun onFailure(error: Throwable) {
                    Log.e("SpotifyPlayerManager", "Failed to connect to Spotify: ${error.message}")
                    _isConnected.value = false
                    onFailure(error)
                }
            })
    }

    private fun subscribeToPlayerState() {
        spotifyAppRemote?.playerApi?.playerState?.setResultCallback { playerState ->
            updatePlayerState(playerState)
        }

        playerStateSubscription = spotifyAppRemote?.playerApi?.subscribeToPlayerState()
            ?.setEventCallback { playerState ->
                updatePlayerState(playerState)
            }
    }

    private fun updatePlayerState(playerState: PlayerState) {
        _isPlaying.value = !playerState.isPaused
        _currentPositionMs.value = playerState.playbackPosition
        _currentTrackDuration.value = playerState.track?.duration ?: 0
    }

    fun playTrack(spotifyUri: String) {
        spotifyAppRemote?.playerApi?.play(spotifyUri)
    }

    fun resume() {
        spotifyAppRemote?.playerApi?.resume()
    }

    fun pause() {
        spotifyAppRemote?.playerApi?.pause()
    }

    fun seekTo(positionMs: Long) {
        spotifyAppRemote?.playerApi?.seekTo(positionMs)
    }

    fun disconnect() {
        playerStateSubscription?.cancel()
        playerStateSubscription = null

        SpotifyAppRemote.disconnect(spotifyAppRemote)
        spotifyAppRemote = null
        _isConnected.value = false
    }
}
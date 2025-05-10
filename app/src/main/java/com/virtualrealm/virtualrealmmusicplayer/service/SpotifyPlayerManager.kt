package com.virtualrealm.virtualrealmmusicplayer.service

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import com.spotify.android.appremote.api.ConnectionParams
import com.spotify.android.appremote.api.Connector
import com.spotify.android.appremote.api.SpotifyAppRemote
import com.spotify.protocol.client.Subscription
import com.spotify.protocol.types.PlayerState
import com.spotify.protocol.types.Track
import com.virtualrealm.virtualrealmmusicplayer.domain.model.AuthState
import com.virtualrealm.virtualrealmmusicplayer.util.ApiCredentials
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SpotifyPlayerManager @Inject constructor(
    private val context: Context
) {
    private val TAG = "SpotifyPlayerManager"

    // Referensi ke SpotifyAppRemote
    private var spotifyAppRemote: SpotifyAppRemote? = null

    // Subscription untuk memantau perubahan status player
    private var playerStateSubscription: Subscription<PlayerState>? = null

    // State flows untuk memantau status player
    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying

    private val _currentTrackDuration = MutableStateFlow(0L)
    val currentTrackDuration: StateFlow<Long> = _currentTrackDuration

    private val _currentPositionMs = MutableStateFlow(0L)
    val currentPositionMs: StateFlow<Long> = _currentPositionMs

    private val _currentTrack = MutableStateFlow<Track?>(null)
    val currentTrack: StateFlow<Track?> = _currentTrack

    /**
     * Periksa versi Spotify yang terpasang
     *
     * Mengembalikan versionCode dari aplikasi Spotify,
     * atau 0 jika tidak terinstall
     */
    private fun getSpotifyVersionCode(): Int {
        return try {
            val packageInfo = context.packageManager.getPackageInfo("com.spotify.music", 0)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                packageInfo.longVersionCode.toInt()
            } else {
                @Suppress("DEPRECATION")
                packageInfo.versionCode
            }
        } catch (e: PackageManager.NameNotFoundException) {
            Log.d(TAG, "Spotify app not installed")
            0
        }
    }

    /**
     * Periksa apakah Spotify terpasang
     */
    fun isSpotifyInstalled(): Boolean {
        return try {
            context.packageManager.getPackageInfo("com.spotify.music", 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    /**
     * Connect ke Spotify app
     *
     * @param authState AuthState yang berisi accessToken (opsional)
     * @param onConnected Callback yang dipanggil ketika berhasil terhubung
     * @param onFailure Callback yang dipanggil ketika gagal terhubung, dengan pesan error
     */
    fun connect(authState: AuthState? = null, onConnected: () -> Unit = {}, onFailure: (String) -> Unit = {}) {
        // Jika sudah terhubung, langsung panggil onConnected
        if (spotifyAppRemote?.isConnected == true) {
            Log.d(TAG, "Already connected to Spotify")
            onConnected()
            return
        }

        // Periksa apakah Spotify terpasang
        if (!isSpotifyInstalled()) {
            Log.e(TAG, "Spotify app not installed")
            onFailure("Spotify app is not installed")
            return
        }

        // Buat ConnectionParams - REMOVE THE PROBLEMATIC PART
        val connectionParams = ConnectionParams.Builder(ApiCredentials.SPOTIFY_CLIENT_ID)
            .setRedirectUri(ApiCredentials.SPOTIFY_REDIRECT_URI)
            .showAuthView(true)
            .build()  // JUST BUILD WITHOUT TRYING TO SET ACCESS TOKEN

        Log.d(TAG, "Connecting to Spotify...")

        // Connect ke Spotify app
        SpotifyAppRemote.connect(context, connectionParams, object : Connector.ConnectionListener {
            override fun onConnected(appRemote: SpotifyAppRemote) {
                spotifyAppRemote = appRemote
                _isConnected.value = true
                Log.d(TAG, "Connected to Spotify! Remote isConnected: ${appRemote.isConnected}")

                // Subscribe ke player state
                subscribeToPlayerState()

                onConnected()
            }

            override fun onFailure(error: Throwable) {
                Log.e(TAG, "Failed to connect to Spotify: ${error.message}", error)
                _isConnected.value = false
                onFailure("Failed to connect to Spotify: ${error.message}")
            }
        })
    }

    /**
     * Subscribe ke perubahan player state
     */
    private fun subscribeToPlayerState() {
        // Ambil state awal
        spotifyAppRemote?.playerApi?.playerState?.setResultCallback { playerState ->
            updatePlayerState(playerState)
        }

        // Subscribe ke event state
        playerStateSubscription = spotifyAppRemote?.playerApi?.subscribeToPlayerState()
            ?.setEventCallback { playerState ->
                updatePlayerState(playerState)
            }
    }

    /**
     * Update state flows ketika player state berubah
     */
    private fun updatePlayerState(playerState: PlayerState) {
        _isPlaying.value = !playerState.isPaused
        _currentPositionMs.value = playerState.playbackPosition
        _currentTrackDuration.value = playerState.track?.duration ?: 0
        _currentTrack.value = playerState.track

        Log.d(TAG, "Player State Updated:")
        Log.d(TAG, "Track: ${playerState.track?.name}, Artist: ${playerState.track?.artist?.name}")
        Log.d(TAG, "Playing: ${!playerState.isPaused}, Position: ${playerState.playbackPosition}ms")
    }

    /**
     * Putar track Spotify berdasarkan URI
     *
     * @param spotifyUri URI track Spotify, format: spotify:track:id atau hanya id
     */
    fun playTrack(spotifyUri: String) {
        if (!_isConnected.value) {
            Log.e(TAG, "Cannot play track - not connected to Spotify")
            return
        }

        try {
            // Format URI dengan benar jika diperlukan
            val formattedUri = when {
                spotifyUri.startsWith("spotify://") -> spotifyUri.substring(10)
                spotifyUri.startsWith("spotify:") -> spotifyUri
                else -> "spotify:track:$spotifyUri"
            }

            Log.d(TAG, "Playing Spotify track: $formattedUri")
            spotifyAppRemote?.playerApi?.play(formattedUri)
        } catch (e: Exception) {
            Log.e(TAG, "Error playing Spotify track: ${e.message}", e)
        }
    }

    /**
     * Lanjutkan pemutaran
     */
    fun resume() {
        if (!_isConnected.value) {
            Log.e(TAG, "Cannot resume - not connected to Spotify")
            return
        }

        try {
            Log.d(TAG, "Resuming Spotify playback")
            spotifyAppRemote?.playerApi?.resume()
        } catch (e: Exception) {
            Log.e(TAG, "Error resuming Spotify playback: ${e.message}", e)
        }
    }

    /**
     * Jeda pemutaran
     */
    fun pause() {
        if (!_isConnected.value) {
            Log.e(TAG, "Cannot pause - not connected to Spotify")
            return
        }

        try {
            Log.d(TAG, "Pausing Spotify playback")
            spotifyAppRemote?.playerApi?.pause()
        } catch (e: Exception) {
            Log.e(TAG, "Error pausing Spotify playback: ${e.message}", e)
        }
    }

    /**
     * Skip ke track berikutnya
     */
    fun skipNext() {
        if (!_isConnected.value) return

        try {
            Log.d(TAG, "Skipping to next track")
            spotifyAppRemote?.playerApi?.skipNext()
        } catch (e: Exception) {
            Log.e(TAG, "Error skipping to next track: ${e.message}", e)
        }
    }

    /**
     * Skip ke track sebelumnya
     */
    fun skipPrevious() {
        if (!_isConnected.value) return

        try {
            Log.d(TAG, "Skipping to previous track")
            spotifyAppRemote?.playerApi?.skipPrevious()
        } catch (e: Exception) {
            Log.e(TAG, "Error skipping to previous track: ${e.message}", e)
        }
    }

    /**
     * Seek ke posisi tertentu
     *
     * @param positionMs Posisi dalam milidetik
     */
    fun seekTo(positionMs: Long) {
        if (!_isConnected.value) return

        try {
            Log.d(TAG, "Seeking to position: $positionMs ms")
            spotifyAppRemote?.playerApi?.seekTo(positionMs)
        } catch (e: Exception) {
            Log.e(TAG, "Error seeking: ${e.message}", e)
        }
    }

    /**
     * Set mode shuffle
     *
     * @param enabled true untuk mengaktifkan shuffle, false untuk menonaktifkan
     */
    fun setShuffle(enabled: Boolean) {
        if (!_isConnected.value) return

        try {
            Log.d(TAG, "Setting shuffle mode: $enabled")
            spotifyAppRemote?.playerApi?.setShuffle(enabled)
        } catch (e: Exception) {
            Log.e(TAG, "Error setting shuffle mode: ${e.message}", e)
        }
    }

    /**
     * Set mode repeat
     *
     * @param repeatMode 0 untuk off, 1 untuk context, 2 untuk track
     */
    fun setRepeat(repeatMode: Int) {
        if (!_isConnected.value) return

        try {
            Log.d(TAG, "Setting repeat mode: $repeatMode")
            spotifyAppRemote?.playerApi?.setRepeat(repeatMode)
        } catch (e: Exception) {
            Log.e(TAG, "Error setting repeat mode: ${e.message}", e)
        }
    }

    /**
     * Dapatkan posisi pemutaran saat ini
     */
    fun getCurrentPosition(): Long {
        return _currentPositionMs.value
    }

    /**
     * Dapatkan durasi track saat ini
     */
    fun getDuration(): Long {
        return _currentTrackDuration.value
    }

    /**
     * Dapatkan info album art untuk track saat ini
     */
    fun getAlbumArtUrl(): String? {
        val track = _currentTrack.value ?: return null

        // In Spotify App Remote API, images are accessed via the imageUri property
        return track.imageUri?.raw
    }

    /**
     * Dapatkan info track saat ini
     */
    fun getCurrentTrackInfo(): Triple<String, String, String>? {
        val track = _currentTrack.value ?: return null

        val title = track.name
        val artist = track.artist.name
        val album = track.album.name

        return Triple(title, artist, album)
    }

    /**
     * Dapatkan URI track saat ini
     */
    fun getCurrentTrackUri(): String? {
        return _currentTrack.value?.uri
    }

    /**
     * Disconnect dari Spotify
     */
    fun disconnect() {
        try {
            playerStateSubscription?.cancel()
            playerStateSubscription = null

            SpotifyAppRemote.disconnect(spotifyAppRemote)
            spotifyAppRemote = null
            _isConnected.value = false

            Log.d(TAG, "Disconnected from Spotify")
        } catch (e: Exception) {
            Log.e(TAG, "Error disconnecting from Spotify: ${e.message}", e)
        }
    }
}
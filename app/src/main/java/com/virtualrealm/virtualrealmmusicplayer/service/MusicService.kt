// app/src/main/java/com/virtualrealm/virtualrealmmusicplayer/service/MusicService.kt
package com.virtualrealm.virtualrealmmusicplayer.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.virtualrealm.virtualrealmmusicplayer.MainActivity
import com.virtualrealm.virtualrealmmusicplayer.R
import com.virtualrealm.virtualrealmmusicplayer.domain.model.Music
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.URL
import javax.inject.Inject

@AndroidEntryPoint
class MusicService : LifecycleService(), MediaPlayer.OnPreparedListener,
    MediaPlayer.OnErrorListener, MediaPlayer.OnCompletionListener,
    AudioManager.OnAudioFocusChangeListener {

    // --- Injeksi Dependensi ---
    @Inject
    lateinit var musicExtractionService: MusicExtractionService
    @Inject
    lateinit var youTubeAudioPlayer: YouTubeAudioPlayer
    @Inject
    lateinit var spotifyPlayerManager: SpotifyPlayerManager
    @Inject
    lateinit var spotifyWebPlayerHelper: SpotifyWebPlayerHelper

    // --- Properti Inti ---
    private val binder = MusicBinder()
    private var mediaPlayer: MediaPlayer? = null
    private var mediaSession: MediaSessionCompat? = null
    private var audioManager: AudioManager? = null
    private var audioFocusRequest: AudioFocusRequest? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    // --- State Management ---
    private var currentMusic: Music? = null
    private var currentAudioUrl: String? = null
    private var currentAlbumArt: Bitmap? = null
    private var isMediaPlayerPrepared = false

    private val _playbackState = MutableStateFlow(PlaybackState.IDLE)
    val playbackState: StateFlow<PlaybackState> = _playbackState

    private val _currentTrack = MutableStateFlow<Music?>(null)
    val currentTrack: StateFlow<Music?> = _currentTrack

    private val _playlist = MutableStateFlow<List<Music>>(emptyList())
    val playlist: StateFlow<List<Music>> = _playlist

    private val _currentIndex = MutableStateFlow(-1)
    val currentIndex: StateFlow<Int> = _currentIndex

    companion object {
        private const val TAG = "MusicService"
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "music_playback_channel"
        private const val MEDIA_SESSION_TAG = "VirtualRealmMusicPlayer"
    }

    enum class PlaybackState { IDLE, PREPARING, PLAYING, PAUSED, STOPPED, ERROR }

    inner class MusicBinder : Binder() {
        fun getService(): MusicService = this@MusicService
    }

    override fun onCreate() {
        super.onCreate()
        initMediaPlayer()
        initMediaSession()
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager

        // Initialize YouTubeAudioPlayer
        youTubeAudioPlayer.initialize()

        // Initialize SpotifyWebPlayerHelper
        spotifyWebPlayerHelper.initialize()

        // Setup callbacks for SpotifyWebPlayerHelper
        spotifyWebPlayerHelper.onPrepared = {
            _playbackState.value = PlaybackState.PLAYING
            updatePlaybackState(PlaybackState.PLAYING)
            updateNotification()
        }

        spotifyWebPlayerHelper.onError = { message ->
            Log.e(TAG, "Spotify Web Player error: $message")

            // Jika error membutuhkan login, tampilkan antarmuka login
            if (message.contains("login", ignoreCase = true)) {
                spotifyWebPlayerHelper.showLoginInterface()
            } else {
                // Otherwise, try fallback
                _playbackState.value = PlaybackState.ERROR
                tryPlayFallbackAudio()
            }
        }

        spotifyWebPlayerHelper.onCompletion = {
            skipToNext()
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel()
        }
    }

    private fun initMediaPlayer() {
        mediaPlayer = MediaPlayer().apply {
            setWakeMode(applicationContext, PowerManager.PARTIAL_WAKE_LOCK)
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            setOnPreparedListener(this@MusicService)
            setOnCompletionListener(this@MusicService)
            setOnErrorListener(this@MusicService)
        }
    }

    private fun initMediaSession() {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_IMMUTABLE
        )

        mediaSession = MediaSessionCompat(this, MEDIA_SESSION_TAG).apply {
            setSessionActivity(pendingIntent)
            isActive = true
        }

        mediaSession?.setCallback(object : MediaSessionCompat.Callback() {
            override fun onPlay() {
                play()
            }

            override fun onPause() {
                pause()
            }

            override fun onStop() {
                stop()
            }

            override fun onSkipToNext() {
                skipToNext()
            }

            override fun onSkipToPrevious() {
                skipToPrevious()
            }
        })
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.playback_channel_name)
            val description = getString(R.string.playback_channel_description)
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                this.description = description
                setShowBadge(false)
            }

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onBind(intent: Intent): IBinder {
        super.onBind(intent)
        return binder
    }

    fun setPlaylist(playlist: List<Music>, startIndex: Int = 0) {
        _playlist.value = playlist
        _currentIndex.value = startIndex
        if (playlist.isNotEmpty() && startIndex < playlist.size) {
            playMusicFromPlaylist(startIndex)
        }
    }

    fun addToPlaylist(music: Music) {
        val currentList = _playlist.value.toMutableList()
        // Check if the track already exists in the playlist to avoid duplicates
        if (!currentList.any { it.id == music.id }) {
            currentList.add(music)
            _playlist.value = currentList

            // If this is the first track added to an empty playlist, start playing it
            if (currentList.size == 1) {
                playMusicFromPlaylist(0)
            }
        }
    }

    fun removeFromPlaylist(index: Int) {
        if (index >= 0 && index < _playlist.value.size) {
            val currentList = _playlist.value.toMutableList()
            currentList.removeAt(index)
            _playlist.value = currentList

            // Adjust current index if needed
            if (index == _currentIndex.value) {
                // If current playing track is removed, play next one
                if (currentList.isNotEmpty()) {
                    val newIndex = if (_currentIndex.value >= currentList.size) {
                        currentList.size - 1
                    } else {
                        _currentIndex.value
                    }
                    playMusicFromPlaylist(newIndex)
                } else {
                    stop()
                }
            } else if (index < _currentIndex.value) {
                // Adjust current index if a track before current one is removed
                _currentIndex.value -= 1
            }
        }
    }

    // Additional helper function to ensure smoother transitions
    private fun playMusicFromPlaylist(index: Int) {
        if (index >= 0 && index < _playlist.value.size) {
            _currentIndex.value = index
            val music = _playlist.value[index]

            // Log which track is being played
            Log.d(TAG, "Playing from playlist: ${music.title} (${music.id}), type: ${
                when(music) {
                    is Music.SpotifyTrack -> "Spotify"
                    is Music.YoutubeVideo -> "YouTube"
                }
            }")

            playMusic(music)
        } else {
            Log.e(TAG, "Invalid playlist index: $index (playlist size: ${_playlist.value.size})")
        }
    }

    fun skipToNext() {
        if (_playlist.value.isNotEmpty()) {
            try {
                // Increment index and keep it within bounds
                val nextIndex = (_currentIndex.value + 1) % _playlist.value.size

                // Log transition
                Log.d(TAG, "Skipping to next track: index $nextIndex of ${_playlist.value.size}")

                // Stop current playback
                when {
                    currentMusic is Music.SpotifyTrack && currentAudioUrl?.startsWith("spotifyweb://") == true -> {
                        spotifyWebPlayerHelper.pause()
                    }
                    currentMusic is Music.SpotifyTrack && currentAudioUrl?.startsWith("spotify://") == true -> {
                        spotifyPlayerManager.pause()
                    }
                    currentMusic is Music.YoutubeVideo && currentAudioUrl?.startsWith("youtube://") == true -> {
                        youTubeAudioPlayer.pause()
                    }
                    else -> {
                        mediaPlayer?.pause()
                    }
                }

                // Play the next track
                playMusicFromPlaylist(nextIndex)
            } catch (e: Exception) {
                Log.e(TAG, "Error skipping to next track: ${e.message}", e)
                // If error, try to continue with current track
                _currentIndex.value.let { playMusicFromPlaylist(it) }
            }
        } else if (currentMusic != null) {
            // If there's a current track but no playlist, restart the current track
            currentMusic?.let { playMusic(it) }
        }
    }

    fun skipToPrevious() {
        if (_playlist.value.isNotEmpty()) {
            try {
                // Decrement index and keep it within bounds (wrap around to end)
                val prevIndex = if (_currentIndex.value > 0) {
                    _currentIndex.value - 1
                } else {
                    _playlist.value.size - 1  // Go to the last track if we're at the first one
                }

                // Log transition
                Log.d(TAG, "Skipping to previous track: index $prevIndex of ${_playlist.value.size}")

                // Stop current playback
                when {
                    currentMusic is Music.SpotifyTrack && currentAudioUrl?.startsWith("spotifyweb://") == true -> {
                        spotifyWebPlayerHelper.pause()
                    }
                    currentMusic is Music.SpotifyTrack && currentAudioUrl?.startsWith("spotify://") == true -> {
                        spotifyPlayerManager.pause()
                    }
                    currentMusic is Music.YoutubeVideo && currentAudioUrl?.startsWith("youtube://") == true -> {
                        youTubeAudioPlayer.pause()
                    }
                    else -> {
                        mediaPlayer?.pause()
                    }
                }

                // Play the previous track
                playMusicFromPlaylist(prevIndex)
            } catch (e: Exception) {
                Log.e(TAG, "Error skipping to previous track: ${e.message}", e)
                // If error, try to continue with current track
                _currentIndex.value.let { playMusicFromPlaylist(it) }
            }
        } else if (currentMusic != null) {
            // If there's a current track but no playlist, restart the current track
            currentMusic?.let { playMusic(it) }
        }
    }

    fun playMusic(music: Music) {
        lifecycleScope.launch {
            try {
                Log.d(TAG, "Starting to play music: ${music.title}")
                _playbackState.value = PlaybackState.PREPARING
                currentMusic = music
                _currentTrack.value = music

                // Extract URL audio
                val audioUrl = musicExtractionService.extractAudioUrl(music)
                currentAudioUrl = audioUrl
                Log.d(TAG, "Using audio URL: $audioUrl")

                when {
                    // Handle Spotify Web Player
                    audioUrl.startsWith("spotifyweb://") -> {
                        val spotifyId = audioUrl.substringAfter("spotifyweb://")
                        Log.d(TAG, "Playing Spotify track with WebPlayer ID: $spotifyId")

                        // Coba putar menggunakan Spotify Web Player
                        spotifyWebPlayerHelper.loadAndPlayTrack(spotifyId)

                        // Set status ke preparing dan update UI
                        _playbackState.value = PlaybackState.PREPARING
                        updatePlaybackState(PlaybackState.PREPARING)
                        updateMediaSessionMetadata(music)
                        updateNotification()
                    }

                    // Handle Spotify track with native app
                    audioUrl.startsWith("spotify://") -> {
                        val spotifyUri = audioUrl.substringAfter("spotify://")

                        // Cek apakah Spotify terpasang
                        if (!spotifyPlayerManager.isSpotifyInstalled()) {
                            Log.e(TAG, "Spotify app not installed, falling back to Spotify Web Player")
                            // Fallback to Spotify Web Player
                            val trackId = music.id
                            spotifyWebPlayerHelper.loadAndPlayTrack(trackId)

                            _playbackState.value = PlaybackState.PREPARING
                            updatePlaybackState(PlaybackState.PREPARING)
                            updateMediaSessionMetadata(music)
                            updateNotification()
                            return@launch
                        }

                        // Connect ke Spotify app
                        if (!spotifyPlayerManager.isConnected.value) {
                            spotifyPlayerManager.connect(
                                onConnected = {
                                    // Play track ketika sudah terhubung
                                    spotifyPlayerManager.playTrack(spotifyUri)
                                    _playbackState.value = PlaybackState.PLAYING
                                    updatePlaybackState(PlaybackState.PLAYING)

                                    // Update UI/notification
                                    updateMediaSessionMetadata(music)
                                    updateNotification()
                                },
                                onFailure = { error ->
                                    Log.e(TAG, "Spotify connection error: $error")

                                    // Fallback to Spotify Web Player
                                    val trackId = music.id
                                    spotifyWebPlayerHelper.loadAndPlayTrack(trackId)
                                }
                            )
                        } else {
                            // Jika sudah terhubung, langsung play
                            spotifyPlayerManager.playTrack(spotifyUri)
                            _playbackState.value = PlaybackState.PLAYING
                            updatePlaybackState(PlaybackState.PLAYING)

                            // Update UI/notification
                            updateMediaSessionMetadata(music)
                            updateNotification()
                        }
                    }

                    // Handle YouTube video
                    audioUrl.startsWith("youtube://") -> {
                        val videoId = audioUrl.substringAfter("youtube://")

                        // Set callbacks
                        youTubeAudioPlayer.onPrepared = {
                            _playbackState.value = PlaybackState.PLAYING
                            updatePlaybackState(PlaybackState.PLAYING)

                            // Start foreground service
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                startForeground(NOTIFICATION_ID, createNotification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
                            } else {
                                startForeground(NOTIFICATION_ID, createNotification())
                            }
                        }

                        youTubeAudioPlayer.onCompletion = {
                            skipToNext()
                        }

                        youTubeAudioPlayer.onError = { message ->
                            Log.e(TAG, "YouTube player error: $message")
                            _playbackState.value = PlaybackState.ERROR
                            tryPlayFallbackAudio()
                        }

                        // Load dan putar YouTube
                        youTubeAudioPlayer.loadAndPlayYouTube(videoId)
                    }

                    // Handle regular media files
                    else -> {
                        withContext(Dispatchers.Main) {
                            try {
                                mediaPlayer?.reset()
                                Log.d(TAG, "MediaPlayer reset successful")
                            } catch (e: Exception) {
                                Log.e(TAG, "Error resetting MediaPlayer: ${e.message}", e)
                            }
                        }

                        withContext(Dispatchers.IO) {
                            try {
                                mediaPlayer?.setDataSource(audioUrl)
                                Log.d(TAG, "Set data source success")
                                mediaPlayer?.prepareAsync()
                                Log.d(TAG, "Preparing media player asynchronously")
                            } catch (e: Exception) {
                                Log.e(TAG, "Error preparing media player: ${e.message}", e)
                                _playbackState.value = PlaybackState.ERROR
                                tryPlayFallbackAudio()
                            }
                        }
                    }
                }

                // Load album art
                loadAlbumArt(music.thumbnailUrl)

            } catch (e: Exception) {
                Log.e(TAG, "Error in playMusic: ${e.message}", e)
                _playbackState.value = PlaybackState.ERROR
                tryPlayFallbackAudio()
            }
        }
    }

    private fun tryPlayFallbackAudio() {
        try {
            // Gunakan file audio yang lebih kecil dan reliable
            val fallbackUrl = "https://www.kozco.com/tech/piano2.wav"

            Log.d(TAG, "Playing fallback audio: $fallbackUrl")
            mediaPlayer?.reset()
            mediaPlayer?.setDataSource(fallbackUrl)
            mediaPlayer?.prepareAsync()
        } catch (e: Exception) {
            Log.e(TAG, "Error playing fallback: ${e.message}", e)
            _playbackState.value = PlaybackState.ERROR
        }
    }

    private fun loadAlbumArt(url: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Load image using URL connection instead of Glide
                val bitmap = BitmapFactory.decodeStream(URL(url).openStream())
                currentAlbumArt = bitmap
                updateNotification()
            } catch (e: Exception) {
                e.printStackTrace()
                // Use a default album art
                currentAlbumArt = BitmapFactory.decodeResource(resources, R.drawable.placeholder_album)
                updateNotification()
            }
        }
    }

    private fun updateMediaSessionMetadata(music: Music) {
        val metadataBuilder = MediaMetadataCompat.Builder()
            .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, music.artists)
            .putString(MediaMetadataCompat.METADATA_KEY_TITLE, music.title)
            .putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI, music.thumbnailUrl)

        if (currentAlbumArt != null) {
            metadataBuilder.putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, currentAlbumArt)
        }

        mediaSession?.setMetadata(metadataBuilder.build())
    }

    private fun updatePlaybackState(state: PlaybackState) {
        val playbackStateBuilder = PlaybackStateCompat.Builder()

        val actions = PlaybackStateCompat.ACTION_PLAY_PAUSE or
                PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS

        val playbackState = when(state) {
            PlaybackState.PLAYING -> PlaybackStateCompat.STATE_PLAYING
            PlaybackState.PAUSED -> PlaybackStateCompat.STATE_PAUSED
            PlaybackState.STOPPED -> PlaybackStateCompat.STATE_STOPPED
            PlaybackState.PREPARING -> PlaybackStateCompat.STATE_BUFFERING
            else -> PlaybackStateCompat.STATE_NONE
        }

        val currentPosition = getCurrentPosition()

        playbackStateBuilder.setState(playbackState, currentPosition, 1.0f)
        playbackStateBuilder.setActions(actions)

        mediaSession?.setPlaybackState(playbackStateBuilder.build())
    }

    private fun requestAudioFocus(): Boolean {
        val result = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setOnAudioFocusChangeListener(this)
                .build()

            audioManager?.requestAudioFocus(audioFocusRequest!!) ?: AudioManager.AUDIOFOCUS_REQUEST_FAILED
        } else {
            @Suppress("DEPRECATION")
            audioManager?.requestAudioFocus(
                this,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN
            ) ?: AudioManager.AUDIOFOCUS_REQUEST_FAILED
        }

        return result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
    }

    private fun abandonAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let { request ->
                audioManager?.abandonAudioFocusRequest(request)
            }
        } else {
            @Suppress("DEPRECATION")
            audioManager?.abandonAudioFocus(this)
        }
    }

    fun play() {
        when {
            // Untuk Spotify Web Player
            currentMusic is Music.SpotifyTrack && currentAudioUrl?.startsWith("spotifyweb://") == true -> {
                spotifyWebPlayerHelper.play()
                _playbackState.value = PlaybackState.PLAYING
                updatePlaybackState(PlaybackState.PLAYING)
                updateNotification()

                // Start foreground service
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    startForeground(NOTIFICATION_ID, createNotification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
                } else {
                    startForeground(NOTIFICATION_ID, createNotification())
                }
            }

            // Untuk Spotify app
            currentMusic is Music.SpotifyTrack && currentAudioUrl?.startsWith("spotify://") == true -> {
                spotifyPlayerManager.resume()
                _playbackState.value = PlaybackState.PLAYING
                updatePlaybackState(PlaybackState.PLAYING)

                // Start foreground service
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    startForeground(NOTIFICATION_ID, createNotification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
                } else {
                    startForeground(NOTIFICATION_ID, createNotification())
                }
            }

            // Untuk YouTube
            currentMusic is Music.YoutubeVideo && currentAudioUrl?.startsWith("youtube://") == true -> {
                youTubeAudioPlayer.play()
                _playbackState.value = PlaybackState.PLAYING
                updatePlaybackState(PlaybackState.PLAYING)

                // Start foreground service
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    startForeground(NOTIFICATION_ID, createNotification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
                } else {
                    startForeground(NOTIFICATION_ID, createNotification())
                }
            }

            // Untuk MediaPlayer
            _playbackState.value == PlaybackState.PAUSED -> {
                mediaPlayer?.start()
                _playbackState.value = PlaybackState.PLAYING
                updatePlaybackState(PlaybackState.PLAYING)

                // Start foreground service
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    startForeground(NOTIFICATION_ID, createNotification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
                } else {
                    startForeground(NOTIFICATION_ID, createNotification())
                }
            }
        }
    }

    fun pause() {
        when {
            // Untuk Spotify Web Player
            currentMusic is Music.SpotifyTrack && currentAudioUrl?.startsWith("spotifyweb://") == true -> {
                spotifyWebPlayerHelper.pause()
                _playbackState.value = PlaybackState.PAUSED
                updatePlaybackState(PlaybackState.PAUSED)
                updateNotification()
            }

            // Untuk Spotify app
            currentMusic is Music.SpotifyTrack && currentAudioUrl?.startsWith("spotify://") == true -> {
                spotifyPlayerManager.pause()
                _playbackState.value = PlaybackState.PAUSED
                updatePlaybackState(PlaybackState.PAUSED)
                updateNotification()
            }

            // Untuk YouTube
            currentMusic is Music.YoutubeVideo && currentAudioUrl?.startsWith("youtube://") == true -> {
                youTubeAudioPlayer.pause()
                _playbackState.value = PlaybackState.PAUSED
                updatePlaybackState(PlaybackState.PAUSED)
                updateNotification()
            }

            // Untuk MediaPlayer
            _playbackState.value == PlaybackState.PLAYING -> {
                mediaPlayer?.pause()
                _playbackState.value = PlaybackState.PAUSED
                updatePlaybackState(PlaybackState.PAUSED)
                updateNotification()
            }
        }
    }

    fun stop() {
        when {
            // Untuk Spotify Web Player
            currentMusic is Music.SpotifyTrack && currentAudioUrl?.startsWith("spotifyweb://") == true -> {
                spotifyWebPlayerHelper.pause() // Web player doesn't have stop method
            }

            // Untuk YouTube
            currentMusic is Music.YoutubeVideo && currentAudioUrl?.startsWith("youtube://") == true -> {
                youTubeAudioPlayer.stop()
            }

            // Untuk MediaPlayer
            else -> {
                try {
                    mediaPlayer?.stop()
                } catch (e: Exception) {
                    Log.e(TAG, "Error stopping media player: ${e.message}")
                }
            }
        }

        _playbackState.value = PlaybackState.STOPPED
        updatePlaybackState(PlaybackState.STOPPED)

        // Use the appropriate overload based on API level
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }

        abandonAudioFocus()
    }

    private fun createNotification(): Notification {
        val music = currentMusic ?: return buildEmptyNotification()

        // Create intent for opening the app
        val contentIntent = Intent(this, MainActivity::class.java)
        val contentPendingIntent = PendingIntent.getActivity(
            this, 0, contentIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Create play/pause action
        val playPauseAction = if (_playbackState.value == PlaybackState.PLAYING) {
            NotificationCompat.Action(
                R.drawable.ic_pause, "Pause",
                createPendingIntent(PlaybackActions.ACTION_PAUSE)
            )
        } else {
            NotificationCompat.Action(
                R.drawable.ic_play, "Play",
                createPendingIntent(PlaybackActions.ACTION_PLAY)
            )
        }

        // Create skip actions
        val skipPreviousAction = NotificationCompat.Action(
            R.drawable.ic_previous, "Previous",
            createPendingIntent(PlaybackActions.ACTION_PREVIOUS)
        )

        val skipNextAction = NotificationCompat.Action(
            R.drawable.ic_next, "Next",
            createPendingIntent(PlaybackActions.ACTION_NEXT)
        )

        // Build notification
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_music_note)
            .setContentTitle(music.title)
            .setContentText(music.artists)
            .setContentIntent(contentPendingIntent)
            .setStyle(androidx.media.app.NotificationCompat.MediaStyle()
                .setMediaSession(mediaSession?.sessionToken)
                .setShowActionsInCompactView(0, 1, 2))
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .addAction(skipPreviousAction)
            .addAction(playPauseAction)
            .addAction(skipNextAction)

        if (currentAlbumArt != null) {
            builder.setLargeIcon(currentAlbumArt)
        }

        return builder.build()
    }

    private fun buildEmptyNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_music_note)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.no_track_playing))
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun createPendingIntent(action: String): PendingIntent {
        val intent = Intent(this, MusicService::class.java).apply {
            this.action = action
        }
        return PendingIntent.getService(
            this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun updateNotification() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, createNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        intent?.action?.let { action ->
            when (action) {
                PlaybackActions.ACTION_PLAY -> play()
                PlaybackActions.ACTION_PAUSE -> pause()
                PlaybackActions.ACTION_STOP -> stop()
                PlaybackActions.ACTION_NEXT -> skipToNext()
                PlaybackActions.ACTION_PREVIOUS -> skipToPrevious()
            }
        }

        return START_NOT_STICKY
    }

    /**
     * Log informasi status pemutaran untuk membantu debugging
     */
    private fun logPlaybackState(state: PlaybackState, additionalInfo: String = "") {
        val stateStr = when (state) {
            PlaybackState.IDLE -> "IDLE"
            PlaybackState.PREPARING -> "PREPARING"
            PlaybackState.PLAYING -> "PLAYING"
            PlaybackState.PAUSED -> "PAUSED"
            PlaybackState.STOPPED -> "STOPPED"
            PlaybackState.ERROR -> "ERROR"
        }

        val positionMs = getCurrentPosition()
        val durationMs = getDuration()
        val musicInfo = currentMusic?.let { "${it.title} (${it.id})" } ?: "No music"

        Log.d("MusicService", "Playback state: $stateStr | Music: $musicInfo | Position: ${positionMs}ms/${durationMs}ms | $additionalInfo")
    }

    override fun onPrepared(mp: MediaPlayer?) {
        try {
            Log.d(TAG, "Media player prepared successfully!")
            isMediaPlayerPrepared = true
            mp?.start() // Panggil start() di sini
            _playbackState.value = PlaybackState.PLAYING
            updatePlaybackState(PlaybackState.PLAYING)

            // Log kondisi pemutaran
            logPlaybackState(PlaybackState.PLAYING, "Media player prepared and started")

            // Pindahkan ke foreground dengan notifikasi
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(NOTIFICATION_ID, createNotification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
            } else {
                startForeground(NOTIFICATION_ID, createNotification())
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error starting playback: ${e.message}", e)
            _playbackState.value = PlaybackState.ERROR
        }
    }

    override fun onError(mp: MediaPlayer?, what: Int, extra: Int): Boolean {
        Log.e("MusicService", "Media player error: what=$what, extra=$extra")

        // If this is a YouTube video, fallback to the WebView player
        currentMusic?.let { music ->
            if (music is Music.YoutubeVideo) {
                serviceScope.launch {
                    try {
                        Log.d("MusicService", "Retrying with YouTube WebView player")
                        youTubeAudioPlayer.loadAndPlayYouTube(music.id)
                        return@launch
                    } catch (e: Exception) {
                        Log.e("MusicService", "Error in YouTube fallback: ${e.message}", e)
                        _playbackState.value = PlaybackState.ERROR
                        updatePlaybackState(PlaybackState.STOPPED)
                        tryPlayFallbackAudio()
                    }
                }
                return true
            }
        }

        // For non-YouTube media, try with a backup URL
        serviceScope.launch {
            tryPlayFallbackAudio()
        }

        return true
    }

    override fun onCompletion(mp: MediaPlayer?) {
        // When current track completes, play next track
        skipToNext()
    }

    override fun onAudioFocusChange(focusChange: Int) {
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS -> {
                // Permanent loss - stop playback
                pause()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                // Temporary loss - pause playback
                pause()
            }
            AudioManager.AUDIOFOCUS_GAIN -> {
                // Gained focus back - resume playback
                play()
            }
        }
    }

    override fun onDestroy() {
        serviceScope.cancel()
        spotifyPlayerManager.disconnect()
        spotifyWebPlayerHelper.release()
        youTubeAudioPlayer.release()
        mediaPlayer?.release()
        mediaPlayer = null
        mediaSession?.release()
        abandonAudioFocus()
        super.onDestroy()
    }

    object PlaybackActions {
        const val ACTION_PLAY = "com.virtualrealm.virtualrealmmusicplayer.action.PLAY"
        const val ACTION_PAUSE = "com.virtualrealm.virtualrealmmusicplayer.action.PAUSE"
        const val ACTION_STOP = "com.virtualrealm.virtualrealmmusicplayer.action.STOP"
        const val ACTION_NEXT = "com.virtualrealm.virtualrealmmusicplayer.action.NEXT"
        const val ACTION_PREVIOUS = "com.virtualrealm.virtualrealmmusicplayer.action.PREVIOUS"
    }

    // Mendapatkan posisi pemutaran saat ini (dalam ms)
    fun getCurrentPosition(): Long {
        return when {
            // Untuk Spotify Web Player
            currentMusic is Music.SpotifyTrack && currentAudioUrl?.startsWith("spotifyweb://") == true -> {
                val position = spotifyWebPlayerHelper.currentPosition.value
                Log.d("MusicService", "Spotify Web current position: $position ms")
                position
            }

            // Untuk Spotify App
            currentMusic is Music.SpotifyTrack && spotifyPlayerManager.isConnected.value -> {
                val position = spotifyPlayerManager.getCurrentPosition()
                Log.d("MusicService", "Spotify current position: $position ms")
                position
            }

            // Jika YouTube player aktif
            (currentMusic is Music.YoutubeVideo &&
                    youTubeAudioPlayer.isPlaying.value) -> {
                val position = youTubeAudioPlayer.currentPosition.value
                Log.d("MusicService", "YouTube current position: $position ms")
                position
            }
            // Jika MediaPlayer aktif dan playing/paused
            (mediaPlayer != null &&
                    (_playbackState.value == PlaybackState.PLAYING ||
                            _playbackState.value == PlaybackState.PAUSED)) -> {
                val position = mediaPlayer?.currentPosition?.toLong() ?: 0
                Log.d("MusicService", "MediaPlayer current position: $position ms")
                position
            }
            // Default jika tidak ada player aktif
            else -> {
                Log.d("MusicService", "No active player, returning position 0")
                0L
            }
        }
    }

    // Mendapatkan durasi total (dalam ms)
    fun getDuration(): Long {
        return when {
            // Untuk Spotify Web Player
            currentMusic is Music.SpotifyTrack && currentAudioUrl?.startsWith("spotifyweb://") == true -> {
                val duration = spotifyWebPlayerHelper.duration.value
                if (duration > 0) {
                    duration
                } else {
                    (currentMusic as Music.SpotifyTrack).durationMs
                }
            }

            // Untuk Spotify App
            currentMusic is Music.SpotifyTrack && spotifyPlayerManager.isConnected.value -> {
                val duration = spotifyPlayerManager.getDuration()
                if (duration > 0) {
                    duration
                } else {
                    (currentMusic as Music.SpotifyTrack).durationMs
                }
            }
            // Jika YouTube player aktif
            (currentMusic is Music.YoutubeVideo) -> {
                val duration = youTubeAudioPlayer.duration.value
                Log.d("MusicService", "YouTube duration: $duration ms")
                if (duration <= 0) {
                    // Jika durasi belum tersedia dari YouTube, gunakan perkiraan
                    estimateDurationFromTrack()
                } else {
                    duration
                }
            }
            // Jika MediaPlayer aktif
            (mediaPlayer != null && mediaPlayer?.duration ?: -1 > 0) -> {
                val duration = mediaPlayer?.duration?.toLong() ?: 0
                Log.d("MusicService", "MediaPlayer duration: $duration ms")
                duration
            }
            // Default jika tidak ada player aktif
            else -> {
                val estimatedDuration = estimateDurationFromTrack()
                Log.d("MusicService", "Using estimated duration: $estimatedDuration ms")
                estimatedDuration
            }
        }
    }

    // Helper function untuk memperkirakan durasi dari metadata track
    private fun estimateDurationFromTrack(): Long {
        return when (val music = currentMusic) {
            is Music.SpotifyTrack -> {
                music.durationMs
            }
            is Music.YoutubeVideo -> {
                // Perkiraan untuk video YouTube (gunakan 4 menit sebagai default)
                4 * 60 * 1000L
            }
            else -> 3 * 60 * 1000L // Default 3 menit
        }
    }

    // Mengubah posisi pemutaran (dalam ms)
    fun seekTo(position: Long) {
        Log.d("MusicService", "Seeking to position: $position ms")

        when {
            // Untuk Spotify Web Player
            currentMusic is Music.SpotifyTrack && currentAudioUrl?.startsWith("spotifyweb://") == true -> {
                spotifyWebPlayerHelper.seekTo(position)
            }

            // Untuk Spotify app
            currentMusic is Music.SpotifyTrack && spotifyPlayerManager.isConnected.value -> {
                spotifyPlayerManager.seekTo(position)
            }
            // Jika YouTube player aktif
            (currentMusic is Music.YoutubeVideo &&
                    youTubeAudioPlayer.isPlaying.value) -> {
                youTubeAudioPlayer.seekTo(position)
            }
            // Jika MediaPlayer aktif
            (mediaPlayer != null) -> {
                try {
                    mediaPlayer?.seekTo(position.toInt())
                } catch (e: Exception) {
                    Log.e("MusicService", "Error seeking MediaPlayer: ${e.message}")
                }
            }
        }
    }
    }

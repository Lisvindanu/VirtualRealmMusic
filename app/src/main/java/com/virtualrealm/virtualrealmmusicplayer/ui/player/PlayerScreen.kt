package com.virtualrealm.virtualrealmmusicplayer.ui.player

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.virtualrealm.virtualrealmmusicplayer.R
import com.virtualrealm.virtualrealmmusicplayer.ui.common.ErrorState
import com.virtualrealm.virtualrealmmusicplayer.ui.main.MainViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    musicId: String,
    musicType: String,
    onNavigateBack: () -> Unit,
    onNavigateToPlaylist: () -> Unit,
    playerViewModel: PlayerViewModel = hiltViewModel(),
    musicViewModel: MusicViewModel = hiltViewModel(),
    mainViewModel: MainViewModel = hiltViewModel()
) {
    val music by playerViewModel.music.collectAsState()
    val isFavorite by playerViewModel.isFavorite.collectAsState()
    val isLoading by playerViewModel.isLoading.collectAsState()
    val error by playerViewModel.error.collectAsState()

    val isPlaying by musicViewModel.isPlaying.collectAsState()
    val currentTrack by musicViewModel.currentTrack.collectAsState()
    val playlist by musicViewModel.playlist.collectAsState()
    val currentIndex by musicViewModel.currentIndex.collectAsState()

    // Saved playlists state
    val savedPlaylists by mainViewModel.savedPlaylists.collectAsState()
    var showAddToPlaylistDialog by remember { mutableStateOf(false) }

    // Playlist floating button state
    var isPlaylistButtonExpanded by remember { mutableStateOf(false) }

    // Track position state
    var sliderPosition by remember { mutableStateOf(0f) }
    var trackDuration by remember { mutableStateOf(0L) }
    var isUserSeeking by remember { mutableStateOf(false) }

    // Snackbar for notifications
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Track playback position
    LaunchedEffect(isPlaying, currentTrack) {
        var lastUpdateTime = System.currentTimeMillis()

        while(true) {
            if (currentTrack != null) {
                val currentTime = System.currentTimeMillis()
                val timeSinceLastUpdate = currentTime - lastUpdateTime

                // Only update at reasonable intervals to avoid too many recompositions
                if (timeSinceLastUpdate >= 500) {
                    val position = musicViewModel.getCurrentPosition()
                    val duration = musicViewModel.getDuration()

                    if (duration > 0) {
                        trackDuration = duration
                        if (!isUserSeeking) {
                            sliderPosition = (position.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
                        }
                    }
                    lastUpdateTime = currentTime
                }
            }
            delay(250) // Check frequently for responsive UI
        }
    }

    // Load music and handle playlist integration
    LaunchedEffect(musicId, musicType) {
        playerViewModel.loadMusic(musicId, musicType)
    }

    // Add to playlist when loaded
    LaunchedEffect(music) {
        val currentMusic = music
        if (currentMusic != null) {
            try {
                if (!musicViewModel.isTrackInPlaylist(currentMusic)) {
                    musicViewModel.addToPlaylist(currentMusic)
                    delay(100) // Small delay to ensure addition completes

                    val newPosition = musicViewModel.getPlaylistPosition(currentMusic.id) ?: 0
                    musicViewModel.setPlaylist(musicViewModel.playlist.value, newPosition)
                } else {
                    val position = musicViewModel.getPlaylistPosition(currentMusic.id)
                    if (position != null && position != currentIndex) {
                        musicViewModel.setPlaylist(playlist, position)
                    }
                }
            } catch (e: Exception) {
                Log.e("PlayerScreen", "Error adding to playlist: ${e.message}")
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            PlayerAppBar(
                onNavigateBack = onNavigateBack,
                onNavigateToPlaylist = onNavigateToPlaylist,
                playlistSize = playlist.size
            )
        },
        floatingActionButton = {
            if (playlist.size > 1 || !music?.let { musicViewModel.isTrackInPlaylist(it) }!!) {
                PlayerFloatingButton(
                    playlistSize = playlist.size,
                    isExpanded = isPlaylistButtonExpanded,
                    onExpandClick = { isPlaylistButtonExpanded = !isPlaylistButtonExpanded },
                    onViewPlaylistClick = {
                        isPlaylistButtonExpanded = false
                        onNavigateToPlaylist()
                    },
                    onAddToPlaylistClick = {
                        isPlaylistButtonExpanded = false
                        showAddToPlaylistDialog = true
                    }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (error != null) {
                ErrorState(
                    message = error ?: stringResource(R.string.unknown_error),
                    onRetry = { playerViewModel.loadMusic(musicId, musicType) }
                )
            } else {
                val currentMusic = music
                if (currentMusic != null) {
                    PlayerContent(
                        music = currentMusic,
                        isFavorite = isFavorite,
                        isPlaying = isPlaying,
                        onToggleFavorite = { playerViewModel.toggleFavorite() },
                        onTogglePlayPause = { musicViewModel.togglePlayPause() },
                        onSkipNext = { musicViewModel.skipToNext() },
                        onSkipPrevious = { musicViewModel.skipToPrevious() },
                        onSeekTo = { position ->
                            isUserSeeking = true
                            sliderPosition = position
                            val seekToMs = (position * trackDuration).toLong()
                            musicViewModel.seekTo(seekToMs)

                            // Release seeking state after a delay
                            scope.launch {
                                delay(1000)
                                isUserSeeking = false
                            }
                        },
                        currentPosition = sliderPosition,
                        duration = trackDuration,
                        musicType = musicType,
                        playlist = playlist,
                        currentIndex = currentIndex,
                        onNavigateToPlaylist = onNavigateToPlaylist
                    )
                }
            }
        }
    }

    // Add to playlist dialog
    if (showAddToPlaylistDialog && music != null) {
        AddToPlaylistDialog(
            track = music!!,
            availablePlaylists = savedPlaylists,
            onDismiss = { showAddToPlaylistDialog = false },
            onAddToExisting = { playlistName ->
                scope.launch {
                    val existingPlaylist = mainViewModel.getSavedPlaylist(playlistName)
                    if (existingPlaylist != null) {
                        if (existingPlaylist.any { it.id == music!!.id }) {
                            snackbarHostState.showSnackbar(
                                message = "Track already exists in playlist '$playlistName'",
                                duration = SnackbarDuration.Short
                            )
                        } else {
                            val updatedPlaylist = existingPlaylist.toMutableList()
                            updatedPlaylist.add(music!!)
                            mainViewModel.saveCurrentPlaylist(playlistName, updatedPlaylist)
                            snackbarHostState.showSnackbar(
                                message = "Added to playlist '$playlistName'",
                                duration = SnackbarDuration.Short
                            )
                        }
                    }
                }
            },
            onCreateNew = { playlistName ->
                mainViewModel.saveCurrentPlaylist(playlistName, listOf(music!!))
                scope.launch {
                    snackbarHostState.showSnackbar(
                        message = "Created new playlist '$playlistName'",
                        duration = SnackbarDuration.Short
                    )
                }
            }
        )
    }
}
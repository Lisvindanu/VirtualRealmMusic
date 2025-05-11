// app/src/main/java/com/virtualrealm/virtualrealmmusicplayer/ui/player/PlayerScreen.kt

package com.virtualrealm.virtualrealmmusicplayer.ui.player

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.NavigateBefore
import androidx.compose.material.icons.filled.NavigateNext
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Badge
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.virtualrealm.virtualrealmmusicplayer.R
import com.virtualrealm.virtualrealmmusicplayer.domain.model.Music
import com.virtualrealm.virtualrealmmusicplayer.ui.common.ErrorState
import com.virtualrealm.virtualrealmmusicplayer.ui.common.SourceTag
import com.virtualrealm.virtualrealmmusicplayer.ui.main.MainViewModel
import com.virtualrealm.virtualrealmmusicplayer.ui.theme.SpotifyGreen
import com.virtualrealm.virtualrealmmusicplayer.ui.theme.YouTubeRed
import com.virtualrealm.virtualrealmmusicplayer.util.Constants
import com.virtualrealm.virtualrealmmusicplayer.util.DateTimeUtils
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

    // Get saved playlists for add to playlist dialog
    val savedPlaylists by mainViewModel.savedPlaylists.collectAsState()

    // Playlist floating button state
    var isPlaylistButtonExpanded by remember { mutableStateOf(false) }

    // Add to playlist dialog
    var showAddToPlaylistDialog by remember { mutableStateOf(false) }

    // Playlist snackbar state
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val playlistModified by musicViewModel.playlistModified.collectAsState()
    val lastPlaylistOperation by musicViewModel.lastPlaylistOperation.collectAsState()

    // Handle playlist operation feedback
    LaunchedEffect(lastPlaylistOperation) {
        lastPlaylistOperation?.let { operation ->
            when (operation) {
                is MusicViewModel.PlaylistOperation.ADD -> {
                    snackbarHostState.showSnackbar(
                        message = "Added to playlist",
                        duration = SnackbarDuration.Short
                    )
                }
                is MusicViewModel.PlaylistOperation.REMOVE -> {
                    val result = snackbarHostState.showSnackbar(
                        message = "Removed from playlist",
                        actionLabel = "UNDO",
                        duration = SnackbarDuration.Short
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        musicViewModel.undoLastPlaylistOperation()
                    }
                }
                else -> { /* No feedback needed for other operations */ }
            }

            // Acknowledge the operation
            musicViewModel.acknowledgePlaylistModification()
        }
    }

    // Track position state
    var sliderPosition by remember { mutableStateOf(0f) }
    var trackDuration by remember { mutableStateOf(0L) }
    var isUserSeeking by remember { mutableStateOf(false) }

    // Collect current position and duration
    LaunchedEffect(isPlaying, currentTrack) {
        while(true) {
            if (currentTrack != null) {
                val position = musicViewModel.getCurrentPosition()
                val duration = musicViewModel.getDuration()

                if (duration > 0) {
                    trackDuration = duration
                    if (!isUserSeeking) {
                        // Normalize between 0 and 1, protect against division by 0
                        sliderPosition = (position.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
                    }
                }
            }
            delay(1000) // Update every second
        }
    }

    // Add current track to playlist when loaded
    LaunchedEffect(music) {
        music?.let {
            // Check if track is in playlist
            if (!musicViewModel.isTrackInPlaylist(it)) {
                // Add to playlist if not already playing from playlist
                musicViewModel.addToPlaylist(it)
            } else {
                // If already in playlist, make sure it's the current playing track
                val position = musicViewModel.getPlaylistPosition(it.id)
                if (position != null && position != currentIndex) {
                    // Set as current track in playlist
                    musicViewModel.setPlaylist(playlist, position)
                }
            }
        }
    }

    // Load the music when the screen is first composed
    LaunchedEffect(musicId, musicType) {
        playerViewModel.loadMusic(musicId, musicType)
    }

    // Start playback when music is loaded
    LaunchedEffect(music) {
        music?.let {
            musicViewModel.playMusic(it)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.now_playing)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                actions = {
                    // Fixed playlist icon button
                    IconButton(
                        onClick = onNavigateToPlaylist,
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        BadgedBox(
                            badge = {
                                if (playlist.size > 0) {
                                    Badge { Text("${playlist.size}") }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlaylistPlay,
                                contentDescription = "View Playlist"
                            )
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            // Show playlist floating button if playlist has multiple items
            if (playlist.size > 1 || !musicViewModel.isTrackInPlaylist(music)) {
                PlaylistFloatingButton(
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
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            } else if (error != null) {
                ErrorState(
                    message = error ?: stringResource(R.string.unknown_error),
                    onRetry = {
                        playerViewModel.loadMusic(musicId, musicType)
                    }
                )
            } else {
                music?.let { music ->
                    PlayerContent(
                        music = music,
                        isFavorite = isFavorite,
                        isPlaying = isPlaying,
                        onToggleFavorite = {
                            playerViewModel.toggleFavorite()
                        },
                        onTogglePlayPause = {
                            musicViewModel.togglePlayPause()
                        },
                        onSkipNext = {
                            musicViewModel.skipToNext()
                        },
                        onSkipPrevious = {
                            musicViewModel.skipToPrevious()
                        },
                        onSeekTo = { position ->
                            isUserSeeking = true
                            sliderPosition = position
                            val seekToMs = (position * trackDuration).toLong()
                            musicViewModel.seekTo(seekToMs)

                            // Release seeking state after a short delay
                            musicViewModel.viewModelScope.launch {
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
                    // Get the playlist
                    val existingPlaylist = mainViewModel.getSavedPlaylist(playlistName)
                    if (existingPlaylist != null) {
                        // Check if track already exists in playlist
                        if (existingPlaylist.any { it.id == music!!.id }) {
                            // Already exists - just show notification
                            snackbarHostState.showSnackbar(
                                message = "Track already exists in playlist '$playlistName'",
                                duration = SnackbarDuration.Short
                            )
                        } else {
                            // Add track to playlist and save
                            val updatedPlaylist = existingPlaylist.toMutableList()
                            updatedPlaylist.add(music!!)
                            mainViewModel.saveCurrentPlaylist(playlistName, updatedPlaylist)

                            // Show confirmation
                            snackbarHostState.showSnackbar(
                                message = "Added to playlist '$playlistName'",
                                duration = SnackbarDuration.Short
                            )
                        }
                    }
                }
            },
            onCreateNew = { playlistName ->
                // Create new playlist with just this track
                mainViewModel.saveCurrentPlaylist(playlistName, listOf(music!!))

                // Show confirmation
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

@Composable
fun PlayerContent(
    music: Music,
    isFavorite: Boolean,
    isPlaying: Boolean,
    onToggleFavorite: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    onSeekTo: (Float) -> Unit,
    currentPosition: Float,
    duration: Long,
    musicType: String,
    playlist: List<Music>,
    currentIndex: Int,
    onNavigateToPlaylist: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Album art section
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.5f)
                .padding(bottom = 16.dp)
        ) {
            if (musicType == Constants.MUSIC_TYPE_YOUTUBE) {
                // YouTube video thumbnail
                val thumbnailUrl = "https://img.youtube.com/vi/${music.id}/hqdefault.jpg"

                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(thumbnailUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = music.title,
                    contentScale = ContentScale.Crop,
                    placeholder = painterResource(id = R.drawable.placeholder_album),
                    error = painterResource(id = R.drawable.placeholder_album),
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(12.dp))
                )

                // YouTube play icon overlay
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .align(Alignment.Center)
                        .background(
                            color = Color.Black.copy(alpha = 0.6f),
                            shape = RoundedCornerShape(8.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_youtube),
                        contentDescription = null,
                        tint = YouTubeRed,
                        modifier = Modifier.size(40.dp)
                    )
                }
            } else if (musicType == Constants.MUSIC_TYPE_SPOTIFY) {
                // Spotify album art
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(music.thumbnailUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = music.title,
                    contentScale = ContentScale.Crop,
                    placeholder = painterResource(id = R.drawable.placeholder_album),
                    error = painterResource(id = R.drawable.placeholder_album),
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(12.dp))
                )

                // Spotify icon overlay
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .align(Alignment.Center)
                        .background(
                            color = Color.Black.copy(alpha = 0.6f),
                            shape = RoundedCornerShape(8.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_spotify),
                        contentDescription = null,
                        tint = SpotifyGreen,
                        modifier = Modifier.size(40.dp)
                    )
                }
            } else {
                // Generic music thumbnail
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(music.thumbnailUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = music.title,
                    contentScale = ContentScale.Crop,
                    placeholder = painterResource(id = R.drawable.placeholder_album),
                    error = painterResource(id = R.drawable.placeholder_album),
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(12.dp))
                )

                if (music.thumbnailUrl.isEmpty()) {
                    // Show music icon if thumbnail is empty
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(64.dp)
                        )
                    }
                }
            }
        }

        // Music info section
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.5f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Song title
            Text(
                text = music.title,
                style = MaterialTheme.typography.titleLarge,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Artist
            Text(
                text = music.artists,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Additional info based on music type
            when (music) {
                is Music.SpotifyTrack -> {
                    Text(
                        text = "${music.albumName} · ${DateTimeUtils.formatDuration(music.durationMs)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    SourceTag(
                        text = "Spotify",
                        contentColor = SpotifyGreen,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }
                is Music.YoutubeVideo -> {
                    Text(
                        text = music.channelTitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    SourceTag(
                        text = "YouTube",
                        contentColor = YouTubeRed,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Playback progress slider
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Slider(
                    value = currentPosition,
                    onValueChange = { onSeekTo(it) },
                    modifier = Modifier.fillMaxWidth()
                )

                // Timestamp display
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Current position
                    Text(
                        text = DateTimeUtils.formatDuration((currentPosition * duration).toLong()),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )

                    // Total duration
                    Text(
                        text = DateTimeUtils.formatDuration(duration),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Playback controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onSkipPrevious,
                    modifier = Modifier.size(48.dp),
                    enabled = playlist.size > 1
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_previous),
                        contentDescription = "Previous",
                        modifier = Modifier.size(32.dp),
                        tint = if (playlist.size > 1)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                    )
                }

                // Play/Pause button with visual feedback
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .clickable(onClick = onTogglePlayPause),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(
                            id = if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play
                        ),
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        modifier = Modifier.size(36.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                IconButton(
                    onClick = onSkipNext,
                    modifier = Modifier.size(48.dp),
                    enabled = playlist.size > 1
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_next),
                        contentDescription = "Next",
                        modifier = Modifier.size(32.dp),
                        tint = if (playlist.size > 1)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                    )
                }
            }

            // Add playlist navigator
            if (playlist.isNotEmpty() && playlist.size > 1) {
                // Use the PlaylistNavigator component
                PlaylistNavigator(
                    playlist = playlist,
                    currentIndex = currentIndex,
                    onPreviousClick = onSkipPrevious,
                    onNextClick = onSkipNext,
                    onViewPlaylistClick = onNavigateToPlaylist,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Favorite button
            IconButton(
                onClick = onToggleFavorite,
                modifier = Modifier
                    .size(48.dp)
                    .align(Alignment.CenterHorizontally)
            ) {
                Icon(
                    imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = if (isFavorite)
                        stringResource(R.string.remove_from_favorites)
                    else
                        stringResource(R.string.add_to_favorites),
                    tint = if (isFavorite) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}

@Composable
fun PlaylistNavigator(
    playlist: List<Music>,
    currentIndex: Int,
    onPreviousClick: () -> Unit,
    onNextClick: () -> Unit,
    onViewPlaylistClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Don't show if playlist is empty or has only one item
    if (playlist.size <= 1) return

    val prevTrack = if (currentIndex > 0) {
        playlist[currentIndex - 1]
    } else if (playlist.size > 1) {
        // Wrap around to the last track
        playlist[playlist.size - 1]
    } else null

    val nextTrack = if (currentIndex < playlist.size - 1) {
        playlist[currentIndex + 1]
    } else if (playlist.size > 1) {
        // Wrap around to the first track
        playlist[0]
    } else null

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Playlist position indicator
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Track ${currentIndex + 1} of ${playlist.size}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.width(4.dp))

                // Playlist icon button
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable(onClick = onViewPlaylistClick)
                        .padding(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlaylistPlay,
                        contentDescription = "View Playlist",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Progress indicator
            LinearProgressIndicator(
                progress = { (currentIndex + 1).toFloat() / playlist.size.toFloat() },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .height(4.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
            )

            // Navigation controls with track previews
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Previous track preview
                prevTrack?.let { track ->
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clip(MaterialTheme.shapes.small)
                            .clickable(onClick = onPreviousClick)
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowLeft,
                            contentDescription = "Previous",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )

                        Column(
                            modifier = Modifier
                                .padding(start = 4.dp)
                                .weight(1f)
                        ) {
                            Text(
                                text = "Previous",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = track.title,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                } ?: Spacer(modifier = Modifier.weight(1f))

                // Spacer in the middle
                Spacer(modifier = Modifier.width(8.dp))

                // Next track preview
                nextTrack?.let { track ->
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clip(MaterialTheme.shapes.small)
                            .clickable(onClick = onNextClick)
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.End
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(end = 4.dp)
                                .weight(1f),
                            horizontalAlignment = Alignment.End
                        ) {
                            Text(
                                text = "Next",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                textAlign = TextAlign.End
                            )
                            Text(
                                text = track.title,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.End
                            )
                        }

                        Icon(
                            imageVector = Icons.Default.KeyboardArrowRight,
                            contentDescription = "Next",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                } ?: Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun MiniPlaylistNavigator(
    playlist: List<Music>,
    currentIndex: Int,
    onNavigate: (Int) -> Unit,
    onViewPlaylistClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (playlist.isEmpty()) return

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Current track info with navigation arrows
        Box(
            modifier = Modifier
                .clip(MaterialTheme.shapes.medium)
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
                .clickable(onClick = onViewPlaylistClick)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                // Previous button
                if (playlist.size > 1) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowLeft,
                        contentDescription = "Previous Track",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .size(24.dp)
                            .clickable {
                                val newIndex = if (currentIndex > 0) {
                                    currentIndex - 1
                                } else {
                                    playlist.size - 1
                                }
                                onNavigate(newIndex)
                            }
                            .padding(4.dp)
                    )
                }

                // Track position
                AnimatedVisibility(
                    targetState = currentIndex,
                    enter = slideInHorizontally { width -> width } + fadeIn(),
                    exit = slideOutHorizontally { width -> -width } + fadeOut()
                ) { index ->
                    Text(
                        text = "${index + 1}/${playlist.size}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }

                // Next button
                if (playlist.size > 1) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowRight,
                        contentDescription = "Next Track",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .size(24.dp)
                            .clickable {
                                val newIndex = if (currentIndex < playlist.size - 1) {
                                    currentIndex + 1
                                } else {
                                    0
                                }
                                onNavigate(newIndex)
                            }
                            .padding(4.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun PlaylistFloatingButton(
    playlistSize: Int,
    isExpanded: Boolean,
    onExpandClick: () -> Unit,
    onViewPlaylistClick: () -> Unit,
    onAddToPlaylistClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .padding(16.dp)
    ) {
        AnimatedVisibility(
            visible = isExpanded,
            enter = slideInHorizontally() + fadeIn(),
            exit = slideOutHorizontally() + fadeOut()
        ) {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // View Playlist Button
                Row(
                    modifier = Modifier
                        .height(48.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .clickable(onClick = onViewPlaylistClick)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlaylistPlay,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )

                    Text(
                        text = "View Playlist ($playlistSize)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                // Add to Playlist Button
                Row(
                    modifier = Modifier
                        .height(48.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .clickable(onClick = onAddToPlaylistClick)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlaylistAdd,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )

                    Text(
                        text = "Add to Playlist",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        FloatingActionButton(
            onClick = onExpandClick,
            modifier = Modifier
                .align(if (isExpanded) Alignment.BottomEnd else Alignment.Center)
                .size(56.dp),
            containerColor = MaterialTheme.colorScheme.primary
        ) {
            Icon(
                imageVector = if (isExpanded) Icons.Default.Close else Icons.Default.PlaylistPlay,
                contentDescription = if (isExpanded) "Hide playlist options" else "Show playlist options",
                tint = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}

@Composable
fun AddToPlaylistDialog(
    track: Music,
    availablePlaylists: List<String>,
    onDismiss: () -> Unit,
    onAddToExisting: (String) -> Unit,
    onCreateNew: (String) -> Unit
) {
    var showNewPlaylistDialog by remember { mutableStateOf(false) }

    if (showNewPlaylistDialog) {
        SavePlaylistDialog(
            existingPlaylists = availablePlaylists,
            onDismiss = { showNewPlaylistDialog = false },
            onSave = { name ->
                onCreateNew(name)
                showNewPlaylistDialog = false
                onDismiss()
            }
        )
    } else {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Add to Playlist") },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 300.dp)
                ) {
                    Text(
                        text = "Add \"${track.title}\" to a playlist:",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    if (availablePlaylists.isEmpty()) {
                        Text(
                            text = "No playlists found. Create a new one.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            modifier = Modifier.padding(vertical = 16.dp)
                        )
                    } else {
                        LazyColumn {
                            items(availablePlaylists) { name ->
                                ListItem(
                                    headlineText = { Text(name) },
                                    leadingContent = {
                                        Icon(
                                            imageVector = Icons.Default.PlaylistPlay,
                                            contentDescription = null
                                        )
                                    },
                                    modifier = Modifier.clickable {
                                        onAddToExisting(name)
                                        onDismiss()
                                    }
                                )
                                Divider()
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showNewPlaylistDialog = true
                    }
                ) {
                    Text("Create New")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        )
    }
}
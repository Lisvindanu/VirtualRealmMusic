// app/src/main/java/com/virtualrealm/virtualrealmmusicplayer/ui/player/PlayerScreen.kt

package com.virtualrealm.virtualrealmmusicplayer.ui.player

import android.util.Log
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.NavigateBefore
import androidx.compose.material.icons.filled.NavigateNext
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberTooltipState
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
    musicViewModel: MusicViewModel = hiltViewModel()
) {
    val music by playerViewModel.music.collectAsState()
    val isFavorite by playerViewModel.isFavorite.collectAsState()
    val isLoading by playerViewModel.isLoading.collectAsState()
    val error by playerViewModel.error.collectAsState()

    val isPlaying by musicViewModel.isPlaying.collectAsState()
    val currentTrack by musicViewModel.currentTrack.collectAsState()
    val playlist by musicViewModel.playlist.collectAsState()
    val currentIndex by musicViewModel.currentIndex.collectAsState()

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
                    // Add tooltip to show playlist status
                    val playlistSize = playlist.size

                    Box {
                        IconButton(
                            onClick = onNavigateToPlaylist
                        ) {
                            BadgedBox(
                                badge = {
                                    if (playlistSize > 0) {
                                        Badge { Text("$playlistSize") }
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlaylistPlay,
                                    contentDescription = "View Playlist"
                                )
                            }
                        }

                        if (playlistSize > 0) {
                            TooltipBox(
                                positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                                tooltip = {
                                    PlainTooltip {
                                        Text("${currentIndex + 1} of $playlistSize in playlist")
                                    }
                                },
                                state = rememberTooltipState()
                            ) {
                                Box(Modifier.size(48.dp))
                            }
                        }
                    }
                }
            )
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
                        .clip(RoundedCornerShape(8.dp))
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
                        .clip(RoundedCornerShape(8.dp))
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
                        .clip(RoundedCornerShape(8.dp))
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
                        tint = if (playlist.size > 1) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                    )
                }

                IconButton(
                    onClick = onTogglePlayPause,
                    modifier = Modifier.size(64.dp)
                ) {
                    Icon(
                        painter = painterResource(
                            id = if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play
                        ),
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        modifier = Modifier.size(48.dp),
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
                        tint = if (playlist.size > 1) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                    )
                }
            }

            // Add advanced playlist navigator
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
            } else {
                // If no playlist or only one track, show the mini navigator
                MiniPlaylistNavigator(
                    playlist = playlist,
                    currentIndex = currentIndex,
                    onNavigate = { index ->
                        if (playlist.isNotEmpty()) {
                            // TODO: Implement this in MusicViewModel
                        }
                    },
                    onViewPlaylistClick = onNavigateToPlaylist,
                    modifier = Modifier.padding(vertical = 8.dp)
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
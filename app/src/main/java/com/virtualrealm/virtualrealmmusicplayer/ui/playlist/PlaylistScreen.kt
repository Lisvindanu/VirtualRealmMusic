package com.virtualrealm.virtualrealmmusicplayer.ui.playlist

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.virtualrealm.virtualrealmmusicplayer.R
import com.virtualrealm.virtualrealmmusicplayer.domain.model.Music
import com.virtualrealm.virtualrealmmusicplayer.ui.common.EmptyState
import com.virtualrealm.virtualrealmmusicplayer.ui.main.MainViewModel
import com.virtualrealm.virtualrealmmusicplayer.ui.player.MusicViewModel
import com.virtualrealm.virtualrealmmusicplayer.ui.theme.SpotifyGreen
import com.virtualrealm.virtualrealmmusicplayer.ui.theme.YouTubeRed
import com.virtualrealm.virtualrealmmusicplayer.util.getMusicType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.foundation.lazy.rememberLazyListState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun PlaylistScreen(
    onNavigateBack: () -> Unit,
    onNavigateToPlayer: (String, String) -> Unit,
    musicViewModel: MusicViewModel = hiltViewModel(),
    mainViewModel: MainViewModel = hiltViewModel()
) {
    val playlist by musicViewModel.playlist.collectAsState()
    val currentIndex by musicViewModel.currentIndex.collectAsState()
    val isPlaying by musicViewModel.isPlaying.collectAsState()
    val currentTrack by musicViewModel.currentTrack.collectAsState()

    // Dialogs state
    var showRemoveConfirmation by remember { mutableStateOf(false) }
    var trackToRemoveIndex by remember { mutableStateOf<Int?>(null) }
    var showClearConfirmation by remember { mutableStateOf(false) }
    var showSaveDialog by remember { mutableStateOf(false) }
    var showLoadDialog by remember { mutableStateOf(false) }
    var showOverwriteDialog by remember { mutableStateOf(false) }
    var playlistToOverwrite by remember { mutableStateOf("") }

    // UI state
    val savedPlaylists by mainViewModel.savedPlaylists.collectAsState()
    val lazyListState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val scaffoldState = remember { SnackbarHostState() }
    val animationsEnabled = remember { mutableStateOf(true) }

    // Scroll to current track when it changes
    LaunchedEffect(currentIndex) {
        if (playlist.isNotEmpty() && currentIndex in playlist.indices && animationsEnabled.value) {
            lazyListState.animateScrollToItem(
                index = currentIndex.coerceIn(0, playlist.lastIndex),
                scrollOffset = -100
            )
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(scaffoldState) },
        topBar = {
            TopAppBar(
                title = { Text("Playlist (${playlist.size})") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                actions = {
                    if (playlist.isNotEmpty()) {
                        PlaylistActions(
                            onSave = { showSaveDialog = true },
                            onLoad = { showLoadDialog = true },
                            onClear = { showClearConfirmation = true }
                        )
                    }
                }
            )
        },
        bottomBar = {
            if (currentTrack != null) {
                MiniPlayer(
                    track = currentTrack!!,
                    isPlaying = isPlaying,
                    onPlayerClick = {
                        onNavigateToPlayer(
                            currentTrack!!.id,
                            when (currentTrack) {
                                is Music.SpotifyTrack -> "spotify"
                                is Music.YoutubeVideo -> "youtube"
                                null -> TODO()
                            }
                        )
                    },
                    onPreviousClick = { musicViewModel.skipToPrevious() },
                    onPlayPauseClick = { musicViewModel.togglePlayPause() },
                    onNextClick = { musicViewModel.skipToNext() }
                )
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (playlist.isEmpty()) {
                EmptyState(
                    message = "Your playlist is empty",
                    actionLabel = "Add Music",
                    onActionClick = onNavigateBack
                )
            } else {
                PlaylistContent(
                    playlist = playlist,
                    currentIndex = currentIndex,
                    isPlaying = isPlaying,
                    animationsEnabled = animationsEnabled,
                    lazyListState = lazyListState,
                    onItemClick = { index, music ->
                        musicViewModel.setPlaylist(playlist, index)
                        onNavigateToPlayer(music.id, music.getMusicType())
                    },
                    onRemoveItem = { index ->
                        showRemoveConfirmation = true
                        trackToRemoveIndex = index
                    },
                    onMoveUp = { index ->
                        if (index > 0) moveItem(
                            playlist = playlist,
                            fromIndex = index,
                            toIndex = index - 1,
                            currentIndex = currentIndex,
                            musicViewModel = musicViewModel,
                            scope = scope,
                            animationsEnabled = animationsEnabled
                        )
                    },
                    onMoveDown = { index ->
                        if (index < playlist.size - 1) moveItem(
                            playlist = playlist,
                            fromIndex = index,
                            toIndex = index + 1,
                            currentIndex = currentIndex,
                            musicViewModel = musicViewModel,
                            scope = scope,
                            animationsEnabled = animationsEnabled
                        )
                    }
                )
            }
        }
    }

    // Show dialogs
    if (showRemoveConfirmation) {
        RemoveTrackDialog(
            trackName = trackToRemoveIndex?.let {
                if (it >= 0 && it < playlist.size) playlist[it].title else "this track"
            } ?: "this track",
            onConfirm = {
                val indexToRemove = trackToRemoveIndex
                if (indexToRemove != null && indexToRemove >= 0 && indexToRemove < playlist.size) {
                    animationsEnabled.value = false
                    musicViewModel.removeFromPlaylist(indexToRemove)
                    scope.launch {
                        delay(300)
                        animationsEnabled.value = true
                    }
                }
                showRemoveConfirmation = false
                trackToRemoveIndex = null
            },
            onDismiss = {
                showRemoveConfirmation = false
                trackToRemoveIndex = null
            }
        )
    }

    if (showClearConfirmation) {
        ClearPlaylistDialog(
            onConfirm = {
                musicViewModel.setPlaylist(emptyList())
                showClearConfirmation = false
            },
            onDismiss = { showClearConfirmation = false }
        )
    }

    if (showSaveDialog) {
        SavePlaylistDialog(
            existingPlaylists = savedPlaylists,
            onDismiss = { showSaveDialog = false },
            onSave = { name ->
                if (savedPlaylists.contains(name)) {
                    playlistToOverwrite = name
                    showOverwriteDialog = true
                } else {
                    savePlaylist(name, playlist, mainViewModel, scope, scaffoldState)
                    showSaveDialog = false
                }
            }
        )
    }

    if (showOverwriteDialog) {
        OverwritePlaylistDialog(
            playlistName = playlistToOverwrite,
            onConfirm = {
                savePlaylist(playlistToOverwrite, playlist, mainViewModel, scope, scaffoldState)
                showOverwriteDialog = false
                showSaveDialog = false
            },
            onDismiss = {
                showOverwriteDialog = false
                showSaveDialog = true
            }
        )
    }

    if (showLoadDialog) {
        LoadPlaylistDialog(
            availablePlaylists = savedPlaylists,
            onSelect = { name ->
                loadPlaylist(name, mainViewModel, musicViewModel, scope, scaffoldState)
                showLoadDialog = false
            },
            onDismiss = { showLoadDialog = false }
        )
    }
}

@Composable
private fun PlaylistActions(
    onSave: () -> Unit,
    onLoad: () -> Unit,
    onClear: () -> Unit
) {
    IconButton(onClick = onSave) {
        Icon(
            imageVector = Icons.Default.Save,
            contentDescription = "Save Playlist"
        )
    }
    IconButton(onClick = onLoad) {
        Icon(
            imageVector = Icons.Default.FolderOpen,
            contentDescription = "Load Playlist"
        )
    }
    IconButton(onClick = onClear) {
        Icon(
            imageVector = Icons.Default.ClearAll,
            contentDescription = "Clear Playlist"
        )
    }
}

@Composable
private fun MiniPlayer(
    track: Music,
    isPlaying: Boolean,
    onPlayerClick: () -> Unit,
    onPreviousClick: () -> Unit,
    onPlayPauseClick: () -> Unit,
    onNextClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .clickable(onClick = onPlayerClick),
        color = MaterialTheme.colorScheme.primaryContainer,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(track.thumbnailUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = track.title,
                modifier = Modifier
                    .size(48.dp)
                    .clip(MaterialTheme.shapes.small),
                contentScale = ContentScale.Crop
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp)
            ) {
                Text(
                    text = track.title,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = track.artists,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }

            IconButton(onClick = onPreviousClick) {
                Icon(
                    imageVector = Icons.Default.SkipPrevious,
                    contentDescription = "Previous"
                )
            }

            IconButton(onClick = onPlayPauseClick) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play"
                )
            }

            IconButton(onClick = onNextClick) {
                Icon(
                    imageVector = Icons.Default.SkipNext,
                    contentDescription = "Next"
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PlaylistContent(
    playlist: List<Music>,
    currentIndex: Int,
    isPlaying: Boolean,
    animationsEnabled: MutableState<Boolean>,
    lazyListState: LazyListState,
    onItemClick: (Int, Music) -> Unit,
    onRemoveItem: (Int) -> Unit,
    onMoveUp: (Int) -> Unit,
    onMoveDown: (Int) -> Unit
) {
    LazyColumn(
        state = lazyListState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        itemsIndexed(
            items = playlist,
            key = { index, _ -> "playlist_item_$index" }
        ) { index, music ->
            AnimatedVisibility(
                visible = true,
                enter = fadeIn(animationSpec = tween(200)),
                exit = fadeOut(animationSpec = tween(200))
            ) {
                PlaylistItemCard(
                    music = music,
                    index = index,
                    isPlaying = isPlaying && index == currentIndex,
                    onClick = { onItemClick(index, music) },
                    onRemove = { onRemoveItem(index) },
                    onMoveUp = { onMoveUp(index) },
                    onMoveDown = { onMoveDown(index) },
                    showUpButton = index > 0,
                    showDownButton = index < playlist.size - 1
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(64.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlaylistItemCard(
    music: Music,
    index: Int,
    isPlaying: Boolean,
    onClick: () -> Unit,
    onRemove: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    showUpButton: Boolean,
    showDownButton: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        onClick = onClick,
        colors = if (isPlaying) {
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
            )
        } else {
            CardDefaults.cardColors()
        },
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isPlaying) 4.dp else 1.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Index number
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(
                        color = if (isPlaying)
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                        else
                            MaterialTheme.colorScheme.surfaceVariant,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${index + 1}",
                    style = MaterialTheme.typography.titleSmall,
                    color = if (isPlaying)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Thumbnail
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(music.thumbnailUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = music.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .size(48.dp)
                    .clip(MaterialTheme.shapes.small)
            )

            // Track details
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp)
            ) {
                Text(
                    text = music.title,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = if (isPlaying) FontWeight.Bold else FontWeight.Normal
                )

                Text(
                    text = music.artists,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Source badge
                MusicSourceBadge(music)
            }

            // Control buttons
            PlaylistItemControls(
                isPlaying = isPlaying,
                showUpButton = showUpButton,
                showDownButton = showDownButton,
                onMoveUp = onMoveUp,
                onMoveDown = onMoveDown,
                onRemove = onRemove
            )
        }
    }
}

@Composable
private fun MusicSourceBadge(music: Music) {
    Box(
        modifier = Modifier
            .padding(top = 4.dp)
            .clip(MaterialTheme.shapes.extraSmall)
            .background(
                when (music) {
                    is Music.SpotifyTrack -> SpotifyGreen.copy(alpha = 0.2f)
                    is Music.YoutubeVideo -> YouTubeRed.copy(alpha = 0.2f)
                }
            )
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = when (music) {
                    is Music.SpotifyTrack -> Icons.Default.Audiotrack
                    is Music.YoutubeVideo -> Icons.Default.VideoLibrary
                },
                contentDescription = null,
                modifier = Modifier.size(12.dp),
                tint = when (music) {
                    is Music.SpotifyTrack -> SpotifyGreen
                    is Music.YoutubeVideo -> YouTubeRed
                }
            )

            Text(
                text = when (music) {
                    is Music.SpotifyTrack -> "Spotify"
                    is Music.YoutubeVideo -> "YouTube"
                },
                style = MaterialTheme.typography.labelSmall,
                color = when (music) {
                    is Music.SpotifyTrack -> SpotifyGreen
                    is Music.YoutubeVideo -> YouTubeRed
                }
            )
        }
    }
}

@Composable
private fun PlaylistItemControls(
    isPlaying: Boolean,
    showUpButton: Boolean,
    showDownButton: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onRemove: () -> Unit
) {
    // Currently playing indicator
    if (isPlaying) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .background(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.MusicNote,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
        }
    }

    // Move up/down buttons
    Column {
        if (showUpButton) {
            IconButton(
                onClick = onMoveUp,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowUp,
                    contentDescription = "Move Up",
                    modifier = Modifier.size(20.dp)
                )
            }
        } else {
            Spacer(modifier = Modifier.size(32.dp))
        }

        if (showDownButton) {
            IconButton(
                onClick = onMoveDown,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = "Move Down",
                    modifier = Modifier.size(20.dp)
                )
            }
        } else {
            Spacer(modifier = Modifier.size(32.dp))
        }
    }

    // Remove button
    IconButton(
        onClick = onRemove,
        colors = IconButtonDefaults.iconButtonColors(
            contentColor = MaterialTheme.colorScheme.error
        )
    ) {
        Icon(
            imageVector = Icons.Default.Delete,
            contentDescription = "Remove from playlist",
            tint = MaterialTheme.colorScheme.error
        )
    }
}

@Composable
private fun RemoveTrackDialog(
    trackName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Remove from Playlist") },
        text = { Text("Remove \"$trackName\" from playlist?") },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Remove")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun ClearPlaylistDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Clear Playlist") },
        text = { Text("Are you sure you want to clear the entire playlist?") },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Clear")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

// Helper functions
private fun moveItem(
    playlist: List<Music>,
    fromIndex: Int,
    toIndex: Int,
    currentIndex: Int,
    musicViewModel: MusicViewModel,
    scope: CoroutineScope,
    animationsEnabled: MutableState<Boolean>
) {
    animationsEnabled.value = false
    val newPlaylist = playlist.toMutableList()
    val item = newPlaylist.removeAt(fromIndex)
    newPlaylist.add(toIndex, item)

    val newCurrentIndex = when (currentIndex) {
        fromIndex -> toIndex  // Item being moved is current
        toIndex -> fromIndex  // Item at destination is current
        else -> currentIndex  // No change needed
    }

    musicViewModel.setPlaylist(newPlaylist, newCurrentIndex)
    scope.launch {
        delay(300)
        animationsEnabled.value = true
    }
}

private fun savePlaylist(
    name: String,
    playlist: List<Music>,
    mainViewModel: MainViewModel,
    scope: CoroutineScope,
    scaffoldState: SnackbarHostState
) {
    mainViewModel.saveCurrentPlaylist(name, playlist)
    scope.launch {
        scaffoldState.showSnackbar(
            message = "Playlist saved as '$name'",
            duration = SnackbarDuration.Short
        )
    }
}

private fun loadPlaylist(
    name: String,
    mainViewModel: MainViewModel,
    musicViewModel: MusicViewModel,
    scope: CoroutineScope,
    scaffoldState: SnackbarHostState
) {
    scope.launch {
        val loadedPlaylist = mainViewModel.getSavedPlaylist(name)
        if (loadedPlaylist != null && loadedPlaylist.isNotEmpty()) {
            musicViewModel.setPlaylist(loadedPlaylist)
            scaffoldState.showSnackbar(
                message = "Loaded playlist '$name'",
                duration = SnackbarDuration.Short
            )
        } else {
            scaffoldState.showSnackbar(
                message = "Failed to load playlist '$name'",
                duration = SnackbarDuration.Short
            )
        }
    }
}
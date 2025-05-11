// app/src/main/java/com/virtualrealm/virtualrealmmusicplayer/ui/playlist/PlaylistScreen.kt

package com.virtualrealm.virtualrealmmusicplayer.ui.playlist

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
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
import kotlinx.coroutines.launch


@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun PlaylistScreen(
    onNavigateBack: () -> Unit,
    onNavigateToPlayer: (String, String) -> Unit,
    musicViewModel: MusicViewModel = hiltViewModel()
) {
    val playlist by musicViewModel.playlist.collectAsState()
    val currentIndex by musicViewModel.currentIndex.collectAsState()
    val isPlaying by musicViewModel.isPlaying.collectAsState()
    val currentTrack by musicViewModel.currentTrack.collectAsState()

    var showRemoveConfirmation by remember { mutableStateOf(false) }
    var trackToRemoveIndex by remember { mutableStateOf<Int?>(null) }
    var showClearConfirmation by remember { mutableStateOf(false) }

    // Add state variables for dialogs
    var showSaveDialog by remember { mutableStateOf(false) }
    var showLoadDialog by remember { mutableStateOf(false) }
    var showOverwriteDialog by remember { mutableStateOf(false) }
    var playlistToOverwrite by remember { mutableStateOf("") }

    // Get saved playlists from MainViewModel
    val mainViewModel: MainViewModel = hiltViewModel()
    val savedPlaylists by mainViewModel.savedPlaylists.collectAsState()

    val lazyListState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    // State to track newly added items for animation
    val addedItems = remember { mutableStateMapOf<String, Boolean>() }

    // Add scaffoldState for snackbars
    val scaffoldState = remember { SnackbarHostState() }

    // Track changes to the playlist to update addedItems
    LaunchedEffect(playlist) {
        playlist.forEach { music ->
            if (!addedItems.containsKey(music.id)) {
                addedItems[music.id] = true
            }
        }
    }

    // Scroll to current track
    LaunchedEffect(currentIndex) {
        if (playlist.isNotEmpty() && currentIndex in playlist.indices) {
            scope.launch {
                lazyListState.animateScrollToItem(currentIndex)
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(scaffoldState) },
        topBar = {
            TopAppBar(
                title = { Text(text = "Playlist (${playlist.size})") },
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
                        // Save button
                        IconButton(onClick = { showSaveDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.Save,
                                contentDescription = "Save Playlist"
                            )
                        }

                        // Load button
                        IconButton(onClick = { showLoadDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.FolderOpen,
                                contentDescription = "Load Playlist"
                            )
                        }

                        // Clear playlist button
                        IconButton(onClick = { showClearConfirmation = true }) {
                            Icon(
                                imageVector = Icons.Default.ClearAll,
                                contentDescription = "Clear Playlist"
                            )
                        }
                    }
                }
            )
        },
        bottomBar = {
            if (currentTrack != null) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shadowElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Mini player
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(currentTrack?.thumbnailUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = currentTrack?.title,
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
                                text = currentTrack?.title ?: "",
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = currentTrack?.artists ?: "",
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }

                        // Playback controls
                        IconButton(onClick = { musicViewModel.skipToPrevious() }) {
                            Icon(
                                imageVector = Icons.Default.SkipPrevious,
                                contentDescription = "Previous"
                            )
                        }

                        IconButton(onClick = { musicViewModel.togglePlayPause() }) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Pause" else "Play"
                            )
                        }

                        IconButton(onClick = { musicViewModel.skipToNext() }) {
                            Icon(
                                imageVector = Icons.Default.SkipNext,
                                contentDescription = "Next"
                            )
                        }
                    }
                }
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
                    onActionClick = {
                        onNavigateBack()
                    }
                )
            } else {
                LazyColumn(
                    state = lazyListState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(
                        items = playlist,
                        key = { _, music -> music.id }
                    ) { index, music ->
                        val visible = remember { MutableTransitionState(false) }

                        LaunchedEffect(music.id) {
                            visible.targetState = true
                        }

                        // Animate new items
                        AnimatedVisibility(
                            visibleState = visible,
                            enter = slideInVertically(
                                initialOffsetY = { it }, // full height from bottom
                                animationSpec = tween(durationMillis = 300)
                            ) + fadeIn(animationSpec = tween(durationMillis = 300)),
                            exit = slideOutVertically(
                                targetOffsetY = { it },
                                animationSpec = tween(durationMillis = 300)
                            ) + fadeOut(animationSpec = tween(durationMillis = 300))
                        ) {
                            PlaylistItemCard(
                                music = music,
                                index = index,
                                isPlaying = isPlaying && index == currentIndex,
                                onClick = {
                                    // Play this track
                                    musicViewModel.setPlaylist(playlist, index)
                                    // Navigate to player
                                    onNavigateToPlayer(music.id,
                                        when (music) {
                                            is Music.SpotifyTrack -> "spotify"
                                            is Music.YoutubeVideo -> "youtube"
                                        }
                                    )
                                },
                                onRemove = {
                                    showRemoveConfirmation = true
                                    trackToRemoveIndex = index
                                },
                                onMoveUp = {
                                    if (index > 0) {
                                        val newPlaylist = playlist.toMutableList()
                                        val item = newPlaylist.removeAt(index)
                                        newPlaylist.add(index - 1, item)
                                        musicViewModel.setPlaylist(newPlaylist,
                                            if (currentIndex == index) index - 1
                                            else if (currentIndex == index - 1) index
                                            else currentIndex
                                        )
                                    }
                                },
                                onMoveDown = {
                                    if (index < playlist.size - 1) {
                                        val newPlaylist = playlist.toMutableList()
                                        val item = newPlaylist.removeAt(index)
                                        newPlaylist.add(index + 1, item)
                                        musicViewModel.setPlaylist(newPlaylist,
                                            if (currentIndex == index) index + 1
                                            else if (currentIndex == index + 1) index
                                            else currentIndex
                                        )
                                    }
                                },
                                showUpButton = index > 0,
                                showDownButton = index < playlist.size - 1
                            )
                        }
                    }

                    // Add some space at the bottom for mini player
                    item {
                        Spacer(modifier = Modifier.height(64.dp))
                    }
                }
            }
        }
    }

    // Remove track confirmation dialog
    if (showRemoveConfirmation) {
        AlertDialog(
            onDismissRequest = {
                showRemoveConfirmation = false
                trackToRemoveIndex = null
            },
            title = { Text("Remove from Playlist") },
            text = {
                Text(
                    "Remove \"${trackToRemoveIndex?.let { playlist.getOrNull(it)?.title } ?: ""}\" from playlist?"
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        trackToRemoveIndex?.let {
                            musicViewModel.removeFromPlaylist(it)
                        }
                        showRemoveConfirmation = false
                        trackToRemoveIndex = null
                    }
                ) {
                    Text("Remove")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showRemoveConfirmation = false
                        trackToRemoveIndex = null
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // Clear playlist confirmation dialog
    if (showClearConfirmation) {
        AlertDialog(
            onDismissRequest = { showClearConfirmation = false },
            title = { Text("Clear Playlist") },
            text = { Text("Are you sure you want to clear the entire playlist?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        musicViewModel.setPlaylist(emptyList())
                        showClearConfirmation = false
                    }
                ) {
                    Text("Clear")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmation = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Save playlist dialog
    if (showSaveDialog) {
        SavePlaylistDialog(
            existingPlaylists = savedPlaylists,
            onDismiss = { showSaveDialog = false },
            onSave = { name ->
                if (savedPlaylists.contains(name)) {
                    // Ask for confirmation to overwrite
                    playlistToOverwrite = name
                    showOverwriteDialog = true
                } else {
                    // Save new playlist
                    mainViewModel.saveCurrentPlaylist(name, playlist)
                    // Show confirmation
                    scope.launch {
                        scaffoldState.showSnackbar(
                            message = "Playlist saved as '$name'",
                            duration = SnackbarDuration.Short
                        )
                    }
                }
            }
        )
    }

    // Overwrite playlist confirmation
    if (showOverwriteDialog) {
        OverwritePlaylistDialog(
            playlistName = playlistToOverwrite,
            onConfirm = {
                // Overwrite the playlist
                mainViewModel.saveCurrentPlaylist(playlistToOverwrite, playlist)
                // Show confirmation
                scope.launch {
                    scaffoldState.showSnackbar(
                        message = "Playlist '$playlistToOverwrite' updated",
                        duration = SnackbarDuration.Short
                    )
                }
            },
            onDismiss = {
                showOverwriteDialog = false
                // Go back to save dialog
                showSaveDialog = true
            }
        )
    }

    // Load playlist dialog
    if (showLoadDialog) {
        LoadPlaylistDialog(
            availablePlaylists = savedPlaylists,
            onSelect = { name ->
                // Load the selected playlist
                scope.launch {
                    val loadedPlaylist = mainViewModel.getSavedPlaylist(name)
                    if (loadedPlaylist != null && loadedPlaylist.isNotEmpty()) {
                        // Set the loaded playlist
                        musicViewModel.setPlaylist(loadedPlaylist)
                        // Show confirmation
                        scaffoldState.showSnackbar(
                            message = "Loaded playlist '$name'",
                            duration = SnackbarDuration.Short
                        )
                    } else {
                        // Show error
                        scaffoldState.showSnackbar(
                            message = "Failed to load playlist '$name'",
                            duration = SnackbarDuration.Short
                        )
                    }
                }
                showLoadDialog = false
            },
            onDismiss = { showLoadDialog = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistItemCard(
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
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            )
        } else {
            CardDefaults.cardColors()
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Index number
            Text(
                text = "${index + 1}",
                style = MaterialTheme.typography.titleSmall,
                color = if (isPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                modifier = Modifier
                    .size(24.dp)
                    .background(
                        color = if (isPlaying) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else Color.Transparent,
                        shape = MaterialTheme.shapes.small
                    )
                    .padding(4.dp)
            )

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
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = music.artists,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Source badge
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

            // Currently playing indicator
            if (isPlaying) {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(8.dp)
                )
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
            IconButton(onClick = onRemove) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Remove from playlist",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
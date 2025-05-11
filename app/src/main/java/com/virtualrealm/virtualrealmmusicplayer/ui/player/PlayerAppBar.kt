package com.virtualrealm.virtualrealmmusicplayer.ui.player

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.virtualrealm.virtualrealmmusicplayer.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerAppBar(
    onNavigateBack: () -> Unit,
    onNavigateToPlaylist: () -> Unit,
    playlistSize: Int
) {
    TopAppBar(
        title = { Text(stringResource(R.string.now_playing)) },
        navigationIcon = {
            IconButton(onClick = onNavigateBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.back)
                )
            }
        },
        actions = {
            IconButton(onClick = onNavigateToPlaylist) {
                BadgedBox(
                    badge = {
                        if (playlistSize > 0)
                            Badge { Text("$playlistSize") }
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
}
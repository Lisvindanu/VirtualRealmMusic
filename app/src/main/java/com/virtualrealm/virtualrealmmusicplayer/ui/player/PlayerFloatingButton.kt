package com.virtualrealm.virtualrealmmusicplayer.ui.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun PlayerFloatingButton(
    playlistSize: Int,
    isExpanded: Boolean,
    onExpandClick: () -> Unit,
    onViewPlaylistClick: () -> Unit,
    onAddToPlaylistClick: () -> Unit
) {
    Box(
        modifier = Modifier.padding(bottom = 16.dp, end = 16.dp)
    ) {
        // Expanded options
        AnimatedVisibility(
            visible = isExpanded,
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = tween(durationMillis = 300)
            ) + fadeIn(
                animationSpec = tween(durationMillis = 300)
            ),
            exit = slideOutVertically(
                targetOffsetY = { it },
                animationSpec = tween(durationMillis = 150)
            ) + fadeOut(
                animationSpec = tween(durationMillis = 150)
            )
        ) {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 68.dp)
            ) {
                FloatingActionButton(
                    onClick = onViewPlaylistClick,
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.List,
                        contentDescription = "View Playlist"
                    )
                }

                FloatingActionButton(
                    onClick = onAddToPlaylistClick,
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlaylistAdd,
                        contentDescription = "Add to Playlist"
                    )
                }
            }
        }

        // Main button
        FloatingActionButton(
            onClick = onExpandClick,
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = if (isExpanded) 16.dp else 0.dp)
            ) {
                if (playlistSize > 0 && !isExpanded) {
                    Badge { Text(text = playlistSize.toString()) }
                }

                Icon(
                    imageVector = if (isExpanded) Icons.Default.Close else Icons.Default.PlaylistPlay,
                    contentDescription = if (isExpanded) "Close" else "Playlist Options"
                )
            }
        }
    }
}
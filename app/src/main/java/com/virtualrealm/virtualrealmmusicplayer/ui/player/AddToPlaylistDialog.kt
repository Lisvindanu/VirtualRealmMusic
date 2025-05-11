package com.virtualrealm.virtualrealmmusicplayer.ui.player

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.virtualrealm.virtualrealmmusicplayer.domain.model.Music
import com.virtualrealm.virtualrealmmusicplayer.ui.playlist.SavePlaylistDialog

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
                        text = "Select a playlist to add:",
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
                                    headlineContent = { Text(name) },
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
                                HorizontalDivider()
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showNewPlaylistDialog = true }) {
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
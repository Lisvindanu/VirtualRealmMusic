// app/src/main/java/com/virtualrealm/virtualrealmmusicplayer/ui/playlist/OverwritePlaylistDialog.kt

package com.virtualrealm.virtualrealmmusicplayer.ui.playlist

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable

@Composable
fun OverwritePlaylistDialog(
    playlistName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Overwrite Playlist") },
        text = { Text("A playlist named '$playlistName' already exists. Do you want to overwrite it?") },
        confirmButton = {
            TextButton(onClick = {
                onConfirm()
                onDismiss()
            }) {
                Text("Overwrite")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
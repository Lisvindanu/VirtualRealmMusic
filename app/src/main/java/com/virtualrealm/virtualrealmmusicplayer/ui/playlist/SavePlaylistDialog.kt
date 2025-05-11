// app/src/main/java/com/virtualrealm/virtualrealmmusicplayer/ui/playlist/SavePlaylistDialog.kt

package com.virtualrealm.virtualrealmmusicplayer.ui.playlist

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavePlaylistDialog(
    existingPlaylists: List<String>,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    var playlistName by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(dismissOnClickOutside = true),
        title = { Text("Save Playlist") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                OutlinedTextField(
                    value = playlistName,
                    onValueChange = {
                        playlistName = it
                        showError = false
                    },
                    label = { Text("Playlist Name") },
                    isError = showError,
                    supportingText = if (showError) {
                        { Text("Please enter a valid playlist name") }
                    } else null,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            keyboardController?.hide()
                            if (playlistName.isNotBlank()) {
                                onSave(playlistName)
                            } else {
                                showError = true
                            }
                        }
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                )

                if (existingPlaylists.isNotEmpty()) {
                    Text(
                        text = "Existing playlists:",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                    )
                    existingPlaylists.take(5).forEach { name ->
                        SuggestionChip(
                            onClick = {
                                playlistName = name
                                showError = false
                            },
                            label = { Text(name) },
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }

                    if (existingPlaylists.size > 5) {
                        Text(
                            text = "... and ${existingPlaylists.size - 5} more",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (playlistName.isNotBlank()) {
                        onSave(playlistName)
                    } else {
                        showError = true
                    }
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
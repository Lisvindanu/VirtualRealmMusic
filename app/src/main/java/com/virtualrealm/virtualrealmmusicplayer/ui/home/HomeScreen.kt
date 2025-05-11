// app/src/main/java/com/virtualrealm/virtualrealmmusicplayer/ui/home/HomeScreen.kt
package com.virtualrealm.virtualrealmmusicplayer.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.virtualrealm.virtualrealmmusicplayer.R
import com.virtualrealm.virtualrealmmusicplayer.domain.model.Music
import com.virtualrealm.virtualrealmmusicplayer.ui.common.EmptyState
import com.virtualrealm.virtualrealmmusicplayer.ui.common.MusicItem
import com.virtualrealm.virtualrealmmusicplayer.util.getMusicType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToPlayer: (String, String) -> Unit,
    onNavigateToSearch: () -> Unit,
    onNavigateToPlaylist: () -> Unit, // Added this parameter
    onLogout: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val favorites by viewModel.favorites.collectAsState()
    val authState by viewModel.authState.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.home)) },
                actions = {
                    // Add playlist button
                    IconButton(onClick = onNavigateToPlaylist) {
                        Icon(
                            imageVector = Icons.Default.PlaylistPlay,
                            contentDescription = stringResource(id = R.string.view_playlist)
                        )
                    }
                    // Logout button
                    IconButton(onClick = { viewModel.logout() }) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = stringResource(R.string.logout))
                    }
                }
            )
        },
        // Button Mengambang
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToSearch,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.search))
            }
        }

    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Auth status
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = if (authState?.isAuthenticated == true) {
                                stringResource(R.string.spotify_connected)
                            } else {
                                stringResource(R.string.spotify_not_connected)
                            },
                            style = MaterialTheme.typography.titleMedium
                        )

                        if (authState?.isAuthenticated != true) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = onLogout // This will navigate to login screen
                            ) {
                                Text(text = stringResource(R.string.connect_spotify))
                            }
                        }
                    }
                }

                // Favorites
                if (favorites.isEmpty()) {
                    EmptyState(
                        message = stringResource(R.string.no_favorites),
                        actionLabel = stringResource(R.string.search_music),
                        onActionClick = onNavigateToSearch
                    )
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(favorites) { music ->
                            MusicItem(
                                music = music,
                                onClick = {
                                    onNavigateToPlayer(music.id, music.getMusicType())
                                },
                                onFavoriteClick = {
                                    viewModel.toggleFavorite(music)
                                },
                                isFavorite = true
                            )
                        }
                    }
                }
            }

            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }
}
// app/src/main/java/com/virtualrealm/virtualrealmmusicplayer/ui/search/SearchScreen.kt
package com.virtualrealm.virtualrealmmusicplayer.ui.search

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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.virtualrealm.virtualrealmmusicplayer.R
import com.virtualrealm.virtualrealmmusicplayer.domain.model.Music
import com.virtualrealm.virtualrealmmusicplayer.domain.model.Resource
import com.virtualrealm.virtualrealmmusicplayer.domain.usecase.music.SearchMusicUseCase
import com.virtualrealm.virtualrealmmusicplayer.ui.common.EmptyState
import com.virtualrealm.virtualrealmmusicplayer.ui.common.ErrorState
import com.virtualrealm.virtualrealmmusicplayer.ui.common.MusicItem
import com.virtualrealm.virtualrealmmusicplayer.util.getMusicType

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun SearchScreen(
    onNavigateToPlayer: (String, String) -> Unit,
    viewModel: SearchViewModel = hiltViewModel()
) {
    val searchResults by viewModel.searchResults.collectAsState()
    val authState by viewModel.authState.collectAsState()
    val selectedSource by viewModel.selectedSource.collectAsState()
    val currentQuery by viewModel.currentQuery.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    val keyboardController = LocalSoftwareKeyboardController.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.search)) }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Search field
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text(text = stringResource(R.string.search_hint)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = stringResource(R.string.clear))
                        }
                    } else {
                        Icon(Icons.Default.Search, contentDescription = null)
                    }
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(
                    onSearch = {
                        viewModel.searchMusic(searchQuery)
                        keyboardController?.hide()
                    }
                ),
                singleLine = true
            )

            // Source selector
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    RadioButton(
                        selected = selectedSource == SearchMusicUseCase.MusicSource.YOUTUBE,
                        onClick = { viewModel.setMusicSource(SearchMusicUseCase.MusicSource.YOUTUBE) }
                    )
                    Text(
                        text = stringResource(R.string.youtube),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    RadioButton(
                        selected = selectedSource == SearchMusicUseCase.MusicSource.SPOTIFY,
                        onClick = { viewModel.setMusicSource(SearchMusicUseCase.MusicSource.SPOTIFY) },
                        enabled = authState?.isAuthenticated == true
                    )
                    Text(
                        text = stringResource(R.string.spotify),
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (authState?.isAuthenticated == true)
                            MaterialTheme.colorScheme.onSurface
                        else
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    )
                }
            }

            // Spotify authentication warning
            if (selectedSource == SearchMusicUseCase.MusicSource.SPOTIFY && authState?.isAuthenticated != true) {
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
                            text = stringResource(R.string.spotify_not_connected_message),
                            style = MaterialTheme.typography.bodyMedium
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Button(
                            onClick = { /* Navigate to login */ }
                        ) {
                            Text(text = stringResource(R.string.connect_spotify))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Search button
            Button(
                onClick = {
                    viewModel.searchMusic(searchQuery)
                    keyboardController?.hide()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                enabled = searchQuery.isNotBlank()
            ) {
                Text(text = stringResource(R.string.search))
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Search results
            Box(modifier = Modifier.weight(1f)) {
                when (searchResults) {
                    is Resource.Loading -> {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                    is Resource.Error -> {
                        ErrorState(
                            message = (searchResults as Resource.Error).message,
                            onRetry = {
                                if (currentQuery.isNotBlank()) {
                                    viewModel.searchMusic(currentQuery)
                                }
                            }
                        )
                    }
                    is Resource.Success -> {
                        val music = (searchResults as Resource.Success<List<Music>>).data
                        if (music.isEmpty() && currentQuery.isNotBlank()) {
                            EmptyState(
                                message = stringResource(R.string.no_results_found),
                                actionLabel = null
                            )
                        } else if (music.isEmpty() && currentQuery.isBlank()) {
                            EmptyState(
                                message = stringResource(R.string.search_prompt),
                                actionLabel = null
                            )
                        } else {
                            LazyColumn(
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(music) { item ->
                                    MusicItem(
                                        music = item,
                                        onClick = {
                                            onNavigateToPlayer(item.id, item.getMusicType())
                                        },
                                        onFavoriteClick = {
                                            viewModel.toggleFavorite(item)
                                        },
                                        isFavorite = false // TODO: Check if in favorites
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
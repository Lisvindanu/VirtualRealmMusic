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
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.virtualrealm.virtualrealmmusicplayer.R
import com.virtualrealm.virtualrealmmusicplayer.domain.model.Music
import com.virtualrealm.virtualrealmmusicplayer.domain.model.Resource
import com.virtualrealm.virtualrealmmusicplayer.domain.usecase.music.SearchMusicUseCase
import com.virtualrealm.virtualrealmmusicplayer.ui.common.EmptyState
import com.virtualrealm.virtualrealmmusicplayer.ui.common.ErrorState
import com.virtualrealm.virtualrealmmusicplayer.ui.common.MusicItem
import com.virtualrealm.virtualrealmmusicplayer.util.getMusicType
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Snackbar
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.flow.collectLatest
import com.virtualrealm.virtualrealmmusicplayer.domain.model.AuthState

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
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collectLatest { event ->
            when (event) {
                is UiEvent.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(event.message)
                }
            }
        }
    }


    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.search)) }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
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
                label = {
                    Text(
                        text = stringResource(R.string.search_hint),
                        fontSize = 12.sp
                    )
                },
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
                ToggleMusicSourceButton(
                    selectedSource = selectedSource,
                    authState = authState,
                    onSelect = { viewModel.setMusicSource(it) }
                )
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

enum class SelectionType { SINGLE }

data class ToggleOption(val text: String, val icon: ImageVector, val source: SearchMusicUseCase.MusicSource)

@Composable
fun ToggleMusicSourceButton(
    selectedSource: SearchMusicUseCase.MusicSource,
    authState: AuthState?,
    onSelect: (SearchMusicUseCase.MusicSource) -> Unit
) {
    val options = listOf(
        ToggleOption("YouTube", Icons.Default.PlayArrow, SearchMusicUseCase.MusicSource.YOUTUBE),
        ToggleOption("Spotify", Icons.Default.Star, SearchMusicUseCase.MusicSource.SPOTIFY)
    )

    val selected = remember { mutableStateMapOf<String, ToggleOption>() }

    Row(Modifier.padding(0.dp).height(52.dp)) {
        options.forEachIndexed { i, opt ->
            val isSelected = selectedSource == opt.source
            val isEnabled = opt.source != SearchMusicUseCase.MusicSource.SPOTIFY || authState?.isAuthenticated == true

            Button(
                onClick = {
                    if (isEnabled) {
                        selected.clear()
                        selected[opt.text] = opt
                        onSelect(opt.source)
                    }
                },
                shape = RoundedCornerShape(6.dp),
                elevation = ButtonDefaults.buttonElevation(0.dp),
                contentPadding = PaddingValues(horizontal = 12.dp),
                colors = ButtonDefaults.buttonColors(MaterialTheme.colorScheme.background),
                modifier = Modifier
                    .padding(end = if (i < options.size - 1) 4.dp else 0.dp)
                    .weight(1f),
                enabled = isEnabled
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val selectedColor = when (opt.source) {
                        SearchMusicUseCase.MusicSource.YOUTUBE -> Color.Red
                        SearchMusicUseCase.MusicSource.SPOTIFY -> Color(0xFF1DB954) // warna hijau Spotify
                    }

                    val textAndIconColor = if (isSelected) selectedColor else Color.LightGray

                    Text(
                        opt.text,
                        color = textAndIconColor
                    )
                    Icon(
                        opt.icon,
                        contentDescription = null,
                        tint = textAndIconColor,
                        modifier = Modifier.padding(start = 8.dp)
                    )

                }
            }
        }
    }
}

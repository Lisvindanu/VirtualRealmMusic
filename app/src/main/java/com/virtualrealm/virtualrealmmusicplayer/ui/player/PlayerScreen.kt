// Update PlayerScreen.kt to fix YouTube player references
package com.virtualrealm.virtualrealmmusicplayer.ui.player

import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.google.accompanist.web.WebView
import com.google.accompanist.web.rememberWebViewState
import com.virtualrealm.virtualrealmmusicplayer.R
import com.virtualrealm.virtualrealmmusicplayer.domain.model.Music
import com.virtualrealm.virtualrealmmusicplayer.ui.common.ErrorState
import com.virtualrealm.virtualrealmmusicplayer.ui.common.SourceTag
import com.virtualrealm.virtualrealmmusicplayer.ui.theme.SpotifyGreen
import com.virtualrealm.virtualrealmmusicplayer.ui.theme.YouTubeRed
import com.virtualrealm.virtualrealmmusicplayer.util.Constants
import com.virtualrealm.virtualrealmmusicplayer.util.DateTimeUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    musicId: String,
    musicType: String,
    onNavigateBack: () -> Unit,
    viewModel: PlayerViewModel = hiltViewModel()
) {
    val music by viewModel.music.collectAsState()
    val isFavorite by viewModel.isFavorite.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    // Load the music when the screen is first composed
    LaunchedEffect(musicId, musicType) {
        viewModel.loadMusic(musicId, musicType)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.now_playing)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            } else if (error != null) {
                ErrorState(
                    message = error ?: stringResource(R.string.unknown_error),
                    onRetry = {
                        viewModel.loadMusic(musicId, musicType)
                    }
                )
            } else {
                music?.let { music ->
                    PlayerContent(
                        music = music,
                        isFavorite = isFavorite,
                        onToggleFavorite = {
                            viewModel.toggleFavorite()
                        },
                        musicType = musicType
                    )
                }
            }
        }
    }
}

@Composable
fun PlayerContent(
    music: Music,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    musicType: String
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Music player
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.5f)
        ) {
            when (musicType) {
                Constants.MUSIC_TYPE_YOUTUBE -> {
                    SimpleYouTubePlayer(videoId = music.id)
                }
                Constants.MUSIC_TYPE_SPOTIFY -> {
                    SpotifyPlayer(trackId = music.id)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Music details
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.5f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Album art
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(music.thumbnailUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = music.title,
                contentScale = ContentScale.Crop,
                placeholder = painterResource(id = R.drawable.placeholder_album),
                error = painterResource(id = R.drawable.placeholder_album),
                modifier = Modifier
                    .size(200.dp)
                    .clip(RoundedCornerShape(8.dp))
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Song title
            Text(
                text = music.title,
                style = MaterialTheme.typography.titleLarge,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Artist
            Text(
                text = music.artists,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Additional info
            when (music) {
                is Music.SpotifyTrack -> {
                    Text(
                        text = "${music.albumName} · ${DateTimeUtils.formatDuration(music.durationMs)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    SourceTag(
                        text = "Spotify",
                        contentColor = SpotifyGreen
                    )
                }
                is Music.YoutubeVideo -> {
                    Text(
                        text = music.channelTitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    SourceTag(
                        text = "YouTube",
                        contentColor = YouTubeRed
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Favorite button
            IconButton(
                onClick = onToggleFavorite,
                modifier = Modifier.size(56.dp)
            ) {
                Icon(
                    imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = if (isFavorite)
                        stringResource(R.string.remove_from_favorites)
                    else
                        stringResource(R.string.add_to_favorites),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}

// Simple replacement for YouTubePlayerView
@Composable
fun SimpleYouTubePlayer(videoId: String) {
    val webViewState = rememberWebViewState(url = "https://www.youtube.com/embed/$videoId")

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
    ) {
        WebView(
            state = webViewState,
            onCreated = { webView ->
                webView.settings.javaScriptEnabled = true
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
fun SpotifyPlayer(trackId: String) {
    val spotifyEmbedUrl = "https://open.spotify.com/embed/track/$trackId"
    val webViewState = rememberWebViewState(url = spotifyEmbedUrl)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
    ) {
        WebView(
            state = webViewState,
            onCreated = { webView ->
                webView.settings.javaScriptEnabled = true
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}
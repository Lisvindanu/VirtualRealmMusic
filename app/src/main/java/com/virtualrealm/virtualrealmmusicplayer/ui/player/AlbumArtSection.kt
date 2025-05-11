package com.virtualrealm.virtualrealmmusicplayer.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.virtualrealm.virtualrealmmusicplayer.R
import com.virtualrealm.virtualrealmmusicplayer.domain.model.Music
import com.virtualrealm.virtualrealmmusicplayer.ui.theme.SpotifyGreen
import com.virtualrealm.virtualrealmmusicplayer.ui.theme.YouTubeRed
import com.virtualrealm.virtualrealmmusicplayer.util.Constants

@Composable
fun AlbumArtSection(
    music: Music,
    musicType: String,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        when (musicType) {
            Constants.MUSIC_TYPE_YOUTUBE -> {
                // YouTube video thumbnail
                val thumbnailUrl = "https://img.youtube.com/vi/${music.id}/hqdefault.jpg"
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(thumbnailUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = music.title,
                    contentScale = ContentScale.Crop,
                    placeholder = painterResource(id = R.drawable.placeholder_album),
                    error = painterResource(id = R.drawable.placeholder_album),
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(12.dp))
                )

                // YouTube play icon overlay
                SourceOverlayIcon(
                    iconRes = R.drawable.ic_youtube,
                    tint = YouTubeRed
                )
            }
            Constants.MUSIC_TYPE_SPOTIFY -> {
                // Spotify album art
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
                        .fillMaxSize()
                        .clip(RoundedCornerShape(12.dp))
                )

                // Spotify icon overlay
                SourceOverlayIcon(
                    iconRes = R.drawable.ic_spotify,
                    tint = SpotifyGreen
                )
            }
            else -> {
                // Generic music thumbnail
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
                        .fillMaxSize()
                        .clip(RoundedCornerShape(12.dp))
                )

                if (music.thumbnailUrl.isEmpty()) {
                    // Show music icon if thumbnail is empty
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(64.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SourceOverlayIcon(
    iconRes: Int,
    tint: Color
) {
    Box(
        modifier = Modifier
            .size(80.dp)
            // Remove the problematic align modifier
            .background(
                color = Color.Black.copy(alpha = 0.6f),
                shape = RoundedCornerShape(8.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(40.dp)
        )
    }
}
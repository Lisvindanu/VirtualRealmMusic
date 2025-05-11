package com.virtualrealm.virtualrealmmusicplayer.ui.player

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.virtualrealm.virtualrealmmusicplayer.R
import com.virtualrealm.virtualrealmmusicplayer.domain.model.Music
import com.virtualrealm.virtualrealmmusicplayer.ui.common.SourceTag
import com.virtualrealm.virtualrealmmusicplayer.ui.theme.SpotifyGreen
import com.virtualrealm.virtualrealmmusicplayer.ui.theme.YouTubeRed
import com.virtualrealm.virtualrealmmusicplayer.util.DateTimeUtils

@Composable
fun MusicInfoSection(
    music: Music,
    isFavorite: Boolean,
    isPlaying: Boolean,
    onToggleFavorite: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    onSeekTo: (Float) -> Unit,
    currentPosition: Float,
    duration: Long,
    playlist: List<Music>,
    currentIndex: Int,
    onNavigateToPlaylist: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Song title
        Text(
            text = music.title,
            style = MaterialTheme.typography.titleLarge,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Artist
        Text(
            text = music.artists,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Additional info based on music type
        when (music) {
            is Music.SpotifyTrack -> {
                Text(
                    text = "${music.albumName} · ${DateTimeUtils.formatDuration(music.durationMs)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
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
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                SourceTag(
                    text = "YouTube",
                    contentColor = YouTubeRed
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Playback progress slider
        PlaybackSlider(
            currentPosition = currentPosition,
            duration = duration,
            onSeekTo = onSeekTo
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Playback controls
        PlaybackControls(
            isPlaying = isPlaying,
            playlistSize = playlist.size,
            onSkipPrevious = onSkipPrevious,
            onTogglePlayPause = onTogglePlayPause,
            onSkipNext = onSkipNext
        )

        // Playlist navigator - using the renamed TrackNavigationControls
        if (playlist.isNotEmpty() && playlist.size > 1) {
            TrackNavigationControls(
                playlist = playlist,
                currentIndex = currentIndex,
                onPreviousClick = onSkipPrevious,
                onNextClick = onSkipNext,
                onViewPlaylistClick = onNavigateToPlaylist,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Favorite button
        IconButton(
            onClick = onToggleFavorite,
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                contentDescription = if (isFavorite)
                    stringResource(R.string.remove_from_favorites)
                else
                    stringResource(R.string.add_to_favorites),
                tint = if (isFavorite) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
        }
    }
}
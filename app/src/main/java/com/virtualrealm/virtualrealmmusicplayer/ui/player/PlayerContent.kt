package com.virtualrealm.virtualrealmmusicplayer.ui.player

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.virtualrealm.virtualrealmmusicplayer.domain.model.Music

@Composable
fun PlayerContent(
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
    musicType: String,
    playlist: List<Music>,
    currentIndex: Int,
    onNavigateToPlaylist: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Album art section
        AlbumArtSection(
            music = music,
            musicType = musicType,
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.5f)
                .padding(bottom = 16.dp)
        )

        // Music info section
        MusicInfoSection(
            music = music,
            isFavorite = isFavorite,
            isPlaying = isPlaying,
            onToggleFavorite = onToggleFavorite,
            onTogglePlayPause = onTogglePlayPause,
            onSkipNext = onSkipNext,
            onSkipPrevious = onSkipPrevious,
            onSeekTo = onSeekTo,
            currentPosition = currentPosition,
            duration = duration,
            playlist = playlist,
            currentIndex = currentIndex,
            onNavigateToPlaylist = onNavigateToPlaylist,
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.5f)
        )
    }
}
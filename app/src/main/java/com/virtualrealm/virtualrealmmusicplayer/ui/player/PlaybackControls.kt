package com.virtualrealm.virtualrealmmusicplayer.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.virtualrealm.virtualrealmmusicplayer.R
import com.virtualrealm.virtualrealmmusicplayer.util.DateTimeUtils

@Composable
fun PlaybackControls(
    isPlaying: Boolean,
    playlistSize: Int,
    onSkipPrevious: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onSkipNext: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onSkipPrevious,
            modifier = Modifier.size(48.dp),
            enabled = playlistSize > 1
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_previous),
                contentDescription = "Previous",
                modifier = Modifier.size(32.dp),
                tint = if (playlistSize > 1)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
            )
        }

        // Play/Pause button with visual feedback
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer)
                .clickable(onClick = onTogglePlayPause),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(
                    id = if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play
                ),
                contentDescription = if (isPlaying) "Pause" else "Play",
                modifier = Modifier.size(36.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }

        IconButton(
            onClick = onSkipNext,
            modifier = Modifier.size(48.dp),
            enabled = playlistSize > 1
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_next),
                contentDescription = "Next",
                modifier = Modifier.size(32.dp),
                tint = if (playlistSize > 1)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
fun PlaybackSlider(
    currentPosition: Float,
    duration: Long,
    onSeekTo: (Float) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Slider(
            value = currentPosition,
            onValueChange = { onSeekTo(it) },
            modifier = Modifier.fillMaxWidth()
        )

        // Timestamp display
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Current position
            Text(
                text = DateTimeUtils.formatDuration((currentPosition * duration).toLong()),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )

            // Total duration
            Text(
                text = DateTimeUtils.formatDuration(duration),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}
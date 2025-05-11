package com.virtualrealm.virtualrealmmusicplayer.ui.player

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.virtualrealm.virtualrealmmusicplayer.domain.model.Music

/**
 * A composable that displays navigation controls for a playlist
 *
 * NOTE: Function name changed from PlaylistNavigator to TrackNavigationControls
 * to avoid conflicts with another function with the same name in the codebase
 */
@Composable
fun TrackNavigationControls(
    playlist: List<Music>,
    currentIndex: Int,
    onPreviousClick: () -> Unit,
    onNextClick: () -> Unit,
    onViewPlaylistClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Don't show if playlist is empty or has only one item
    if (playlist.size <= 1) return

    val prevTrack = if (currentIndex > 0) {
        playlist[currentIndex - 1]
    } else if (playlist.size > 1) {
        // Wrap around to the last track
        playlist[playlist.size - 1]
    } else null

    val nextTrack = if (currentIndex < playlist.size - 1) {
        playlist[currentIndex + 1]
    } else if (playlist.size > 1) {
        // Wrap around to the first track
        playlist[0]
    } else null

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Playlist position indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Track ${currentIndex + 1} of ${playlist.size}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.width(4.dp))

                // Playlist icon button
                IconButton(
                    onClick = onViewPlaylistClick,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlaylistPlay,
                        contentDescription = "View Playlist",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Progress indicator
            LinearProgressIndicator(
                progress = { (currentIndex + 1).toFloat() / playlist.size.toFloat() },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .height(4.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
            )

            // Navigation controls with track previews
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Previous track preview
                if (prevTrack != null) {
                    PrevTrackPreview(
                        track = prevTrack,
                        onClick = onPreviousClick,
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }

                // Spacer in the middle
                Spacer(modifier = Modifier.width(8.dp))

                // Next track preview
                if (nextTrack != null) {
                    NextTrackPreview(
                        track = nextTrack,
                        onClick = onNextClick,
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

/**
 * Preview for the previous track
 */
@Composable
private fun PrevTrackPreview(
    track: Music,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(MaterialTheme.shapes.small)
            .clickable(onClick = onClick)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        Icon(
            imageVector = Icons.Default.KeyboardArrowLeft,
            contentDescription = "Previous",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(16.dp)
        )

        Column(
            modifier = Modifier
                .padding(horizontal = 4.dp)
                .weight(1f),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = "Previous",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Start
            )
            Text(
                text = track.title,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Start
            )
        }
    }
}

/**
 * Preview for the next track
 */
@Composable
private fun NextTrackPreview(
    track: Music,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(MaterialTheme.shapes.small)
            .clickable(onClick = onClick)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.End
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 4.dp)
                .weight(1f),
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = "Next",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.End
            )
            Text(
                text = track.title,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.End
            )
        }

        Icon(
            imageVector = Icons.Default.KeyboardArrowRight,
            contentDescription = "Next",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(16.dp)
        )
    }
}
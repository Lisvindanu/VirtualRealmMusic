// app/src/main/java/com/virtualrealm/virtualrealmmusicplayer/ui/playlist/PlaylistNavigator.kt

package com.virtualrealm.virtualrealmmusicplayer.ui.playlist

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
 * A compact playlist navigator component that shows the previous, current, and next tracks
 * and allows navigation between them.
 */
@Composable
fun PlaylistNavigator(
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

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        // Playlist position indicator
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Track ${currentIndex + 1} of ${playlist.size}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )

            Spacer(modifier = Modifier.width(4.dp))

            // Playlist icon button
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable(onClick = onViewPlaylistClick)
                    .padding(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PlaylistPlay,
                    contentDescription = "View Playlist",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Navigation controls with track previews
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Previous track preview
            prevTrack?.let { track ->
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(MaterialTheme.shapes.small)
                        .clickable(onClick = onPreviousClick)
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowLeft,
                        contentDescription = "Previous",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )

                    Column(
                        modifier = Modifier
                            .padding(start = 4.dp)
                            .weight(1f)
                    ) {
                        Text(
                            text = "Previous",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = track.title,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            } ?: Spacer(modifier = Modifier.weight(1f))

            // Spacer in the middle
            Spacer(modifier = Modifier.width(8.dp))

            // Next track preview
            nextTrack?.let { track ->
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(MaterialTheme.shapes.small)
                        .clickable(onClick = onNextClick)
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End
                ) {
                    Column(
                        modifier = Modifier
                            .padding(end = 4.dp)
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
            } ?: Spacer(modifier = Modifier.weight(1f))
        }
    }
}

/**
 * A simple mini playlist navigator that can be integrated into the player screen.
 */
@Composable
fun MiniPlaylistNavigator(
    playlist: List<Music>,
    currentIndex: Int,
    onNavigate: (Int) -> Unit,
    onViewPlaylistClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (playlist.isEmpty()) return

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Current track info with navigation arrows
        Box(
            modifier = Modifier
                .clip(MaterialTheme.shapes.medium)
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
                .clickable(onClick = onViewPlaylistClick)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                // Previous button
                if (playlist.size > 1) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowLeft,
                        contentDescription = "Previous Track",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .size(24.dp)
                            .clickable {
                                val newIndex = if (currentIndex > 0) {
                                    currentIndex - 1
                                } else {
                                    playlist.size - 1
                                }
                                onNavigate(newIndex)
                            }
                            .padding(4.dp)
                    )
                }

                // Track position
                AnimatedContent(
                    targetState = currentIndex,
                    transitionSpec = {
                        (slideInHorizontally { width -> width } + fadeIn()).togetherWith(
                            slideOutHorizontally { width -> -width } + fadeOut()
                        )
                    },
                    label = "Track Position Animation"
                ) { index ->
                    Text(
                        text = "${index + 1}/${playlist.size}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }

                // Next button
                if (playlist.size > 1) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowRight,
                        contentDescription = "Next Track",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .size(24.dp)
                            .clickable {
                                val newIndex = if (currentIndex < playlist.size - 1) {
                                    currentIndex + 1
                                } else {
                                    0
                                }
                                onNavigate(newIndex)
                            }
                            .padding(4.dp)
                    )
                }
            }
        }
    }
}
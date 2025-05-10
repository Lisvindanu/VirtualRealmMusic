// app/src/main/java/com/virtualrealm/virtualrealmmusicplayer/service/MusicExtractionService.kt
package com.virtualrealm.virtualrealmmusicplayer.service

import android.util.Log
import com.virtualrealm.virtualrealmmusicplayer.domain.model.Music
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MusicExtractionService @Inject constructor() {

    suspend fun extractAudioUrl(music: Music): String = withContext(Dispatchers.IO) {
        try {
            Log.d("MusicExtractionService", "Extracting audio URL for: ${music.id} - ${music.title}")

            when (music) {
                is Music.SpotifyTrack -> {
                    // For Spotify, return the Spotify URI to be used by SpotifyPlayerManager
                    "spotify://${music.uri}"
                }
                is Music.YoutubeVideo -> {
                    // Return URL for WebView, not MediaPlayer
                    val videoId = music.id
                    "youtube://$videoId"
                }
            }
        } catch (e: Exception) {
            Log.e("MusicExtractionService", "Error extracting audio: ${e.message}", e)
            // Fallback to test URL if failed
            "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3"
        }
    }

    suspend fun getHighQualityArtUrl(music: Music): String = withContext(Dispatchers.IO) {
        try {
            when (music) {
                is Music.SpotifyTrack -> {
                    // Try to get better album art for Spotify tracks
                    music.thumbnailUrl.takeIf { it.isNotEmpty() }
                        ?: "https://via.placeholder.com/400x400?text=Spotify"
                }
                is Music.YoutubeVideo -> {
                    // Use high quality YouTube thumbnail
                    val videoId = music.id
                    "https://img.youtube.com/vi/$videoId/hqdefault.jpg"
                }
            }
        } catch (e: Exception) {
            Log.e("MusicExtractionService", "Error getting high quality art: ${e.message}", e)
            music.thumbnailUrl.takeIf { it.isNotEmpty() }
                ?: "https://i.ytimg.com/vi/default/hqdefault.jpg"
        }
    }
}
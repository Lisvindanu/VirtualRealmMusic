// app/src/main/java/com/virtualrealm/virtualrealmmusicplayer/service/MusicExtractionService.kt

package com.virtualrealm.virtualrealmmusicplayer.service

import android.util.Log
import com.virtualrealm.virtualrealmmusicplayer.domain.model.Music
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MusicExtractionService @Inject constructor(
    private val youtubeAudioHelper: YoutubeAudioHelper
) {
    suspend fun extractAudioUrl(music: Music): String = withContext(Dispatchers.IO) {
        try {
            Log.d("MusicExtractionService", "Extracting audio URL for: ${music.id} - ${music.title}")

            when (music) {
                is Music.SpotifyTrack -> {
                    // Kode untuk Spotify tetap sama
                    "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3"
                }
                is Music.YoutubeVideo -> {
                    // Gunakan YoutubeAudioHelper untuk mendapatkan URL audio
                    val videoId = music.id
                    val audioUrl = youtubeAudioHelper.getAudioUrlFromYoutube(videoId)

                    if (!audioUrl.isNullOrEmpty()) {
                        Log.d("MusicExtractionService", "Got YouTube audio URL: $audioUrl")
                        audioUrl
                    } else {
                        // Fallback ke URL langsung
                        val fallbackUrl = "https://invidious.sethforprivacy.com/latest_version?id=$videoId&itag=140&local=true"
                        Log.d("MusicExtractionService", "Using fallback YouTube URL: $fallbackUrl")
                        fallbackUrl
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("MusicExtractionService", "Error extracting audio: ${e.message}", e)
            // Fallback ke URL test jika gagal
            "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3"
        }
    }

    suspend fun getHighQualityArtUrl(music: Music): String = withContext(Dispatchers.IO) {
        try {
            when (music) {
                is Music.SpotifyTrack -> {
                    music.thumbnailUrl.takeIf { it.isNotEmpty() }
                        ?: "https://via.placeholder.com/400x400?text=Spotify"
                }
                is Music.YoutubeVideo -> {
                    // Gunakan thumbnail YouTube berkualitas tinggi
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
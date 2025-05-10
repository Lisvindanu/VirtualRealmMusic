// app/src/main/java/com/virtualrealm/virtualrealmmusicplayer/data/repository/MusicRepositoryImpl.kt

package com.virtualrealm.virtualrealmmusicplayer.data.repository

import android.util.Log
import com.virtualrealm.virtualrealmmusicplayer.data.local.cache.LocalCacheManager
import com.virtualrealm.virtualrealmmusicplayer.data.local.dao.MusicDao
import com.virtualrealm.virtualrealmmusicplayer.data.local.entity.MusicEntity
import com.virtualrealm.virtualrealmmusicplayer.data.remote.api.SpotifyApi
import com.virtualrealm.virtualrealmmusicplayer.data.remote.api.YouTubeApi
import com.virtualrealm.virtualrealmmusicplayer.data.util.NetworkConnectivityHelper
import com.virtualrealm.virtualrealmmusicplayer.domain.model.Music
import com.virtualrealm.virtualrealmmusicplayer.domain.model.Resource
import com.virtualrealm.virtualrealmmusicplayer.domain.repository.MusicRepository
import com.virtualrealm.virtualrealmmusicplayer.util.ApiCredentials
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MusicRepositoryImpl @Inject constructor(
    private val musicDao: MusicDao,
    private val spotifyApi: SpotifyApi,
    private val youtubeApi: YouTubeApi,
    private val networkConnectivityHelper: NetworkConnectivityHelper,
    private val localCacheManager: LocalCacheManager
) : MusicRepository {

    override suspend fun searchSpotifyTracks(query: String, accessToken: String): Flow<Resource<List<Music.SpotifyTrack>>> = flow {
        try {
            Log.d("MusicRepository", "Searching Spotify tracks: $query")
            emit(Resource.Loading)

            if (!networkConnectivityHelper.isNetworkAvailable()) {
                Log.w("MusicRepository", "Network not available for Spotify search")
                emit(Resource.Error("No internet connection available"))
                return@flow
            }

            // Make sure we're using the proper authorization format
            val authHeader = "Bearer $accessToken"
            Log.d("MusicRepository", "Using auth header: $authHeader (length: ${authHeader.length})")

            val response = spotifyApi.search(
                query = query,
                authorization = authHeader
            )
            val tracks = response.tracks.items.map { it.toSpotifyTrack() }

            Log.d("MusicRepository", "Found ${tracks.size} Spotify tracks")

            // Cache the results for offline access
            withContext(Dispatchers.IO) {
                tracks.forEach { track ->
                    saveLocalMusic(track)
                }
            }

            emit(Resource.Success(tracks))
        } catch (e: Exception) {
            Log.e("MusicRepository", "Failed to search Spotify tracks: ${e.message}", e)
            emit(Resource.Error("Failed to search Spotify tracks: ${e.message}", e))
        }
    }

    override suspend fun searchYoutubeVideos(query: String): Flow<Resource<List<Music.YoutubeVideo>>> = flow {
        try {
            Log.d("MusicRepository", "Searching YouTube videos: $query")
            emit(Resource.Loading)

            if (!networkConnectivityHelper.isNetworkAvailable()) {
                Log.w("MusicRepository", "Network not available for YouTube search")
                emit(Resource.Error("No internet connection available"))
                return@flow
            }

            val response = youtubeApi.searchVideos(
                query = query,
                apiKey = ApiCredentials.YOUTUBE_API_KEY
            )
            val videos = response.items.mapNotNull { it.toYouTubeVideo() }

            Log.d("MusicRepository", "Found ${videos.size} YouTube videos")

            // Cache the results for offline access
            withContext(Dispatchers.IO) {
                videos.forEach { video ->
                    saveLocalMusic(video)
                }
            }

            emit(Resource.Success(videos))
        } catch (e: Exception) {
            Log.e("MusicRepository", "Failed to search YouTube videos: ${e.message}", e)
            emit(Resource.Error("Failed to search YouTube videos: ${e.message}", e))
        }
    }

    override fun getFavorites(): Flow<List<Music>> {
        return musicDao.getFavorites().map { entities ->
            entities.map { it.toMusic() }
        }
    }

    override suspend fun addToFavorites(music: Music) {
        try {
            Log.d("MusicRepository", "Adding to favorites: ${music.title}")
            val entity = when (music) {
                is Music.SpotifyTrack -> MusicEntity.fromSpotifyTrack(music, true)
                is Music.YoutubeVideo -> MusicEntity.fromYoutubeVideo(music, true)
            }
            musicDao.insertMusic(entity)
        } catch (e: Exception) {
            Log.e("MusicRepository", "Error adding to favorites: ${e.message}", e)
        }
    }

    override suspend fun removeFromFavorites(music: Music) {
        try {
            Log.d("MusicRepository", "Removing from favorites: ${music.title}")
            musicDao.removeFromFavorites(music.id)
        } catch (e: Exception) {
            Log.e("MusicRepository", "Error removing from favorites: ${e.message}", e)
        }
    }

    override suspend fun isInFavorites(musicId: String): Boolean {
        return try {
            musicDao.isInFavorites(musicId)
        } catch (e: Exception) {
            Log.e("MusicRepository", "Error checking favorites status: ${e.message}", e)
            false
        }
    }

    // New methods for local cache
    override suspend fun getLocalMusicById(id: String): Music? {
        return try {
            Log.d("MusicRepository", "Getting local music: $id")
            val entity = musicDao.getMusicById(id)
            if (entity != null) {
                Log.d("MusicRepository", "Found local music: ${entity.title}")
                entity.toMusic()
            } else {
                Log.d("MusicRepository", "Local music not found: $id")
                null
            }
        } catch (e: Exception) {
            Log.e("MusicRepository", "Error retrieving local music: ${e.message}", e)
            null
        }
    }

    override suspend fun saveLocalMusic(music: Music) {
        try {
            Log.d("MusicRepository", "Saving local music: ${music.title}")
            val entity = when (music) {
                is Music.SpotifyTrack -> MusicEntity.fromSpotifyTrack(music, isInFavorites(music.id))
                is Music.YoutubeVideo -> MusicEntity.fromYoutubeVideo(music, isInFavorites(music.id))
            }
            musicDao.insertMusic(entity)
        } catch (e: Exception) {
            Log.e("MusicRepository", "Error saving local music: ${e.message}", e)
        }
    }

    override suspend fun getYoutubeVideoDetails(videoId: String): Music.YoutubeVideo? {
        try {
            Log.d("MusicRepository", "Getting YouTube video details: $videoId")

            // Check network connectivity first
            if (!networkConnectivityHelper.isNetworkAvailable()) {
                Log.w("MusicRepository", "No network available for video details")
                return null
            }

            // Use a direct API call to get video details
            val response = youtubeApi.getVideoDetails(
                id = videoId,
                part = "snippet,contentDetails",
                apiKey = ApiCredentials.YOUTUBE_API_KEY  // <-- Perbaikan nama parameter
            )

            if (response.items.isEmpty()) {
                Log.w("MusicRepository", "No video found with ID: $videoId")
                return null
            }

            val video = response.items.firstOrNull()?.toYouTubeVideo()

            // If we got video details, save it to local cache
            if (video != null) {
                Log.d("MusicRepository", "Received video details: ${video.title}")
                saveLocalMusic(video)
                return video
            } else {
                Log.w("MusicRepository", "Failed to convert video details to domain model")
                return null
            }
        } catch (e: Exception) {
            Log.e("MusicRepository", "Error fetching video details: ${e.message}", e)
            return null
        }
    }

    override suspend fun getSpotifyTrackDetails(trackId: String, accessToken: String): Music.SpotifyTrack? {
        try {
            Log.d("MusicRepository", "Getting Spotify track details: $trackId")

            // Check network connectivity first
            if (!networkConnectivityHelper.isNetworkAvailable()) {
                Log.w("MusicRepository", "No network available for track details")
                return null
            }

            // Implement Spotify track details fetching
            // This is a placeholder - you'd need to add this method to your SpotifyApi interface
            // val response = spotifyApi.getTrack(trackId, "Bearer $accessToken")
            // val track = response.toSpotifyTrack()

            // For now, return null
            Log.w("MusicRepository", "Spotify track details not implemented yet")
            return null

        } catch (e: Exception) {
            Log.e("MusicRepository", "Error fetching track details: ${e.message}", e)
            return null
        }
    }

    override suspend fun clearAllCache() {
        try {
            Log.d("MusicRepository", "Clearing all cache")
            musicDao.clearAll()
        } catch (e: Exception) {
            Log.e("MusicRepository", "Error clearing cache: ${e.message}", e)
        }
    }

    override suspend fun clearFavorites() {
        try {
            Log.d("MusicRepository", "Clearing favorites")
            musicDao.clearFavorites()
        } catch (e: Exception) {
            Log.e("MusicRepository", "Error clearing favorites: ${e.message}", e)
        }
    }

    override suspend fun getMusicByType(type: String): List<Music> {
        return try {
            Log.d("MusicRepository", "Getting music by type: $type")
            val entities = musicDao.getMusicByType(type)
            entities.map { it.toMusic() }
        } catch (e: Exception) {
            Log.e("MusicRepository", "Error getting music by type: ${e.message}", e)
            emptyList()
        }
    }
}
// data/repository/MusicRepositoryImpl.kt
package com.virtualrealm.virtualrealmmusicplayer.data.repository

import com.virtualrealm.virtualrealmmusicplayer.BuildConfig
import com.virtualrealm.virtualrealmmusicplayer.data.local.dao.MusicDao
import com.virtualrealm.virtualrealmmusicplayer.data.local.entity.MusicEntity
import com.virtualrealm.virtualrealmmusicplayer.data.remote.api.SpotifyApi
import com.virtualrealm.virtualrealmmusicplayer.data.remote.api.YouTubeApi
import com.virtualrealm.virtualrealmmusicplayer.domain.model.Music
import com.virtualrealm.virtualrealmmusicplayer.domain.model.Resource
import com.virtualrealm.virtualrealmmusicplayer.domain.repository.MusicRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MusicRepositoryImpl @Inject constructor(
    private val musicDao: MusicDao,
    private val spotifyApi: SpotifyApi,
    private val youtubeApi: YouTubeApi
) : MusicRepository {

    override suspend fun searchSpotifyTracks(query: String, accessToken: String): Flow<Resource<List<Music.SpotifyTrack>>> = flow {
        try {
            emit(Resource.Loading)
            val response = spotifyApi.search(query, authorization = "Bearer $accessToken")
            val tracks = response.tracks.items.map { it.toSpotifyTrack() }
            emit(Resource.Success(tracks))
        } catch (e: Exception) {
            emit(Resource.Error("Failed to search Spotify tracks: ${e.message}", e))
        }
    }

    override suspend fun searchYoutubeVideos(query: String): Flow<Resource<List<Music.YoutubeVideo>>> = flow {
        try {
            emit(Resource.Loading)
            val response = youtubeApi.searchVideos(
                query = query,
                apiKey = BuildConfig.YOUTUBE_API_KEY
            )
            val videos = response.items.mapNotNull { it.toYouTubeVideo() }
            emit(Resource.Success(videos))
        } catch (e: Exception) {
            emit(Resource.Error("Failed to search YouTube videos: ${e.message}", e))
        }
    }

    override suspend fun getFavorites(): Flow<List<Music>> {
        return musicDao.getFavorites().map { entities ->
            entities.map { it.toMusic() }
        }
    }

    override suspend fun addToFavorites(music: Music) {
        val entity = when (music) {
            is Music.SpotifyTrack -> MusicEntity.fromSpotifyTrack(music, true)
            is Music.YoutubeVideo -> MusicEntity.fromYoutubeVideo(music, true)
        }
        musicDao.insertMusic(entity)
    }

    override suspend fun removeFromFavorites(music: Music) {
        musicDao.removeFromFavorites(music.id)
    }

    override suspend fun isInFavorites(musicId: String): Boolean {
        return musicDao.isInFavorites(musicId)
    }
}
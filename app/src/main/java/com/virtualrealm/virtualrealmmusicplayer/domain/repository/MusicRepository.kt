// app/src/main/java/com/virtualrealm/virtualrealmmusicplayer/domain/repository/MusicRepository.kt

package com.virtualrealm.virtualrealmmusicplayer.domain.repository

import com.virtualrealm.virtualrealmmusicplayer.domain.model.Music
import com.virtualrealm.virtualrealmmusicplayer.domain.model.Resource
import kotlinx.coroutines.flow.Flow

interface MusicRepository {
    suspend fun searchSpotifyTracks(query: String, accessToken: String): Flow<Resource<List<Music.SpotifyTrack>>>
    suspend fun searchYoutubeVideos(query: String): Flow<Resource<List<Music.YoutubeVideo>>>
    fun getFavorites(): Flow<List<Music>>
    suspend fun addToFavorites(music: Music)
    suspend fun removeFromFavorites(music: Music)
    suspend fun isInFavorites(musicId: String): Boolean

    // Local cache methods
    suspend fun getLocalMusicById(id: String): Music?
    suspend fun saveLocalMusic(music: Music)
    suspend fun getYoutubeVideoDetails(videoId: String): Music.YoutubeVideo?
    suspend fun getSpotifyTrackDetails(trackId: String, accessToken: String): Music.SpotifyTrack?
    suspend fun clearAllCache()
    suspend fun clearFavorites()
    suspend fun getMusicByType(type: String): List<Music>
}
// domain/repository/MusicRepository.kt
package com.virtualrealm.virtualrealmmusicplayer.domain.repository

import com.virtualrealm.virtualrealmmusicplayer.domain.model.Music
import com.virtualrealm.virtualrealmmusicplayer.domain.model.Resource
import kotlinx.coroutines.flow.Flow

interface MusicRepository {
    suspend fun searchSpotifyTracks(query: String, accessToken: String): Flow<Resource<List<Music.SpotifyTrack>>>
    suspend fun searchYoutubeVideos(query: String): Flow<Resource<List<Music.YoutubeVideo>>>
    suspend fun getFavorites(): Flow<List<Music>>
    suspend fun addToFavorites(music: Music)
    suspend fun removeFromFavorites(music: Music)
    suspend fun isInFavorites(musicId: String): Boolean
}

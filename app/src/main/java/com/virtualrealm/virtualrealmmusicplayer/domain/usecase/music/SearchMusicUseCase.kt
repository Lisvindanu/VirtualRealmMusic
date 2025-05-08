// domain/usecase/music/SearchMusicUseCase.kt
package com.virtualrealm.virtualrealmmusicplayer.domain.usecase.music

import com.virtualrealm.virtualrealmmusicplayer.domain.model.AuthState
import com.virtualrealm.virtualrealmmusicplayer.domain.model.Music
import com.virtualrealm.virtualrealmmusicplayer.domain.model.Resource
import com.virtualrealm.virtualrealmmusicplayer.domain.repository.AuthRepository
import com.virtualrealm.virtualrealmmusicplayer.domain.repository.MusicRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class SearchMusicUseCase @Inject constructor(
    private val musicRepository: MusicRepository,
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(query: String, source: MusicSource): Flow<Resource<List<Music>>> = flow {
        emit(Resource.Loading)

        when (source) {
            MusicSource.YOUTUBE -> {
                musicRepository.searchYoutubeVideos(query).collect { resource ->
                    when (resource) {
                        is Resource.Success -> emit(Resource.Success(resource.data))
                        is Resource.Error -> emit(Resource.Error(resource.message, resource.throwable))
                        Resource.Loading -> { /* Already emitted above */ }
                    }
                }
            }
            MusicSource.SPOTIFY -> {
                val authState = authRepository.getAuthState().first()
                if (authState.isAuthenticated && !authState.accessToken.isNullOrEmpty()) {
                    musicRepository.searchSpotifyTracks(query, authState.accessToken).collect { resource ->
                        when (resource) {
                            is Resource.Success -> emit(Resource.Success(resource.data))
                            is Resource.Error -> emit(Resource.Error(resource.message, resource.throwable))
                            Resource.Loading -> { /* Already emitted above */ }
                        }
                    }
                } else {
                    emit(Resource.Error("Not authenticated with Spotify"))
                }
            }
        }
    }

    enum class MusicSource {
        YOUTUBE, SPOTIFY
    }
}

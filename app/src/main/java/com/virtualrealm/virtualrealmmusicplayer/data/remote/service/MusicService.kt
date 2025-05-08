// app/src/main/java/com/virtualrealm/virtualrealmmusicplayer/data/remote/service/MusicService.kt
package com.virtualrealm.virtualrealmmusicplayer.data.remote.service

import com.spotify.sdk.android.auth.BuildConfig
import com.virtualrealm.virtualrealmmusicplayer.data.remote.api.SpotifyApi
import com.virtualrealm.virtualrealmmusicplayer.data.remote.api.YouTubeApi
import com.virtualrealm.virtualrealmmusicplayer.data.remote.dto.SpotifySearchResponse
import com.virtualrealm.virtualrealmmusicplayer.data.remote.dto.YouTubeSearchResponse
import com.virtualrealm.virtualrealmmusicplayer.data.util.NetworkConnectivityHelper
import com.virtualrealm.virtualrealmmusicplayer.data.util.safeApiCall
import com.virtualrealm.virtualrealmmusicplayer.domain.model.Resource
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service class that handles direct API calls to music providers.
 */
@Singleton
class MusicService @Inject constructor(
    private val spotifyApi: SpotifyApi,
    private val youtubeApi: YouTubeApi,
    private val networkConnectivityHelper: NetworkConnectivityHelper
) {

    suspend fun searchSpotifyTracks(
        query: String,
        accessToken: String
    ): Resource<SpotifySearchResponse> {
        if (!networkConnectivityHelper.isNetworkAvailable()) {
            return Resource.Error("No internet connection available")
        }

        return safeApiCall {
            spotifyApi.search(
                query = query,
                authorization = "Bearer $accessToken"
            )
        }
    }

    suspend fun searchYouTubeVideos(query: String): Resource<YouTubeSearchResponse> {
        if (!networkConnectivityHelper.isNetworkAvailable()) {
            return Resource.Error("No internet connection available")
        }

        return safeApiCall {
            youtubeApi.searchVideos(
                query = query,
                apiKey = BuildConfig.YOUTUBE_API_KEY
            )
        }
    }
}

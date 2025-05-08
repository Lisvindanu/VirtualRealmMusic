// app/src/main/java/com/virtualrealm/virtualrealmmusicplayer/data/remote/api/YouTubeApi.kt

package com.virtualrealm.virtualrealmmusicplayer.data.remote.api

import com.virtualrealm.virtualrealmmusicplayer.data.remote.dto.YouTubeSearchResponse
import com.virtualrealm.virtualrealmmusicplayer.data.remote.dto.YouTubeVideoDetailsResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface YouTubeApi {
    @GET("youtube/v3/search")
    suspend fun searchVideos(
        @Query("q") query: String,
        @Query("part") part: String = "snippet",
        @Query("maxResults") maxResults: Int = 20,
        @Query("type") type: String = "video",
        @Query("videoCategoryId") videoCategoryId: String = "10", // Music category
        @Query("key") apiKey: String
    ): YouTubeSearchResponse

    @GET("youtube/v3/videos")
    suspend fun getVideoDetails(
        @Query("id") id: String,
        @Query("part") part: String = "snippet,contentDetails",
        @Query("key") apiKey: String
    ): YouTubeVideoDetailsResponse
}
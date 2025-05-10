// app/src/main/java/com/virtualrealm/virtualrealmmusicplayer/data/remote/api/SpotifyApi.kt
package com.virtualrealm.virtualrealmmusicplayer.data.remote.api

import com.virtualrealm.virtualrealmmusicplayer.data.remote.dto.SpotifySearchResponse
import retrofit2.http.*

interface SpotifyApi {
    @GET("v1/search")
    suspend fun search(
        @Query("q") query: String,
        @Query("type") type: String = "track",
        @Query("limit") limit: Int = 20,
        @Header("Authorization") authorization: String
    ): SpotifySearchResponse
}
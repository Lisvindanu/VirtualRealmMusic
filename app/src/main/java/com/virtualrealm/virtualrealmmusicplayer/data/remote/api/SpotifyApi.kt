// data/remote/api/SpotifyApi.kt
package com.virtualrealm.virtualrealmmusicplayer.data.remote.api

import com.virtualrealm.virtualrealmmusicplayer.data.remote.dto.SpotifySearchResponse
import com.virtualrealm.virtualrealmmusicplayer.data.remote.dto.SpotifyTokenResponse
import retrofit2.http.*

interface SpotifyApi {
    @FormUrlEncoded
    @POST("api/token")
    suspend fun getToken(
        @Field("grant_type") grantType: String,
        @Field("code") code: String,
        @Field("redirect_uri") redirectUri: String,
        @Field("client_id") clientId: String,
        @Field("client_secret") clientSecret: String
    ): SpotifyTokenResponse

    @FormUrlEncoded
    @POST("api/token")
    suspend fun refreshToken(
        @Field("grant_type") grantType: String = "refresh_token",
        @Field("refresh_token") refreshToken: String,
        @Field("client_id") clientId: String,
        @Field("client_secret") clientSecret: String
    ): SpotifyTokenResponse

    @GET("v1/search")
    suspend fun search(
        @Query("q") query: String,
        @Query("type") type: String = "track",
        @Query("limit") limit: Int = 20,
        @Header("Authorization") authorization: String
    ): SpotifySearchResponse
}

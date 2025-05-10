// Create a new file: app/src/main/java/com/virtualrealm/virtualrealmmusicplayer/data/remote/api/SpotifyAuthApi.kt
package com.virtualrealm.virtualrealmmusicplayer.data.remote.api

import com.virtualrealm.virtualrealmmusicplayer.data.remote.dto.SpotifyTokenResponse
import retrofit2.http.*

interface SpotifyAuthApi {
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
}
// File: app/src/main/java/com/virtualrealm/virtualrealmmusicplayer/data/remote/service/AuthAuthenticator.kt
package com.virtualrealm.virtualrealmmusicplayer.data.remote.service

import com.virtualrealm.virtualrealmmusicplayer.data.local.preferences.AuthPreferences
import com.virtualrealm.virtualrealmmusicplayer.data.remote.api.SpotifyApi
import com.virtualrealm.virtualrealmmusicplayer.util.ApiCredentials
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthAuthenticator @Inject constructor(
    private val spotifyApi: SpotifyApi,
    private val authPreferences: AuthPreferences
) : Authenticator {

    override fun authenticate(route: Route?, response: Response): Request? {
        // Check if the request was already authenticated
        if (response.request.header("Authorization") == null) {
            return null
        }

        // Only attempt to refresh token once
        if (response.request.header("X-Refresh-Attempt") != null) {
            return null
        }

        return runBlocking {
            val authState = authPreferences.authStateFlow.first()

            if (!authState.isAuthenticated || authState.refreshToken.isNullOrEmpty()) {
                return@runBlocking null
            }

            try {
                val tokenResponse = spotifyApi.refreshToken(
                    refreshToken = authState.refreshToken,
                    clientId = ApiCredentials.SPOTIFY_CLIENT_ID,
                    clientSecret = ApiCredentials.SPOTIFY_CLIENT_SECRET
                )

                // Update auth state
                authPreferences.saveAuthState(
                    authState.copy(
                        accessToken = tokenResponse.accessToken,
                        refreshToken = tokenResponse.refreshToken ?: authState.refreshToken,
                        expiresIn = tokenResponse.expiresIn,
                        tokenType = tokenResponse.tokenType
                    )
                )

                // Create a new request with the new token
                response.request.newBuilder()
                    .header("Authorization", "${tokenResponse.tokenType} ${tokenResponse.accessToken}")
                    .header("X-Refresh-Attempt", "1")
                    .build()
            } catch (e: Exception) {
                null
            }
        }
    }
}
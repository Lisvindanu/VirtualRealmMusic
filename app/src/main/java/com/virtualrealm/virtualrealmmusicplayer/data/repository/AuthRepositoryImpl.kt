// File: app/src/main/java/com/virtualrealm/virtualrealmmusicplayer/data/repository/AuthRepositoryImpl.kt
package com.virtualrealm.virtualrealmmusicplayer.data.repository

import android.content.Intent
import android.net.Uri
import com.virtualrealm.virtualrealmmusicplayer.data.auth.SpotifyAuthHandler
import com.virtualrealm.virtualrealmmusicplayer.data.local.preferences.AuthPreferences
import com.virtualrealm.virtualrealmmusicplayer.data.remote.api.SpotifyApi
import com.virtualrealm.virtualrealmmusicplayer.domain.model.AuthState
import com.virtualrealm.virtualrealmmusicplayer.domain.model.Resource
import com.virtualrealm.virtualrealmmusicplayer.domain.repository.AuthRepository
import com.virtualrealm.virtualrealmmusicplayer.util.ApiCredentials
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val authPreferences: AuthPreferences,
    private val spotifyApi: SpotifyApi,
    private val spotifyAuthHandler: SpotifyAuthHandler
) : AuthRepository {

    override suspend fun getAuthState(): Flow<AuthState> {
        return authPreferences.authStateFlow
    }

    override suspend fun saveAuthState(authState: AuthState) {
        authPreferences.saveAuthState(authState)
    }

    override suspend fun clearAuthState() {
        authPreferences.clearAuthState()
    }

    override fun startSpotifyAuthFlow() {
        spotifyAuthHandler.launchSpotifyAuth()
    }

    // New method to handle intent-based authorization
    override suspend fun exchangeSpotifyCode(intent: Intent): Flow<Resource<AuthState>> = flow {
        emit(Resource.Loading)

        val code = spotifyAuthHandler.extractAuthCode(intent)
        if (code == null) {
            emit(Resource.Error("No authorization code found"))
            return@flow
        }

        val result = spotifyAuthHandler.exchangeAuthorizationCode(code)
        emit(result)
    }

    // Keep original method for backward compatibility
    override suspend fun exchangeSpotifyCode(code: String): Flow<Resource<AuthState>> = flow {
        emit(Resource.Loading)
        try {
            val response = spotifyApi.getToken(
                grantType = "authorization_code",
                code = code,
                redirectUri = ApiCredentials.SPOTIFY_REDIRECT_URI,
                clientId = ApiCredentials.SPOTIFY_CLIENT_ID,
                clientSecret = ApiCredentials.SPOTIFY_CLIENT_SECRET
            )

            val authState = AuthState(
                isAuthenticated = true,
                accessToken = response.accessToken,
                refreshToken = response.refreshToken,
                expiresIn = response.expiresIn,
                tokenType = response.tokenType
            )

            authPreferences.saveAuthState(authState)
            emit(Resource.Success(authState))
        } catch (e: Exception) {
            emit(Resource.Error("Failed to exchange code: ${e.message}", e))
        }
    }

    override suspend fun refreshSpotifyToken(): Flow<Resource<AuthState>> = flow {
        emit(Resource.Loading)
        try {
            val currentState = authPreferences.authStateFlow.first()
            val refreshToken = currentState.refreshToken
                ?: return@flow emit(Resource.Error("No refresh token available"))

            // Use direct refresh with SpotifyAuthHandler
            val result = spotifyAuthHandler.refreshAccessToken(refreshToken)
            emit(result)
        } catch (e: Exception) {
            emit(Resource.Error("Failed to refresh token: ${e.message}", e))
        }
    }

    override fun getSpotifyAuthUrl(): String {
        return Uri.Builder()
            .scheme("https")
            .authority("accounts.spotify.com")
            .appendPath("authorize")
            .appendQueryParameter("client_id", ApiCredentials.SPOTIFY_CLIENT_ID)
            .appendQueryParameter("response_type", "code")
            .appendQueryParameter("redirect_uri", ApiCredentials.SPOTIFY_REDIRECT_URI)
            .appendQueryParameter("scope", "user-read-private user-read-email user-library-read streaming")
            .build()
            .toString()
    }
}

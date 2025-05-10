// app/src/main/java/com/virtualrealm/virtualrealmmusicplayer/data/auth/SpotifyAuthHandler.kt

package com.virtualrealm.virtualrealmmusicplayer.data.auth

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Base64
import android.util.Log
import androidx.browser.customtabs.CustomTabsIntent
import com.google.gson.Gson
import com.virtualrealm.virtualrealmmusicplayer.data.remote.dto.SpotifyTokenResponse
import com.virtualrealm.virtualrealmmusicplayer.domain.model.AuthState
import com.virtualrealm.virtualrealmmusicplayer.domain.model.Resource
import com.virtualrealm.virtualrealmmusicplayer.util.ApiCredentials
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SpotifyAuthHandler @Inject constructor(
    private val context: Context,
    private val okHttpClient: OkHttpClient,
    private val gson: Gson
) {
    private val TAG = "SpotifyAuthHandler"

    // Spotify OAuth endpoints
    private val authEndpoint = "https://accounts.spotify.com/authorize"
    private val tokenEndpoint = "https://accounts.spotify.com/api/token"

    // Required scopes for Spotify API
    private val scopes = listOf(
        "user-read-private",
        "user-read-email",
        "user-library-read",
        "streaming"
    )

    /**
     * Launch the Spotify authorization page in a Chrome Custom Tab
     */
    fun launchSpotifyAuth() {
        val uri = Uri.parse(authEndpoint).buildUpon()
            .appendQueryParameter("client_id", ApiCredentials.SPOTIFY_CLIENT_ID)
            .appendQueryParameter("response_type", "code")
            .appendQueryParameter("redirect_uri", ApiCredentials.SPOTIFY_REDIRECT_URI)
            .appendQueryParameter("scope", scopes.joinToString(" "))
            .build()

        val customTabsIntent = CustomTabsIntent.Builder()
            .setShowTitle(true)
            .build()

        try {
            customTabsIntent.launchUrl(context, uri)
        } catch (e: Exception) {
            Log.e(TAG, "Error launching Spotify auth: ${e.message}")
            // Fallback to regular browser if Custom Tabs not available
            val browserIntent = Intent(Intent.ACTION_VIEW, uri)
            context.startActivity(browserIntent)
        }
    }

    /**
     * Extract the authorization code from the intent data
     */
    fun extractAuthCode(intent: Intent): String? {
        val uri = intent.data ?: return null
        return uri.getQueryParameter("code")
    }

    /**
     * Exchange the authorization code for an access token and refresh token
     */
    suspend fun exchangeAuthorizationCode(code: String): Resource<AuthState> = withContext(Dispatchers.IO) {
        try {
            val authString = "${ApiCredentials.SPOTIFY_CLIENT_ID}:${ApiCredentials.SPOTIFY_CLIENT_SECRET}"
            val encodedAuth = Base64.encodeToString(authString.toByteArray(), Base64.NO_WRAP)

            val formBody = FormBody.Builder()
                .add("grant_type", "authorization_code")
                .add("code", code)
                .add("redirect_uri", ApiCredentials.SPOTIFY_REDIRECT_URI)
                .build()

            val request = Request.Builder()
                .url(tokenEndpoint)
                .post(formBody)
                .header("Authorization", "Basic $encodedAuth")
                .header("Content-Type", "application/x-www-form-urlencoded")
                .build()

            val response = okHttpClient.newCall(request).execute()

            if (!response.isSuccessful) {
                Log.e(TAG, "Token exchange failed: ${response.code}")
                return@withContext Resource.Error("Failed to get token: ${response.message}")
            }

            val responseBody = response.body?.string()
            val tokenResponse = gson.fromJson(responseBody, SpotifyTokenResponse::class.java)

            val authState = AuthState(
                isAuthenticated = true,
                accessToken = tokenResponse.accessToken,
                refreshToken = tokenResponse.refreshToken,
                expiresIn = tokenResponse.expiresIn,
                tokenType = tokenResponse.tokenType
            )

            return@withContext Resource.Success(authState)
        } catch (e: IOException) {
            Log.e(TAG, "Network error: ${e.message}")
            return@withContext Resource.Error("Network error: ${e.message}", e)
        } catch (e: Exception) {
            Log.e(TAG, "Error exchanging code for token: ${e.message}")
            return@withContext Resource.Error("Error: ${e.message}", e)
        }
    }

    /**
     * Refresh the access token using the refresh token
     */
    suspend fun refreshAccessToken(refreshToken: String): Resource<AuthState> = withContext(Dispatchers.IO) {
        try {
            val authString = "${ApiCredentials.SPOTIFY_CLIENT_ID}:${ApiCredentials.SPOTIFY_CLIENT_SECRET}"
            val encodedAuth = Base64.encodeToString(authString.toByteArray(), Base64.NO_WRAP)

            val formBody = FormBody.Builder()
                .add("grant_type", "refresh_token")
                .add("refresh_token", refreshToken)
                .build()

            val request = Request.Builder()
                .url(tokenEndpoint)
                .post(formBody)
                .header("Authorization", "Basic $encodedAuth")
                .header("Content-Type", "application/x-www-form-urlencoded")
                .build()

            val response = okHttpClient.newCall(request).execute()

            if (!response.isSuccessful) {
                Log.e(TAG, "Token refresh failed: ${response.code}")
                return@withContext Resource.Error("Failed to refresh token: ${response.message}")
            }

            val responseBody = response.body?.string()
            val tokenResponse = gson.fromJson(responseBody, SpotifyTokenResponse::class.java)

            // Note: refresh_token might not be included in the refresh response,
            // so we keep using the one we have
            val authState = AuthState(
                isAuthenticated = true,
                accessToken = tokenResponse.accessToken,
                refreshToken = tokenResponse.refreshToken ?: refreshToken,
                expiresIn = tokenResponse.expiresIn,
                tokenType = tokenResponse.tokenType
            )

            return@withContext Resource.Success(authState)
        } catch (e: IOException) {
            Log.e(TAG, "Network error: ${e.message}")
            return@withContext Resource.Error("Network error: ${e.message}", e)
        } catch (e: Exception) {
            Log.e(TAG, "Error refreshing token: ${e.message}")
            return@withContext Resource.Error("Error: ${e.message}", e)
        }
    }
}
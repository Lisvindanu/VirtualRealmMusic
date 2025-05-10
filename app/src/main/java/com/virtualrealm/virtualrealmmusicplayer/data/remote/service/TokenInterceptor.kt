// app/src/main/java/com/virtualrealm/virtualrealmmusicplayer/data/remote/service/TokenInterceptor.kt (continued)
package com.virtualrealm.virtualrealmmusicplayer.data.remote.service

import com.virtualrealm.virtualrealmmusicplayer.data.local.preferences.AuthPreferences
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TokenInterceptor @Inject constructor(
    private val authPreferences: AuthPreferences
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        // Check if the request URL contains "api.spotify.com"
        if (!originalRequest.url.toString().contains("api.spotify.com")) {
            return chain.proceed(originalRequest)
        }

        // Add the authentication token to the request
        val authState = runBlocking {
            authPreferences.authStateFlow.first()
        }

        if (!authState.isAuthenticated || authState.accessToken.isNullOrEmpty()) {
            return chain.proceed(originalRequest)
        }

        // Check if the request already has an Authorization header
        if (originalRequest.header("Authorization") != null) {
            // If it already has one, use it as is
            return chain.proceed(originalRequest)
        }

        val authenticatedRequest = originalRequest.newBuilder()
            .header("Authorization", "${authState.tokenType ?: "Bearer"} ${authState.accessToken}")
            .build()

        return chain.proceed(authenticatedRequest)
    }
}
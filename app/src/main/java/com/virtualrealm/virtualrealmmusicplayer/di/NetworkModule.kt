// app/src/main/java/com/virtualrealm/virtualrealmmusicplayer/di/NetworkModule.kt
package com.virtualrealm.virtualrealmmusicplayer.di

import com.virtualrealm.virtualrealmmusicplayer.data.local.preferences.AuthPreferences
import com.virtualrealm.virtualrealmmusicplayer.data.remote.api.SpotifyApi
import com.virtualrealm.virtualrealmmusicplayer.data.remote.api.SpotifyAuthApi
import com.virtualrealm.virtualrealmmusicplayer.data.remote.api.YouTubeApi
import com.virtualrealm.virtualrealmmusicplayer.data.remote.service.AuthAuthenticator
import com.virtualrealm.virtualrealmmusicplayer.data.remote.service.TokenInterceptor
import com.virtualrealm.virtualrealmmusicplayer.util.Constants
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Provider
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideHttpLoggingInterceptor(): HttpLoggingInterceptor {
        return HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
    }

    @Provides
    @Singleton
    fun provideTokenInterceptor(authPreferences: AuthPreferences): TokenInterceptor {
        return TokenInterceptor(authPreferences)
    }

    @Provides
    @Singleton
    fun provideAuthAuthenticator(
        spotifyAuthApiProvider: Provider<SpotifyAuthApi>, // Changed to SpotifyAuthApi
        authPreferences: AuthPreferences
    ): AuthAuthenticator {
        return AuthAuthenticator(spotifyAuthApiProvider, authPreferences)
    }

    @Provides
    @Singleton
    @Named("baseOkHttpClient")
    fun provideBaseOkHttpClient(
        loggingInterceptor: HttpLoggingInterceptor
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(Constants.NETWORK_TIMEOUT, TimeUnit.SECONDS)
            .readTimeout(Constants.NETWORK_TIMEOUT, TimeUnit.SECONDS)
            .writeTimeout(Constants.NETWORK_TIMEOUT, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    @Named("authenticatedOkHttpClient")
    fun provideAuthenticatedOkHttpClient(
        @Named("baseOkHttpClient") baseOkHttpClient: OkHttpClient,
        tokenInterceptor: TokenInterceptor,
        authAuthenticator: AuthAuthenticator
    ): OkHttpClient {
        return baseOkHttpClient.newBuilder()
            .addInterceptor(tokenInterceptor)
            .authenticator(authAuthenticator)
            .build()
    }

    @Provides
    @Singleton
    @Named("spotifyAuthRetrofit")
    fun provideSpotifyAuthRetrofit(
        @Named("baseOkHttpClient") okHttpClient: OkHttpClient
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl(Constants.SPOTIFY_ACCOUNTS_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    @Named("spotifyApiRetrofit")
    fun provideSpotifyApiRetrofit(
        @Named("authenticatedOkHttpClient") okHttpClient: OkHttpClient
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl(Constants.SPOTIFY_API_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    @Named("youtubeRetrofit")
    fun provideYoutubeRetrofit(
        @Named("baseOkHttpClient") okHttpClient: OkHttpClient
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl(Constants.YOUTUBE_API_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideSpotifyApi(
        @Named("spotifyAuthRetrofit") spotifyAuthRetrofit: Retrofit,
        @Named("spotifyApiRetrofit") spotifyApiRetrofit: Retrofit
    ): SpotifyApi {
        // Fix: use spotifyApiRetrofit instead of spotifyAuthRetrofit
        return spotifyApiRetrofit.create(SpotifyApi::class.java)
    }

    @Provides
    @Singleton
    fun provideYoutubeApi(
        @Named("youtubeRetrofit") youtubeRetrofit: Retrofit
    ): YouTubeApi {
        return youtubeRetrofit.create(YouTubeApi::class.java)
    }

    @Provides
    @Singleton
    fun provideSpotifyAuthApi(
        @Named("spotifyAuthRetrofit") spotifyAuthRetrofit: Retrofit
    ): SpotifyAuthApi {
        return spotifyAuthRetrofit.create(SpotifyAuthApi::class.java)
    }
}

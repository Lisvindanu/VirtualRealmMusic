// File: app/src/main/java/com/virtualrealm/virtualrealmmusicplayer/util/ApiCredentials.kt
package com.virtualrealm.virtualrealmmusicplayer.util

/**
 * Class untuk menangani API credentials secara terpusat.
 * Menggunakan konstanta dari AppConfig untuk menghindari masalah BuildConfig.
 */
object ApiCredentials {
    // Spotify credentials
    val SPOTIFY_CLIENT_ID: String = AppConfig.SPOTIFY_CLIENT_ID
    val SPOTIFY_CLIENT_SECRET: String = AppConfig.SPOTIFY_CLIENT_SECRET
    val SPOTIFY_REDIRECT_URI: String = AppConfig.SPOTIFY_REDIRECT_URI

    // YouTube credentials
    val YOUTUBE_API_KEY: String = AppConfig.YOUTUBE_API_KEY
}
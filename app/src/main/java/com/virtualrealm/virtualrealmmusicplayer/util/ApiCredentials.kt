// File: app/src/main/java/com/virtualrealm/virtualrealmmusicplayer/util/ApiCredentials.kt
package com.virtualrealm.virtualrealmmusicplayer.util

import com.virtualrealm.virtualrealmmusicplayer.BuildConfig

/**
 * Class untuk menangani API credentials secara terpusat.
 * Ini lebih baik daripada mengakses BuildConfig langsung di seluruh kode.
 */
object ApiCredentials {
    // Spotify credentials
    val SPOTIFY_CLIENT_ID: String = BuildConfig.SPOTIFY_CLIENT_ID
    val SPOTIFY_CLIENT_SECRET: String = BuildConfig.SPOTIFY_CLIENT_SECRET
    val SPOTIFY_REDIRECT_URI: String = BuildConfig.SPOTIFY_REDIRECT_URI

    // YouTube credentials
    val YOUTUBE_API_KEY: String = BuildConfig.YOUTUBE_API_KEY
}
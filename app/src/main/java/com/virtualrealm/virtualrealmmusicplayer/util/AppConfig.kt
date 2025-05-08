// File: app/src/main/java/com/virtualrealm/virtualrealmmusicplayer/util/AppConfig.kt
package com.virtualrealm.virtualrealmmusicplayer.util

/**
 * Class untuk menyimpan konstanta konfigurasi aplikasi.
 * Ini akan diisi dengan nilai dari local.properties melalui BuildConfig.
 */
object AppConfig {
    // Spotify credentials
    const val SPOTIFY_CLIENT_ID: String = "1cc6b4968a154bc686aba8caf0dbd1c0"
    const val SPOTIFY_CLIENT_SECRET: String = "4a33faf4d0ed45708ab452c6450882e5"
    const val SPOTIFY_REDIRECT_URI: String = "com.virtualrealm.virtualrealmmusicplayer://callback"

    // YouTube credentials
    const val YOUTUBE_API_KEY: String = "AIzaSyCvMokdapr1y3mdVbirFdHMAuJpp3BhUXY"
}
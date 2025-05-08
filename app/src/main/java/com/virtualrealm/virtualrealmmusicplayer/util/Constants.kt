// app/src/main/java/com/virtualrealm/virtualrealmmusicplayer/util/Constants.kt
package com.virtualrealm.virtualrealmmusicplayer.util

object Constants {
    // API Base URLs
    const val SPOTIFY_ACCOUNTS_BASE_URL = "https://accounts.spotify.com/"
    const val SPOTIFY_API_BASE_URL = "https://api.spotify.com/"
    const val YOUTUBE_API_BASE_URL = "https://www.googleapis.com/"

    // Music Types
    const val MUSIC_TYPE_SPOTIFY = "spotify"
    const val MUSIC_TYPE_YOUTUBE = "youtube"

    // Preferences
    const val AUTH_PREFERENCES_NAME = "auth_preferences"

    // Database
    const val DATABASE_NAME = "music_database"

    // Network timeouts (in seconds)
    const val NETWORK_TIMEOUT = 15L
}
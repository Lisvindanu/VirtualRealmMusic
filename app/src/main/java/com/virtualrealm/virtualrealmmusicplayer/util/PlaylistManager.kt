// app/src/main/java/com/virtualrealm/virtualrealmmusicplayer/util/PlaylistManager.kt

package com.virtualrealm.virtualrealmmusicplayer.util

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.virtualrealm.virtualrealmmusicplayer.domain.model.Music
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton
import java.io.IOException


/**
 * A utility class to manage playlist persistence across app sessions
 */
@Singleton
class PlaylistManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val Context.playlistDataStore: DataStore<Preferences> by preferencesDataStore("playlist_preferences")
    private val gson = Gson()

    // Keys for DataStore
    companion object {
        private val CURRENT_PLAYLIST = stringPreferencesKey("current_playlist")
        private val LAST_PLAYED_ID = stringPreferencesKey("last_played_id")
        private val LAST_POSITION = longPreferencesKey("last_position")
        private val LAST_INDEX = intPreferencesKey("last_index")

        // Saved playlists
        private val SAVED_PLAYLISTS = stringPreferencesKey("saved_playlists")
    }

    /**
     * Save the current playlist state
     */
    suspend fun saveCurrentPlaylist(
        playlist: List<Music>,
        currentIndex: Int,
        position: Long = 0
    ) {
        try {
            // Convert playlist to JSON string
            val playlistJson = gson.toJson(playlist)

            // Save to preferences
            context.playlistDataStore.edit { preferences ->
                preferences[CURRENT_PLAYLIST] = playlistJson
                preferences[LAST_INDEX] = currentIndex
                preferences[LAST_POSITION] = position

                if (playlist.isNotEmpty() && currentIndex in playlist.indices) {
                    preferences[LAST_PLAYED_ID] = playlist[currentIndex].id
                }
            }

            Log.d("PlaylistManager", "Saved playlist with ${playlist.size} tracks, current index: $currentIndex")
        } catch (e: Exception) {
            Log.e("PlaylistManager", "Error saving playlist: ${e.message}", e)
        }
    }

    /**
     * Get the saved playlist state
     */
    fun getCurrentPlaylist(): Flow<PlaylistState> {
        return context.playlistDataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    Log.e("PlaylistManager", "Error reading playlist data: ${exception.message}")
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }
            .map { preferences ->
                val playlistJson = preferences[CURRENT_PLAYLIST] ?: ""
                val lastIndex = preferences[LAST_INDEX] ?: 0
                val lastPosition = preferences[LAST_POSITION] ?: 0
                val lastPlayedId = preferences[LAST_PLAYED_ID]

                if (playlistJson.isEmpty()) {
                    return@map PlaylistState(emptyList(), 0, 0)
                }

                try {
                    // Parse the playlist from JSON
                    val type = object : TypeToken<List<Music>>() {}.type
                    val playlist: List<Music> = gson.fromJson(playlistJson, type)

                    PlaylistState(
                        playlist = playlist,
                        currentIndex = lastIndex.coerceIn(0, playlist.size - 1),
                        positionMs = lastPosition,
                        lastPlayedId = lastPlayedId
                    )
                } catch (e: Exception) {
                    Log.e("PlaylistManager", "Error parsing playlist: ${e.message}", e)
                    PlaylistState(emptyList(), 0, 0)
                }
            }
    }

    /**
     * Save a named playlist for future use
     */
    suspend fun saveNamedPlaylist(name: String, playlist: List<Music>) {
        try {
            // Get current saved playlists
            val savedPlaylists = getSavedPlaylists()

            // Add or update the playlist
            savedPlaylists[name] = playlist

            // Save back to preferences
            val playlistsJson = gson.toJson(savedPlaylists)
            context.playlistDataStore.edit { preferences ->
                preferences[SAVED_PLAYLISTS] = playlistsJson
            }

            Log.d("PlaylistManager", "Saved playlist '$name' with ${playlist.size} tracks")
        } catch (e: Exception) {
            Log.e("PlaylistManager", "Error saving named playlist: ${e.message}", e)
        }
    }

    /**
     * Get all saved playlists
     */
    private suspend fun getSavedPlaylists(): MutableMap<String, List<Music>> {
        return try {
            val playlistsJson = context.playlistDataStore.data.first()[SAVED_PLAYLISTS] ?: ""

            if (playlistsJson.isEmpty()) {
                return mutableMapOf()
            }

            val type = object : TypeToken<Map<String, List<Music>>>() {}.type
            gson.fromJson(playlistsJson, type) ?: mutableMapOf()
        } catch (e: Exception) {
            Log.e("PlaylistManager", "Error getting saved playlists: ${e.message}", e)
            mutableMapOf()
        }
    }

    /**
     * Get a Flow of all saved playlist names
     */
    fun getSavedPlaylistNames(): Flow<List<String>> {
        return context.playlistDataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    Log.e("PlaylistManager", "Error reading playlist data: ${exception.message}")
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }
            .map { preferences ->
                val playlistsJson = preferences[SAVED_PLAYLISTS] ?: ""

                if (playlistsJson.isEmpty()) {
                    return@map emptyList()
                }

                try {
                    val type = object : TypeToken<Map<String, List<Music>>>() {}.type
                    val playlists: Map<String, List<Music>> = gson.fromJson(playlistsJson, type)
                    playlists.keys.toList()
                } catch (e: Exception) {
                    Log.e("PlaylistManager", "Error parsing playlist names: ${e.message}", e)
                    emptyList()
                }
            }
    }

    /**
     * Load a saved playlist by name
     */
    suspend fun loadNamedPlaylist(name: String): List<Music>? {
        return try {
            val savedPlaylists = getSavedPlaylists()
            savedPlaylists[name]
        } catch (e: Exception) {
            Log.e("PlaylistManager", "Error loading playlist '$name': ${e.message}", e)
            null
        }
    }

    /**
     * Delete a saved playlist
     */
    suspend fun deleteNamedPlaylist(name: String) {
        try {
            val savedPlaylists = getSavedPlaylists()

            if (savedPlaylists.containsKey(name)) {
                savedPlaylists.remove(name)

                // Save back to preferences
                val playlistsJson = gson.toJson(savedPlaylists)
                context.playlistDataStore.edit { preferences ->
                    preferences[SAVED_PLAYLISTS] = playlistsJson
                }

                Log.d("PlaylistManager", "Deleted playlist '$name'")
            }
        } catch (e: Exception) {
            Log.e("PlaylistManager", "Error deleting playlist '$name': ${e.message}", e)
        }
    }

    /**
     * Clear all playlist data
     */
    suspend fun clearAll() {
        context.playlistDataStore.edit { preferences ->
            preferences.clear()
        }
        Log.d("PlaylistManager", "Cleared all playlist data")
    }
}

/**
 * Data class representing the state of a playlist
 */
data class PlaylistState(
    val playlist: List<Music>,
    val currentIndex: Int,
    val positionMs: Long,
    val lastPlayedId: String? = null
) {
    val currentTrack: Music? get() =
        if (playlist.isNotEmpty() && currentIndex in playlist.indices) {
            playlist[currentIndex]
        } else {
            null
        }
}
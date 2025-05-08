// app/src/main/java/com/virtualrealm/virtualrealmmusicplayer/data/local/cache/LocalCacheManager.kt

package com.virtualrealm.virtualrealmmusicplayer.data.local.cache


import android.util.Log
import com.virtualrealm.virtualrealmmusicplayer.data.local.dao.MusicDao
import com.virtualrealm.virtualrealmmusicplayer.data.local.entity.MusicEntity
import com.virtualrealm.virtualrealmmusicplayer.domain.model.Music
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalCacheManager @Inject constructor(
    private val musicDao: MusicDao
) {
    suspend fun clearAllCache() = withContext(Dispatchers.IO) {
        try {
            // This assumes you've added a clearAll method to your DAO
            musicDao.clearAll()
            Log.d("LocalCacheManager", "Cleared all cache")
        } catch (e: Exception) {
            Log.e("LocalCacheManager", "Error clearing cache: ${e.message}", e)
        }
    }

    suspend fun clearFavorites() = withContext(Dispatchers.IO) {
        try {
            musicDao.clearFavorites()
            Log.d("LocalCacheManager", "Cleared favorites cache")
        } catch (e: Exception) {
            Log.e("LocalCacheManager", "Error clearing favorites cache: ${e.message}", e)
        }
    }

    suspend fun saveToCache(music: Music) = withContext(Dispatchers.IO) {
        try {
            val entity = when (music) {
                is Music.SpotifyTrack -> MusicEntity.fromSpotifyTrack(music, false)
                is Music.YoutubeVideo -> MusicEntity.fromYoutubeVideo(music, false)
            }
            musicDao.insertMusic(entity)
            Log.d("LocalCacheManager", "Saved to cache: ${music.title}")
        } catch (e: Exception) {
            Log.e("LocalCacheManager", "Error saving to cache: ${e.message}", e)
        }
    }

    suspend fun removeFromCache(musicId: String) = withContext(Dispatchers.IO) {
        try {
            musicDao.deleteMusic(musicId)
            Log.d("LocalCacheManager", "Removed from cache: $musicId")
        } catch (e: Exception) {
            Log.e("LocalCacheManager", "Error removing from cache: ${e.message}", e)
        }
    }
}
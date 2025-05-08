// data/local/dao/MusicDao.kt
package com.virtualrealm.virtualrealmmusicplayer.data.local.dao

import androidx.room.*
import com.virtualrealm.virtualrealmmusicplayer.data.local.entity.MusicEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MusicDao {
    @Query("SELECT * FROM music WHERE isFavorite = 1 ORDER BY title ASC")
    fun getFavorites(): Flow<List<MusicEntity>>

    @Query("SELECT * FROM music WHERE id = :id")
    suspend fun getMusicById(id: String): MusicEntity?

    @Query("SELECT EXISTS(SELECT 1 FROM music WHERE id = :id AND isFavorite = 1)")
    suspend fun isInFavorites(id: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMusic(musicEntity: MusicEntity)

    @Query("UPDATE music SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun updateFavoriteStatus(id: String, isFavorite: Boolean)

    @Query("UPDATE music SET isFavorite = 0 WHERE id = :id")
    suspend fun removeFromFavorites(id: String)

    @Query("DELETE FROM music WHERE id = :id")
    suspend fun deleteMusic(id: String)
}
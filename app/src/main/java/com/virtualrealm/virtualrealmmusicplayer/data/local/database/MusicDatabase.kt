// data/local/database/MusicDatabase.kt
package com.virtualrealm.virtualrealmmusicplayer.data.local.database


import androidx.room.Database
import androidx.room.RoomDatabase
import com.virtualrealm.virtualrealmmusicplayer.data.local.dao.MusicDao
import com.virtualrealm.virtualrealmmusicplayer.data.local.entity.MusicEntity

@Database(entities = [MusicEntity::class], version = 1, exportSchema = false)
abstract class MusicDatabase : RoomDatabase() {
    abstract fun musicDao(): MusicDao
}
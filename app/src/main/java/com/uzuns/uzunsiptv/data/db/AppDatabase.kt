package com.uzuns.uzunsiptv.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

// Versiyon 4 yapıldı
@Database(entities = [FavoriteChannel::class, WatchProgress::class], version = 4, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun favoriteDao(): FavoriteDao
    abstract fun watchDao(): WatchDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "uzuns_iptv_database"
                )
                    .fallbackToDestructiveMigration() // Eski veriyi sil, yenisini kur
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

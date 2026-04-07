package com.uzuns.uzunsiptv.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [FavoriteChannel::class, WatchProgress::class], version = 6, exportSchema = false)
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
                    .addMigrations(MIGRATION_4_5)
                    .addMigrations(MIGRATION_5_6)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE favorite_channels ADD COLUMN directSource TEXT")
        database.execSQL("ALTER TABLE watch_progress ADD COLUMN directSource TEXT")
    }
}

val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS favorite_channels_new (
                streamId INTEGER NOT NULL,
                name TEXT NOT NULL,
                streamType TEXT NOT NULL,
                streamIcon TEXT,
                categoryName TEXT,
                directSource TEXT,
                PRIMARY KEY(streamId, streamType)
            )
            """.trimIndent()
        )

        database.execSQL(
            """
            INSERT OR REPLACE INTO favorite_channels_new (streamId, name, streamType, streamIcon, categoryName, directSource)
            SELECT fc.streamId, fc.name, fc.streamType, fc.streamIcon, fc.categoryName, fc.directSource
            FROM favorite_channels fc
            INNER JOIN (
                SELECT streamId, streamType, MAX(id) AS maxId
                FROM favorite_channels
                GROUP BY streamId, streamType
            ) latest
            ON fc.streamId = latest.streamId
            AND fc.streamType = latest.streamType
            AND fc.id = latest.maxId
            """.trimIndent()
        )

        database.execSQL("DROP TABLE favorite_channels")
        database.execSQL("ALTER TABLE favorite_channels_new RENAME TO favorite_channels")
    }
}

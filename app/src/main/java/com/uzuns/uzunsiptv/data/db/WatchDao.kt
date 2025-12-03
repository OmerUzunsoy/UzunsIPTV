package com.uzuns.uzunsiptv.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface WatchDao {

    // Yarım kalanları getir (En son izlenen en üstte)
    @Query("SELECT * FROM watch_progress ORDER BY timestamp DESC")
    fun getAllProgress(): Flow<List<WatchProgress>>

    // Kaydet veya Güncelle
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProgress(progress: WatchProgress)

    // İzleme kaydını sil (Film bittiyse veya listeden çıkarılacaksa)
    @Query("DELETE FROM watch_progress WHERE streamId = :streamId")
    suspend fun deleteProgress(streamId: Int)

    // Belirli bir içeriğin kaldığı yeri getir
    @Query("SELECT position FROM watch_progress WHERE streamId = :streamId")
    suspend fun getPosition(streamId: Int): Long?

    @Query("SELECT * FROM watch_progress WHERE streamId = :streamId LIMIT 1")
    suspend fun getProgress(streamId: Int): WatchProgress?
}

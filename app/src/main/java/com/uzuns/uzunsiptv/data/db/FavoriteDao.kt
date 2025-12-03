package com.uzuns.uzunsiptv.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteDao {

    @Query("SELECT * FROM favorite_channels ORDER BY id DESC")
    fun getAllFavorites(): Flow<List<FavoriteChannel>>

    @Query("SELECT * FROM favorite_channels")
    suspend fun getAllFavoritesOnce(): List<FavoriteChannel>

    @Query("SELECT EXISTS(SELECT * FROM favorite_channels WHERE streamId = :streamId)")
    suspend fun isFavorite(streamId: Int): Boolean

    // Fonksiyon adını 'insert' yaptık (PlayerActivity ile uyumlu olsun diye)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(favorite: FavoriteChannel)

    @Query("DELETE FROM favorite_channels WHERE streamId = :streamId")
    suspend fun deleteByStreamId(streamId: Int)
}

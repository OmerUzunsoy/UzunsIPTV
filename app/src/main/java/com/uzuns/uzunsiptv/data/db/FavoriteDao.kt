package com.uzuns.uzunsiptv.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteDao {

    @Query("SELECT * FROM favorite_channels ORDER BY name COLLATE NOCASE ASC")
    fun getAllFavorites(): Flow<List<FavoriteChannel>>

    @Query("SELECT * FROM favorite_channels")
    suspend fun getAllFavoritesOnce(): List<FavoriteChannel>

    @Query("SELECT EXISTS(SELECT * FROM favorite_channels WHERE streamId = :streamId AND streamType = :streamType)")
    suspend fun isFavorite(streamId: Int, streamType: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(favorite: FavoriteChannel)

    @Query("DELETE FROM favorite_channels WHERE streamId = :streamId AND streamType = :streamType")
    suspend fun deleteByStreamId(streamId: Int, streamType: String)
}

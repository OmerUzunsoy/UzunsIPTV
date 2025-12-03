package com.uzuns.uzunsiptv.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorite_channels")
data class FavoriteChannel(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val streamId: Int,
    val name: String,
    val streamType: String,
    val streamIcon: String?,
    val categoryName: String? // Eksik olan bu alanı ekledik
)
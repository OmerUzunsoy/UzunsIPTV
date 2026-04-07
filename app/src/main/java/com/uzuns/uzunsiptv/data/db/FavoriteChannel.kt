package com.uzuns.uzunsiptv.data.db

import androidx.room.Entity

@Entity(
    tableName = "favorite_channels",
    primaryKeys = ["streamId", "streamType"]
)
data class FavoriteChannel(
    val streamId: Int,
    val name: String,
    val streamType: String,
    val streamIcon: String?,
    val categoryName: String?,
    val directSource: String? = null
)

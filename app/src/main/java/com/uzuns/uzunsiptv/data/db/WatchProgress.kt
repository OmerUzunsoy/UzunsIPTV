package com.uzuns.uzunsiptv.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "watch_progress")
data class WatchProgress(
    @PrimaryKey
    val streamId: Int,

    val name: String,
    val streamType: String,
    val streamIcon: String?,
    val position: Long,
    val duration: Long,
    val timestamp: Long,
    val containerExtension: String?,
    val parentSeriesId: Int?
)

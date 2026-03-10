package com.lomen.tv.data.local.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "watch_history",
    // 移除外键约束，因为 movieId 可能引用 MovieEntity 或 WebDavMediaEntity
    // Room 不支持多表外键，我们通过代码逻辑来保证数据一致性
    indices = [
        Index(value = ["movieId"]),
        Index(value = ["lastWatchedAt"])
    ]
)
data class WatchHistoryEntity(
    @PrimaryKey val id: String,
    val movieId: String,
    val episodeId: String? = null,
    val progress: Long = 0,
    val duration: Long = 0,
    val lastWatchedAt: Long = System.currentTimeMillis(),
    val watchCount: Int = 1
)

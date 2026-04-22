package com.lomen.tv.data.local.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "tmdb_sync_queue",
    indices = [
        Index(value = ["state"]),
        Index(value = ["taskType"]),
        Index(value = ["updatedAt"])
    ]
)
data class TmdbSyncQueueEntity(
    @PrimaryKey val key: String,
    val tmdbId: Int,
    val taskType: TaskType,
    val seasonNumber: Int? = null,
    val priority: Int = 0,
    val state: State = State.PENDING,
    val attemptCount: Int = 0,
    val lastError: String? = null,
    val updatedAt: Long = System.currentTimeMillis()
) {
    enum class TaskType { MEDIA, SEASON_EPISODES }
    enum class State { PENDING, RUNNING, DONE, FAILED }

    companion object {
        fun mediaKey(tmdbId: Int): String = "media:$tmdbId"
        fun seasonKey(tmdbId: Int, seasonNumber: Int): String = "season:$tmdbId:$seasonNumber"
    }
}


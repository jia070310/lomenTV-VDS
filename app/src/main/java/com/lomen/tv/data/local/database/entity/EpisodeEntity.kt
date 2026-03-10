package com.lomen.tv.data.local.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "episodes",
    foreignKeys = [
        ForeignKey(
            entity = MovieEntity::class,
            parentColumns = ["id"],
            childColumns = ["movieId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["movieId"])]
)
data class EpisodeEntity(
    @PrimaryKey val id: String,
    val movieId: String,
    val seasonNumber: Int,
    val episodeNumber: Int,
    val title: String? = null,
    val overview: String? = null,
    val stillUrl: String? = null,
    val duration: Long? = null,
    val quarkFileId: String? = null,
    val quarkPath: String? = null,
    val watchProgress: Long = 0,
    val isWatched: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

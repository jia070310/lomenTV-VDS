package com.lomen.tv.data.local.database.entity

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "tmdb_episode",
    primaryKeys = ["tmdbId", "seasonNumber", "episodeNumber"],
    indices = [
        Index(value = ["tmdbId", "seasonNumber"]),
        Index(value = ["updatedAt"])
    ]
)
data class TmdbEpisodeEntity(
    val tmdbId: Int,
    val seasonNumber: Int,
    val episodeNumber: Int,
    val name: String? = null,
    val overview: String? = null,
    val stillUrl: String? = null,
    val airDate: String? = null,
    val runtimeMinutes: Int? = null,
    val updatedAt: Long = System.currentTimeMillis()
)


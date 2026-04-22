package com.lomen.tv.data.local.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index
import com.lomen.tv.domain.model.MediaType

@Entity(
    tableName = "tmdb_media",
    indices = [
        Index(value = ["type"]),
        Index(value = ["updatedAt"])
    ]
)
data class TmdbMediaEntity(
    @PrimaryKey val tmdbId: Int,
    val type: MediaType,
    val title: String,
    val originalTitle: String? = null,
    val overview: String? = null,
    val posterUrl: String? = null,
    val backdropUrl: String? = null,
    val rating: Float? = null,
    val year: Int? = null,
    val genres: String? = null,
    val seasonCount: Int? = null,
    val episodeCount: Int? = null,
    val updatedAt: Long = System.currentTimeMillis()
)


package com.lomen.tv.data.local.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.lomen.tv.domain.model.MediaType

@Entity(tableName = "movies")
data class MovieEntity(
    @PrimaryKey val id: String,
    val title: String,
    val originalTitle: String? = null,
    val overview: String? = null,
    val posterUrl: String? = null,
    val backdropUrl: String? = null,
    val releaseDate: String? = null,
    val rating: Float? = null,
    val genre: String? = null,
    val type: MediaType = MediaType.MOVIE,
    val quarkFileId: String? = null,
    val quarkPath: String? = null,
    val tmdbId: Int? = null,
    val doubanId: String? = null,
    val seasonCount: Int = 0,
    val episodeCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

package com.lomen.tv.data.local.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.lomen.tv.data.local.database.entity.TmdbEpisodeEntity

@Dao
interface TmdbEpisodeDao {
    @Query("SELECT * FROM tmdb_episode WHERE tmdbId = :tmdbId AND seasonNumber = :seasonNumber ORDER BY episodeNumber ASC")
    suspend fun getBySeason(tmdbId: Int, seasonNumber: Int): List<TmdbEpisodeEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<TmdbEpisodeEntity>)
}


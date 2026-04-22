package com.lomen.tv.data.local.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.lomen.tv.data.local.database.entity.TmdbMediaEntity

@Dao
interface TmdbMediaDao {
    @Query("SELECT * FROM tmdb_media WHERE tmdbId = :tmdbId LIMIT 1")
    suspend fun getByTmdbId(tmdbId: Int): TmdbMediaEntity?

    @Query("SELECT * FROM tmdb_media WHERE tmdbId IN (:tmdbIds)")
    suspend fun getByTmdbIds(tmdbIds: List<Int>): List<TmdbMediaEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: TmdbMediaEntity)
}


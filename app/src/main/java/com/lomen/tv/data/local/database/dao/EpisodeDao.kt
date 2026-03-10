package com.lomen.tv.data.local.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.lomen.tv.data.local.database.entity.EpisodeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EpisodeDao {

    @Query("SELECT * FROM episodes WHERE movieId = :movieId ORDER BY seasonNumber, episodeNumber")
    fun getEpisodesByMovieId(movieId: String): Flow<List<EpisodeEntity>>

    @Query("SELECT * FROM episodes WHERE movieId = :movieId AND seasonNumber = :seasonNumber ORDER BY episodeNumber")
    fun getEpisodesBySeason(movieId: String, seasonNumber: Int): Flow<List<EpisodeEntity>>

    @Query("SELECT * FROM episodes WHERE id = :id")
    suspend fun getEpisodeById(id: String): EpisodeEntity?

    @Query("SELECT * FROM episodes WHERE quarkFileId = :fileId")
    suspend fun getEpisodeByQuarkFileId(fileId: String): EpisodeEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEpisode(episode: EpisodeEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEpisodes(episodes: List<EpisodeEntity>)

    @Update
    suspend fun updateEpisode(episode: EpisodeEntity)

    @Query("UPDATE episodes SET watchProgress = :progress, isWatched = :isWatched WHERE id = :id")
    suspend fun updateWatchProgress(id: String, progress: Long, isWatched: Boolean)

    @Query("DELETE FROM episodes WHERE id = :id")
    suspend fun deleteEpisode(id: String)

    @Query("DELETE FROM episodes WHERE movieId = :movieId")
    suspend fun deleteEpisodesByMovieId(movieId: String)

    @Query("SELECT COUNT(*) FROM episodes WHERE movieId = :movieId")
    suspend fun getEpisodeCountByMovieId(movieId: String): Int

    @Query("SELECT DISTINCT seasonNumber FROM episodes WHERE movieId = :movieId ORDER BY seasonNumber")
    suspend fun getSeasonsByMovieId(movieId: String): List<Int>
}

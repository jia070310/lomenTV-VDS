package com.lomen.tv.data.local.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.lomen.tv.data.local.database.entity.MovieEntity
import com.lomen.tv.domain.model.MediaType
import kotlinx.coroutines.flow.Flow

@Dao
interface MovieDao {

    @Query("SELECT * FROM movies ORDER BY updatedAt DESC")
    fun getAllMovies(): Flow<List<MovieEntity>>

    @Query("SELECT * FROM movies WHERE type = :type ORDER BY updatedAt DESC")
    fun getMoviesByType(type: MediaType): Flow<List<MovieEntity>>

    @Query("SELECT * FROM movies WHERE id = :id")
    suspend fun getMovieById(id: String): MovieEntity?

    @Query("SELECT * FROM movies WHERE quarkFileId = :fileId")
    suspend fun getMovieByQuarkFileId(fileId: String): MovieEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMovie(movie: MovieEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMovies(movies: List<MovieEntity>)

    @Update
    suspend fun updateMovie(movie: MovieEntity)

    @Query("UPDATE movies SET updatedAt = :timestamp WHERE id = :id")
    suspend fun updateTimestamp(id: String, timestamp: Long = System.currentTimeMillis())

    @Query("DELETE FROM movies WHERE id = :id")
    suspend fun deleteMovie(id: String)

    @Query("DELETE FROM movies")
    suspend fun deleteAllMovies()

    @Query("SELECT COUNT(*) FROM movies")
    suspend fun getMovieCount(): Int

    @Query("SELECT * FROM movies WHERE title LIKE '%' || :query || '%' OR originalTitle LIKE '%' || :query || '%'")
    fun searchMovies(query: String): Flow<List<MovieEntity>>
}

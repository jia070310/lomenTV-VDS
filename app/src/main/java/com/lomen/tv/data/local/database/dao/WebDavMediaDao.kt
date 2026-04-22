package com.lomen.tv.data.local.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.lomen.tv.data.local.database.entity.WebDavMediaEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WebDavMediaDao {
    @Query("SELECT * FROM webdav_media")
    suspend fun getAllSync(): List<WebDavMediaEntity>

    @Query("SELECT * FROM webdav_media WHERE libraryId = :libraryId ORDER BY title ASC")
    fun getByLibraryId(libraryId: String): Flow<List<WebDavMediaEntity>>

    @Query("SELECT * FROM webdav_media WHERE libraryId = :libraryId AND isMovie = 1 ORDER BY title ASC")
    fun getMoviesByLibraryId(libraryId: String): Flow<List<WebDavMediaEntity>>

    @Query("SELECT * FROM webdav_media WHERE libraryId = :libraryId AND isMovie = 0 ORDER BY title ASC")
    fun getTvShowsByLibraryId(libraryId: String): Flow<List<WebDavMediaEntity>>

    @Query("SELECT * FROM webdav_media WHERE filePath = :filePath LIMIT 1")
    suspend fun getByFilePath(filePath: String): WebDavMediaEntity?

    @Query("SELECT filePath, createdAt FROM webdav_media WHERE filePath IN (:filePaths)")
    suspend fun getCreatedAtByFilePaths(filePaths: List<String>): List<FileCreatedAt>

    @Query("SELECT * FROM webdav_media WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): WebDavMediaEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(media: WebDavMediaEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(mediaList: List<WebDavMediaEntity>)

    @Update
    suspend fun update(media: WebDavMediaEntity)

    @Query("DELETE FROM webdav_media WHERE libraryId = :libraryId")
    suspend fun deleteByLibraryId(libraryId: String)

    @Query("DELETE FROM webdav_media WHERE filePath = :filePath")
    suspend fun deleteByFilePath(filePath: String)
    
    @Query("DELETE FROM webdav_media")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM webdav_media WHERE libraryId = :libraryId")
    suspend fun countByLibraryId(libraryId: String): Int

    @Query("SELECT * FROM webdav_media WHERE title LIKE '%' || :query || '%' OR originalTitle LIKE '%' || :query || '%'")
    suspend fun search(query: String): List<WebDavMediaEntity>

    // ========== 增量扫描相关方法 ==========

    @Query("SELECT filePath, fileFingerprint FROM webdav_media WHERE libraryId = :libraryId")
    suspend fun getFingerprintsByLibraryId(libraryId: String): List<FileFingerprint>

    @Query("DELETE FROM webdav_media WHERE libraryId = :libraryId AND filePath IN (:filePaths)")
    suspend fun deleteByFilePaths(libraryId: String, filePaths: List<String>)

    data class FileFingerprint(
        val filePath: String,
        val fileFingerprint: String?
    )

    data class FileCreatedAt(
        val filePath: String,
        val createdAt: Long
    )
}

package com.lomen.tv.data.local.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index
import com.lomen.tv.domain.model.MediaType

@Entity(
    tableName = "webdav_media",
    indices = [
        Index(value = ["libraryId"]),
        Index(value = ["filePath"], unique = true),
        Index(value = ["fileFingerprint"])
    ]
)
data class WebDavMediaEntity(
    @PrimaryKey val id: String,
    val libraryId: String,
    val title: String,
    val originalTitle: String? = null,
    val overview: String? = null,
    val posterUrl: String? = null,
    val backdropUrl: String? = null,
    val year: Int? = null,
    val rating: Float? = null,
    val genres: String? = null, // 逗号分隔
    val type: MediaType = MediaType.MOVIE,  // 新增：媒体类型
    val isMovie: Boolean = true,  // 保留用于向后兼容
    val seasonNumber: Int? = null,
    val episodeNumber: Int? = null,
    val tmdbId: String? = null,   // TMDB ID，用于获取剧集详情
    val filePath: String,
    val fileName: String,
    val fileSize: Long = 0,
    val fileFingerprint: String? = null, // 文件指纹（路径+大小+修改时间的MD5）
    val source: String = "local", // tmdb, douban, local
    val scrapedAt: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

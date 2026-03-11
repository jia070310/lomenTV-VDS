package com.lomen.tv.domain.model

/**
 * 版本信息模型
 */
data class VersionInfo(
    val versionName: String,
    val versionCode: Int,
    val downloadUrl: String,
    val releaseNotes: String,
    val releaseDate: String
)
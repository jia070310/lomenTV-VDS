package com.lomen.tv.domain.model

data class ResourceLibrary(
    val id: String,
    val name: String,
    val type: LibraryType,
    val protocol: String = "",
    val host: String = "",
    val port: Int = 0,
    val path: String = "/",
    val username: String = "",
    val password: String = "",
    val isActive: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
) {
    enum class LibraryType {
        WEBDAV, QUARK
    }

    fun getDisplayUrl(): String {
        return when (type) {
            LibraryType.WEBDAV -> "$protocol://$host:$port$path"
            LibraryType.QUARK -> "网盘"
        }
    }

    fun getTypeIcon(): String {
        return when (type) {
            LibraryType.WEBDAV -> "🌐"
            LibraryType.QUARK -> "☁️"
        }
    }
}

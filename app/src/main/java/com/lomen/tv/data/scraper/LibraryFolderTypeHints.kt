package com.lomen.tv.data.scraper

import com.lomen.tv.domain.model.MediaType
import java.util.Locale

/**
 * 按「类型分库存储」的目录名识别：从文件所在目录开始向上逐级查找，
 * 若某级文件夹名与已知类型根目录一致，则采用该类型（**离文件最近的一级优先**）。
 *
 * 示例（路径均为相对资源库的路径）：
 * - `电影/1.mp4` → 电影
 * - `媒体库/电视剧/4-1080p.mkv` → 电视剧
 * - `综艺/子目录/1 出嫁.mkv` → 综艺
 */
object LibraryFolderTypeHints {

    private val VIDEO_EXTENSIONS = listOf(
        ".mp4", ".mkv", ".avi", ".mov", ".wmv", ".flv", ".webm", ".m4v", ".3gp",
        ".ts", ".m2ts", ".vob", ".mpg", ".mpeg", ".rmvb", ".rm"
    )

    /**
     * 每种 [MediaType] 对应的文件夹名（英文不区分大小写，需与文件夹名 **完全一致**）
     */
    private val TYPE_BY_FOLDER_NAME: Map<String, MediaType> = buildMap {
        fun putAllNames(type: MediaType, names: Collection<String>) {
            names.forEach { name ->
                put(normalizeFolderKey(name), type)
            }
        }
        putAllNames(
            MediaType.MOVIE,
            listOf("电影", "movie", "movies", "影片", "films", "film")
        )
        putAllNames(
            MediaType.TV_SHOW,
            listOf("电视剧", "剧集", "tv", "tv shows", "tv series", "series", "剧集库")
        )
        putAllNames(
            MediaType.VARIETY,
            listOf("综艺", "variety", "variety show", "真人秀")
        )
        putAllNames(
            MediaType.DOCUMENTARY,
            listOf("纪录片", "纪实", "documentary", "documentaries")
        )
        putAllNames(
            MediaType.CONCERT,
            listOf("演唱会", "音乐会", "concert", "concerts", "live show")
        )
        putAllNames(
            MediaType.ANIME,
            listOf("动漫", "动画", "番剧", "anime", "animation")
        )
    }

    private fun normalizeFolderKey(name: String): String =
        name.trim().lowercase(Locale.ROOT)

    private fun looksLikeVideoFile(segment: String): Boolean {
        val lower = segment.lowercase(Locale.ROOT)
        return VIDEO_EXTENSIONS.any { lower.endsWith(it) }
    }

    /**
     * @param relativePath 相对资源库根目录的路径，可含文件名，如 `电影/a.mp4` 或 `媒体库/电视剧/剧名/S01E01.mkv`
     * @return 若路径中任一级目录名命中类型根目录则返回该类型，否则 null（由调用方回退到文件名/关键词逻辑）
     */
    fun detectMediaTypeFromPath(relativePath: String): MediaType? {
        val normalized = relativePath.replace('\\', '/').trim().trim('/')
        if (normalized.isEmpty()) return null
        val parts = normalized.split('/').map { it.trim() }.filter { it.isNotEmpty() }
        if (parts.isEmpty()) return null
        val dirParts = if (looksLikeVideoFile(parts.last())) {
            parts.dropLast(1)
        } else {
            parts
        }
        if (dirParts.isEmpty()) return null
        // 从文件所在目录向上到根：先检查紧邻父目录，再上一级…
        for (i in dirParts.indices.reversed()) {
            val segment = dirParts[i]
            if (segment.isEmpty()) continue
            val key = normalizeFolderKey(segment)
            TYPE_BY_FOLDER_NAME[key]?.let { return it }
        }
        return null
    }
}

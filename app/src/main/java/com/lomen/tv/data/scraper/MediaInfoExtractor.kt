package com.lomen.tv.data.scraper

import android.util.Log
import com.lomen.tv.domain.model.MediaType

/**
 * 媒体信息数据类
 */
data class MediaInfo(
    val title: String,           // 剧名/电影名（清理后，用于TMDB搜索）
    val displayTitle: String,    // 显示标题（保留原始信息）
    val year: Int? = null,
    val season: Int? = null,
    val episode: Int? = null,
    val type: MediaType = MediaType.MOVIE,
    val originalFilename: String = "",
    val tmdbId: String? = null
) {
    val isMovie: Boolean
        get() = type == MediaType.MOVIE
}

/**
 * 多规则媒体信息提取器
 * 
 * 支持的文件命名规则：
 * 
 * 【规则1】标准SxxExx格式（文件名包含剧名）
 *   - 淬火年代.2025.S01E01.mp4
 *   - 淬火年代.S01E01.2160p.HDR.mp4
 *   - The.Last.of.Us.S01E01.1080p.WEB-DL.mp4
 * 
 * 【规则2】带tmdbid的格式
 *   - 淬火年代.2025.S01E01.[tmdbid=233969].mp4
 *   - [tmdbid=233969]淬火年代.S01E01.mp4
 * 
 * 【规则3】纯集数格式（父文件夹为剧名）
 *   - 父文件夹：淬火年代
 *   - 文件名：01.mp4 / 01-4k.mp4 / 01高清.mp4 / 01xx.mp4 / 01-4k.高码率.mp4
 * 
 * 【规则4】SxxExx在文件名（父文件夹为剧名）
 *   - 父文件夹：淬火年代
 *   - 文件名：S01E01.mp4 / S01E01.2160p.HDR.mp4 / S03E01_4K_HDR_60fps.mkv
 * 
 * 【规则5】复杂格式
 *   - S01E01.2026.2160p.HDR.60fps.WEB-DL.H265.10bit.FLAC2.0–xinghanWEB.mp4
 */
object MediaInfoExtractor {
    private const val TAG = "MediaInfoExtractor"
    
    // 视频文件扩展名
    private val VIDEO_EXTENSIONS = listOf(
        ".mp4", ".mkv", ".avi", ".mov", ".wmv", ".flv", ".webm", ".m4v", ".3gp",
        ".ts", ".m2ts", ".vob", ".mpg", ".mpeg", ".rmvb", ".rm"
    )
    
    // 需要移除的技术参数词汇（用于清理标题）
    private val TECH_WORDS = listOf(
        // 分辨率
        "1080p", "720p", "2160p", "4k", "8k", "uhd", "fhd", "hd", "sd",
        // HDR相关
        "hdr", "hdr10", "hdr10+", "dolby vision", "dv", "hlg",
        // 音频
        "dolby", "atmos", "truehd", "dts", "dts-hd", "aac", "ac3", "flac", "flac2.0",
        // 编码
        "x264", "x265", "hevc", "h264", "h265", "h.264", "h.265", "avc", "av1", "10bit", "8bit",
        // 来源
        "bluray", "blu-ray", "bdrip", "brrip", "webrip", "web-dl", "webdl", "hdrip", "dvdrip", 
        "hdtv", "pdtv", "dsr", "dvdscr", "r5", "tc", "ts", "cam", "hdcam",
        // 帧率
        "60fps", "30fps", "24fps", "fps",
        // 字幕/语言
        "简体中文", "繁体中文", "中英字幕", "国语", "粤语", "英语", "日语", "韩语",
        "chinese", "english", "subbed", "dubbed", "sub", "dub",
        // 版本
        "uncut", "directors cut", "extended", "remastered", "unrated", "proper", "rerip",
        // 发布组
        "web", "amzn", "nf", "dsnp", "hmax", "atvp", "pmtp", "pcok", "hulu",
        // 其他
        "gb", "mb", "高清", "超清", "蓝光", "收藏版", "纪念版", "高码率"
    )
    
    // 演唱会相关词汇
    private val CONCERT_KEYWORDS = listOf(
        "演唱会", "音乐会", "concert", "live"
    )
    
    // 综艺相关词汇
    private val VARIETY_KEYWORDS = listOf(
        "综艺", "真人秀", "脱口秀", "variety", "show"
    )
    
    // 纪录片相关词汇
    private val DOCUMENTARY_KEYWORDS = listOf(
        "纪录片", "documentary"
    )
    
    /**
     * 主提取方法
     */
    fun extract(filename: String, parentFolder: String = ""): MediaInfo {
        Log.d(TAG, "========== 开始解析 ==========")
        Log.d(TAG, "文件名: $filename")
        Log.d(TAG, "父文件夹: $parentFolder")
        
        // 1. 提取tmdbid（优先级最高）
        val tmdbId = extractTmdbId(filename) ?: extractTmdbId(parentFolder)
        Log.d(TAG, "TMDB ID: $tmdbId")
        
        // 2. 移除扩展名
        val nameWithoutExt = removeExtension(filename)
        val folderWithoutExt = removeExtension(parentFolder)
        
        // 3. 提取季集信息
        val episodeInfo = extractEpisodeInfo(nameWithoutExt)
        Log.d(TAG, "季集信息: S${episodeInfo?.first ?: "?"}E${episodeInfo?.second ?: "?"}")
        
        // 4. 提取年份
        val yearFromFile = extractYear(nameWithoutExt)
        val yearFromFolder = extractYear(folderWithoutExt)
        val year = yearFromFile ?: yearFromFolder
        Log.d(TAG, "年份: $year (文件: $yearFromFile, 文件夹: $yearFromFolder)")
        
        // 5. 判断文件名类型并提取标题
        val (title, displayTitle) = extractTitle(nameWithoutExt, folderWithoutExt, episodeInfo)
        Log.d(TAG, "标题: $title")
        Log.d(TAG, "显示标题: $displayTitle")
        
        // 6. 判断媒体类型
        val mediaType = determineMediaType(title, filename, parentFolder, episodeInfo)
        Log.d(TAG, "媒体类型: $mediaType")
        
        val result = MediaInfo(
            title = title,
            displayTitle = displayTitle,
            year = year,
            season = episodeInfo?.first,
            episode = episodeInfo?.second,
            type = mediaType,
            originalFilename = filename,
            tmdbId = tmdbId
        )
        
        Log.d(TAG, "========== 解析完成 ==========")
        Log.d(TAG, "结果: $result")
        return result
    }
    
    /**
     * 提取标题
     * 返回 Pair<搜索标题, 显示标题>
     */
    private fun extractTitle(filename: String, parentFolder: String, episodeInfo: Pair<Int, Int>?): Pair<String, String> {
        val cleanedFilename = cleanTechWords(filename)
        val cleanedFolder = cleanTechWords(parentFolder)
        
        // 判断文件名类型
        val fileNameType = analyzeFileNameType(filename)
        Log.d(TAG, "文件名类型: $fileNameType")
        
        return when (fileNameType) {
            FileNameType.PURE_EPISODE_NUMBER -> {
                // 纯集数格式：01.mp4, 01-4k.mp4, 01高清.mp4
                // 使用父文件夹作为标题
                val title = extractTitleFromFolder(cleanedFolder)
                Pair(title, title)
            }
            
            FileNameType.EPISODE_ONLY -> {
                // 只有SxxExx没有剧名：S01E01.mp4
                // 使用父文件夹作为标题
                val title = extractTitleFromFolder(cleanedFolder)
                Pair(title, title)
            }
            
            FileNameType.FULL_INFO -> {
                // 完整格式：淬火年代.2025.S01E01.mp4
                // 从文件名提取标题
                val title = extractTitleBeforeSeasonEpisode(cleanedFilename)
                val displayTitle = if (title.isNotEmpty()) title else extractTitleFromFolder(cleanedFolder)
                Pair(title.ifEmpty { extractTitleFromFolder(cleanedFolder) }, displayTitle.ifEmpty { title })
            }
            
            FileNameType.MOVIE_OR_UNKNOWN -> {
                // 电影或未知格式
                val title = if (cleanedFilename.length > 2 && !cleanedFilename.matches(Regex("^\\d+$"))) {
                    cleanedFilename
                } else {
                    extractTitleFromFolder(cleanedFolder)
                }
                Pair(title, title)
            }
        }
    }
    
    /**
     * 分析文件名类型
     */
    private fun analyzeFileNameType(filename: String): FileNameType {
        val cleanName = removeExtension(filename)
        
        // 检查是否是纯集数（01, 01-4k, 01高清, 01xx等）
        if (cleanName.matches(Regex("^\\d{1,3}([\\-_.].*)?$"))) {
            return FileNameType.PURE_EPISODE_NUMBER
        }
        
        // 检查是否以SxxExx开头
        if (cleanName.matches(Regex("^[Ss]\\d{1,2}[Ee]\\d{1,3}.*"))) {
            return FileNameType.EPISODE_ONLY
        }
        
        // 检查是否包含SxxExx（中间位置）
        if (Regex("[Ss]\\d{1,2}[Ee]\\d{1,3}").containsMatchIn(cleanName)) {
            return FileNameType.FULL_INFO
        }
        
        // 检查其他集数格式
        if (Regex("第\\d{1,3}集|[Ee]\\d{1,3}|\\d{1,2}x\\d{1,3}").containsMatchIn(cleanName)) {
            return FileNameType.FULL_INFO
        }
        
        return FileNameType.MOVIE_OR_UNKNOWN
    }
    
    /**
     * 从文件夹名提取标题
     */
    private fun extractTitleFromFolder(folder: String): String {
        if (folder.isEmpty()) return ""
        
        var title = folder
        // 移除年份
        title = title.replace(Regex("\\(?(19|20)\\d{2}\\)?"), " ")
        // 移除tmdbid
        title = title.replace(Regex("\\[tmdbid=\\d+\\]"), " ")
        // 替换分隔符
        title = title.replace(".", " ").replace("_", " ").replace("-", " ")
        // 移除多余空格
        title = title.replace(Regex("\\s+"), " ").trim()
        
        return title
    }
    
    /**
     * 从文件名提取SxxExx之前的标题部分
     */
    private fun extractTitleBeforeSeasonEpisode(filename: String): String {
        // 查找 S01E01 位置
        val patterns = listOf(
            Regex("[Ss]\\d{1,2}[.\\s_-]*[Ee]\\d{1,3}"),
            Regex("第\\d{1,2}季"),
            Regex("[Ee]\\d{1,3}"),
            Regex("\\d{1,2}x\\d{1,3}")
        )
        
        for (pattern in patterns) {
            val match = pattern.find(filename)
            if (match != null && match.range.first > 0) {
                var title = filename.substring(0, match.range.first)
                // 移除年份
                title = title.replace(Regex("\\(?(19|20)\\d{2}\\)?"), " ")
                // 移除tmdbid
                title = title.replace(Regex("\\[tmdbid=\\d+\\]"), " ")
                // 替换分隔符
                title = title.replace(".", " ").replace("_", " ").replace("-", " ")
                // 移除多余空格
                title = title.replace(Regex("\\s+"), " ").trim()
                
                if (title.isNotEmpty() && title.length > 1) {
                    return title
                }
            }
        }
        
        return ""
    }
    
    /**
     * 清理技术参数词汇
     */
    private fun cleanTechWords(text: String): String {
        var result = text
        TECH_WORDS.forEach { word ->
            result = result.replace(word, " ", ignoreCase = true)
        }
        // 移除发布组标识（通常在末尾，格式如 -GroupName 或 @GroupName）
        result = result.replace(Regex("[\\-–@][A-Za-z0-9]+$"), "")
        result = result.replace(Regex("\\s+"), " ").trim()
        return result
    }
    
    /**
     * 提取季集信息
     */
    private fun extractEpisodeInfo(filename: String): Pair<Int, Int>? {
        // 按优先级排列的匹配模式
        val patterns = listOf(
            // 标准格式
            Regex("[Ss](\\d{1,2})[.\\s_-]*[Ee](\\d{1,3})"),           // S01E01, S01.E01, S01_E01
            Regex("[Ss]eason\\s*(\\d{1,2}).*[Ee]pisode\\s*(\\d{1,3})"), // Season 01 Episode 01
            Regex("(\\d{1,2})x(\\d{1,3})"),                            // 1x01
            Regex("第(\\d{1,2})季.*第(\\d{1,3})集"),                    // 第1季第1集
            
            // 只有集数（默认第1季）
            Regex("[Ee][Pp]?(\\d{1,3})"),                              // E01, EP01
            Regex("第(\\d{1,3})集"),                                   // 第01集
            
            // 纯数字格式（在文件名开头或作为整体）
            Regex("^(\\d{1,3})[\\-_.]"),                               // 01-, 01_, 01.
            Regex("^(\\d{1,3})$"),                                     // 纯数字 01
        )
        
        for (pattern in patterns) {
            val match = pattern.find(filename)
            if (match != null) {
                val groups = match.groupValues
                return when {
                    groups.size >= 3 -> {
                        // 有季和集
                        Pair(groups[1].toIntOrNull() ?: 1, groups[2].toIntOrNull() ?: 1)
                    }
                    groups.size == 2 -> {
                        // 只有集数，默认第1季
                        Pair(1, groups[1].toIntOrNull() ?: 1)
                    }
                    else -> null
                }
            }
        }
        
        return null
    }
    
    /**
     * 提取年份
     */
    private fun extractYear(text: String): Int? {
        // 优先匹配 .2025. 或 (2025) 格式
        val patterns = listOf(
            Regex("[.\\s_-](19|20\\d{2})[.\\s_-]"),  // .2025.
            Regex("\\((19|20\\d{2})\\)"),             // (2025)
            Regex("(19|20\\d{2})")                    // 任意位置
        )
        
        for (pattern in patterns) {
            val match = pattern.find(text)
            if (match != null) {
                val yearStr = match.groupValues.lastOrNull { it.length == 4 }
                val year = yearStr?.toIntOrNull()
                if (year != null && year in 1900..2099) {
                    return year
                }
            }
        }
        
        return null
    }
    
    /**
     * 提取TMDB ID
     */
    private fun extractTmdbId(text: String): String? {
        val patterns = listOf(
            Regex("\\[tmdbid=(\\d+)\\]", RegexOption.IGNORE_CASE),
            Regex("tmdbid[=\\-](\\d+)", RegexOption.IGNORE_CASE),
            Regex("\\{tmdb[\\-_]?(\\d+)\\}", RegexOption.IGNORE_CASE)
        )
        
        for (pattern in patterns) {
            val match = pattern.find(text)
            if (match != null) {
                return match.groupValues.getOrNull(1)
            }
        }
        
        return null
    }
    
    /**
     * 判断媒体类型
     */
    private fun determineMediaType(
        title: String, 
        filename: String, 
        parentFolder: String,
        episodeInfo: Pair<Int, Int>?
    ): MediaType {
        val combinedText = "$title $filename $parentFolder".lowercase()
        
        // 检查演唱会
        if (CONCERT_KEYWORDS.any { combinedText.contains(it.lowercase()) }) {
            return MediaType.CONCERT
        }
        
        // 检查综艺（需要有季集信息）
        if (episodeInfo != null && VARIETY_KEYWORDS.any { combinedText.contains(it.lowercase()) }) {
            return MediaType.VARIETY
        }
        
        // 检查纪录片
        if (DOCUMENTARY_KEYWORDS.any { combinedText.contains(it.lowercase()) }) {
            return MediaType.DOCUMENTARY
        }
        
        // 有季集信息 → 电视剧
        if (episodeInfo != null) {
            return MediaType.TV_SHOW
        }
        
        // 默认电影
        return MediaType.MOVIE
    }
    
    /**
     * 移除文件扩展名
     */
    private fun removeExtension(filename: String): String {
        VIDEO_EXTENSIONS.forEach { ext ->
            if (filename.lowercase().endsWith(ext)) {
                return filename.substring(0, filename.length - ext.length)
            }
        }
        return filename
    }
    
    /**
     * 文件名类型枚举
     */
    private enum class FileNameType {
        PURE_EPISODE_NUMBER,  // 纯集数：01.mp4, 01-4k.mp4
        EPISODE_ONLY,         // 只有季集：S01E01.mp4
        FULL_INFO,            // 完整信息：淬火年代.S01E01.mp4
        MOVIE_OR_UNKNOWN      // 电影或未知
    }
}

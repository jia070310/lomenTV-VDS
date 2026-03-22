package com.lomen.tv.data.scraper

import android.util.Log
import com.lomen.tv.data.preferences.MediaClassificationStrategyHolder
import com.lomen.tv.domain.model.MediaClassificationStrategy
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
 *   - 文件名：01.mp4 / 01-4k.mp4 / 01高清.mp4 / 01xx.mp4 / 01-4k.高码率.mp4 / 01 4K.国&粤.mp4
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

    // 仅匹配“独立年份标记”（前后有分隔符或括号），避免误删标题内数字（如：你好1983）
    private val STANDALONE_YEAR_REGEX = Regex(
        """(^|[.\s_\-\(\[【])((?:19|20)\d{2})($|[.\s_\-\)\]】])"""
    )

    /** 日期分期综艺：如 2024-01-15、2024.12.31 */
    private val DATE_EPISODE_REGEX =
        Regex("""(20\d{2})[-_. ](0[1-9]|1[0-2])[-_. ](0[1-9]|[12]\d|3[01])""")
    
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
        "综艺", "真人秀", "脱口秀", "选秀", "variety", "reality", "talk show", "season"
    )

    // 动漫相关词汇
    private val ANIME_KEYWORDS = listOf(
        "动漫", "动画", "番剧", "ova", "剧场版", "anime", "animation"
    )
    
    // 纪录片相关词汇
    private val DOCUMENTARY_KEYWORDS = listOf(
        "纪录片", "纪实", "bbc", "discovery", "national geographic", "documentary", "docu"
    )
    
    /**
     * 主提取方法
     *
     * @param fullRelativePath 相对资源库根的路径（含文件名更佳），用于「按类型分目录」时从路径向上匹配类型文件夹名；
     * 若为空则仅用 [parentFolder]/[filename] 做单层目录名匹配。
     */
    fun extract(
        filename: String,
        parentFolder: String = "",
        fullRelativePath: String = ""
    ): MediaInfo {
        Log.d(TAG, "========== 开始解析 ==========")
        Log.d(TAG, "文件名: $filename")
        Log.d(TAG, "父文件夹: $parentFolder")
        Log.d(TAG, "相对路径: $fullRelativePath")
        
        // 1. 提取tmdbid（优先级最高）
        val tmdbId = extractTmdbId(filename) ?: extractTmdbId(parentFolder)
        Log.d(TAG, "TMDB ID: $tmdbId")
        
        // 2. 移除扩展名
        val nameWithoutExt = removeExtension(filename)
        val folderWithoutExt = removeExtension(parentFolder)
        
        // 3. 提取季集信息（文件名）
        val episodeInfo = extractEpisodeInfo(nameWithoutExt)
        Log.d(TAG, "季集信息(文件): S${episodeInfo?.first ?: "?"}E${episodeInfo?.second ?: "?"}")
        
        // 4. 提取年份
        val yearFromFile = extractYear(nameWithoutExt)
        val yearFromFolder = extractYear(folderWithoutExt)
        val year = yearFromFile ?: yearFromFolder
        Log.d(TAG, "年份: $year (文件: $yearFromFile, 文件夹: $yearFromFolder)")
        
        // 5. 判断文件名类型并提取标题（文件夹名可解析「第X季」→ 搜索用剧名 + 季号）
        val titleEx = extractTitle(nameWithoutExt, folderWithoutExt)
        val title = titleEx.title
        val displayTitle = titleEx.displayTitle
        val seasonFromFolder = titleEx.seasonFromFolder
        val mergedSeason = seasonFromFolder ?: episodeInfo?.first
        val mergedEpisode = episodeInfo?.second
        Log.d(TAG, "标题: $title")
        Log.d(TAG, "显示标题: $displayTitle")
        if (seasonFromFolder != null) {
            Log.d(TAG, "文件夹解析季号: $seasonFromFolder，合并后 S${mergedSeason ?: "?"}")
        }
        
        // 6. 判断媒体类型：优先「目录类型」分库（路径自下而上匹配类型根文件夹名），否则再按策略/关键词/结构
        val pathForHint = when {
            fullRelativePath.isNotBlank() -> fullRelativePath
            parentFolder.isNotBlank() -> "$parentFolder/$filename"
            else -> ""
        }
        val typeFromFolderHint =
            if (pathForHint.isNotBlank()) LibraryFolderTypeHints.detectMediaTypeFromPath(pathForHint) else null
        val mediaType = typeFromFolderHint
            ?: determineMediaType(title, filename, parentFolder, episodeInfo)
        if (typeFromFolderHint != null) {
            Log.d(TAG, "目录类型命中: $typeFromFolderHint (路径: $pathForHint)")
        }
        Log.d(TAG, "媒体类型: $mediaType")
        
        val result = MediaInfo(
            title = title,
            displayTitle = displayTitle,
            year = year,
            season = mergedSeason,
            episode = mergedEpisode,
            type = mediaType,
            originalFilename = filename,
            tmdbId = tmdbId
        )
        
        Log.d(TAG, "========== 解析完成 ==========")
        Log.d(TAG, "结果: $result")
        return result
    }
    
    private data class TitleExtract(
        val title: String,
        val displayTitle: String,
        /** 从父文件夹名解析的季（如 圆桌派 第1季） */
        val seasonFromFolder: Int? = null
    )

    /**
     * 提取标题；若剧名在文件夹上且含「第X季」，会剥离季信息并返回 [TitleExtract.seasonFromFolder]。
     */
    private fun extractTitle(filename: String, parentFolder: String): TitleExtract {
        val cleanedFilename = cleanTechWords(filename)
        val cleanedFolder = cleanTechWords(parentFolder)
        
        // 判断文件名类型
        val fileNameType = analyzeFileNameType(filename)
        Log.d(TAG, "文件名类型: $fileNameType")
        
        return when (fileNameType) {
            FileNameType.PURE_EPISODE_NUMBER -> {
                // 纯集数格式：01.mp4, 01-4k.mp4, 01高清.mp4
                // 使用父文件夹作为标题
                val (t, season) = extractTitleAndSeasonFromFolder(cleanedFolder)
                TitleExtract(t, t, season)
            }
            
            FileNameType.EPISODE_ONLY -> {
                // 只有SxxExx没有剧名：S01E01.mp4
                // 使用父文件夹作为标题
                val (t, season) = extractTitleAndSeasonFromFolder(cleanedFolder)
                TitleExtract(t, t, season)
            }
            
            FileNameType.FULL_INFO -> {
                // 完整格式：淬火年代.2025.S01E01.mp4
                // 从文件名提取标题
                val title = extractTitleBeforeSeasonEpisode(cleanedFilename)
                val (folderTitle, folderSeason) = extractTitleAndSeasonFromFolder(cleanedFolder)
                val displayTitle = if (title.isNotEmpty()) title else folderTitle
                val primary = title.ifEmpty { folderTitle }
                TitleExtract(primary, displayTitle.ifEmpty { primary }, folderSeason.takeIf { title.isEmpty() })
            }
            
            FileNameType.MOVIE_OR_UNKNOWN -> {
                // 电影或未知格式
                val (folderTitle, folderSeason) = extractTitleAndSeasonFromFolder(cleanedFolder)
                val title = if (cleanedFilename.length > 2 && !cleanedFilename.matches(Regex("^\\d+$"))) {
                    cleanedFilename
                } else {
                    folderTitle
                }
                TitleExtract(title, title, folderSeason.takeIf { title == folderTitle })
            }
        }
    }
    
    /**
     * 分析文件名类型
     */
    private fun analyzeFileNameType(filename: String): FileNameType {
        val cleanName = removeExtension(filename)
        
        // 检查是否是纯集数（01, 01-4k, 01高清, 01xx, 01 4K.国&粤 等）
        if (cleanName.matches(Regex("^\\d{1,3}([\\-_.\\s].*)?$"))) {
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
     * 从文件夹名提取标题，并解析「第X季 / S02 / Season 1」为季号，从剧名中剥离。
     */
    private fun extractTitleAndSeasonFromFolder(folder: String): Pair<String, Int?> {
        if (folder.isEmpty()) return "" to null
        
        var title = folder
        title = removeStandaloneYear(title)
        title = title.replace(Regex("\\[tmdbid=\\d+\\]"), " ")
        val (withoutSeason, season) = FolderSeriesNameParser.stripSeasonMarkers(title)
        title = withoutSeason
        title = title.replace(".", " ").replace("_", " ").replace("-", " ")
        title = title.replace(Regex("\\s+"), " ").trim()
        
        return title to season
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
                title = removeStandaloneYear(title)
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
            // 前缀排序数字 + 技术标签（如: 01 4K.国&粤）
            Regex(
                "^(\\d{1,3})\\s+(?:2160p|1080p|720p|480p|4k|8k|hd|国语|粤语|国粤|国&粤|中字|中英|简中|繁中|x264|x265|h264|h265).*",
                RegexOption.IGNORE_CASE
            ),
            
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
            Regex("[.\\s_-]((?:19|20)\\d{2})[.\\s_-]"),  // .2025.
            Regex("\\(((?:19|20)\\d{2})\\)"),            // (2025)
            Regex("((?:19|20)\\d{2})")                   // 任意位置
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
        return when (MediaClassificationStrategyHolder.strategy) {
            MediaClassificationStrategy.KEYWORD_FIRST ->
                determineMediaTypeKeywordFirst(title, filename, parentFolder, episodeInfo)
            MediaClassificationStrategy.STRUCTURE_FIRST ->
                determineMediaTypeStructureFirst(title, filename, parentFolder, episodeInfo)
        }
    }

    /**
     * 结构优先：先看季集标识，再用关键词细分综艺/纪录片/动漫等（适合规范命名）
     */
    private fun determineMediaTypeStructureFirst(
        title: String,
        filename: String,
        parentFolder: String,
        episodeInfo: Pair<Int, Int>?
    ): MediaType {
        val combinedText = "$title $filename $parentFolder".lowercase()
        val hasEpisodeInfo = episodeInfo != null
        val hasDateEpisodePattern = DATE_EPISODE_REGEX.containsMatchIn(filename)

        val isVariety = VARIETY_KEYWORDS.any { combinedText.contains(it.lowercase()) }
        val isAnime = ANIME_KEYWORDS.any { combinedText.contains(it.lowercase()) }
        val isDocumentary = DOCUMENTARY_KEYWORDS.any { combinedText.contains(it.lowercase()) }
        val isConcert = CONCERT_KEYWORDS.any { combinedText.contains(it.lowercase()) }

        if (isAnime) return MediaType.ANIME
        if (isDocumentary) return MediaType.DOCUMENTARY

        if (hasEpisodeInfo) {
            if (isVariety || hasDateEpisodePattern) return MediaType.VARIETY
            return MediaType.TV_SHOW
        }

        if (isConcert) return MediaType.CONCERT
        if (isVariety && hasDateEpisodePattern) return MediaType.VARIETY

        return MediaType.MOVIE
    }

    /**
     * 关键词优先：先看路径/文件名中的类型词，再看季集结构（适合按「综艺/纪录片」等分文件夹整理的资源）
     */
    private fun determineMediaTypeKeywordFirst(
        title: String,
        filename: String,
        parentFolder: String,
        episodeInfo: Pair<Int, Int>?
    ): MediaType {
        val combinedText = "$title $filename $parentFolder".lowercase()
        val hasEpisodeInfo = episodeInfo != null
        val hasDateEpisodePattern = DATE_EPISODE_REGEX.containsMatchIn(filename)

        val isVariety = VARIETY_KEYWORDS.any { combinedText.contains(it.lowercase()) }
        val isAnime = ANIME_KEYWORDS.any { combinedText.contains(it.lowercase()) }
        val isDocumentary = DOCUMENTARY_KEYWORDS.any { combinedText.contains(it.lowercase()) }
        val isConcert = CONCERT_KEYWORDS.any { combinedText.contains(it.lowercase()) }

        if (isConcert) return MediaType.CONCERT
        if (isVariety || hasDateEpisodePattern) return MediaType.VARIETY
        if (isDocumentary) return MediaType.DOCUMENTARY
        if (isAnime) return MediaType.ANIME

        if (hasEpisodeInfo) return MediaType.TV_SHOW
        return MediaType.MOVIE
    }

    /**
     * 移除“独立年份标记”
     * 例如：`剧名 (2024)`、`剧名.2024.` 会移除；`你好1983` 不移除
     */
    private fun removeStandaloneYear(text: String): String {
        return STANDALONE_YEAR_REGEX.replace(text) { match ->
            val prefix = match.groupValues[1]
            val suffix = match.groupValues[3]
            when {
                prefix.isNotEmpty() && suffix.isNotEmpty() -> "$prefix $suffix"
                prefix.isNotEmpty() -> "$prefix "
                suffix.isNotEmpty() -> " $suffix"
                else -> " "
            }
        }
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

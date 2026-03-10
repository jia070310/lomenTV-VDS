package com.lomen.tv.utils

import com.lomen.tv.domain.model.MediaType
import java.util.regex.Pattern

/**
 * 文件名解析器
 * 用于从文件名中提取影视信息（名称、年份、季、集等）
 */
object FileNameParser {

    // 视频文件扩展名
    private val VIDEO_EXTENSIONS = listOf(
        "mp4", "mkv", "avi", "mov", "wmv", "flv", "webm", "m4v", "ts", "m2ts"
    )

    // 需要过滤的无意义词汇
    private val FILTER_WORDS = listOf(
        "1080p", "1080P", "720p", "720P", "2160p", "2160P", "4k", "4K", "8k", "8K",
        "bluray", "BluRay", "BLURAY", "bdrip", "BDRip", "brrip", "BRRip",
        "webrip", "WEBRip", "web-dl", "WEB-DL", "hdtv", "HDTV", "hdrip", "HDRip",
        "x264", "x265", "h264", "h265", "hevc", "HEVC", "avc", "AVC",
        "aac", "dts", "ac3", "dd5.1", "ddp5.1", "atmos",
        "hdr", "HDR", "sdr", "SDR", "dv", "DV", "dolby vision",
        "remux", "REMUX", "proper", "PROPER", "repack", "REPACK",
        "extended", "EXTENDED", "edition", "EDITION", "cut", "CUT", "directors cut", "theatrical", "uncut",
        // 常见中文/混合信息
        "hd", "HD", "国语", "中字", "国语中字", "HD国语中字", "中英字幕", "简体中文", "繁体中文",
        "subs", "subbed", "dubbed", "dual audio", "multi audio",
        "internal", "limited", "complete", "final",
        "www", "com", "net", "org", "cc"
    )

    // 剧集识别模式
    private val EPISODE_PATTERNS = listOf(
        // S01E01 格式（标准格式）
        Pattern.compile("""[.\s_-]*[Ss](\d+)[Ee](\d+)[.\s_-]*"""),
        // S01 E01 格式（带空格）
        Pattern.compile("""[.\s_-]*[Ss](\d+)[.\s_-]*[Ee](\d+)[.\s_-]*"""),
        // Season 01 Episode 01 格式（完整英文）
        Pattern.compile("""[.\s_-]*[Ss]eason\s*(\d+).*?[Ee]pisode\s*(\d+)[.\s_-]*"""),
        // 1x01 格式
        Pattern.compile("""[.\s_-]*(\d+)x(\d+)[.\s_-]*"""),
        // 第1季第1集 / 第1季.第1集 / 第一季第一集 等格式（允许中间有分隔符）
        Pattern.compile("""[.\s_-]*第?([0-9一二三四五六七八九十]+)季.*?第?([0-9一二三四五六七八九十]+)集?[.\s_-]*"""),
        // EP01 或 E01 格式
        Pattern.compile("""[.\s_-]*[Ee][Pp]?(\d+)[.\s_-]*"""),
        // 第01集 格式（无季号）
        Pattern.compile("""[.\s_-]*第([0-9一二三四五六七八九十]+)集[.\s_-]*"""),
        // 纯数字集数格式（如 01, 1, 001）
        Pattern.compile("""^([0-9]+)$""")
    )

    // 年份识别模式
    private val YEAR_PATTERN = Pattern.compile("""[.\s_-]*(19\d{2}|20\d{2})[.\s_-]*""")

    // 分辨率识别
    private val RESOLUTION_PATTERN = Pattern.compile("""(2160p|1080p|720p|480p|4k|8k)""", Pattern.CASE_INSENSITIVE)

    /**
     * 解析结果数据类
     */
    data class ParseResult(
        val title: String,
        val year: Int? = null,
        val season: Int? = null,
        val episode: Int? = null,
        val type: MediaType = MediaType.MOVIE,
        val resolution: String? = null,
        val isValid: Boolean = true
    )

    /**
     * 解析文件名
     */
    fun parse(fileName: String): ParseResult {
        // 移除扩展名
        val nameWithoutExt = removeExtension(fileName)
        
        // 提取年份
        val year = extractYear(nameWithoutExt)
        
        // 提取季和集
        val (season, episode) = extractSeasonEpisode(nameWithoutExt)
        
        // 判断类型
        val type = if (season != null || episode != null) MediaType.TV_SHOW else MediaType.MOVIE
        
        // 提取分辨率
        val resolution = extractResolution(fileName)
        
        // 清理标题
        var cleanTitle = cleanFileName(nameWithoutExt)
        
        // 移除年份信息
        year?.let { cleanTitle = cleanTitle.replace(it.toString(), "") }
        
        // 移除季集信息
        cleanTitle = removeSeasonEpisodeInfo(cleanTitle)
        
        // 最终清理
        cleanTitle = finalClean(cleanTitle)
        
        return ParseResult(
            title = cleanTitle,
            year = year,
            season = season,
            episode = episode,
            type = type,
            resolution = resolution,
            isValid = cleanTitle.isNotBlank()
        )
    }

    /**
     * 移除文件扩展名
     */
    private fun removeExtension(fileName: String): String {
        val lastDot = fileName.lastIndexOf('.')
        return if (lastDot > 0) fileName.substring(0, lastDot) else fileName
    }

    /**
     * 提取年份
     */
    private fun extractYear(fileName: String): Int? {
        val matcher = YEAR_PATTERN.matcher(fileName)
        if (matcher.find()) {
            return matcher.group(1)?.toIntOrNull()
        }
        return null
    }

    /**
     * 提取季和集
     */
    private fun extractSeasonEpisode(fileName: String): Pair<Int?, Int?> {
        for (pattern in EPISODE_PATTERNS) {
            val matcher = pattern.matcher(fileName)
            if (matcher.find()) {
                val groupCount = matcher.groupCount()
                
                if (groupCount >= 2) {
                    // S01E01 或 1x01 格式
                    val seasonStr = matcher.group(1)
                    val episodeStr = matcher.group(2)
                    
                    val season = seasonStr?.let { parseNumber(it) }
                    val episode = episodeStr?.let { parseNumber(it) }
                    
                    // 如果只匹配到一个数字，可能是集号
                    if (season == null && episode != null) {
                        return Pair(null, episode)
                    }
                    
                    return Pair(season, episode)
                } else if (groupCount == 1) {
                    // EP01 或 第01集 格式，只有集号
                    val episodeStr = matcher.group(1)
                    val episode = episodeStr?.let { parseNumber(it) }
                    return Pair(null, episode)
                }
            }
        }
        return Pair(null, null)
    }

    /**
     * 解析数字（支持中文数字）
     */
    private fun parseNumber(str: String): Int? {
        // 先尝试直接解析
        str.toIntOrNull()?.let { return it }
        
        // 解析中文数字
        return parseChineseNumber(str)
    }

    /**
     * 解析中文数字
     */
    private fun parseChineseNumber(str: String): Int? {
        val chineseNumbers = mapOf(
            '一' to 1, '二' to 2, '三' to 3, '四' to 4, '五' to 5,
            '六' to 6, '七' to 7, '八' to 8, '九' to 9, '十' to 10
        )
        
        var result = 0
        var temp = 0
        
        for (char in str) {
            val num = chineseNumbers[char]
            if (num != null) {
                if (num == 10) {
                    if (temp == 0) temp = 1
                    result += temp * 10
                    temp = 0
                } else {
                    temp = num
                }
            }
        }
        
        result += temp
        return if (result > 0) result else null
    }

    /**
     * 提取分辨率
     */
    private fun extractResolution(fileName: String): String? {
        val matcher = RESOLUTION_PATTERN.matcher(fileName)
        return if (matcher.find()) matcher.group(1)?.uppercase() else null
    }

    /**
     * 清理文件名
     */
    private fun cleanFileName(fileName: String): String {
        var cleaned = fileName
        
        // 替换分隔符为空格
        cleaned = cleaned.replace(".", " ")
            .replace("_", " ")
            .replace("-", " ")

        // 移除常见括号/方括号/花括号符号（避免残留 "()", "[]" 之类进入标题）
        cleaned = cleaned.replace("(", " ")
            .replace(")", " ")
            .replace("[", " ")
            .replace("]", " ")
            .replace("{", " ")
            .replace("}", " ")
        
        // 移除过滤词汇
        for (word in FILTER_WORDS) {
            cleaned = cleaned.replace(word, "", ignoreCase = true)
        }
        
        return cleaned
    }

    /**
     * 移除季集信息
     */
    private fun removeSeasonEpisodeInfo(fileName: String): String {
        var cleaned = fileName
        
        for (pattern in EPISODE_PATTERNS) {
            cleaned = pattern.matcher(cleaned).replaceAll(" ")
        }
        
        // 移除"第X季"、"第X集"等残留
        cleaned = cleaned.replace(Regex("""第[0-9一二三四五六七八九十]+季"""), " ")
        cleaned = cleaned.replace(Regex("""第[0-9一二三四五六七八九十]+集"""), " ")
        
        return cleaned
    }

    /**
     * 最终清理
     */
    private fun finalClean(fileName: String): String {
        return fileName
            .trim()
            .replace(Regex("""\s+"""), " ")  // 多个空格合并
            .replace(Regex("""^\s+|\s+$"""), "")  // 移除首尾空格
    }

    /**
     * 检查是否是视频文件
     */
    fun isVideoFile(fileName: String): Boolean {
        val extension = fileName.substringAfterLast('.', "").lowercase()
        return VIDEO_EXTENSIONS.contains(extension)
    }

    /**
     * 批量解析文件名
     */
    fun parseBatch(fileNames: List<String>): List<ParseResult> {
        return fileNames.map { parse(it) }
    }

    /**
     * 根据文件名判断是否为剧集文件
     */
    fun isEpisodeFile(fileName: String): Boolean {
        val result = parse(fileName)
        return result.type == MediaType.TV_SHOW
    }

    /**
     * 提取系列名称（用于剧集分组）
     */
    fun extractSeriesName(fileName: String): String {
        val result = parse(fileName)
        return result.title
    }
}

/**
 * 扩展函数：简化调用
 */
fun String.parseFileName(): FileNameParser.ParseResult {
    return FileNameParser.parse(this)
}

fun String.isVideoFile(): Boolean {
    return FileNameParser.isVideoFile(this)
}

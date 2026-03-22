package com.lomen.tv.data.scraper

/**
 * 从剧集/资源文件夹名中解析「第几季」，并剥离季标记，得到用于 TMDB 搜索的剧名。
 *
 * 例：`圆桌派 第1季` → `圆桌派`, season=1
 * `Show Name S02` → `Show Name`, season=2
 */
object FolderSeriesNameParser {

    private val arabicSeason = Regex("""第\s*([0-9]+)\s*季""")
    private val cnSeason = Regex("""第\s*([一二三四五六七八九十百千万两]+)\s*季""")
    private val sSeasonLoose = Regex("""(?i)S(?:eason)?\s*(\d{1,2})""")
    private val seasonWord = Regex("""(?i)Season\s*(\d{1,2})""")

    /**
     * @return first: 去掉季信息后的名称；second: 解析到的季号（若未识别则为 null）
     */
    fun stripSeasonMarkers(rawFolderName: String): Pair<String, Int?> {
        var s = rawFolderName.trim()
        if (s.isEmpty()) return "" to null

        arabicSeason.find(s)?.let { m ->
            val n = m.groupValues[1].toIntOrNull() ?: return@let
            s = s.removeRange(m.range).trim()
            return normalizeSpaces(s) to n
        }
        cnSeason.find(s)?.let { m ->
            val n = chineseNumeralToInt(m.groupValues[1]) ?: return@let
            s = s.removeRange(m.range).trim()
            return normalizeSpaces(s) to n
        }
        seasonWord.find(s)?.let { m ->
            val n = m.groupValues[1].toIntOrNull() ?: return@let
            s = s.removeRange(m.range).trim()
            return normalizeSpaces(s) to n
        }
        sSeasonLoose.find(s)?.let { m ->
            val n = m.groupValues[1].toIntOrNull() ?: return@let
            s = s.removeRange(m.range).trim()
            return normalizeSpaces(s) to n
        }
        return normalizeSpaces(s) to null
    }

    /**
     * 反复剥离所有「第X季 / Sxx / Season x」片段，直到标题中不再含季标记。
     * 用于展示名：避免 TMDB 已带「第八季」又拼接本地「第2季」→「…第八季 第2季」。
     */
    fun stripAllSeasonMarkers(raw: String): String {
        var s = raw.trim()
        if (s.isEmpty()) return ""
        var previous = ""
        while (s != previous) {
            previous = s
            val (next, _) = stripSeasonMarkers(s)
            s = next.trim()
        }
        return normalizeSpaces(s)
    }

    private fun normalizeSpaces(s: String): String =
        s.replace(Regex("\\s+"), " ").trim()

    private fun chineseNumeralToInt(s: String): Int? {
        if (s.isEmpty()) return null
        val map = mapOf(
            '零' to 0, '一' to 1, '二' to 2, '三' to 3, '四' to 4, '五' to 5,
            '六' to 6, '七' to 7, '八' to 8, '九' to 9, '十' to 10,
            '两' to 2, '壹' to 1, '贰' to 2, '叁' to 3, '肆' to 4, '伍' to 5,
            '陆' to 6, '柒' to 7, '捌' to 8, '玖' to 9
        )
        if (s.length == 1) {
            val d = map[s[0]]
            if (d != null && d in 1..9) return d
        }
        // 十、十一、十二、二十
        if (s == "十") return 10
        if (s.startsWith("十") && s.length == 2) {
            val u = map[s[1]] ?: return null
            return 10 + u
        }
        if (s.endsWith("十") && s.length == 2) {
            val t = map[s[0]] ?: return null
            return t * 10
        }
        if (s.length == 3 && s[1] == '十') {
            val a = map[s[0]] ?: return null
            val b = map[s[2]] ?: return null
            return a * 10 + b
        }
        return s.toIntOrNull()
    }
}

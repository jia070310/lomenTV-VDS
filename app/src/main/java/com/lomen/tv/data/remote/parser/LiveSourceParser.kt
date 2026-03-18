package com.lomen.tv.data.remote.parser

import android.util.Log
import com.lomen.tv.data.model.live.LiveChannel
import com.lomen.tv.data.model.live.LiveChannelGroup
import com.lomen.tv.data.model.live.LiveChannelGroupList
import com.lomen.tv.data.model.live.LiveChannelList
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.net.URL
import java.util.concurrent.TimeUnit

/**
 * 直播源解析器
 * 支持 M3U/M3U8 格式
 */
class LiveSourceParser {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    /**
     * 从 URL 解析直播源
     * 如果是 aptv.app 域名，使用 APTV 专用 UA
     */
    fun parseFromUrl(url: String): Result<LiveChannelGroupList> {
        return try {
            // 判断是否需要使用 APTV 专用 UA
            val userAgent = if (url.contains("aptv.app", ignoreCase = true)) {
                com.lomen.tv.data.preferences.LiveSettingsPreferences.Companion.APTV_SPECIAL_UA
            } else {
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"
            }
            
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", userAgent)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return Result.failure(IOException("请求失败: ${response.code}"))
                }
                val content = response.body?.string() ?: ""
                Result.success(parseM3uContent(content))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 从文本内容解析 M3U 格式
     */
    fun parseM3uContent(content: String): LiveChannelGroupList {
        val lines = content.lines().map { it.trim() }
        
        if (lines.isEmpty() || !lines[0].startsWith("#EXTM3U")) {
            // 尝试解析简单文本格式（每行一个URL）
            return parseSimpleFormat(lines)
        }

        // 解析全局属性（从第一行 #EXTM3U）
        val firstLine = lines[0]
        val globalEpgUrls = extractXTvgUrl(firstLine)

        val groups = mutableMapOf<String, MutableList<LiveChannel>>()
        var currentChannel: LiveChannel? = null
        var currentGroup = "默认分组"

        for (line in lines) {
            when {
                line.startsWith("#EXTINF:") -> {
                    // 解析频道信息
                    val name = extractChannelName(line)
                    val group = extractGroupTitle(line) ?: currentGroup
                    // 优先提取 http-user-agent，其次 tvg-user-agent
                    val userAgent = extractAttribute(line, "http-user-agent") 
                        ?: extractAttribute(line, "tvg-user-agent")
                    val referer = extractAttribute(line, "http-referer")
                        ?: extractAttribute(line, "tvg-referer")
                    
                    android.util.Log.d("LiveSourceParser", "解析频道: $name, UA: $userAgent, Referer: $referer")
                    
                    currentChannel = LiveChannel(
                        name = name,
                        channelName = name,
                        userAgent = userAgent,
                        referer = referer,
                        epgUrls = globalEpgUrls, // 继承全局 EPG URL
                    )
                    currentGroup = group
                }
                line.isNotBlank() && !line.startsWith("#") -> {
                    // 这是URL行
                    currentChannel?.let { channel ->
                        val url = line
                        val updatedChannel = channel.copy(urlList = listOf(url))
                        
                        groups.getOrPut(currentGroup) { mutableListOf() }.add(updatedChannel)
                    }
                    currentChannel = null
                }
            }
        }

        // 转换为 LiveChannelGroupList
        val groupList = groups.map { (name, channels) ->
            LiveChannelGroup(
                name = name,
                channelList = LiveChannelList(channels)
            )
        }

        return LiveChannelGroupList(groupList)
    }

    /**
     * 解析简单格式（每行一个URL）
     */
    private fun parseSimpleFormat(lines: List<String>): LiveChannelGroupList {
        val channels = lines
            .filter { it.isNotBlank() && (it.startsWith("http://") || it.startsWith("https://")) }
            .mapIndexed { index, url ->
                LiveChannel(
                    name = "频道${index + 1}",
                    channelName = "频道${index + 1}",
                    urlList = listOf(url),
                )
            }

        return LiveChannelGroupList(
            listOf(
                LiveChannelGroup(
                    name = "默认分组",
                    channelList = LiveChannelList(channels)
                )
            )
        )
    }

    /**
     * 从 EXTINF 行提取频道名称
     */
    private fun extractChannelName(line: String): String {
        // 尝试从 tvg-name 属性提取
        val tvgName = extractAttribute(line, "tvg-name")
        if (tvgName != null) return tvgName

        // 从逗号后提取
        val commaIndex = line.lastIndexOf(",")
        if (commaIndex != -1 && commaIndex < line.length - 1) {
            return line.substring(commaIndex + 1).trim()
        }

        return "未知频道"
    }

    /**
     * 从 EXTINF 行提取分组名称
     */
    private fun extractGroupTitle(line: String): String? {
        return extractAttribute(line, "group-title")
    }

    /**
     * 从 EXTINF 行提取指定属性
     */
    private fun extractAttribute(line: String, attrName: String): String? {
        val regex = "$attrName=\"([^\"]*)\"".toRegex()
        val match = regex.find(line)
        return match?.groupValues?.get(1)
    }

    /**
     * 从 #EXTM3U 行提取 x-tvg-url（可能包含多个URL，用逗号分隔）
     */
    private fun extractXTvgUrl(line: String): List<String> {
        val regex = "x-tvg-url=\"([^\"]*)\"".toRegex()
        val match = regex.find(line)
        return match?.groupValues?.get(1)?.let { urlString ->
            // 支持多个URL，用逗号或空格分隔
            urlString.split(",", " ").map { it.trim() }.filter { it.isNotBlank() }
        } ?: emptyList()
    }
}

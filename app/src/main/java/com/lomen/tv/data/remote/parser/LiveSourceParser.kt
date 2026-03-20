package com.lomen.tv.data.remote.parser

import android.util.Log
import com.lomen.tv.data.model.live.LiveChannel
import com.lomen.tv.data.model.live.LiveChannelGroup
import com.lomen.tv.data.model.live.LiveChannelGroupList
import com.lomen.tv.data.model.live.LiveChannelList
import okhttp3.Dns
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.net.InetAddress
import java.net.URL
import java.util.concurrent.TimeUnit

/**
 * 直播源解析器
 * 支持 M3U/M3U8 格式
 */
class LiveSourceParser {

    companion object {
        private const val TAG = "LiveSourceParser"
        
        // gh.aptv.app 的正确 IP 地址（通过 8.8.8.8 DNS 解析）
        private val APTV_APP_IP = listOf(
            InetAddress.getByName("104.21.82.229"),
            InetAddress.getByName("172.67.184.198")
        )
    }
    
    // 自定义 DNS 解析器
    private val customDns = object : Dns {
        override fun lookup(hostname: String): List<InetAddress> {
            return when {
                hostname.contains("aptv.app", ignoreCase = true) -> {
                    Log.d(TAG, "使用自定义 DNS 解析: $hostname -> $APTV_APP_IP")
                    APTV_APP_IP
                }
                else -> Dns.SYSTEM.lookup(hostname)
            }
        }
    }

    private val client = OkHttpClient.Builder()
        .dns(customDns)
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
            val isAptvDomain = url.contains("aptv.app", ignoreCase = true)
            val userAgent = if (isAptvDomain) {
                Log.d(TAG, "使用 APTV 专用 UA 请求: $url")
                com.lomen.tv.data.preferences.LiveSettingsPreferences.Companion.APTV_SPECIAL_UA
            } else {
                Log.d(TAG, "使用默认 UA 请求: $url")
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"
            }
            
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", userAgent)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e(TAG, "请求失败: ${response.code}")
                    return Result.failure(IOException("请求失败: ${response.code}"))
                }
                val content = response.body?.string() ?: ""
                Log.d(TAG, "获取内容长度: ${content.length}")
                Result.success(parseM3uContent(content))
            }
        } catch (e: Exception) {
            Log.e(TAG, "解析异常: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * 从文本内容解析 M3U 格式
     * 同名频道会合并成一个频道，多个URL作为多线路
     */
    fun parseM3uContent(content: String): LiveChannelGroupList {
        val lines = content.lines().map { it.trim() }
        
        Log.d(TAG, "解析 M3U 内容，总行数: ${lines.size}")
        Log.d(TAG, "第一行: ${lines.firstOrNull()}")
        
        if (lines.isEmpty() || !lines[0].startsWith("#EXTM3U")) {
            Log.d(TAG, "不是标准 M3U 格式，尝试简单格式解析")
            // 尝试解析简单文本格式（每行一个URL）
            return parseSimpleFormat(lines)
        }

        // 解析全局属性（从第一行 #EXTM3U）
        val firstLine = lines[0]
        val globalEpgUrls = extractXTvgUrl(firstLine)

        // 临时存储：分组 -> 频道名 -> 频道信息列表（用于合并同名频道）
        val groupChannelsMap = mutableMapOf<String, MutableMap<String, MutableList<ChannelInfo>>>()
        var currentChannelInfo: ChannelInfo? = null
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
                    
                    currentChannelInfo = ChannelInfo(
                        name = name,
                        group = group,
                        userAgent = userAgent,
                        referer = referer
                    )
                    currentGroup = group
                }
                line.isNotBlank() && !line.startsWith("#") -> {
                    // 这是URL行
                    currentChannelInfo?.let { info ->
                        val url = line
                        // 将频道信息添加到对应的分组和名称下
                        val groupMap = groupChannelsMap.getOrPut(info.group) { mutableMapOf() }
                        val channelList = groupMap.getOrPut(info.name) { mutableListOf() }
                        channelList.add(info.copy(url = url))
                    }
                    currentChannelInfo = null
                }
            }
        }

        // 合并同名频道，生成最终的频道列表
        val groups = mutableMapOf<String, MutableList<LiveChannel>>()
        
        groupChannelsMap.forEach { (groupName, channelsMap) ->
            val mergedChannels = mutableListOf<LiveChannel>()
            
            channelsMap.forEach { (channelName, channelInfoList) ->
                if (channelInfoList.isNotEmpty()) {
                    val firstInfo = channelInfoList.first()
                    // 合并所有同名频道的URL到一个频道
                    val allUrls = channelInfoList.map { it.url }
                    
                    val mergedChannel = LiveChannel(
                        name = channelName,
                        channelName = channelName,
                        urlList = allUrls,  // 所有同名频道的URL作为多线路
                        userAgent = firstInfo.userAgent,
                        referer = firstInfo.referer,
                        epgUrls = globalEpgUrls,
                        uniqueId = channelName  // 使用频道名作为唯一ID
                    )
                    mergedChannels.add(mergedChannel)
                    
                    if (allUrls.size > 1) {
                        Log.d(TAG, "合并同名频道: $channelName, 线路数: ${allUrls.size}")
                    }
                }
            }
            
            groups[groupName] = mergedChannels
        }

        // 转换为 LiveChannelGroupList
        val groupList = groups.map { (name, channels) ->
            LiveChannelGroup(
                name = name,
                channelList = LiveChannelList(channels)
            )
        }
        
        val totalChannels = groups.values.sumOf { it.size }
        val totalRoutes = groups.values.sumOf { channelList -> channelList.sumOf { it.urlList.size } }
        Log.d(TAG, "解析完成，分组数: ${groupList.size}, 频道数: $totalChannels, 总线路数: $totalRoutes")

        return LiveChannelGroupList(groupList)
    }
    
    /**
     * 频道信息临时数据类
     */
    private data class ChannelInfo(
        val name: String,
        val group: String,
        val url: String = "",
        val userAgent: String? = null,
        val referer: String? = null
    )

    /**
     * 解析简单格式（每行一个URL）
     */
    private fun parseSimpleFormat(lines: List<String>): LiveChannelGroupList {
        val channels = lines
            .filter { it.isNotBlank() && (it.startsWith("http://") || it.startsWith("https://")) }
            .mapIndexed { index, url ->
                val name = "频道${index + 1}"
                LiveChannel(
                    name = name,
                    channelName = name,
                    urlList = listOf(url),
                    uniqueId = "${name}_$index"  // 生成唯一ID
                )
            }
        
        Log.d(TAG, "简单格式解析完成，频道数: ${channels.size}")

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

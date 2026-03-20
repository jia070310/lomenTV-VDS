package com.lomen.tv.data.remote.parser

import android.util.Log
import com.lomen.tv.data.model.live.ChannelEpg
import com.lomen.tv.data.model.live.ChannelEpgList
import com.lomen.tv.data.model.live.EpgProgramme
import com.lomen.tv.data.model.live.EpgProgrammeList
import okhttp3.Dns
import okhttp3.OkHttpClient
import okhttp3.Request
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.BufferedReader
import java.io.ByteArrayInputStream
import java.io.InputStreamReader
import java.io.StringReader
import java.net.InetAddress
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit
import java.util.zip.GZIPInputStream

/**
 * EPG 节目单解析器
 * 支持 XMLTV 格式
 */
class EpgParser {

    companion object {
        private const val TAG = "EpgParser"
        
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

    private val dateFormat = SimpleDateFormat("yyyyMMddHHmmss Z", Locale.US).apply {
        // 使用 UTC 时区解析，因为 XMLTV 时间通常带有时区偏移
        timeZone = java.util.TimeZone.getTimeZone("UTC")
    }

    /**
     * 从 URL 解析 EPG
     * 支持普通 XML 和 GZIP 压缩格式
     * 如果是 aptv.app 域名，使用 APTV 专用 UA
     */
    fun parseFromUrl(url: String): Result<ChannelEpgList> {
        return try {
            // 判断是否需要使用 APTV 专用 UA
            val isAptvDomain = url.contains("aptv.app", ignoreCase = true)
            val userAgent = if (isAptvDomain) {
                android.util.Log.d("EpgParser", "使用 APTV 专用 UA 请求: $url")
                com.lomen.tv.data.preferences.LiveSettingsPreferences.Companion.APTV_SPECIAL_UA
            } else {
                android.util.Log.d("EpgParser", "使用默认 UA 请求: $url")
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"
            }
            
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", userAgent)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return Result.failure(Exception("请求失败: ${response.code}"))
                }
                
                // 根据 URL 或 Content-Type 判断是否使用 GZIP 解压
                val content = if (url.endsWith(".gz", ignoreCase = true)) {
                    // GZIP 压缩格式
                    android.util.Log.d("EpgParser", "检测到 GZIP 格式，开始解压")
                    decompressGzip(response.body?.bytes())
                } else {
                    // 普通 XML 格式
                    response.body?.string() ?: ""
                }
                
                Result.success(parseXmlContent(content))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 解压 GZIP 数据
     */
    private fun decompressGzip(bytes: ByteArray?): String {
        if (bytes == null || bytes.isEmpty()) return ""
        
        return try {
            val stringBuilder = StringBuilder()
            GZIPInputStream(ByteArrayInputStream(bytes)).use { gzipInputStream ->
                BufferedReader(InputStreamReader(gzipInputStream, "UTF-8")).use { reader ->
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        stringBuilder.append(line).append("\n")
                    }
                }
            }
            stringBuilder.toString()
        } catch (e: Exception) {
            android.util.Log.e("EpgParser", "GZIP 解压失败: ${e.message}")
            // 如果解压失败，尝试直接作为普通文本返回
            String(bytes, Charsets.UTF_8)
        }
    }

    /**
     * 解析 XMLTV 格式内容
     */
    fun parseXmlContent(content: String): ChannelEpgList {
        val epgList = mutableListOf<ChannelEpg>()
        val programmesMap = mutableMapOf<String, MutableList<EpgProgramme>>()
        // channel id 到 display-name 的映射
        val channelIdToNameMap = mutableMapOf<String, String>()

        try {
            val factory = XmlPullParserFactory.newInstance()
            val parser = factory.newPullParser()
            parser.setInput(StringReader(content))

            var eventType = parser.eventType
            var currentChannelId = ""

            while (eventType != XmlPullParser.END_DOCUMENT) {
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        when (parser.name) {
                            "channel" -> {
                                currentChannelId = parser.getAttributeValue(null, "id") ?: ""
                            }
                            "display-name" -> {
                                if (currentChannelId.isNotEmpty()) {
                                    val channelName = parser.nextText()
                                    // 保存 channel id 到 display-name 的映射
                                    channelIdToNameMap[currentChannelId] = channelName
                                    android.util.Log.d("EpgParser", "频道映射: $currentChannelId -> $channelName")
                                }
                            }
                            "programme" -> {
                                val channelId = parser.getAttributeValue(null, "channel") ?: ""
                                val startStr = parser.getAttributeValue(null, "start") ?: ""
                                val stopStr = parser.getAttributeValue(null, "stop") ?: ""
                                
                                var title = ""
                                
                                // 解析 programme 内部元素
                                var innerEvent = parser.next()
                                while (innerEvent != XmlPullParser.END_TAG || parser.name != "programme") {
                                    if (innerEvent == XmlPullParser.START_TAG && parser.name == "title") {
                                        title = parser.nextText()
                                    }
                                    innerEvent = parser.next()
                                }

                                val startAt = parseDate(startStr)
                                val endAt = parseDate(stopStr)

                                if (startAt != null && endAt != null) {
                                    val programme = EpgProgramme(
                                        startAt = startAt,
                                        endAt = endAt,
                                        title = title
                                    )
                                    programmesMap.getOrPut(channelId) { mutableListOf() }.add(programme)
                                }
                            }
                        }
                    }
                }
                eventType = parser.next()
            }

            // 转换为 ChannelEpgList，使用 display-name 作为频道名称
            programmesMap.forEach { (channelId, programmes) ->
                // 优先使用 display-name，如果没有则使用 channel id
                val channelName = channelIdToNameMap[channelId] ?: channelId
                epgList.add(
                    ChannelEpg(
                        channel = channelName,
                        programmes = EpgProgrammeList(programmes.sortedBy { it.startAt })
                    )
                )
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }

        return ChannelEpgList(epgList)
    }

    /**
     * 解析日期字符串
     */
    private fun parseDate(dateStr: String): Long? {
        return try {
            val result = dateFormat.parse(dateStr)?.time
            android.util.Log.d("EpgParser", "解析时间: '$dateStr' -> $result -> ${java.util.Date(result ?: 0)}")
            result
        } catch (e: Exception) {
            android.util.Log.e("EpgParser", "解析时间失败: '$dateStr', 错误: ${e.message}")
            null
        }
    }
}

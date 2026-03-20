package com.lomen.tv.data.model.live

import androidx.compose.runtime.Immutable

/**
 * 直播频道
 */
@Immutable
data class LiveChannel(
    /**
     * 直播源名称
     */
    val name: String = "",

    /**
     * 频道名称，用于查询节目单
     */
    val channelName: String = "",

    /**
     * 播放地址列表
     */
    val urlList: List<String> = emptyList(),

    /**
     * HTTP User-Agent（每个频道可能不同）
     */
    val userAgent: String? = null,

    /**
     * HTTP Referer（部分频道需要）
     */
    val referer: String? = null,

    /**
     * EPG 节目单 URL 列表（从 x-tvg-url 解析）
     */
    val epgUrls: List<String> = emptyList(),
    
    /**
     * 唯一标识符，用于区分同名频道
     * 格式：频道名_索引
     */
    val uniqueId: String = "",
) {
    /**
     * 是否有多线路
     */
    fun hasMultipleRoutes(): Boolean = urlList.size > 1
    
    companion object {
        val EXAMPLE = LiveChannel(
            name = "CCTV-1",
            channelName = "cctv1",
            urlList = listOf(
                "http://dbiptv.sn.chinamobile.com/PLTV/88888890/224/3221226231/index.m3u8",
                "http://[2409:8087:5e01:34::20]:6610/ZTE_CMS/00000001000000060000000000000131/index.m3u8?IAS",
            ),
        )
    }
}

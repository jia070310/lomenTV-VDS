package com.lomen.tv.data.model.live

import androidx.compose.runtime.Immutable

/**
 * 直播频道列表
 */
@Immutable
data class LiveChannelList(
    val value: List<LiveChannel> = emptyList(),
) : List<LiveChannel> by value {
    companion object {
        val EXAMPLE = LiveChannelList(
            List(10) { idx ->
                LiveChannel(
                    name = "频道${idx + 1}",
                    channelName = "频道${idx + 1}",
                    urlList = emptyList(),
                )
            },
        )
    }
}

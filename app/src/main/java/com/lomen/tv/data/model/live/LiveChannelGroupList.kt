package com.lomen.tv.data.model.live

import androidx.compose.runtime.Immutable

/**
 * 直播频道分组列表
 */
@Immutable
data class LiveChannelGroupList(
    val value: List<LiveChannelGroup> = emptyList(),
) : List<LiveChannelGroup> by value {
    companion object {
        val EXAMPLE = LiveChannelGroupList(List(5) { groupIdx ->
            LiveChannelGroup(
                name = "频道分组${groupIdx + 1}",
                channelList = LiveChannelList(
                    List(10) { idx ->
                        LiveChannel(
                            name = "频道${groupIdx + 1}-${idx + 1}",
                            channelName = "频道${groupIdx + 1}-${idx + 1}",
                            urlList = emptyList(),
                        )
                    },
                )
            )
        })

        fun LiveChannelGroupList.groupIdx(channel: LiveChannel) =
            this.indexOfFirst { group -> group.channelList.any { it == channel } }

        fun LiveChannelGroupList.channelIdx(channel: LiveChannel) =
            this.flatMap { it.channelList }.indexOfFirst { it == channel }

        fun LiveChannelGroupList.allChannels(): List<LiveChannel> = this.flatMap { it.channelList }
    }
}

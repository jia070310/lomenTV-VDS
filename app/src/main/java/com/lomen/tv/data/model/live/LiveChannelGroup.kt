package com.lomen.tv.data.model.live

import androidx.compose.runtime.Immutable

/**
 * 直播频道分组
 */
@Immutable
data class LiveChannelGroup(
    /**
     * 分组名称
     */
    val name: String = "",

    /**
     * 频道列表
     */
    val channelList: LiveChannelList = LiveChannelList(),
)

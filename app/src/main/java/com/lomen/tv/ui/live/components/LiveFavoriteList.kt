package com.lomen.tv.ui.live.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.tv.foundation.lazy.list.TvLazyRow
import androidx.tv.foundation.lazy.list.items
import com.lomen.tv.data.model.live.ChannelEpgList
import com.lomen.tv.data.model.live.LiveChannel
import com.lomen.tv.data.model.live.LiveChannelList
import com.lomen.tv.ui.theme.LomenTVTheme

@Composable
fun LiveFavoriteList(
    modifier: Modifier = Modifier,
    channelListProvider: () -> LiveChannelList = { LiveChannelList() },
    epgListProvider: () -> ChannelEpgList = { ChannelEpgList() },
    currentChannelProvider: () -> LiveChannel = { LiveChannel() },
    showProgrammeProgressProvider: () -> Boolean = { false },
    onChannelSelected: (LiveChannel) -> Unit = {},
    onChannelFavoriteToggle: (LiveChannel) -> Unit = {},
    onClose: () -> Unit = {},
    onUserAction: () -> Unit = {},
) {
    val channelList = channelListProvider()

    TvLazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(horizontal = 20.dp),
    ) {
        items(channelList) { channel ->
            val currentProgramme = remember(channel, epgListProvider()) {
                epgListProvider().firstOrNull { epg -> epg.channel == channel.channelName }
                    ?.let { epg ->
                        val now = System.currentTimeMillis()
                        epg.programmes.firstOrNull { 
                            now >= it.startAt && now <= it.endAt 
                        }
                    }
            }

            LiveChannelItem(
                channelProvider = { channel },
                currentProgrammeProvider = { currentProgramme },
                showProgrammeProgressProvider = { showProgrammeProgressProvider() },
                onChannelSelected = { onChannelSelected(channel) },
                onChannelFavoriteToggle = { onChannelFavoriteToggle(channel) },
                onShowEpg = { },
            )
        }
    }
}

@Preview(device = "id:Android TV (720p)")
@Composable
private fun LiveFavoriteListPreview() {
    LomenTVTheme {
        LiveFavoriteList(
            channelListProvider = { LiveChannelList.EXAMPLE },
            currentChannelProvider = { LiveChannel.EXAMPLE },
        )
    }
}

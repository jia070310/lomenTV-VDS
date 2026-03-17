package com.lomen.tv.ui.live.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.tv.foundation.lazy.list.TvLazyRow
import androidx.tv.foundation.lazy.list.items
import androidx.tv.foundation.lazy.list.rememberTvLazyListState
import com.lomen.tv.data.model.live.ChannelEpgList
import com.lomen.tv.data.model.live.EpgProgramme
import com.lomen.tv.data.model.live.LiveChannel
import com.lomen.tv.data.model.live.LiveChannelList
import com.lomen.tv.ui.live.utils.handleLiveKeyEvents
import com.lomen.tv.ui.theme.LomenTVTheme
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlin.math.max

@Composable
fun LiveChannelList(
    modifier: Modifier = Modifier,
    channelListProvider: () -> LiveChannelList = { LiveChannelList() },
    epgListProvider: () -> ChannelEpgList = { ChannelEpgList() },
    currentChannelProvider: () -> LiveChannel = { LiveChannel() },
    showProgrammeProgressProvider: () -> Boolean = { false },
    onChannelSelected: (LiveChannel) -> Unit = {},
    onChannelFavoriteToggle: (LiveChannel) -> Unit = {},
    onUserAction: () -> Unit = {},
) {
    val channelList = channelListProvider()

    val listState = rememberTvLazyListState(max(0, channelList.indexOf(currentChannelProvider()) - 2))

    var hasFocused by rememberSaveable { mutableStateOf(false) }

    var showEpgDialog by remember { mutableStateOf(false) }
    var currentShowEpgChannel by remember { mutableStateOf(LiveChannel()) }

    LaunchedEffect(listState) {
        snapshotFlow { listState.isScrollInProgress }
            .distinctUntilChanged()
            .collect { _ -> onUserAction() }
    }

    TvLazyRow(
        state = listState,
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
                onShowEpg = {
                    currentShowEpgChannel = channel
                    showEpgDialog = true
                },
                initialFocusedProvider = { channel == currentChannelProvider() && !hasFocused },
                onHasFocused = { hasFocused = true },
            )
        }
    }

    // EPG 弹窗
    LiveEpgDialog(
        showDialogProvider = { showEpgDialog },
        onDismissRequest = { showEpgDialog = false },
        channelProvider = { currentShowEpgChannel },
        epgListProvider = epgListProvider,
        modifier = Modifier
            .handleLiveKeyEvents(
                onLeft = {
                    val idx = channelList.indexOf(currentShowEpgChannel)
                    if (idx > 0) {
                        currentShowEpgChannel = channelList[idx - 1]
                    }
                },
                onRight = {
                    val idx = channelList.indexOf(currentShowEpgChannel)
                    if (idx < channelList.size - 1) {
                        currentShowEpgChannel = channelList[idx + 1]
                    }
                },
            ),
        onUserAction = onUserAction,
    )
}

@Preview
@Composable
private fun LiveChannelListPreview() {
    LomenTVTheme {
        LiveChannelList(
            modifier = Modifier.padding(20.dp),
            channelListProvider = { LiveChannelList.EXAMPLE },
            currentChannelProvider = { LiveChannel.EXAMPLE },
        )
    }
}

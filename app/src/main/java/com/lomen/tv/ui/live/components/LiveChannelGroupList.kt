package com.lomen.tv.ui.live.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.tv.foundation.lazy.list.TvLazyColumn
import androidx.tv.foundation.lazy.list.itemsIndexed
import androidx.tv.foundation.lazy.list.rememberTvLazyListState
import com.lomen.tv.data.model.live.ChannelEpgList
import com.lomen.tv.data.model.live.LiveChannel
import com.lomen.tv.data.model.live.LiveChannelGroupList
import com.lomen.tv.data.model.live.LiveChannelGroupList.Companion.groupIdx
import com.lomen.tv.ui.live.utils.handleLiveKeyEvents
import com.lomen.tv.ui.theme.LomenTVTheme
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlin.math.max

@Composable
fun LiveChannelGroupList(
    modifier: Modifier = Modifier,
    channelGroupListProvider: () -> LiveChannelGroupList = { LiveChannelGroupList() },
    epgListProvider: () -> ChannelEpgList = { ChannelEpgList() },
    currentChannelProvider: () -> LiveChannel = { LiveChannel() },
    showProgrammeProgressProvider: () -> Boolean = { false },
    onChannelSelected: (LiveChannel) -> Unit = {},
    onChannelFavoriteToggle: (LiveChannel) -> Unit = {},
    onToFavorite: () -> Unit = {},
    onUserAction: () -> Unit = {},
) {
    val channelGroupList = channelGroupListProvider()

    val listState =
        rememberTvLazyListState(max(0, channelGroupList.groupIdx(currentChannelProvider())))

    LaunchedEffect(listState) {
        snapshotFlow { listState.isScrollInProgress }
            .distinctUntilChanged()
            .collect { _ -> onUserAction() }
    }

    TvLazyColumn(
        modifier = modifier,
        state = listState,
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(bottom = 20.dp),
    ) {
        itemsIndexed(channelGroupList) { index, channelGroup ->
            Row(
                modifier = Modifier.padding(start = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                CompositionLocalProvider(
                    LocalTextStyle provides MaterialTheme.typography.labelMedium,
                ) {
                    Text(text = channelGroup.name)
                    Text(
                        text = "${channelGroup.channelList.size}个频道",
                        color = LocalContentColor.current.copy(alpha = 0.8f),
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            LiveChannelList(
                modifier = if (index == 0) {
                    Modifier.handleLiveKeyEvents(onUp = { onToFavorite() })
                } else Modifier,
                channelListProvider = { channelGroup.channelList },
                epgListProvider = epgListProvider,
                currentChannelProvider = currentChannelProvider,
                showProgrammeProgressProvider = showProgrammeProgressProvider,
                onChannelSelected = onChannelSelected,
                onChannelFavoriteToggle = onChannelFavoriteToggle,
                onUserAction = onUserAction,
            )
        }
    }
}

@Preview(device = "id:Android TV (720p)")
@Composable
private fun LiveChannelGroupListPreview() {
    LomenTVTheme {
        Box(modifier = Modifier.height(150.dp)) {
            LiveChannelGroupList(
                channelGroupListProvider = { LiveChannelGroupList.EXAMPLE },
                currentChannelProvider = { LiveChannel.EXAMPLE },
            )
        }
    }
}

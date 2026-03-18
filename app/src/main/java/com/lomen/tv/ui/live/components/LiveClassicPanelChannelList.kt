package com.lomen.tv.ui.live.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.tv.foundation.lazy.list.TvLazyColumn
import androidx.tv.foundation.lazy.list.TvLazyListState
import androidx.tv.foundation.lazy.list.itemsIndexed
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.ListItemDefaults
import kotlinx.coroutines.flow.distinctUntilChanged
import com.lomen.tv.data.model.live.ChannelEpgList
import com.lomen.tv.data.model.live.LiveChannel
import com.lomen.tv.data.model.live.LiveChannelGroup
import com.lomen.tv.data.model.live.LiveChannelList
import com.lomen.tv.ui.live.utils.handleLiveKeyEvents
import com.lomen.tv.ui.theme.LomenTVTheme
import com.lomen.tv.ui.theme.PrimaryYellow
import kotlin.math.max

/**
 * 经典面板 - 中间频道列表
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun LiveClassicPanelChannelList(
    modifier: Modifier = Modifier,
    groupProvider: () -> LiveChannelGroup = { LiveChannelGroup() },
    channelListProvider: () -> LiveChannelList = { LiveChannelList() },
    epgListProvider: () -> ChannelEpgList = { ChannelEpgList() },
    initialChannelProvider: () -> LiveChannel = { LiveChannel() },
    onChannelSelected: (LiveChannel) -> Unit = {},
    onChannelFavoriteToggle: (LiveChannel) -> Unit = {},
    onChannelFocused: (LiveChannel, FocusRequester) -> Unit = { _, _ -> },
    showProgrammeProgressProvider: () -> Boolean = { false },
    onUserAction: () -> Unit = {},
) {
    val focusManager = LocalFocusManager.current
    val channelList = channelListProvider()
    val initialChannel = initialChannelProvider()

    var hasFocused by rememberSaveable { mutableStateOf(!channelList.contains(initialChannel)) }
    val itemFocusRequesterList = remember(channelList) {
        List(channelList.size) { FocusRequester() }
    }
    var focusedChannel by remember(channelList) { mutableStateOf(initialChannel) }

    LaunchedEffect(channelList) {
        if (channelList.isNotEmpty()) {
            if (hasFocused) {
                onChannelFocused(channelList[0], itemFocusRequesterList[0])
            } else {
                onChannelFocused(
                    initialChannel,
                    itemFocusRequesterList[max(0, channelList.indexOf(initialChannel))],
                )
            }
        }
    }

    val listState = remember(groupProvider()) {
        TvLazyListState(
            if (hasFocused) 0
            else max(0, channelList.indexOf(initialChannel) - 2)
        )
    }

    LaunchedEffect(listState) {
        snapshotFlow { listState.isScrollInProgress }
            .distinctUntilChanged()
            .collect { _ -> onUserAction() }
    }

    val currentChannel = initialChannelProvider()

    TvLazyColumn(
        state = listState,
        contentPadding = PaddingValues(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier
            .fillMaxHeight()
            .width(260.dp)
            .background(Color.Black.copy(0.8f)),
    ) {
        itemsIndexed(channelList, key = { _, channel -> channel.hashCode() }) { index, channel ->
            val isSelected by remember { derivedStateOf { channel == currentChannel } }
            val initialFocused by remember {
                derivedStateOf { !hasFocused && channel == initialChannel }
            }

            LiveClassicPanelChannelItem(
                channelProvider = { channel },
                epgListProvider = epgListProvider,
                focusRequesterProvider = { itemFocusRequesterList[index] },
                isSelectedProvider = { isSelected },
                initialFocusedProvider = { initialFocused },
                onInitialFocused = { hasFocused = true },
                onFocused = {
                    focusedChannel = channel
                    onChannelFocused(channel, itemFocusRequesterList[index])
                },
                onSelected = { onChannelSelected(channel) },
                onFavoriteToggle = { onChannelFavoriteToggle(channel) },
                showProgrammeProgressProvider = showProgrammeProgressProvider,
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun LiveClassicPanelChannelItem(
    modifier: Modifier = Modifier,
    channelProvider: () -> LiveChannel = { LiveChannel() },
    epgListProvider: () -> ChannelEpgList = { ChannelEpgList() },
    focusRequesterProvider: () -> FocusRequester = { FocusRequester() },
    isSelectedProvider: () -> Boolean = { false },
    initialFocusedProvider: () -> Boolean = { false },
    onInitialFocused: () -> Unit = {},
    onFocused: () -> Unit = {},
    onSelected: () -> Unit = {},
    onFavoriteToggle: () -> Unit = {},
    showProgrammeProgressProvider: () -> Boolean = { false },
) {
    val channel = channelProvider()
    val focusRequester = focusRequesterProvider()
    val epgList = epgListProvider()
    val currentProgramme = epgList.currentProgrammes(channel)?.now

    var isFocused by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (initialFocusedProvider()) {
            onInitialFocused()
            focusRequester.requestFocus()
        }
    }

    CompositionLocalProvider(
        LocalContentColor provides when {
            isFocused -> Color.Black
            isSelectedProvider() -> Color.White
            else -> Color.White.copy(alpha = 0.9f)
        }
    ) {
        Box(
            modifier = Modifier.clip(ListItemDefaults.shape().shape),
        ) {
            androidx.tv.material3.ListItem(
                modifier = modifier
                    .focusRequester(focusRequester)
                    .onFocusChanged {
                        isFocused = it.isFocused || it.hasFocus

                        if (isFocused) {
                            onFocused()
                        }
                    }
                    .handleLiveKeyEvents(
                        key = channel.hashCode(),
                        onSelect = {
                            if (isFocused) onSelected()
                            else focusRequester.requestFocus()
                        },
                        onLongSelect = {
                            if (isFocused) onFavoriteToggle()
                            else focusRequester.requestFocus()
                        },
                    ),
                colors = ListItemDefaults.colors(
                    focusedContainerColor = PrimaryYellow,
                    selectedContainerColor = PrimaryYellow.copy(alpha = 0.3f),
                    containerColor = Color.Transparent,
                ),
                selected = isSelectedProvider(),
                onClick = { },
                headlineContent = {
                    Text(
                        text = channel.name,
                        maxLines = 1,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                },
                supportingContent = {
                    Text(
                        text = currentProgramme?.title ?: "无节目",
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1,
                        modifier = Modifier.alpha(if (isFocused) 0.7f else 0.6f),
                    )
                },
            )

            if (showProgrammeProgressProvider() && currentProgramme != null) {
                val progress = remember(currentProgramme) {
                    val now = System.currentTimeMillis()
                    when {
                        now < currentProgramme.startAt -> 0f
                        now > currentProgramme.endAt -> 1f
                        else -> (now - currentProgramme.startAt).toFloat() /
                                (currentProgramme.endAt - currentProgramme.startAt).toFloat()
                    }
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth(progress)
                        .height(2.dp)
                        .background(
                            if (isFocused) Color.Black.copy(alpha = 0.3f)
                            else PrimaryYellow.copy(alpha = 0.8f),
                        ),
                )
            }
        }
    }
}

@Preview(device = "id:Android TV (720p)")
@Composable
private fun LiveClassicPanelChannelListPreview() {
    LomenTVTheme {
        LiveClassicPanelChannelList(
            modifier = Modifier.padding(20.dp),
            channelListProvider = { LiveChannelList.EXAMPLE },
        )
    }
}

package com.lomen.tv.ui.live.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.lomen.tv.data.model.live.ChannelEpg
import com.lomen.tv.data.model.live.ChannelEpgList
import com.lomen.tv.data.model.live.LiveChannel
import com.lomen.tv.data.model.live.LiveChannelGroup
import com.lomen.tv.data.model.live.LiveChannelGroupList
import com.lomen.tv.data.model.live.LiveChannelGroupList.Companion.allChannels
import com.lomen.tv.data.model.live.LiveChannelList
import com.lomen.tv.ui.live.utils.handleLiveKeyEvents
import com.lomen.tv.ui.theme.LomenTVTheme
import com.lomen.tv.ui.theme.PrimaryYellow
import kotlinx.coroutines.delay
import kotlin.math.max

/**
 * 经典三栏布局频道列表面板
 * 左侧：分类列表 | 中间：频道列表 | 右侧：节目单（默认隐藏，按右键显示）
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun LivePanelScreen(
    modifier: Modifier = Modifier,
    channelGroupListProvider: () -> LiveChannelGroupList = { LiveChannelGroupList() },
    epgListProvider: () -> ChannelEpgList = { ChannelEpgList() },
    currentChannelProvider: () -> LiveChannel = { LiveChannel() },
    currentChannelUrlIdxProvider: () -> Int = { 0 },
    showProgrammeProgressProvider: () -> Boolean = { false },
    channelFavoriteEnableProvider: () -> Boolean = { true },
    channelFavoriteListProvider: () -> List<String> = { emptyList() },
    channelFavoriteListVisibleProvider: () -> Boolean = { false },
    onChannelFavoriteListVisibleChange: (Boolean) -> Unit = {},
    onChannelSelected: (LiveChannel) -> Unit = {},
    onChannelFavoriteToggle: (LiveChannel) -> Unit = {},
    onClose: () -> Unit = {},
    autoCloseDelay: Long = 10000L, // 10秒后自动关闭
) {
    // 自动关闭状态
    var lastUserAction by remember { mutableStateOf(System.currentTimeMillis()) }
    
    LaunchedEffect(lastUserAction) {
        delay(autoCloseDelay)
        if (System.currentTimeMillis() - lastUserAction >= autoCloseDelay) {
            onClose()
        }
    }

    val onUserAction = {
        lastUserAction = System.currentTimeMillis()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) { detectTapGestures(onTap = { onClose() }) },
    ) {
        // 内层容器，阻止点击事件冒泡
        Box(
            modifier = Modifier
                .pointerInput(Unit) { detectTapGestures(onTap = { }) }
                .padding(start = 40.dp, top = 40.dp, bottom = 40.dp, end = 40.dp)
                .clip(MaterialTheme.shapes.medium),
        ) {
            LivePanelScreenContent(
                channelGroupListProvider = channelGroupListProvider,
                epgListProvider = epgListProvider,
                currentChannelProvider = currentChannelProvider,
                showProgrammeProgressProvider = showProgrammeProgressProvider,
                channelFavoriteEnableProvider = channelFavoriteEnableProvider,
                channelFavoriteListProvider = channelFavoriteListProvider,
                channelFavoriteListVisibleProvider = channelFavoriteListVisibleProvider,
                onChannelFavoriteListVisibleChange = onChannelFavoriteListVisibleChange,
                onChannelSelected = onChannelSelected,
                onChannelFavoriteToggle = onChannelFavoriteToggle,
                onUserAction = onUserAction,
            )
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun LivePanelScreenContent(
    modifier: Modifier = Modifier,
    channelGroupListProvider: () -> LiveChannelGroupList = { LiveChannelGroupList() },
    epgListProvider: () -> ChannelEpgList = { ChannelEpgList() },
    currentChannelProvider: () -> LiveChannel = { LiveChannel() },
    showProgrammeProgressProvider: () -> Boolean = { false },
    channelFavoriteEnableProvider: () -> Boolean = { true },
    channelFavoriteListProvider: () -> List<String> = { emptyList() },
    channelFavoriteListVisibleProvider: () -> Boolean = { false },
    onChannelFavoriteListVisibleChange: (Boolean) -> Unit = {},
    onChannelSelected: (LiveChannel) -> Unit = {},
    onChannelFavoriteToggle: (LiveChannel) -> Unit = {},
    onUserAction: () -> Unit = {},
) {
    val channelGroupList = channelGroupListProvider()
    val currentChannel = currentChannelProvider()

    // 当前聚焦的分类
    var focusedGroup by remember {
        mutableStateOf(
            channelGroupList.find { group ->
                group.channelList.any { it.channelName == currentChannel.channelName }
            } ?: channelGroupList.firstOrNull() ?: LiveChannelGroup()
        )
    }

    // 当前聚焦的频道
    var focusedChannel by remember { mutableStateOf(currentChannel) }
    var focusedChannelFocusRequester by remember { mutableStateOf(FocusRequester.Default) }

    // 节目单是否可见
    var epgListVisible by remember { mutableStateOf(false) }

    // 构建带收藏的分类列表
    val groupList = remember(channelGroupList, channelFavoriteListProvider()) {
        val list = mutableListOf<LiveChannelGroup>()
        val favoriteList = channelFavoriteListProvider()
        if (favoriteList.isNotEmpty()) {
            val favoriteChannels = channelGroupList.allChannels()
                .filter { favoriteList.contains(it.channelName) }
            if (favoriteChannels.isNotEmpty()) {
                list.add(LiveChannelGroup("我的收藏", LiveChannelList(favoriteChannels)))
            }
        }
        list.addAll(channelGroupList)
        LiveChannelGroupList(list)
    }

    // 初始选中的分类
    val initialGroup = remember(channelGroupList, currentChannel) {
        groupList.find { group ->
            group.channelList.any { it.channelName == currentChannel.channelName }
        } ?: groupList.firstOrNull() ?: LiveChannelGroup()
    }

    Row(modifier = modifier.fillMaxHeight()) {
        // 左侧：分类列表
        LiveClassicPanelGroupList(
            groupListProvider = { groupList },
            initialGroupProvider = { initialGroup },
            exitFocusRequesterProvider = { focusedChannelFocusRequester },
            onGroupFocused = { group ->
                focusedGroup = group
                onChannelFavoriteListVisibleChange(group.name == "我的收藏")
            },
            onUserAction = onUserAction,
        )

        // 中间：频道列表
        LiveClassicPanelChannelList(
            modifier = Modifier
                .handleLiveKeyEvents(
                    onRight = { epgListVisible = true },
                    onLeft = { epgListVisible = false }
                )
                .focusProperties {
                    exit = {
                        if (epgListVisible && it == FocusDirection.Left) {
                            epgListVisible = false
                            FocusRequester.Cancel
                        } else {
                            FocusRequester.Default
                        }
                    }
                },
            groupProvider = { focusedGroup },
            channelListProvider = {
                if (focusedGroup.name == "我的收藏") {
                    LiveChannelList(
                        channelGroupListProvider().allChannels()
                            .filter { channelFavoriteListProvider().contains(it.channelName) }
                    )
                } else {
                    focusedGroup.channelList
                }
            },
            epgListProvider = epgListProvider,
            initialChannelProvider = { currentChannel },
            onChannelSelected = onChannelSelected,
            onChannelFavoriteToggle = onChannelFavoriteToggle,
            onChannelFocused = { channel, focusRequester ->
                focusedChannel = channel
                focusedChannelFocusRequester = focusRequester
            },
            showProgrammeProgressProvider = showProgrammeProgressProvider,
            onUserAction = onUserAction,
        )

        // 右侧：节目单（可显示/隐藏）
        if (epgListVisible) {
            LiveClassicPanelEpgList(
                epgProvider = {
                    epgListProvider().firstOrNull { it.channel == focusedChannel.channelName }
                        ?: ChannelEpg()
                },
                exitFocusRequesterProvider = { focusedChannelFocusRequester },
                onUserAction = onUserAction,
            )
        } else {
            // 提示文字
            LivePanelEpgTip(
                modifier = Modifier
                    .background(Color.Black.copy(0.7f))
                    .padding(horizontal = 6.dp),
                text = "向右查看节目单",
                onTap = { epgListVisible = true },
            )
        }
    }
}

@Composable
private fun LivePanelEpgTip(
    modifier: Modifier = Modifier,
    text: String,
    onTap: () -> Unit = {},
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .pointerInput(Unit) {
                detectTapGestures(onTap = { onTap() })
            },
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        text.map {
            Text(
                text = it.toString(),
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.7f)
            )
        }
    }
}

@Preview(device = "id:Android TV (720p)")
@Composable
private fun LivePanelScreenPreview() {
    LomenTVTheme {
        LivePanelScreen(
            currentChannelProvider = { LiveChannel.EXAMPLE },
            channelGroupListProvider = { LiveChannelGroupList.EXAMPLE },
        )
    }
}

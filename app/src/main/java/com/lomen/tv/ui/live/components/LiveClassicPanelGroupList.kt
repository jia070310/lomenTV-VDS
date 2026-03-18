package com.lomen.tv.ui.live.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.tv.foundation.lazy.list.TvLazyColumn
import androidx.tv.foundation.lazy.list.items
import androidx.tv.foundation.lazy.list.rememberTvLazyListState
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.ListItemDefaults
import com.lomen.tv.data.model.live.LiveChannelGroup
import com.lomen.tv.data.model.live.LiveChannelGroupList
import com.lomen.tv.ui.live.utils.handleLiveKeyEvents
import com.lomen.tv.ui.theme.LomenTVTheme
import com.lomen.tv.ui.theme.PrimaryYellow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlin.math.max

/**
 * 经典面板 - 左侧分类列表
 */
@OptIn(ExperimentalComposeUiApi::class, ExperimentalTvMaterial3Api::class)
@Composable
fun LiveClassicPanelGroupList(
    modifier: Modifier = Modifier,
    groupListProvider: () -> LiveChannelGroupList = { LiveChannelGroupList() },
    initialGroupProvider: () -> LiveChannelGroup = { LiveChannelGroup() },
    exitFocusRequesterProvider: () -> FocusRequester = { FocusRequester.Default },
    onGroupFocused: (LiveChannelGroup) -> Unit = {},
    onUserAction: () -> Unit = {},
) {
    val groupList = groupListProvider()
    val initialGroup = initialGroupProvider()

    val focusRequester = remember { FocusRequester() }
    var focusedGroup by remember { mutableStateOf(initialGroup) }

    val listState = rememberTvLazyListState(max(0, groupList.indexOf(initialGroup) - 2))

    LaunchedEffect(listState) {
        snapshotFlow { listState.isScrollInProgress }
            .distinctUntilChanged()
            .collect { _ -> onUserAction() }
    }

    TvLazyColumn(
        state = listState,
        contentPadding = PaddingValues(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier
            .width(140.dp)
            .fillMaxHeight()
            .background(Color.Black.copy(0.85f))
            .focusRequester(focusRequester)
            .focusProperties {
                exit = {
                    focusRequester.saveFocusedChild()
                    exitFocusRequesterProvider()
                }
                enter = {
                    if (focusRequester.restoreFocusedChild()) FocusRequester.Cancel
                    else FocusRequester.Default
                }
            },
    ) {
        items(groupList) { group ->
            val isSelected by remember { derivedStateOf { group == focusedGroup } }

            LiveClassicPanelGroupItem(
                groupProvider = { group },
                isSelectedProvider = { isSelected },
                initialFocusedProvider = { group == initialGroup },
                onFocused = {
                    focusedGroup = it
                    onGroupFocused(it)
                },
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun LiveClassicPanelGroupItem(
    modifier: Modifier = Modifier,
    groupProvider: () -> LiveChannelGroup = { LiveChannelGroup() },
    isSelectedProvider: () -> Boolean = { false },
    initialFocusedProvider: () -> Boolean = { false },
    onFocused: (LiveChannelGroup) -> Unit = {},
) {
    val group = groupProvider()

    val focusRequester = remember { FocusRequester() }
    var hasFocused by rememberSaveable { mutableStateOf(false) }
    var isFocused by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (!hasFocused && initialFocusedProvider()) {
            focusRequester.requestFocus()
        }
        hasFocused = true
    }

    CompositionLocalProvider(
        LocalContentColor provides when {
            isFocused -> Color.Black
            isSelectedProvider() -> Color.White
            else -> Color.White.copy(alpha = 0.8f)
        }
    ) {
        androidx.tv.material3.ListItem(
            modifier = modifier
                .focusRequester(focusRequester)
                .onFocusChanged {
                    isFocused = it.isFocused || it.hasFocus

                    if (isFocused) {
                        onFocused(group)
                    }
                }
                .handleLiveKeyEvents(
                    onSelect = {
                        focusRequester.requestFocus()
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
                    text = group.name,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
        )
    }
}

@Preview(device = "id:Android TV (720p)")
@Composable
private fun LiveClassicPanelGroupListPreview() {
    LomenTVTheme {
        LiveClassicPanelGroupList(
            modifier = Modifier.padding(20.dp),
            groupListProvider = { LiveChannelGroupList.EXAMPLE },
            initialGroupProvider = { LiveChannelGroupList.EXAMPLE.firstOrNull() ?: LiveChannelGroup() },
        )
    }
}

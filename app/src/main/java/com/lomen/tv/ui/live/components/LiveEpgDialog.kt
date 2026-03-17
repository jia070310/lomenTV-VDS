package com.lomen.tv.ui.live.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.tv.foundation.lazy.list.TvLazyColumn
import androidx.tv.foundation.lazy.list.TvLazyListState
import androidx.tv.foundation.lazy.list.items
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.ListItemDefaults
import com.lomen.tv.data.model.live.ChannelEpg
import com.lomen.tv.data.model.live.ChannelEpgList
import com.lomen.tv.data.model.live.EpgProgramme
import com.lomen.tv.data.model.live.LiveChannel
import com.lomen.tv.ui.live.utils.handleLiveKeyEvents
import com.lomen.tv.ui.theme.LomenTVTheme
import kotlinx.coroutines.flow.distinctUntilChanged
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.max

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun LiveEpgDialog(
    modifier: Modifier = Modifier,
    showDialogProvider: () -> Boolean = { false },
    onDismissRequest: () -> Unit = {},
    channelProvider: () -> LiveChannel = { LiveChannel() },
    epgListProvider: () -> ChannelEpgList = { ChannelEpgList() },
    onUserAction: () -> Unit = {}
) {
    if (showDialogProvider()) {
        val channel = channelProvider()
        val epg = epgListProvider().firstOrNull { it.channel == channel.channelName } ?: ChannelEpg()

        AlertDialog(
            modifier = modifier,
            onDismissRequest = onDismissRequest,
            confirmButton = { Text(text = "左右切换频道") },
            title = { Text(channel.channelName) },
            text = {
                val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                var hasFocused by remember(channel) { mutableStateOf(false) }

                val now = System.currentTimeMillis()
                val liveIndex = epg.programmes.indexOfFirst { 
                    now >= it.startAt && now <= it.endAt 
                }
                val listState = TvLazyListState(max(0, liveIndex - 2))

                LaunchedEffect(listState) {
                    snapshotFlow { listState.isScrollInProgress }
                        .collect { _ -> onUserAction() }
                }

                TvLazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(vertical = 4.dp),
                ) {
                    if (epg.programmes.isNotEmpty()) {
                        items(epg.programmes, key = { it.hashCode() }) { programme ->
                            var isFocused by remember { mutableStateOf(false) }
                            val focusRequester = remember { FocusRequester() }
                            val isLive = now >= programme.startAt && now <= programme.endAt

                            LaunchedEffect(Unit) {
                                if (isLive && !hasFocused) {
                                    hasFocused = true
                                    focusRequester.requestFocus()
                                }
                            }

                            CompositionLocalProvider(
                                LocalContentColor provides if (isFocused) MaterialTheme.colorScheme.background
                                else MaterialTheme.colorScheme.onBackground
                            ) {
                                androidx.tv.material3.ListItem(
                                    modifier = Modifier
                                        .focusRequester(focusRequester)
                                        .onFocusChanged { isFocused = it.isFocused || it.hasFocus }
                                        .handleLiveKeyEvents(
                                            onSelect = { focusRequester.requestFocus() },
                                        ),
                                    colors = ListItemDefaults.colors(
                                        containerColor = Color.Transparent,
                                        focusedContainerColor = MaterialTheme.colorScheme.onBackground,
                                        selectedContainerColor = Color.Transparent,
                                    ),
                                    selected = isLive,
                                    onClick = { },
                                    headlineContent = {
                                        Text(
                                            text = programme.title,
                                            maxLines = if (isFocused) Int.MAX_VALUE else 1,
                                        )
                                    },
                                    overlineContent = {
                                        val start = timeFormat.format(programme.startAt)
                                        val end = timeFormat.format(programme.endAt)
                                        Text(
                                            text = "$start  ~ $end",
                                            style = MaterialTheme.typography.labelMedium,
                                            modifier = Modifier.alpha(0.8f),
                                        )
                                    },
                                    trailingContent = {
                                        if (isLive) {
                                            Icon(
                                                Icons.Default.PlayArrow,
                                                contentDescription = "playing",
                                            )
                                        }
                                    },
                                )
                            }
                        }
                    } else {
                        item {
                            var isFocused by remember { mutableStateOf(false) }
                            val focusRequester = remember { FocusRequester() }
                            LaunchedEffect(Unit) {
                                focusRequester.requestFocus()
                            }

                            CompositionLocalProvider(
                                LocalContentColor provides if (isFocused) MaterialTheme.colorScheme.background
                                else MaterialTheme.colorScheme.onBackground
                            ) {
                                androidx.tv.material3.ListItem(
                                    modifier = Modifier
                                        .focusRequester(focusRequester)
                                        .onFocusChanged { isFocused = it.isFocused || it.hasFocus },
                                    colors = ListItemDefaults.colors(
                                        containerColor = Color.Transparent,
                                        focusedContainerColor = MaterialTheme.colorScheme.onBackground,
                                        selectedContainerColor = Color.Transparent,
                                    ),
                                    selected = true,
                                    onClick = { },
                                    headlineContent = {
                                        Text(
                                            text = "当前频道暂无节目",
                                            maxLines = 1,
                                        )
                                    },
                                )
                            }
                        }
                    }
                }
            }
        )
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Preview(device = "id:Android TV (720p)")
@Composable
private fun LiveEpgDialogPreview() {
    LomenTVTheme {
        LiveEpgDialog(
            showDialogProvider = { true },
            channelProvider = { LiveChannel.EXAMPLE },
            epgListProvider = {
                ChannelEpgList(
                    listOf(
                        ChannelEpg(
                            "CCTV1",
                            listOf(
                                EpgProgramme(
                                    startAt = System.currentTimeMillis() - 2000000,
                                    endAt = System.currentTimeMillis() - 1000000,
                                    title = "新闻联播"
                                ),
                                EpgProgramme(
                                    startAt = System.currentTimeMillis() - 1000000,
                                    endAt = System.currentTimeMillis() + 1000000,
                                    title = "新闻联播1"
                                ),
                                EpgProgramme(
                                    startAt = System.currentTimeMillis() + 1000000,
                                    endAt = System.currentTimeMillis() + 2000000,
                                    title = "新闻联播2"
                                ),
                            ).let { com.lomen.tv.data.model.live.EpgProgrammeList(it) }
                        )
                    )
                )
            },
        )
    }
}

package com.lomen.tv.ui.live.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.tv.foundation.ExperimentalTvFoundationApi
import androidx.tv.foundation.lazy.list.TvLazyColumn
import androidx.tv.foundation.lazy.list.TvLazyListState
import androidx.tv.foundation.lazy.list.items
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.ListItemDefaults
import kotlinx.coroutines.flow.distinctUntilChanged
import com.lomen.tv.data.model.live.ChannelEpg
import com.lomen.tv.data.model.live.EpgProgramme
import com.lomen.tv.data.model.live.EpgProgramme.Companion.isLive
import com.lomen.tv.data.model.live.EpgProgrammeList
import com.lomen.tv.ui.live.utils.handleLiveKeyEvents
import com.lomen.tv.ui.theme.LomenTVTheme
import com.lomen.tv.ui.theme.PrimaryYellow
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.max

/**
 * 经典面板 - 右侧节目单列表（支持多天节目显示）
 */
@OptIn(ExperimentalComposeUiApi::class, ExperimentalTvFoundationApi::class, ExperimentalTvMaterial3Api::class)
@Composable
fun LiveClassicPanelEpgList(
    modifier: Modifier = Modifier,
    epgProvider: () -> ChannelEpg = { ChannelEpg() },
    exitFocusRequesterProvider: () -> FocusRequester = { FocusRequester.Default },
    onUserAction: () -> Unit = {},
) {
    val dateFormat = remember { SimpleDateFormat("E MM-dd", Locale.getDefault()) }
    val epg = epgProvider()

    if (epg.programmes.isNotEmpty()) {
        // 按日期分组节目
        val programmesGroup = remember(epg) {
            epg.programmes.groupBy { dateFormat.format(it.startAt) }
        }
        
        // 默认选中今天
        var currentDay by remember(programmesGroup) { 
            mutableStateOf(dateFormat.format(System.currentTimeMillis())) 
        }
        
        // 当前选中日期的节目列表
        val programmes = remember(currentDay, programmesGroup) {
            programmesGroup.getOrElse(currentDay) { emptyList() }
        }

        // 节目列表滚动状态
        val programmesListState = remember(programmes) {
            TvLazyListState(max(0, programmes.indexOfFirst { it.isLive() } - 2))
        }
        
        // 日期列表滚动状态
        val daysListState = remember(programmesGroup) {
            TvLazyListState(max(0, programmesGroup.keys.indexOf(currentDay) - 2))
        }

        LaunchedEffect(programmesListState) {
            snapshotFlow { programmesListState.isScrollInProgress }
                .distinctUntilChanged()
                .collect { _ -> onUserAction() }
        }
        LaunchedEffect(daysListState) {
            snapshotFlow { daysListState.isScrollInProgress }
                .distinctUntilChanged()
                .collect { _ -> onUserAction() }
        }

        Row {
            // 左侧：节目列表
            TvLazyColumn(
                state = programmesListState,
                contentPadding = PaddingValues(vertical = 8.dp, horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = modifier
                    .fillMaxHeight()
                    .width(240.dp)
                    .background(Color.Black.copy(0.7f))
                    .focusProperties {
                        exit = {
                            if (it == FocusDirection.Left) exitFocusRequesterProvider()
                            else FocusRequester.Default
                        }
                    },
            ) {
                items(programmes) { programme ->
                    LiveClassicPanelEpgItem(
                        programmeProvider = { programme },
                    )
                }
            }

            // 右侧：日期选择列表（当有多天数据时显示）
            if (programmesGroup.size > 1) {
                TvLazyColumn(
                    state = daysListState,
                    contentPadding = PaddingValues(vertical = 8.dp, horizontal = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = modifier
                        .fillMaxHeight()
                        .width(100.dp)
                        .background(Color.Black.copy(0.7f))
                ) {
                    items(programmesGroup.keys.toList()) { day ->
                        LiveClassicPanelEpgDayItem(
                            dayProvider = { day },
                            currentDayProvider = { currentDay },
                            onChangeCurrentDay = { currentDay = day },
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun LiveClassicPanelEpgItem(
    modifier: Modifier = Modifier,
    programmeProvider: () -> EpgProgramme = { EpgProgramme() },
) {
    val programme = programmeProvider()
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    val focusRequester = remember { FocusRequester() }
    var isFocused by remember { mutableStateOf(false) }

    CompositionLocalProvider(
        LocalContentColor provides when {
            isFocused -> Color.Black
            programme.isLive() -> PrimaryYellow
            else -> Color.White.copy(alpha = 0.9f)
        }
    ) {
        androidx.tv.material3.ListItem(
            modifier = modifier
                .focusRequester(focusRequester)
                .onFocusChanged {
                    isFocused = it.isFocused || it.hasFocus
                }
                .handleLiveKeyEvents(
                    onSelect = {
                        focusRequester.requestFocus()
                    },
                ),
            colors = ListItemDefaults.colors(
                focusedContainerColor = PrimaryYellow,
                selectedContainerColor = PrimaryYellow.copy(alpha = 0.3f),
            ),
            selected = programme.isLive(),
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
                    text = "$start ~ $end",
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1,
                )
            },
            trailingContent = {
                if (programme.isLive()) {
                    Icon(Icons.Default.PlayArrow, contentDescription = "playing")
                }
            },
        )
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun LiveClassicPanelEpgDayItem(
    modifier: Modifier = Modifier,
    dayProvider: () -> String = { "" },
    currentDayProvider: () -> String = { "" },
    onChangeCurrentDay: () -> Unit = {},
) {
    val day = dayProvider()

    val dateFormat = remember { SimpleDateFormat("E MM-dd", Locale.getDefault()) }
    val today = dateFormat.format(System.currentTimeMillis())
    val tomorrow = dateFormat.format(System.currentTimeMillis() + 24 * 3600 * 1000)
    val dayAfterTomorrow = dateFormat.format(System.currentTimeMillis() + 48 * 3600 * 1000)

    val focusRequester = remember { FocusRequester() }
    val isSelected by remember(currentDayProvider()) { derivedStateOf { day == currentDayProvider() } }
    var isFocused by remember { mutableStateOf(false) }

    CompositionLocalProvider(
        LocalContentColor provides when {
            isFocused -> Color.Black
            isSelected -> Color.White
            else -> Color.White.copy(alpha = 0.9f)
        }
    ) {
        androidx.tv.material3.ListItem(
            modifier = modifier
                .focusRequester(focusRequester)
                .onFocusChanged {
                    isFocused = it.isFocused || it.hasFocus
                }
                .handleLiveKeyEvents(
                    onSelect = {
                        if (isFocused) onChangeCurrentDay()
                        else focusRequester.requestFocus()
                    }
                ),
            colors = ListItemDefaults.colors(
                focusedContainerColor = PrimaryYellow,
                selectedContainerColor = PrimaryYellow.copy(alpha = 0.3f),
            ),
            selected = isSelected,
            onClick = {},
            headlineContent = {
                Column {
                    val key = day.split(" ")

                    Text(
                        text = when (day) {
                            today -> "今天"
                            tomorrow -> "明天"
                            dayAfterTomorrow -> "后天"
                            else -> key[0]
                        },
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                    )

                    Text(
                        text = key[1],
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                    )
                }
            },
        )
    }
}

@Preview(device = "id:Android TV (720p)")
@Composable
private fun LiveClassicPanelEpgListPreview() {
    LomenTVTheme {
        LiveClassicPanelEpgList(
            modifier = Modifier.padding(20.dp),
            epgProvider = {
                ChannelEpg(
                    channel = "CCTV1",
                    programmes = EpgProgrammeList(List(200) { idx ->
                        EpgProgramme(
                            title = "节目$idx",
                            startAt = System.currentTimeMillis() - 3600 * 1000 * 24 * 5 + idx * 3600 * 1000,
                            endAt = System.currentTimeMillis() - 3600 * 1000 * 24 * 5 + idx * 3600 * 1000 + 3600 * 1000
                        )
                    })
                )
            },
        )
    }
}

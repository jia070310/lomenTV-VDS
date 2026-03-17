package com.lomen.tv.ui.live.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.tv.foundation.lazy.list.TvLazyColumn
import androidx.tv.foundation.lazy.list.itemsIndexed
import androidx.tv.foundation.lazy.list.rememberTvLazyListState
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
 * 经典面板 - 右侧节目单列表
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun LiveClassicPanelEpgList(
    modifier: Modifier = Modifier,
    epgProvider: () -> ChannelEpg = { ChannelEpg() },
    exitFocusRequesterProvider: () -> FocusRequester = { FocusRequester.Default },
    onUserAction: () -> Unit = {},
) {
    val epg = epgProvider()
    val programmes = epg.programmes

    val focusRequester = remember { FocusRequester() }
    var hasFocused by rememberSaveable { mutableStateOf(false) }

    val listState = rememberTvLazyListState(
        max(0, programmes.indexOfFirst { it.isLive() } - 2)
    )

    LaunchedEffect(listState) {
        snapshotFlow { listState.isScrollInProgress }
            .distinctUntilChanged()
            .collect { _ -> onUserAction() }
    }

    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    TvLazyColumn(
        state = listState,
        contentPadding = PaddingValues(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier
            .width(300.dp)
            .fillMaxHeight()
            .background(Color.Black.copy(0.85f))
            .focusRequester(focusRequester),
    ) {
        itemsIndexed(programmes) { index, programme ->
            val isLive = programme.isLive()
            val initialFocused = !hasFocused && isLive

            LiveClassicPanelEpgItem(
                programmeProvider = { programme },
                timeFormatProvider = { timeFormat },
                isLiveProvider = { isLive },
                initialFocusedProvider = { initialFocused },
                onInitialFocused = { hasFocused = true },
                onFocused = { },
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun LiveClassicPanelEpgItem(
    modifier: Modifier = Modifier,
    programmeProvider: () -> EpgProgramme = { EpgProgramme() },
    timeFormatProvider: () -> SimpleDateFormat = { SimpleDateFormat("HH:mm", Locale.getDefault()) },
    isLiveProvider: () -> Boolean = { false },
    initialFocusedProvider: () -> Boolean = { false },
    onInitialFocused: () -> Unit = {},
    onFocused: () -> Unit = {},
) {
    val programme = programmeProvider()
    val timeFormat = timeFormatProvider()
    val isLive = isLiveProvider()

    val focusRequester = remember { FocusRequester() }
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
            isLive -> PrimaryYellow
            else -> Color.White.copy(alpha = 0.85f)
        }
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
                    onSelect = {
                        focusRequester.requestFocus()
                    },
                ),
            colors = ListItemDefaults.colors(
                focusedContainerColor = PrimaryYellow,
                selectedContainerColor = if (isLive) PrimaryYellow.copy(alpha = 0.2f)
                else Color.Transparent,
                containerColor = Color.Transparent,
            ),
            selected = isLive,
            onClick = { },
            overlineContent = {
                val start = timeFormat.format(programme.startAt)
                val end = timeFormat.format(programme.endAt)
                Text(
                    text = "$start ~ $end",
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    modifier = Modifier.alpha(if (isFocused) 0.7f else 0.6f),
                )
            },
            headlineContent = {
                Text(
                    text = programme.title,
                    maxLines = if (isFocused) Int.MAX_VALUE else 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyMedium,
                )
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
                    programmes = EpgProgrammeList(List(10) { idx ->
                        EpgProgramme(
                            startAt = System.currentTimeMillis() + (idx - 5) * 60 * 60 * 1000L,
                            endAt = System.currentTimeMillis() + (idx - 4) * 60 * 60 * 1000L,
                            title = "节目${idx + 1}",
                        )
                    })
                )
            },
        )
    }
}

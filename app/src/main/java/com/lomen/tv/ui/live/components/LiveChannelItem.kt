package com.lomen.tv.ui.live.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import androidx.tv.material3.Border
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import com.lomen.tv.data.model.live.EpgProgramme
import com.lomen.tv.data.model.live.LiveChannel
import com.lomen.tv.ui.live.utils.handleLiveKeyEvents
import com.lomen.tv.ui.theme.LomenTVTheme
import com.lomen.tv.ui.theme.PrimaryYellow

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun LiveChannelItem(
    modifier: Modifier = Modifier,
    channelProvider: () -> LiveChannel = { LiveChannel() },
    currentProgrammeProvider: () -> EpgProgramme? = { null },
    showProgrammeProgressProvider: () -> Boolean = { false },
    onChannelSelected: () -> Unit = {},
    onChannelFavoriteToggle: () -> Unit = {},
    onShowEpg: () -> Unit = {},
    initialFocusedProvider: () -> Boolean = { false },
    onHasFocused: () -> Unit = {},
    onFocused: () -> Unit = {},
) {
    var isFocused by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }

    val channel = channelProvider()
    val currentProgramme = currentProgrammeProvider()
    val showProgrammeProgress = showProgrammeProgressProvider()

    LaunchedEffect(Unit) {
        if (initialFocusedProvider()) {
            onHasFocused()
            focusRequester.requestFocus()
        }
    }

    Card(
        onClick = { },
        modifier = modifier
            .width(130.dp)
            .height(54.dp)
            .focusRequester(focusRequester)
            .onFocusChanged {
                isFocused = it.isFocused || it.hasFocus
                if (isFocused) onFocused()
            }
            .handleLiveKeyEvents(
                onSelect = {
                    if (isFocused) onChannelSelected()
                    else focusRequester.requestFocus()
                },
                onLongSelect = {
                    if (isFocused) onChannelFavoriteToggle()
                    else focusRequester.requestFocus()
                },
                onSettings = {
                    if (isFocused) onShowEpg()
                    else focusRequester.requestFocus()
                }
            ),
        colors = CardDefaults.colors(
            containerColor = Color.Transparent,
        ),
        border = CardDefaults.border(
            focusedBorder = Border(
                border = BorderStroke(width = 2.dp, color = PrimaryYellow),
            ),
        ),
    ) {
        Box(
            modifier = Modifier.background(
                color = if (isFocused) PrimaryYellow
                else MaterialTheme.colorScheme.background.copy(alpha = 0.8f),
            ),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.SpaceAround,
            ) {
                Text(
                    text = channel.name,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = if (isFocused) Color.Black
                    else MaterialTheme.colorScheme.onBackground,
                )

                Text(
                    text = currentProgramme?.title ?: "",
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.alpha(0.8f),
                    color = if (isFocused) Color.Black
                    else MaterialTheme.colorScheme.onBackground,
                )
            }

            // 节目进度条
            if (showProgrammeProgress && currentProgramme != null) {
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
                        .height(3.dp)
                        .background(
                            if (isFocused) Color.Black.copy(alpha = 0.3f)
                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f)
                        ),
                )
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Preview(device = "id:Android TV (720p)")
@Composable
private fun LiveChannelItemPreview() {
    LomenTVTheme {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            LiveChannelItem(
                channelProvider = { LiveChannel.EXAMPLE },
                currentProgrammeProvider = {
                    EpgProgramme(
                        startAt = System.currentTimeMillis() - 100000,
                        endAt = System.currentTimeMillis() + 200000,
                        title = "新闻联播",
                    )
                },
                showProgrammeProgressProvider = { true },
            )

            LiveChannelItem(
                channelProvider = { LiveChannel.EXAMPLE },
                currentProgrammeProvider = {
                    EpgProgramme(
                        startAt = System.currentTimeMillis() - 100000,
                        endAt = System.currentTimeMillis() + 200000,
                        title = "新闻联播",
                    )
                },
                showProgrammeProgressProvider = { true },
                initialFocusedProvider = { true },
            )
        }
    }
}

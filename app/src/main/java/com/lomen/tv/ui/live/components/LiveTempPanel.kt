package com.lomen.tv.ui.live.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.lomen.tv.data.model.live.CurrentProgramme
import com.lomen.tv.data.model.live.LiveChannel
import com.lomen.tv.ui.theme.LomenTVTheme
import com.lomen.tv.ui.theme.PrimaryYellow

@Composable
fun LiveTempPanel(
    modifier: Modifier = Modifier,
    channelNoProvider: () -> Int = { 0 },
    currentChannelProvider: () -> LiveChannel = { LiveChannel() },
    currentChannelUrlIdxProvider: () -> Int = { 0 },
    currentProgrammesProvider: () -> CurrentProgramme? = { null },
    showProgrammeProgressProvider: () -> Boolean = { false },
) {
    val channel = currentChannelProvider()
    val channelUrlIdx = currentChannelUrlIdxProvider()
    val currentProgrammes = currentProgrammesProvider()
    val channelNo = channelNoProvider()

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(start = 40.dp, bottom = 40.dp, end = 40.dp, top = 20.dp)
    ) {
        // 右上角：频道号和时间
        Row(
            modifier = Modifier.align(Alignment.TopEnd),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 频道号（白色）
            Text(
                text = channelNo.toString().padStart(2, '0'),
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White
            )
            // 时间
            LiveDateTimeBar(showMode = LiveTimeShowMode.ALWAYS)
        }

        // 左下角信息面板 - 带深色半透明背景
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .background(
                    color = Color.Black.copy(alpha = 0.7f),
                    shape = MaterialTheme.shapes.medium
                )
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // 频道名称行（带标签）
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = channel.name,
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                // 多线路标识
                if (channel.urlList.size > 1) {
                    Text(
                        text = "${channelUrlIdx + 1}/${channel.urlList.size}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.9f),
                        modifier = Modifier
                            .background(
                                Color.Gray.copy(alpha = 0.6f),
                                MaterialTheme.shapes.extraSmall
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                // IPV4/IPV6 标识
                if (channel.urlList.isNotEmpty() && channelUrlIdx < channel.urlList.size) {
                    val url = channel.urlList[channelUrlIdx]
                    val isIPv6 = url.contains("[") && url.contains("]")
                    Text(
                        text = if (isIPv6) "IPV6" else "IPV4",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.9f),
                        modifier = Modifier
                            .background(
                                Color.Gray.copy(alpha = 0.6f),
                                MaterialTheme.shapes.extraSmall
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            // 当前节目信息
            Text(
                text = "正在播放：${currentProgrammes?.now?.title ?: "无节目"}",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.85f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            // 稍后播放信息
            if (currentProgrammes?.next?.title != null) {
                Text(
                    text = "稍后播放：${currentProgrammes.next.title}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            // 节目进度条
            if (showProgrammeProgressProvider() && currentProgrammes?.now != null) {
                Spacer(modifier = Modifier.height(8.dp))
                val programme = currentProgrammes.now
                val progress = remember(programme) {
                    val now = System.currentTimeMillis()
                    when {
                        now < programme.startAt -> 0f
                        now > programme.endAt -> 1f
                        else -> (now - programme.startAt).toFloat() / 
                                (programme.endAt - programme.startAt).toFloat()
                    }
                }
                Box(
                    modifier = Modifier
                        .width(200.dp)
                        .height(3.dp)
                        .background(Color.Gray.copy(alpha = 0.5f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progress)
                            .height(3.dp)
                            .background(PrimaryYellow)
                    )
                }
            }
        }
    }
}

@Preview(device = "id:Android TV (720p)")
@Composable
private fun LiveTempPanelPreview() {
    LomenTVTheme {
        LiveTempPanel(
            channelNoProvider = { 1 },
            currentChannelProvider = { LiveChannel.EXAMPLE },
            currentChannelUrlIdxProvider = { 0 },
            currentProgrammesProvider = { CurrentProgramme.EXAMPLE },
            showProgrammeProgressProvider = { true },
        )
    }
}

package com.lomen.tv.ui.live.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.lomen.tv.data.model.live.CurrentProgramme
import com.lomen.tv.data.model.live.LiveChannel
import com.lomen.tv.ui.theme.LomenTVTheme

@Composable
fun LiveChannelInfo(
    modifier: Modifier = Modifier,
    channelProvider: () -> LiveChannel = { LiveChannel() },
    channelUrlIdxProvider: () -> Int = { 0 },
    currentProgrammesProvider: () -> CurrentProgramme? = { null },
) {
    val channel = channelProvider()
    val channelUrlIdx = channelUrlIdxProvider()
    val currentProgrammes = currentProgrammesProvider()

    Column(modifier = modifier) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = channel.name,
                style = MaterialTheme.typography.headlineLarge,
                modifier = Modifier.alignByBaseline(),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            Spacer(modifier = Modifier.width(6.dp))

            Row(
                modifier = Modifier.padding(bottom = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                CompositionLocalProvider(
                    LocalTextStyle provides MaterialTheme.typography.labelMedium,
                    LocalContentColor provides LocalContentColor.current.copy(alpha = 0.8f),
                ) {
                    val textModifier = Modifier
                        .background(
                            LocalContentColor.current.copy(alpha = 0.3f),
                            MaterialTheme.shapes.extraSmall,
                        )
                        .padding(vertical = 2.dp, horizontal = 4.dp)

                    // 多线路标识
                    if (channel.urlList.size > 1) {
                        Text(
                            text = "${channelUrlIdx + 1}/${channel.urlList.size}",
                            modifier = textModifier,
                        )
                    }

                    // ipv4、ipv6标识
                    if (channel.urlList.isNotEmpty() && channelUrlIdx < channel.urlList.size) {
                        val url = channel.urlList[channelUrlIdx]
                        val isIPv6 = url.contains("[") && url.contains("]")
                        Text(
                            text = if (isIPv6) "IPV6" else "IPV4",
                            modifier = textModifier,
                        )
                    }
                }
            }
        }

        CompositionLocalProvider(
            LocalTextStyle provides MaterialTheme.typography.bodyLarge,
            LocalContentColor provides LocalContentColor.current.copy(alpha = 0.8f),
        ) {
            Text(
                text = "正在播放：${currentProgrammes?.now?.title ?: "无节目"}",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "稍后播放：${currentProgrammes?.next?.title ?: "无节目"}",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Preview
@Composable
private fun LiveChannelInfoPreview() {
    LomenTVTheme {
        LiveChannelInfo(
            channelProvider = { LiveChannel.EXAMPLE },
            channelUrlIdxProvider = { 1 },
            currentProgrammesProvider = { CurrentProgramme.EXAMPLE },
        )
    }
}

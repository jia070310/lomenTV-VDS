package com.lomen.tv.ui.live.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.lomen.tv.ui.theme.LomenTVTheme

@Composable
fun LiveChannelInput(
    modifier: Modifier = Modifier,
    channelNoProvider: () -> String = { "" },
) {
    val channelNo = channelNoProvider()
    if (channelNo.isNotBlank()) {
        Box(
            modifier = modifier
                .alpha(0.9f)
                .background(
                    color = MaterialTheme.colorScheme.background.copy(alpha = 0.8f),
                    shape = RoundedCornerShape(8.dp)
                )
                .padding(horizontal = 24.dp, vertical = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = channelNo,
                style = MaterialTheme.typography.displayLarge,
            )
        }
    }
}

@Preview(device = "id:Android TV (720p)")
@Composable
private fun LiveChannelInputPreview() {
    LomenTVTheme {
        Box(
            modifier = Modifier.padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            LiveChannelInput(channelNoProvider = { "123" })
        }
    }
}

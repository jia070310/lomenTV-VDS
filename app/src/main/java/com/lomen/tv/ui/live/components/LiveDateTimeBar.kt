package com.lomen.tv.ui.live.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.lomen.tv.ui.theme.LomenTVTheme
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun LiveDateTimeBar(
    modifier: Modifier = Modifier,
    showMode: LiveTimeShowMode = LiveTimeShowMode.ALWAYS,
) {
    var currentTime by remember { mutableStateOf(System.currentTimeMillis()) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            currentTime = System.currentTimeMillis()
        }
    }

    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    val dateFormat = SimpleDateFormat("MM/dd E", Locale.getDefault())

    val shouldShow = when (showMode) {
        LiveTimeShowMode.ALWAYS -> true
        LiveTimeShowMode.HIDE -> false
    }

    if (shouldShow) {
        Box(
            modifier = modifier
                .alpha(0.8f)
                .background(
                    color = MaterialTheme.colorScheme.background.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(4.dp)
                )
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text(
                text = "${dateFormat.format(Date(currentTime))} ${timeFormat.format(Date(currentTime))}",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
fun LiveChannelNumber(
    modifier: Modifier = Modifier,
    channelNoProvider: () -> String = { "" },
) {
    val channelNo = channelNoProvider()
    if (channelNo.isNotBlank()) {
        Box(
            modifier = modifier
                .alpha(0.8f)
                .background(
                    color = MaterialTheme.colorScheme.background.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(4.dp)
                )
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text(
                text = channelNo,
                style = MaterialTheme.typography.headlineMedium,
            )
        }
    }
}

enum class LiveTimeShowMode {
    ALWAYS,
    HIDE,
}

@Preview
@Composable
private fun LiveDateTimeBarPreview() {
    LomenTVTheme {
        LiveDateTimeBar()
    }
}

@Preview
@Composable
private fun LiveChannelNumberPreview() {
    LomenTVTheme {
        LiveChannelNumber(channelNoProvider = { "01" })
    }
}

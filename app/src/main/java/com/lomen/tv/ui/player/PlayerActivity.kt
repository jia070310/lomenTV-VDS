package com.lomen.tv.ui.player

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import com.lomen.tv.R
import com.lomen.tv.ui.theme.BackgroundDark
import com.lomen.tv.ui.theme.LomenTVTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class PlayerActivity : ComponentActivity() {

    companion object {
        const val EXTRA_VIDEO_URL = "extra_video_url"
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_EPISODE_TITLE = "extra_episode_title"
        const val EXTRA_MEDIA_ID = "extra_media_id"
        const val EXTRA_EPISODE_ID = "extra_episode_id"
        const val EXTRA_START_POSITION = "extra_start_position"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.setBackgroundDrawableResource(R.color.background_dark)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        val videoUrl = intent.getStringExtra(EXTRA_VIDEO_URL) ?: ""
        val title = intent.getStringExtra(EXTRA_TITLE)
        val episodeTitle = intent.getStringExtra(EXTRA_EPISODE_TITLE)
        val mediaId = intent.getStringExtra(EXTRA_MEDIA_ID)
        val episodeId = intent.getStringExtra(EXTRA_EPISODE_ID)
        val startPosition = intent.getLongExtra(EXTRA_START_POSITION, 0L)

        setContent {
            LomenTVTheme {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(BackgroundDark)
                ) {
                    PlayerScreen(
                        videoUrl = videoUrl,
                        title = title,
                        episodeTitle = episodeTitle,
                        mediaId = mediaId,
                        episodeId = episodeId,
                        startPosition = startPosition,
                        onBackPressed = { finish() }
                    )
                }
            }
        }
    }
}

package com.lomen.tv.ui.player

import android.view.KeyEvent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBackIos
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.IconButton
import androidx.tv.material3.IconButtonDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.lomen.tv.domain.service.PlayerState
import com.lomen.tv.ui.theme.BackgroundDark
import com.lomen.tv.ui.theme.PrimaryYellow
import com.lomen.tv.ui.theme.SurfaceDark
import com.lomen.tv.ui.theme.TextPrimary
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

// 控制栏模式枚举
enum class ControlsMode {
    SEEK,  // 快进退模式：无焦点，按钮浅灰色锁定，只能左右快进退
    NAV    // 导航模式：有焦点，可操作按钮，不能快进退
}

@OptIn(ExperimentalTvMaterial3Api::class, androidx.media3.common.util.UnstableApi::class)
@Composable
fun PlayerScreen(
    videoUrl: String,
    title: String? = null,
    episodeTitle: String? = null,
    mediaId: String? = null,
    episodeId: String? = null,
    startPosition: Long = 0L,
    onBackPressed: () -> Unit,
    viewModel: PlayerViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val playerState by viewModel.playerState.collectAsState()
    val episodeMessage by viewModel.episodeNavigationMessage.collectAsState()
    var showControls by remember { mutableStateOf(true) }
    
    // 当前播放的标题（可在切换剧集时更新）
    var currentTitle by remember { mutableStateOf(title) }
    var currentEpisodeTitle by remember { mutableStateOf(episodeTitle) }
    
    // 对话框状态
    var showSubtitleDialog by remember { mutableStateOf(false) }
    var showAudioTrackDialog by remember { mutableStateOf(false) }
    var showSpeedDialog by remember { mutableStateOf(false) }
    var showEpisodeListDialog by remember { mutableStateOf(false) }
    var showQualityDialog by remember { mutableStateOf(false) }
    
    // 获取字幕、音轨、倍速数据
    val availableSubtitles by viewModel.availableSubtitles.collectAsState()
    val availableAudioTracks by viewModel.availableAudioTracks.collectAsState()
    val selectedSubtitleIndex by viewModel.selectedSubtitleIndex.collectAsState()
    val selectedAudioTrackIndex by viewModel.selectedAudioTrackIndex.collectAsState()
    val availableSpeeds = remember { viewModel.getAvailableSpeeds() }
    val currentSpeed = remember { mutableStateOf(1.0f) }
    
    // 剧集列表和清晰度选项
    var episodeList by remember { mutableStateOf<List<PlayerViewModel.EpisodeListItem>>(emptyList()) }
    var qualityOptions by remember { mutableStateOf<List<PlayerViewModel.QualityOption>>(emptyList()) }
    var currentQualityLabel by remember { mutableStateOf("高清") }
    
    // 显示错误信息（用 Snackbar 替代 Toast，更适合 TV）
    var showErrorDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    
    LaunchedEffect(playerState.error) {
        if (playerState.error != null) {
            android.util.Log.e("PlayerScreen", "Player error: ${playerState.error}")
            errorMessage = playerState.error!!
            showErrorDialog = true
        }
    }
    val focusRequester = remember { FocusRequester() }
    
    // 控制栏模式：null=隐藏, SEEK=快进退模式(无焦点,按钮锁定), NAV=导航模式(有焦点,可操作按钮)
    var controlsMode by remember { mutableStateOf<ControlsMode?>(null) }
    
    // 自动隐藏控制栏的状态：记录最后一次交互时间
    var lastInteractionTime by remember { mutableStateOf(System.currentTimeMillis()) }
    
    // 播放/暂停按钮焦点请求器
    val playPauseFocusRequester = remember { FocusRequester() }
    // 返回按钮焦点请求器
    val backButtonFocusRequester = remember { FocusRequester() }

    // 设置媒体信息（用于保存播放历史）
    LaunchedEffect(mediaId, episodeId) {
        if (mediaId != null) {
            viewModel.setMediaInfo(mediaId, episodeId)
        }
    }
    
    // 设置剧集导航回调
    LaunchedEffect(Unit) {
        viewModel.setEpisodeNavigationCallback { videoUrl, title, episodeTitle, mediaId, episodeId, startPosition ->
            // 启动新的播放器 Activity
            val intent = android.content.Intent(context, PlayerActivity::class.java).apply {
                putExtra(PlayerActivity.EXTRA_VIDEO_URL, videoUrl)
                putExtra(PlayerActivity.EXTRA_TITLE, title)
                putExtra(PlayerActivity.EXTRA_EPISODE_TITLE, episodeTitle)
                putExtra(PlayerActivity.EXTRA_MEDIA_ID, mediaId)
                putExtra(PlayerActivity.EXTRA_EPISODE_ID, episodeId)
                putExtra(PlayerActivity.EXTRA_START_POSITION, startPosition)
            }
            context.startActivity(intent)
            // 关闭当前播放器
            onBackPressed()
        }
    }
    
    // 显示提示消息
    LaunchedEffect(episodeMessage) {
        if (episodeMessage != null) {
            android.widget.Toast.makeText(context, episodeMessage, android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    // 初始化播放器
    LaunchedEffect(Unit) {
        android.util.Log.d("PlayerScreen", "Initializing player...")
        viewModel.initializePlayer()
        android.util.Log.d("PlayerScreen", "Player initialized, preparing media...")
        viewModel.prepareMedia(videoUrl, title, episodeTitle, startPosition)
        android.util.Log.d("PlayerScreen", "Media preparation started, startPosition=$startPosition")
        focusRequester.requestFocus()
    }
    
    // 加载剧集列表和清晰度选项
    LaunchedEffect(mediaId, episodeId) {
        if (mediaId != null) {
            episodeList = viewModel.getEpisodeList()
            qualityOptions = viewModel.getQualityOptions()
            // 检测当前清晰度
            if (qualityOptions.isNotEmpty()) {
                val currentId = episodeId ?: mediaId
                val currentQuality = qualityOptions.find { it.id == currentId }
                currentQualityLabel = currentQuality?.label ?: "高清"
            }
        }
    }
    
    // 更新当前播放速度
    LaunchedEffect(playerState.isPlaying) {
        // 可以从 PlayerService 获取当前速度，这里简化处理
    }
    
    // 注意：起始位置现在在 prepareMedia 时直接设置，不需要在这里再次跳转

    // 自动隐藏控制栏：从用户停止操作开始计时
    LaunchedEffect(showControls, lastInteractionTime) {
        if (showControls) {
            // 等待5秒
            delay(5000)
            // 检查是否已经超过5秒没有操作
            val timeSinceLastInteraction = System.currentTimeMillis() - lastInteractionTime
            if (timeSinceLastInteraction >= 5000) {
                // 超过5秒没有操作，隐藏控制栏
                showControls = false
                controlsMode = null
            }
        }
    }
    
    // 当控制栏显示时，根据模式处理焦点
    LaunchedEffect(showControls, controlsMode) {
        if (showControls) {
            delay(100) // 等待UI渲染
            when (controlsMode) {
                ControlsMode.NAV -> {
                    // 导航模式：焦点聚焦到播放/暂停按钮
                    playPauseFocusRequester.requestFocus()
                }
                ControlsMode.SEEK -> {
                    // 快进退模式：焦点保持在根容器，不聚焦到任何按钮
                    focusRequester.requestFocus()
                }
                null -> {
                    // 默认情况
                    focusRequester.requestFocus()
                }
            }
        }
    }

    // 释放播放器
    DisposableEffect(Unit) {
        onDispose {
            viewModel.releasePlayer()
        }
    }

    // 双击返回键处理
    var backPressedOnce by remember { mutableStateOf(false) }
    var backPressJob by remember { mutableStateOf<Job?>(null) }
    
    BackHandler {
        if (showControls) {
            showControls = false
            controlsMode = null
        } else {
            if (backPressedOnce) {
                // 第二次按返回键，退出播放
                backPressJob?.cancel()
                onBackPressed()
            } else {
                // 第一次按返回键，显示提示
                backPressedOnce = true
                // 显示提示（使用Toast）
                android.widget.Toast.makeText(
                    context,
                    "再按一次返回键退出播放",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
                // 2秒后重置状态
                backPressJob = CoroutineScope(Dispatchers.Main).launch {
                    delay(2000)
                    backPressedOnce = false
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .focusable()
            .focusRequester(focusRequester)
            .onKeyEvent { event ->
                if (event.nativeKeyEvent.action == KeyEvent.ACTION_DOWN) {
                    when (event.nativeKeyEvent.keyCode) {
                        // OK键：切换播放/暂停，同时呼出控制栏（进入导航模式）
                        KeyEvent.KEYCODE_DPAD_CENTER,
                        KeyEvent.KEYCODE_ENTER -> {
                            showControls = true
                            controlsMode = ControlsMode.NAV
                            updateInteractionTime { lastInteractionTime = it }
                            viewModel.togglePlayPause()
                            true
                        }
                        // 左键：快退模式
                        KeyEvent.KEYCODE_DPAD_LEFT -> {
                            if (controlsMode == ControlsMode.NAV) {
                                // 导航模式下，左右键用于按钮间切换，不处理快进退
                                false
                            } else {
                                // 快进退模式或无控制栏时，执行快退
                                showControls = true
                                controlsMode = ControlsMode.SEEK
                                updateInteractionTime { lastInteractionTime = it }
                                viewModel.seekBackward()
                                true
                            }
                        }
                        // 右键：快进模式
                        KeyEvent.KEYCODE_DPAD_RIGHT -> {
                            if (controlsMode == ControlsMode.NAV) {
                                // 导航模式下，左右键用于按钮间切换，不处理快进退
                                false
                            } else {
                                // 快进退模式或无控制栏时，执行快进
                                showControls = true
                                controlsMode = ControlsMode.SEEK
                                updateInteractionTime { lastInteractionTime = it }
                                viewModel.seekForward()
                                true
                            }
                        }
                        // 上键：不作为呼出键
                        KeyEvent.KEYCODE_DPAD_UP -> {
                            if (!showControls) {
                                // 控制栏隐藏时，上键无效（不作为呼出键）
                                true
                            } else if (controlsMode == ControlsMode.NAV) {
                                // 导航模式下，返回false让系统处理焦点移动
                                updateInteractionTime { lastInteractionTime = it }
                                false
                            } else {
                                // 快进退模式下，上键无效（被锁定）
                                true
                            }
                        }
                        // 下键：呼出控制栏并进入导航模式，焦点聚焦到播放/暂停按钮
                        KeyEvent.KEYCODE_DPAD_DOWN -> {
                            if (!showControls) {
                                // 控制栏隐藏时，下键呼出控制栏并进入导航模式
                                showControls = true
                                controlsMode = ControlsMode.NAV
                                updateInteractionTime { lastInteractionTime = it }
                                // 返回false让系统继续处理焦点移动，焦点会自然落到播放/暂停按钮
                                false
                            } else if (controlsMode == ControlsMode.NAV) {
                                // 导航模式下，返回false让系统处理焦点移动（从返回按钮移到播放按钮）
                                updateInteractionTime { lastInteractionTime = it }
                                false
                            } else {
                                // 快进退模式下，下键无效（被锁定）
                                true
                            }
                        }
                        KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
                            showControls = true
                            controlsMode = ControlsMode.NAV
                            updateInteractionTime { lastInteractionTime = it }
                            viewModel.togglePlayPause()
                            true
                        }
                        KeyEvent.KEYCODE_MEDIA_REWIND -> {
                            showControls = true
                            controlsMode = ControlsMode.SEEK
                            updateInteractionTime { lastInteractionTime = it }
                            viewModel.seekBackward(30000)
                            true
                        }
                        KeyEvent.KEYCODE_MEDIA_FAST_FORWARD -> {
                            showControls = true
                            controlsMode = ControlsMode.SEEK
                            updateInteractionTime { lastInteractionTime = it }
                            viewModel.seekForward(30000)
                            true
                        }
                        // 返回键：隐藏状态栏
                        KeyEvent.KEYCODE_BACK -> {
                            if (showControls) {
                                showControls = false
                                controlsMode = null
                                true
                            } else {
                                false
                            }
                        }
                        else -> false
                    }
                } else {
                    false
                }
            }
    ) {
        // 视频播放器
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    useController = false
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                    // 保持屏幕常亮
                    setKeepScreenOn(true)
                    // 设置播放器
                    player = viewModel.getPlayer()
                    android.util.Log.d("PlayerScreen", "PlayerView created, player=${player}")
                }
            },
            update = { playerView ->
                // 确保player始终是最新的
                val currentPlayer = viewModel.getPlayer()
                if (playerView.player != currentPlayer) {
                    android.util.Log.d("PlayerScreen", "Updating PlayerView player: ${playerView.player} -> $currentPlayer")
                    playerView.player = currentPlayer
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // 顶部信息栏
        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            PlayerTopBar(
                title = currentTitle ?: "",
                episodeTitle = currentEpisodeTitle,
                onBackPressed = onBackPressed,
                backButtonFocusRequester = backButtonFocusRequester
            )
        }

        // 中央播放/暂停按钮（当暂停时显示）
        if (!playerState.isPlaying && playerState.type == PlayerState.Type.READY) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                IconButton(
                    onClick = { viewModel.play() },
                    modifier = Modifier.size(80.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play",
                        tint = Color.White,
                        modifier = Modifier.size(64.dp)
                    )
                }
            }
        }

        // 加载指示器
        if (playerState.type == PlayerState.Type.BUFFERING) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "加载中...",
                    style = MaterialTheme.typography.headlineMedium,
                    color = TextPrimary
                )
            }
        }

        // 底部控制栏
        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            PlayerControls(
                playerState = playerState,
                onPlayPause = { viewModel.togglePlayPause() },
                onSeekTo = { position -> viewModel.seekTo(position) },
                onSeekBackward = { viewModel.seekBackward() },
                onSeekForward = { viewModel.seekForward() },
                onSkipPrevious = { viewModel.seekToPrevious() },
                onSkipNext = { viewModel.seekToNext() },
                onUpdateInteractionTime = { lastInteractionTime = System.currentTimeMillis() },
                playPauseFocusRequester = playPauseFocusRequester,
                isLocked = controlsMode == ControlsMode.SEEK,
                availableSubtitles = availableSubtitles,
                availableAudioTracks = availableAudioTracks,
                currentSpeed = currentSpeed.value,
                currentQualityLabel = currentQualityLabel,
                onShowSubtitleDialog = { showSubtitleDialog = true },
                onShowAudioTrackDialog = { showAudioTrackDialog = true },
                onShowSpeedDialog = { showSpeedDialog = true },
                onShowQualityDialog = { showQualityDialog = true },
                // 选集相关
                episodes = episodeList,
                currentEpisodeId = episodeId,
                onSelectEpisode = { episode ->
                    CoroutineScope(Dispatchers.Main).launch {
                        val videoUrl = viewModel.resolvePlaybackUrl(episode.path ?: "")
                        
                        // 构造正确的 episodeTitle（只包含集数和副标题，不包含剧名）
                        val newEpisodeTitle = if (episode.title.isNotEmpty()) {
                            "第${episode.episodeNumber}集 ${episode.title}"
                        } else {
                            "第${episode.episodeNumber}集"
                        }
                        
                        // 更新当前剧集信息
                        viewModel.setMediaInfo(mediaId ?: "", episode.id)
                        
                        // 更新显示的标题
                        currentEpisodeTitle = newEpisodeTitle
                        
                        // 在当前播放器中切换剧集，不启动新 Activity
                        viewModel.prepareMedia(videoUrl, currentTitle, newEpisodeTitle, 0L)
                    }
                }
            )
        }
        
        // 字幕选择对话框
        if (showSubtitleDialog) {
            SubtitleSelectionDialog(
                subtitles = availableSubtitles,
                selectedIndex = selectedSubtitleIndex,
                onDismiss = { showSubtitleDialog = false },
                onSelect = { index ->
                    viewModel.selectSubtitle(index)
                    showSubtitleDialog = false
                }
            )
        }
        
        // 音轨选择对话框
        if (showAudioTrackDialog) {
            AudioTrackSelectionDialog(
                audioTracks = availableAudioTracks,
                selectedIndex = selectedAudioTrackIndex,
                onDismiss = { showAudioTrackDialog = false },
                onSelect = { index ->
                    viewModel.selectAudioTrack(index)
                    showAudioTrackDialog = false
                }
            )
        }
        
        // 倍速选择对话框
        if (showSpeedDialog) {
            SpeedSelectionDialog(
                speeds = availableSpeeds,
                currentSpeed = currentSpeed.value,
                onDismiss = { showSpeedDialog = false },
                onSelect = { speed ->
                    viewModel.setPlaybackSpeed(speed)
                    currentSpeed.value = speed
                    showSpeedDialog = false
                }
            )
        }
        
        // 清晰度选择对话框
        if (showQualityDialog) {
            QualitySelectionDialog(
                qualities = qualityOptions,
                currentQualityId = episodeId ?: mediaId ?: "",
                onDismiss = { showQualityDialog = false },
                onSelect = { quality ->
                    CoroutineScope(Dispatchers.Main).launch {
                        if (viewModel.switchQuality(quality.id)) {
                            currentQualityLabel = quality.label
                            showQualityDialog = false
                        }
                    }
                }
            )
        }
        
        // 错误对话框
        if (showErrorDialog) {
            ErrorDialog(
                errorMessage = errorMessage,
                onDismiss = {
                    showErrorDialog = false
                    // 清除错误状态
                    viewModel.clearError()
                },
                onRetry = {
                    showErrorDialog = false
                    // 重新初始化播放器并重试
                    viewModel.clearError()
                    viewModel.initializePlayer()
                    viewModel.prepareMedia(videoUrl, title, episodeTitle, startPosition)
                }
            )
        }
    }
}

// 更新最后交互时间，用于自动隐藏控制栏计时
private fun updateInteractionTime(setTime: (Long) -> Unit) {
    setTime(System.currentTimeMillis())
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun PlayerTopBar(
    title: String,
    episodeTitle: String?,
    onBackPressed: () -> Unit,
    backButtonFocusRequester: FocusRequester
) {
    // 获取当前时间
    var currentTime by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        while (true) {
            currentTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
            delay(60000) // 每分钟更新
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color.Black.copy(alpha = 0.6f),
                        Color.Transparent
                    )
                )
            )
            .padding(horizontal = 24.dp, vertical = 24.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            // 左侧：返回按钮 + 标题信息
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                var isBackFocused by remember { mutableStateOf(false) }
                IconButton(
                    onClick = onBackPressed,
                    colors = IconButtonDefaults.colors(
                        containerColor = SurfaceDark,
                        focusedContainerColor = PrimaryYellow,
                        contentColor = TextPrimary,
                        focusedContentColor = Color.Black
                    ),
                    modifier = Modifier
                        .size(40.dp)
                        .focusRequester(backButtonFocusRequester)
                        .onFocusChanged { isBackFocused = it.isFocused }
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBackIos,
                        contentDescription = "Back",
                        tint = if (isBackFocused) Color.Black else TextPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Column {
                    // 主标题 + 集数标题
                    // 添加调试日志
                    LaunchedEffect(title, episodeTitle) {
                        android.util.Log.d("PlayerScreen", "Title: $title, EpisodeTitle: $episodeTitle")
                    }
                    val displayTitle = if (episodeTitle != null && episodeTitle.isNotBlank()) {
                        "$title $episodeTitle"
                    } else {
                        title ?: ""
                    }
                    Text(
                        text = displayTitle,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 0.5.sp
                        ),
                        color = TextPrimary
                    )

                    // "正在播放"标签
                    Text(
                        text = "正在播放",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp
                        ),
                        color = PrimaryYellow,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }

            // 右侧：当前时间
            Text(
                text = currentTime,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = Color.White.copy(alpha = 0.9f)
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun PlayerControls(
    playerState: PlayerState,
    onPlayPause: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onSeekBackward: () -> Unit,
    onSeekForward: () -> Unit,
    onSkipPrevious: () -> Unit,
    onSkipNext: () -> Unit,
    onUpdateInteractionTime: () -> Unit,
    playPauseFocusRequester: FocusRequester,
    isLocked: Boolean = false,
    availableSubtitles: List<com.lomen.tv.domain.service.TrackInfo> = emptyList(),
    availableAudioTracks: List<com.lomen.tv.domain.service.TrackInfo> = emptyList(),
    currentSpeed: Float = 1.0f,
    currentQualityLabel: String = "高清",
    onShowSubtitleDialog: () -> Unit = {},
    onShowAudioTrackDialog: () -> Unit = {},
    onShowSpeedDialog: () -> Unit = {},
    onShowQualityDialog: () -> Unit = {},
    // 选集相关
    episodes: List<PlayerViewModel.EpisodeListItem> = emptyList(),
    currentEpisodeId: String? = null,
    onSelectEpisode: (PlayerViewModel.EpisodeListItem) -> Unit = {}
) {
    // 选集弹出菜单状态
    var showEpisodePopup by remember { mutableStateOf(false) }
    var episodeButtonCoords by remember { mutableStateOf(androidx.compose.ui.geometry.Rect.Zero) }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color.Black.copy(alpha = 0.8f)
                    )
                )
            )
            .padding(horizontal = 24.dp, vertical = 24.dp)
            .onFocusChanged { if (it.hasFocus) onUpdateInteractionTime() }
    ) {
        // 进度条区域（带时间提示气泡）
        val progress = if (playerState.duration > 0) {
            playerState.currentPosition.toFloat() / playerState.duration.toFloat()
        } else 0f

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        ) {
            // 时间提示气泡 - 显示在进度条上方，始终单行显示
            val bubbleWidth = 100.dp // 固定气泡宽度
            val progressWidth = progress * 100 // 进度百分比
            
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.CenterStart
            ) {
                // 计算气泡位置，确保不超出边界
                val bubbleOffset = (progress * 1000).dp.coerceIn(0.dp, 1000.dp)
                
                Box(
                    modifier = Modifier
                        .offset(
                            x = ((progress * 1000).toInt()).dp.coerceIn(0.dp, (1000 - 100).dp),
                            y = (-8).dp
                        )
                        .widthIn(min = 90.dp)
                        .background(
                            color = PrimaryYellow,
                            shape = RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "${formatDuration(playerState.currentPosition)} / ${formatDuration(playerState.duration)}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        color = Color.Black,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }

        // 进度条
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(3.dp))
        ) {
            // 已播放进度（黄色）
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress)
                    .height(6.dp)
                    .background(PrimaryYellow, RoundedCornerShape(3.dp))
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 控制按钮栏
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 左侧：播放控制 + 时间显示
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // 上一集/播放/下一集按钮组
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // 上一集
                    var isSkipPreviousFocused by remember { mutableStateOf(false) }
                    IconButton(
                        onClick = {
                            if (!isLocked) {
                                onSkipPrevious()
                                onUpdateInteractionTime()
                            }
                        },
                        enabled = !isLocked,
                        colors = IconButtonDefaults.colors(
                            containerColor = if (isLocked) Color.White.copy(alpha = 0.05f) else SurfaceDark,
                            focusedContainerColor = PrimaryYellow,
                            contentColor = if (isLocked) Color.White.copy(alpha = 0.3f) else TextPrimary,
                            focusedContentColor = Color.Black,
                            disabledContainerColor = Color.White.copy(alpha = 0.05f),
                            disabledContentColor = Color.White.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier
                            .size(36.dp)
                            .onFocusChanged { isSkipPreviousFocused = it.isFocused }
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipPrevious,
                            contentDescription = "Previous",
                            tint = if (isLocked) Color.White.copy(alpha = 0.3f) else if (isSkipPreviousFocused) Color.Black else TextPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // 播放/暂停按钮
                    var isPlayPauseFocused by remember { mutableStateOf(false) }
                    IconButton(
                        onClick = {
                            if (!isLocked) {
                                onPlayPause()
                                onUpdateInteractionTime()
                            }
                        },
                        enabled = !isLocked,
                        colors = IconButtonDefaults.colors(
                            containerColor = if (isLocked) Color.White.copy(alpha = 0.05f) else SurfaceDark,
                            focusedContainerColor = PrimaryYellow,
                            contentColor = if (isLocked) Color.White.copy(alpha = 0.3f) else TextPrimary,
                            focusedContentColor = Color.Black,
                            disabledContainerColor = Color.White.copy(alpha = 0.05f),
                            disabledContentColor = Color.White.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier
                            .size(48.dp)
                            .focusRequester(playPauseFocusRequester)
                            .onFocusChanged { isPlayPauseFocused = it.isFocused }
                    ) {
                        Icon(
                            imageVector = if (playerState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (playerState.isPlaying) "Pause" else "Play",
                            tint = if (isLocked) Color.White.copy(alpha = 0.3f) else if (isPlayPauseFocused) Color.Black else TextPrimary,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    // 下一集
                    var isSkipNextFocused by remember { mutableStateOf(false) }
                    IconButton(
                        onClick = {
                            if (!isLocked) {
                                onSkipNext()
                                onUpdateInteractionTime()
                            }
                        },
                        enabled = !isLocked,
                        colors = IconButtonDefaults.colors(
                            containerColor = if (isLocked) Color.White.copy(alpha = 0.05f) else SurfaceDark,
                            focusedContainerColor = PrimaryYellow,
                            contentColor = if (isLocked) Color.White.copy(alpha = 0.3f) else TextPrimary,
                            focusedContentColor = Color.Black,
                            disabledContainerColor = Color.White.copy(alpha = 0.05f),
                            disabledContentColor = Color.White.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier
                            .size(36.dp)
                            .onFocusChanged { isSkipNextFocused = it.isFocused }
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipNext,
                            contentDescription = "Next",
                            tint = if (isLocked) Color.White.copy(alpha = 0.3f) else if (isSkipNextFocused) Color.Black else TextPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                // 时间显示
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formatDuration(playerState.currentPosition),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        color = Color.White
                    )
                    Text(
                        text = " / ",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 14.sp
                        ),
                        color = Color.White.copy(alpha = 0.3f)
                    )
                    Text(
                        text = formatDuration(playerState.duration),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 14.sp
                        ),
                        color = Color.White.copy(alpha = 0.6f)
                    )
                }
            }

            // 右侧：功能按钮
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 字幕按钮
                ControlPillButton(
                    text = "字幕",
                    onClick = { 
                        if (!isLocked) {
                            onShowSubtitleDialog()
                            onUpdateInteractionTime()
                        }
                    },
                    isLocked = isLocked
                )

                // 音轨按钮
                ControlPillButton(
                    text = "音轨",
                    onClick = { 
                        if (!isLocked) {
                            onShowAudioTrackDialog()
                            onUpdateInteractionTime()
                        }
                    },
                    isLocked = isLocked
                )

                // 倍速按钮
                ControlPillButton(
                    text = "${currentSpeed}X",
                    onClick = { 
                        if (!isLocked) {
                            onShowSpeedDialog()
                            onUpdateInteractionTime()
                        }
                    },
                    isLocked = isLocked
                )

                // 选集按钮（带弹出菜单）
                Box {
                    ControlPillButton(
                        text = "选集",
                        onClick = { 
                            if (!isLocked) {
                                showEpisodePopup = true
                                onUpdateInteractionTime()
                            }
                        },
                        isLocked = isLocked,
                        modifier = Modifier.onGloballyPositioned { coordinates ->
                            val bounds = coordinates.boundsInWindow()
                            episodeButtonCoords = bounds
                        }
                    )
                    
                    // 选集弹出菜单
                    if (showEpisodePopup && episodes.isNotEmpty()) {
                        EpisodeListPopup(
                            episodes = episodes,
                            currentEpisodeId = currentEpisodeId,
                            onDismiss = { showEpisodePopup = false },
                            onSelectEpisode = { episode ->
                                showEpisodePopup = false
                                onSelectEpisode(episode)
                            },
                            anchorPosition = episodeButtonCoords
                        )
                    }
                }

                // 清晰度按钮（原蓝光按钮）
                ControlPillButton(
                    text = currentQualityLabel,
                    onClick = { 
                        if (!isLocked) {
                            onShowQualityDialog()
                            onUpdateInteractionTime()
                        }
                    },
                    isHighlighted = false,
                    isLocked = isLocked
                )
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun ControlPillButton(
    text: String,
    onClick: () -> Unit,
    isHighlighted: Boolean = false,
    isLocked: Boolean = false,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }
    
    Button(
        onClick = onClick,
        enabled = !isLocked,
        shape = ButtonDefaults.shape(shape = RoundedCornerShape(50)),
        colors = ButtonDefaults.colors(
            containerColor = if (isLocked) Color.White.copy(alpha = 0.05f) else SurfaceDark,
            focusedContainerColor = PrimaryYellow,
            contentColor = if (isLocked) Color.White.copy(alpha = 0.3f) else TextPrimary,
            focusedContentColor = Color.Black,
            disabledContainerColor = Color.White.copy(alpha = 0.05f),
            disabledContentColor = Color.White.copy(alpha = 0.3f)
        ),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        modifier = modifier.onFocusChanged { isFocused = it.isFocused }
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal
            ),
            color = if (isLocked) Color.White.copy(alpha = 0.3f) else if (isFocused) Color.Black else TextPrimary,
            maxLines = 1
        )
    }
}

private fun formatDuration(durationMs: Long): String {
    if (durationMs <= 0) return "00:00"

    val hours = TimeUnit.MILLISECONDS.toHours(durationMs)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(durationMs) % 60
    val seconds = TimeUnit.MILLISECONDS.toSeconds(durationMs) % 60

    return if (hours > 0) {
        String.format("%02d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%02d:%02d", minutes, seconds)
    }
}

// 字幕选择对话框
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun SubtitleSelectionDialog(
    subtitles: List<com.lomen.tv.domain.service.TrackInfo>,
    selectedIndex: Int,
    onDismiss: () -> Unit,
    onSelect: (Int) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .width(400.dp)
                .background(Color(0xFF1A1A1A), RoundedCornerShape(12.dp))
                .padding(24.dp)
        ) {
            Text(
                text = "选择字幕",
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            if (subtitles.isEmpty()) {
                Text(
                    text = "暂无可用字幕",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.6f)
                )
            } else {
                subtitles.forEachIndexed { index, subtitle ->
                    var isFocused by remember { mutableStateOf(false) }
                    val isSelected = index == selectedIndex
                    Button(
                        onClick = { onSelect(index) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .onFocusChanged { isFocused = it.isFocused },
                        colors = ButtonDefaults.colors(
                            containerColor = SurfaceDark,
                            focusedContainerColor = PrimaryYellow,
                            contentColor = TextPrimary,
                            focusedContentColor = Color.Black
                        )
                    ) {
                        Text(
                            text = subtitle.label,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isFocused) Color.Black else TextPrimary
                        )
                    }
                }
            }
        }
    }
}

// 音轨选择对话框
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun AudioTrackSelectionDialog(
    audioTracks: List<com.lomen.tv.domain.service.TrackInfo>,
    selectedIndex: Int,
    onDismiss: () -> Unit,
    onSelect: (Int) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .width(400.dp)
                .background(Color(0xFF1A1A1A), RoundedCornerShape(12.dp))
                .padding(24.dp)
        ) {
            Text(
                text = "选择音轨",
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            if (audioTracks.isEmpty()) {
                Text(
                    text = "暂无可用音轨",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.6f)
                )
            } else {
                audioTracks.forEachIndexed { index, track ->
                    var isFocused by remember { mutableStateOf(false) }
                    val isSelected = index == selectedIndex
                    Button(
                        onClick = { onSelect(index) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .onFocusChanged { isFocused = it.isFocused },
                        colors = ButtonDefaults.colors(
                            containerColor = SurfaceDark,
                            focusedContainerColor = PrimaryYellow,
                            contentColor = TextPrimary,
                            focusedContentColor = Color.Black
                        )
                    ) {
                        Text(
                            text = track.label,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isFocused) Color.Black else TextPrimary
                        )
                    }
                }
            }
        }
    }
}

// 倍速选择对话框
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun SpeedSelectionDialog(
    speeds: List<Float>,
    currentSpeed: Float,
    onDismiss: () -> Unit,
    onSelect: (Float) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .width(400.dp)
                .background(Color(0xFF1A1A1A), RoundedCornerShape(12.dp))
                .padding(24.dp)
        ) {
            Text(
                text = "选择播放倍速",
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            speeds.forEach { speed ->
                var isFocused by remember { mutableStateOf(false) }
                val isSelected = speed == currentSpeed
                Button(
                    onClick = { onSelect(speed) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .onFocusChanged { isFocused = it.isFocused },
                    colors = ButtonDefaults.colors(
                        containerColor = SurfaceDark,
                        focusedContainerColor = PrimaryYellow,
                        contentColor = TextPrimary,
                        focusedContentColor = Color.Black
                    )
                ) {
                    Text(
                        text = "${speed}X",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isFocused) Color.Black else TextPrimary
                    )
                }
            }
        }
    }
}

// 选集列表弹出菜单（在选集按钮上方弹出）
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun EpisodeListPopup(
    episodes: List<PlayerViewModel.EpisodeListItem>,
    currentEpisodeId: String?,
    onDismiss: () -> Unit,
    onSelectEpisode: (PlayerViewModel.EpisodeListItem) -> Unit,
    anchorPosition: androidx.compose.ui.geometry.Rect
) {
    val density = LocalDensity.current
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp
    val screenWidth = configuration.screenWidthDp.dp
    
    // 计算弹出位置：在按钮上方居中，高度拉满屏幕
    val popupHeight = screenHeight - 100.dp // 距离顶部和底部各留一些边距
    val popupWidth = 400.dp
    
    // 计算按钮中心位置
    val buttonCenterX = with(density) { (anchorPosition.left + anchorPosition.right) / 2 }
    
    // 计算偏移量：让弹出菜单在按钮上方居中，顶部对齐
    val offsetX = with(density) { buttonCenterX.toDp() - popupWidth / 2 }
    val offsetY = 50.dp // 距离顶部50dp
    
    Popup(
        offset = IntOffset(
            x = with(density) { offsetX.toPx() }.toInt().coerceIn(0, with(density) { (screenWidth - popupWidth).toPx() }.toInt()),
            y = with(density) { offsetY.toPx() }.toInt()
        ),
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true)
    ) {
        Column(
            modifier = Modifier
                .width(popupWidth)
                .height(popupHeight)
                .background(Color.Black.copy(alpha = 0.95f), RoundedCornerShape(8.dp))
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // 标题
            Text(
                text = "播放列表",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = Color.White,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            
            // 剧集列表
            LazyColumn(
                modifier = Modifier.fillMaxSize()
            ) {
                items(episodes.size) { index ->
                    val episode = episodes[index]
                    val isSelected = episode.id == currentEpisodeId
                    
                    var isFocused by remember { mutableStateOf(false) }
                    
                    // 显示格式：第X集 - 副标题（如果有）
                    val displayText = if (episode.title.isNotEmpty()) {
                        "第${episode.episodeNumber}集 - ${episode.title}"
                    } else {
                        "第${episode.episodeNumber}集"
                    }
                    
                    Button(
                        onClick = { onSelectEpisode(episode) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp)
                            .height(40.dp)
                            .onFocusChanged { isFocused = it.isFocused },
                        shape = ButtonDefaults.shape(shape = RoundedCornerShape(4.dp)),
                        colors = ButtonDefaults.colors(
                            containerColor = SurfaceDark,
                            focusedContainerColor = PrimaryYellow,
                            contentColor = TextPrimary,
                            focusedContentColor = Color.Black
                        )
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = displayText,
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontSize = 14.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                ),
                                color = if (isFocused) Color.Black else TextPrimary,
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                            
                            // 选中标记
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "正在播放",
                                    tint = if (isFocused) Color.Black else TextPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// 清晰度选择对话框
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun QualitySelectionDialog(
    qualities: List<PlayerViewModel.QualityOption>,
    currentQualityId: String,
    onDismiss: () -> Unit,
    onSelect: (PlayerViewModel.QualityOption) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .width(400.dp)
                .background(Color(0xFF1A1A1A), RoundedCornerShape(12.dp))
                .padding(24.dp)
        ) {
            Text(
                text = "选择清晰度",
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            if (qualities.isEmpty()) {
                Text(
                    text = "暂无可用清晰度",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.6f)
                )
            } else {
                qualities.forEach { quality ->
                    var isFocused by remember { mutableStateOf(false) }
                    val isSelected = quality.id == currentQualityId
                    Button(
                        onClick = { onSelect(quality) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .onFocusChanged { isFocused = it.isFocused },
                        colors = ButtonDefaults.colors(
                            containerColor = SurfaceDark,
                            focusedContainerColor = PrimaryYellow,
                            contentColor = TextPrimary,
                            focusedContentColor = Color.Black
                        )
                    ) {
                        Text(
                            text = quality.label,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isFocused) Color.Black else TextPrimary
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun ErrorDialog(
    errorMessage: String,
    onDismiss: () -> Unit,
    onRetry: () -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    val retryFocusRequester = remember { FocusRequester() }
    
    LaunchedEffect(Unit) {
        // 自动聚焦到重试按钮
        delay(100)
        retryFocusRequester.requestFocus()
    }
    
    Dialog(
        onDismissRequest = onDismiss
    ) {
        Box(
            modifier = Modifier
                .width(500.dp)
                .wrapContentHeight()
                .background(Color.Black.copy(alpha = 0.95f), RoundedCornerShape(12.dp))
                .focusable()
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 错误图标
                Text(
                    text = "⚠️",
                    style = MaterialTheme.typography.displayMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                // 标题
                Text(
                    text = "播放错误",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = Color.White,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                
                // 错误信息
                Text(
                    text = errorMessage,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 14.sp
                    ),
                    color = Color.White.copy(alpha = 0.8f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp)
                )
                
                // 按钮行
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)
                ) {
                    // 重试按钮
                    Button(
                        onClick = onRetry,
                        modifier = Modifier
                            .width(120.dp)
                            .focusRequester(retryFocusRequester),
                        colors = ButtonDefaults.colors(
                            containerColor = PrimaryYellow,
                            contentColor = Color.Black,
                            focusedContainerColor = PrimaryYellow.copy(alpha = 0.8f),
                            focusedContentColor = Color.Black
                        )
                    ) {
                        Text(
                            text = "重试",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                    
                    // 关闭按钮
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier
                            .width(120.dp)
                            .focusRequester(focusRequester),
                        colors = ButtonDefaults.colors(
                            containerColor = Color.Gray.copy(alpha = 0.3f),
                            contentColor = Color.White,
                            focusedContainerColor = Color.Gray.copy(alpha = 0.5f),
                            focusedContentColor = Color.White
                        )
                    ) {
                        Text(
                            text = "关闭",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        }
    }
}

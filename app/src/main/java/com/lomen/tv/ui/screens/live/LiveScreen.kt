package com.lomen.tv.ui.screens.live

import android.net.Uri
import android.view.KeyEvent
import android.view.SurfaceView
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.util.Util
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import com.lomen.tv.data.preferences.LiveSettingsPreferences
import com.lomen.tv.ui.live.components.LiveChannelGroupList
import com.lomen.tv.ui.live.components.LiveChannelInfo
import com.lomen.tv.ui.live.components.LiveChannelInput
import com.lomen.tv.ui.live.components.LiveDateTimeBar
import com.lomen.tv.ui.live.components.LiveFavoriteList
import com.lomen.tv.ui.live.components.LivePanelScreen
import com.lomen.tv.ui.live.components.LiveStatusBar
import com.lomen.tv.ui.live.components.LiveTempPanel
import com.lomen.tv.ui.live.components.LiveTimeShowMode
import com.lomen.tv.ui.live.utils.handleLiveDragGestures
import com.lomen.tv.ui.live.utils.handleLiveKeyEvents
import com.lomen.tv.ui.theme.LomenTVTheme
import com.lomen.tv.data.model.live.ChannelEpgList
import kotlinx.coroutines.delay

@Composable
fun LiveScreen(
    modifier: Modifier = Modifier,
    onNavigateBack: () -> Unit = {},
    viewModel: LiveViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val channelGroupList by viewModel.channelGroupList.collectAsState()
    val epgList by viewModel.epgList.collectAsState()
    val currentChannel by viewModel.currentChannel.collectAsState()
    val currentChannelUrlIdx by viewModel.currentChannelUrlIdx.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val isPanelVisible by viewModel.isPanelVisible.collectAsState()
    val channelInput by viewModel.channelInput.collectAsState()
    val favoriteList by viewModel.favoriteList.collectAsState()
    val favoriteListVisible by viewModel.favoriteListVisible.collectAsState()

    // 状态栏显示控制
    var isStatusBarVisible by remember { mutableStateOf(false) }

    // 直播源列表
    val sourceList by viewModel.sourceList.collectAsState()
    val currentSourceUrl by viewModel.currentSourceUrl.collectAsState()

    // 画面比例
    val videoAspectRatio by viewModel.videoAspectRatio.collectAsState()
    
    // 功能开关设置
    val channelChangeFlip by viewModel.channelChangeFlip.collectAsState()
    val channelNoSelectEnable by viewModel.channelNoSelectEnable.collectAsState()
    val epgEnable by viewModel.epgEnable.collectAsState()
    
    // 错误提示
    val errorToastMessage by viewModel.errorToastMessage.collectAsState()
    val showErrorDialog by viewModel.showErrorDialog.collectAsState()
    val errorDialogMessage by viewModel.errorDialogMessage.collectAsState()
    
    val configuration = LocalConfiguration.current

    // ExoPlayer
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            playWhenReady = true
        }
    }

    // 计算画面比例
    val aspectRatio = remember(videoAspectRatio, configuration) {
        when (videoAspectRatio) {
            LiveSettingsPreferences.Companion.VideoAspectRatio.ORIGINAL -> null
            LiveSettingsPreferences.Companion.VideoAspectRatio.SIXTEEN_NINE -> 16f / 9f
            LiveSettingsPreferences.Companion.VideoAspectRatio.FOUR_THREE -> 4f / 3f
            LiveSettingsPreferences.Companion.VideoAspectRatio.AUTO -> 
                configuration.screenHeightDp.toFloat() / configuration.screenWidthDp.toFloat()
        }
    }

    // 视频元数据
    var videoWidth by remember { mutableIntStateOf(0) }
    var videoHeight by remember { mutableIntStateOf(0) }
    var networkSpeed by remember { mutableLongStateOf(0L) }
    var lastBytesTransferred by remember { mutableLongStateOf(0L) }
    var lastSpeedUpdateTime by remember { mutableLongStateOf(0L) }
    
    // 内容类型重试记录
    var contentTypeAttempts by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var currentContentType by remember { mutableIntStateOf(C.CONTENT_TYPE_OTHER) }
    
    // 当切换频道时，重置重试状态
    LaunchedEffect(currentChannel, currentChannelUrlIdx) {
        contentTypeAttempts = emptySet()
        currentContentType = Util.inferContentType(Uri.parse(currentChannel.urlList.getOrNull(currentChannelUrlIdx) ?: ""))
        android.util.Log.d("LiveScreen", "切换频道，重置内容类型为: $currentContentType")
    }

    // 添加播放器监听
    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onVideoSizeChanged(videoSize: VideoSize) {
                videoWidth = videoSize.width
                videoHeight = videoSize.height
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                android.util.Log.d("LiveScreen", "播放状态变化: $playbackState")
                if (playbackState == Player.STATE_READY) {
                    // 播放就绪时更新分辨率
                    exoPlayer.videoFormat?.let { format ->
                        videoWidth = format.width
                        videoHeight = format.height
                    }
                }
            }

            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                android.util.Log.e("LiveScreen", "播放器错误: ${error.errorCodeName}, ${error.message}")
                
                // 当解析容器不支持时，尝试其他内容类型
                if (error.errorCode == androidx.media3.common.PlaybackException.ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED) {
                    android.util.Log.d("LiveScreen", "尝试其他内容类型，当前已尝试: $contentTypeAttempts")
                    
                    when {
                        !contentTypeAttempts.contains(C.CONTENT_TYPE_HLS) -> {
                            contentTypeAttempts = contentTypeAttempts + C.CONTENT_TYPE_HLS
                            currentContentType = C.CONTENT_TYPE_HLS
                            android.util.Log.d("LiveScreen", "切换到 HLS 内容类型重试")
                        }
                        !contentTypeAttempts.contains(C.CONTENT_TYPE_OTHER) -> {
                            contentTypeAttempts = contentTypeAttempts + C.CONTENT_TYPE_OTHER
                            currentContentType = C.CONTENT_TYPE_OTHER
                            android.util.Log.d("LiveScreen", "切换到 OTHER 内容类型重试")
                        }
                        else -> {
                            android.util.Log.e("LiveScreen", "所有内容类型都尝试过，播放失败，切换到下一个频道")
                            // 所有内容类型都尝试过，通知 ViewModel 处理错误
                            viewModel.handlePlayError(error.message)
                        }
                    }
                } else {
                    // 其他播放错误，直接通知 ViewModel 处理
                    android.util.Log.e("LiveScreen", "播放错误，通知 ViewModel 处理")
                    viewModel.handlePlayError(error.message)
                }
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
        }
    }

    // 网速计算
    LaunchedEffect(exoPlayer) {
        while (true) {
            delay(1000)
            val currentBytes = exoPlayer.totalBufferedDuration
            val currentTime = System.currentTimeMillis()
            
            if (lastSpeedUpdateTime > 0) {
                val bytesDiff = currentBytes - lastBytesTransferred
                val timeDiff = currentTime - lastSpeedUpdateTime
                if (timeDiff > 0) {
                    // 转换为 KB/s
                    networkSpeed = (bytesDiff / 1024 * 1000 / timeDiff)
                }
            }
            
            lastBytesTransferred = currentBytes
            lastSpeedUpdateTime = currentTime
        }
    }

    // 临时面板显示控制
    var isTempPanelVisible by remember { mutableStateOf(false) }
    LaunchedEffect(currentChannel) {
        isTempPanelVisible = true
        delay(3000)
        isTempPanelVisible = false
    }

    // 播放当前频道
    @OptIn(UnstableApi::class)
    LaunchedEffect(currentChannel, currentChannelUrlIdx, currentContentType) {
        android.util.Log.d("LiveScreen", "播放频道: ${currentChannel.name}, urlList大小: ${currentChannel.urlList.size}, urlIdx: $currentChannelUrlIdx")
        android.util.Log.d("LiveScreen", "频道UA: ${currentChannel.userAgent}, 频道Referer: ${currentChannel.referer}")
        
        if (currentChannel.urlList.isNotEmpty() && currentChannelUrlIdx < currentChannel.urlList.size) {
            val url = currentChannel.urlList[currentChannelUrlIdx]
            android.util.Log.d("LiveScreen", "播放URL: $url")
            
            // 获取频道的 UA 和 Referer（优先使用频道的，其次使用全局的）
            val channelUA = currentChannel.userAgent
            val channelReferer = currentChannel.referer
            
            try {
                // 先停止当前播放
                exoPlayer.stop()
                exoPlayer.clearMediaItems()
                
                val mediaItem = MediaItem.fromUri(url)
                
                // 创建 DataSource Factory
                val httpDataSourceFactory = DefaultHttpDataSource.Factory().apply {
                    // 使用频道的 UA，如果没有则使用默认 UA
                    if (!channelUA.isNullOrBlank()) {
                        setUserAgent(channelUA)
                        android.util.Log.d("LiveScreen", "设置自定义UA: $channelUA")
                    }
                    // 设置 Referer
                    if (!channelReferer.isNullOrBlank()) {
                        setDefaultRequestProperties(mapOf("Referer" to channelReferer))
                        android.util.Log.d("LiveScreen", "设置Referer: $channelReferer")
                    }
                    // 允许跨协议重定向（HTTP 到 HTTPS 或反之）
                    setAllowCrossProtocolRedirects(true)
                    // 设置连接和读取超时
                    setConnectTimeoutMs(15000)
                    setReadTimeoutMs(15000)
                }
                
                val dataSourceFactory = DefaultDataSource.Factory(context, httpDataSourceFactory)
                
                // 根据内容类型选择正确的 MediaSource
                android.util.Log.d("LiveScreen", "使用内容类型: $currentContentType")
                
                val mediaSource = when (currentContentType) {
                    C.CONTENT_TYPE_HLS -> {
                        android.util.Log.d("LiveScreen", "使用 HLS MediaSource")
                        HlsMediaSource.Factory(dataSourceFactory).createMediaSource(mediaItem)
                    }
                    else -> {
                        android.util.Log.d("LiveScreen", "使用 Progressive MediaSource")
                        ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource(mediaItem)
                    }
                }
                
                exoPlayer.setMediaSource(mediaSource)
                exoPlayer.prepare()
                exoPlayer.play()
            } catch (e: Exception) {
                android.util.Log.e("LiveScreen", "播放失败: ${e.message}", e)
            }
        } else {
            android.util.Log.w("LiveScreen", "无法播放: urlList为空或索引越界")
        }
    }

    // 释放播放器
    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }

    // 焦点请求
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        android.util.Log.d("LiveScreen", "初始请求焦点")
        focusRequester.requestFocus()
    }
    
    // 当面板关闭时，重新请求焦点到按键处理层
    LaunchedEffect(isPanelVisible) {
        if (!isPanelVisible) {
            android.util.Log.d("LiveScreen", "面板关闭，重新请求焦点")
            kotlinx.coroutines.delay(200) // 延迟确保面板已关闭
            focusRequester.requestFocus()
        }
    }

    // 当状态栏关闭时，重新请求焦点到按键处理层
    LaunchedEffect(isStatusBarVisible) {
        if (!isStatusBarVisible) {
            android.util.Log.d("LiveScreen", "状态栏关闭，重新请求焦点")
            kotlinx.coroutines.delay(200) // 延迟确保状态栏已关闭
            focusRequester.requestFocus()
        }
    }
    
    // 当临时面板关闭时，重新请求焦点到按键处理层
    LaunchedEffect(isTempPanelVisible) {
        if (!isTempPanelVisible) {
            android.util.Log.d("LiveScreen", "临时面板关闭，重新请求焦点")
            kotlinx.coroutines.delay(200) // 延迟确保面板已关闭
            focusRequester.requestFocus()
        }
    }

    // 数字选台超时处理
    LaunchedEffect(channelInput) {
        if (channelInput.isNotEmpty()) {
            delay(2000)
            viewModel.confirmChannelInput()
        }
    }

    // 双击返回键退出计时
    var backPressCount by remember { mutableStateOf(0) }
    LaunchedEffect(backPressCount) {
        if (backPressCount > 0) {
            android.util.Log.d("LiveScreen", "backPressCount=$backPressCount, 等待2秒后重置")
            delay(2000) // 2秒内再次按返回键才有效
            backPressCount = 0
            android.util.Log.d("LiveScreen", "backPressCount 已重置为0")
        }
    }

    BackHandler {
        android.util.Log.d("LiveScreen", "BackHandler 触发, isPanelVisible=$isPanelVisible, isStatusBarVisible=$isStatusBarVisible, backPressCount=$backPressCount")
        when {
            isPanelVisible -> {
                android.util.Log.d("LiveScreen", "关闭面板")
                viewModel.hidePanel()
            }
            isStatusBarVisible -> {
                android.util.Log.d("LiveScreen", "关闭状态栏")
                isStatusBarVisible = false
            }
            else -> {
                // 无窗口打开时，按两次返回键退出
                backPressCount++
                android.util.Log.d("LiveScreen", "backPressCount 增加到: $backPressCount")
                if (backPressCount >= 2) {
                    android.util.Log.d("LiveScreen", "调用 onNavigateBack")
                    onNavigateBack()
                } else {
                    // 第一次按返回键，显示提示
                    android.util.Log.d("LiveScreen", "显示 Toast 提示")
                    Toast.makeText(context, "再按一次返回键退出", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        // 视频播放器 - 使用 SurfaceView 支持画面比例调整
        AndroidView(
            factory = { context ->
                SurfaceView(context)
            },
            update = { surfaceView ->
                exoPlayer.setVideoSurfaceView(surfaceView)
            },
            modifier = Modifier
                .align(Alignment.Center)
                .then(
                    if (aspectRatio != null) {
                        Modifier.aspectRatio(aspectRatio)
                    } else {
                        Modifier.fillMaxSize()
                    }
                )
        )

        // 按键处理层
        Box(
            modifier = Modifier
                .fillMaxSize()
                .focusRequester(focusRequester)
                .focusable()
                .onPreviewKeyEvent { keyEvent ->
                    // 直接处理返回键
                    if (keyEvent.key == androidx.compose.ui.input.key.Key.Back && 
                        keyEvent.type == androidx.compose.ui.input.key.KeyEventType.KeyUp) {
                        android.util.Log.d("LiveScreen", "onPreviewKeyEvent 收到返回键，直接处理")
                        when {
                            isPanelVisible -> {
                                android.util.Log.d("LiveScreen", "关闭面板")
                                viewModel.hidePanel()
                            }
                            isStatusBarVisible -> {
                                android.util.Log.d("LiveScreen", "关闭状态栏")
                                isStatusBarVisible = false
                            }
                            else -> {
                                backPressCount++
                                android.util.Log.d("LiveScreen", "backPressCount 增加到: $backPressCount")
                                if (backPressCount >= 2) {
                                    android.util.Log.d("LiveScreen", "调用 onNavigateBack")
                                    onNavigateBack()
                                } else {
                                    android.util.Log.d("LiveScreen", "显示 Toast 提示")
                                    Toast.makeText(context, "再按一次返回键退出", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                        true // 消费事件
                    } else {
                        false
                    }
                }
                .handleLiveKeyEvents(
                    onUp = {
                        if (isPanelVisible) {
                            // 频道列表打开时，上下键在列表内处理
                        } else {
                            viewModel.changeToPreviousChannel()
                        }
                    },
                    onDown = {
                        if (isPanelVisible) {
                            // 频道列表打开时，上下键在列表内处理
                        } else {
                            viewModel.changeToNextChannel()
                        }
                    },
                    onLeft = {
                        if (isPanelVisible) {
                            // 频道列表已打开，左键关闭列表
                            viewModel.hidePanel()
                        } else {
                            // 频道列表未打开，左键呼出列表
                            viewModel.showPanel()
                        }
                    },
                    onRight = {
                        when {
                            isPanelVisible -> {
                                // 频道列表已打开，右键关闭列表
                                viewModel.hidePanel()
                            }
                            isStatusBarVisible -> {
                                // 状态栏已打开，右键关闭状态栏
                                isStatusBarVisible = false
                            }
                            else -> {
                                // 什么都没打开，右键呼出状态栏
                                isStatusBarVisible = true
                            }
                        }
                    },
                    onSelect = {
                        if (!isPanelVisible) {
                            // 确认键切换线路
                            if (currentChannel.urlList.size > 1) {
                                viewModel.changeUrlIdx(1)
                            }
                        }
                    },
                    onSettings = {
                        // 可以打开快速设置面板
                    },
                    onNumber = { number ->
                        // 只有启用数字选台时才处理数字键
                        if (!isPanelVisible && channelNoSelectEnable) {
                            viewModel.inputNumber(number)
                        }
                    }
                )
                .handleLiveDragGestures(
                    onSwipeUp = { 
                        if (!isPanelVisible) {
                            // 根据换台方向反转设置决定上下滑动行为
                            if (channelChangeFlip) viewModel.changeToNextChannel()
                            else viewModel.changeToPreviousChannel()
                        }
                    },
                    onSwipeDown = { 
                        if (!isPanelVisible) {
                            // 根据换台方向反转设置决定上下滑动行为
                            if (channelChangeFlip) viewModel.changeToPreviousChannel()
                            else viewModel.changeToNextChannel()
                        }
                    },
                    onSwipeLeft = { 
                        if (isPanelVisible) {
                            viewModel.hidePanel()
                        } else {
                            viewModel.showPanel()
                        }
                    },
                    onSwipeRight = { 
                        if (isPanelVisible) {
                            viewModel.hidePanel()
                        } else {
                            viewModel.showPanel()
                        }
                    }
                )
        )

        // 临时信息面板（换台时显示）- 包含频道号和时间
        // 注意：不获取焦点，不拦截按键事件，让事件传递到下面的按键处理层
        if (isTempPanelVisible && !isPanelVisible && channelInput.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .focusable(false) // 不获取焦点
                    .onPreviewKeyEvent { false } // 不拦截按键事件
            ) {
                LiveTempPanel(
                    modifier = Modifier.align(Alignment.BottomStart),
                    channelNoProvider = { viewModel.getCurrentChannelNo() },
                    currentChannelProvider = { currentChannel },
                    currentChannelUrlIdxProvider = { currentChannelUrlIdx },
                    currentProgrammesProvider = { if (epgEnable) epgList.currentProgrammes(currentChannel) else null },
                    showProgrammeProgressProvider = { epgEnable }
                )
            }
        }

        // 数字选台输入
        if (channelInput.isNotEmpty() && !isPanelVisible) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(20.dp)
            ) {
                LiveChannelInput(channelNoProvider = { channelInput })
            }
        }

        // 频道列表面板
        if (isPanelVisible) {
            LivePanelScreen(
                channelGroupListProvider = { channelGroupList },
                epgListProvider = { if (epgEnable) epgList else ChannelEpgList(emptyList()) },
                currentChannelProvider = { currentChannel },
                currentChannelUrlIdxProvider = { currentChannelUrlIdx },
                showProgrammeProgressProvider = { epgEnable },
                channelFavoriteEnableProvider = { true },
                channelFavoriteListProvider = { favoriteList },
                channelFavoriteListVisibleProvider = { favoriteListVisible },
                onChannelFavoriteListVisibleChange = { viewModel.setFavoriteListVisible(it) },
                onChannelSelected = { viewModel.changeChannel(it) },
                onChannelFavoriteToggle = { viewModel.toggleFavorite(it) },
                onClose = { viewModel.hidePanel() }
            )
        }

        // 状态栏（右键呼出）
        if (isStatusBarVisible) {
            LiveStatusBar(
                channelProvider = { currentChannel },
                channelNoProvider = { viewModel.getCurrentChannelNo() },
                currentChannelUrlIdxProvider = { currentChannelUrlIdx },
                currentProgrammesProvider = { if (epgEnable) epgList.currentProgrammes(currentChannel) else null },
                videoWidthProvider = { videoWidth },
                videoHeightProvider = { videoHeight },
                networkSpeedProvider = { networkSpeed },
                videoAspectRatioProvider = { videoAspectRatio },
                sourceListProvider = { sourceList },
                currentSourceUrlProvider = { currentSourceUrl },
                onChangeVideoAspectRatio = { 
                    viewModel.switchAspectRatio()
                },
                onClearCache = { 
                    // 清除缓存逻辑
                    viewModel.clearCache()
                },
                onSwitchSource = { url ->
                    // 切换源逻辑
                    viewModel.switchSource(url)
                },
                onRefreshAllSources = {
                    // 刷新全部源逻辑
                    viewModel.refreshAllSources()
                },
                onClose = { isStatusBarVisible = false }
            )
        }

        // 加载中提示
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.material3.CircularProgressIndicator()
            }
        }

        // 错误提示
        errorMessage?.let { error ->
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.material3.Text(
                    text = "加载失败: $error",
                    color = androidx.compose.ui.graphics.Color.Red
                )
            }
        }
        
        // Toast 错误提示
        errorToastMessage?.let { message ->
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.material3.Surface(
                    color = androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.8f),
                    shape = androidx.compose.material3.MaterialTheme.shapes.medium
                ) {
                    androidx.compose.material3.Text(
                        text = message,
                        color = androidx.compose.ui.graphics.Color.White,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
                    )
                }
            }
            
            // 2秒后自动清除
            LaunchedEffect(message) {
                delay(2000)
                viewModel.clearErrorToast()
            }
        }
        
        // 错误对话框（连续10次错误后显示）
        if (showErrorDialog) {
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { viewModel.dismissErrorDialog() },
                title = { androidx.compose.material3.Text("播放错误") },
                text = { androidx.compose.material3.Text(errorDialogMessage) },
                confirmButton = {}
            )
        }
    }
}

@Preview(device = "id:Android TV (720p)")
@Composable
private fun LiveScreenPreview() {
    LomenTVTheme {
        LiveScreen()
    }
}

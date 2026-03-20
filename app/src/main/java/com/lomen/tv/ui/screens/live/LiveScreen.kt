package com.lomen.tv.ui.screens.live

import android.net.Uri
import android.view.KeyEvent
import android.view.SurfaceView
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.text.font.FontWeight
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
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
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
    
    // 重试状态（来自新的错误处理器）
    val retryStatus by viewModel.retryStatus.collectAsState()
    
    val configuration = LocalConfiguration.current

    // ExoPlayer - 配置支持4K和解码器回退
    val exoPlayer = remember {
        // 创建渲染器工厂，启用软件解码器作为回退
        val renderersFactory = DefaultRenderersFactory(context).apply {
            // 设置扩展渲染器模式为优先使用
            setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER)
            // 启用解码器回退：当硬件解码器不支持时，自动回退到软件解码器
            setEnableDecoderFallback(true)
            // 允许视频轨道切换时的缓冲时间
            setAllowedVideoJoiningTimeMs(5000)
        }
        
        // 创建 TrackSelector 支持 4K 视频
        val trackSelector = DefaultTrackSelector(context)
        val trackParams = DefaultTrackSelector.Parameters.Builder(context)
            .setMaxVideoSize(3840, 2160)  // 支持4K
            .setMaxVideoBitrate(100_000_000)  // 100Mbps，支持高码率4K
            .setForceHighestSupportedBitrate(false)  // 允许回退到较低质量
            .setTunnelingEnabled(false)  // 禁用隧道模式，确保可以使用软件解码器
            .build()
        trackSelector.parameters = trackParams
        
        // 配置缓冲控制
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                10_000,  // minBufferMs - 最小缓冲10秒
                30_000,  // maxBufferMs - 最大缓冲30秒
                1_000,   // bufferForPlaybackMs - 开始播放需要1秒
                2_000    // bufferForPlaybackAfterRebufferMs - 重新缓冲需要2秒
            )
            .build()
        
        ExoPlayer.Builder(context, renderersFactory)
            .setTrackSelector(trackSelector)
            .setLoadControl(loadControl)
            .build().apply {
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
    
    // 内容类型重试记录
    var contentTypeAttempts by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var currentContentType by remember { mutableIntStateOf(C.CONTENT_TYPE_OTHER) }
    
    // 缓冲超时检测
    var bufferingStartTime by remember { mutableLongStateOf(0L) }
    val bufferingTimeoutMs = 10_000L  // 10秒超时（缩短以便快速测试）
    
    // 当切换频道时，重置重试状态和缓冲超时
    LaunchedEffect(currentChannel, currentChannelUrlIdx) {
        contentTypeAttempts = emptySet()
        bufferingStartTime = 0L  // 重置缓冲超时计时器
        currentContentType = Util.inferContentType(Uri.parse(currentChannel.urlList.getOrNull(currentChannelUrlIdx) ?: ""))
        android.util.Log.d("LiveScreen", "切换频道，重置内容类型为: $currentContentType")
    }

    // 添加播放器监听
    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onVideoSizeChanged(videoSize: VideoSize) {
                videoWidth = videoSize.width
                videoHeight = videoSize.height
                android.util.Log.d("LiveScreen", "视频尺寸: ${videoSize.width}x${videoSize.height}")
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                val stateName = when (playbackState) {
                    Player.STATE_IDLE -> "IDLE"
                    Player.STATE_BUFFERING -> "BUFFERING"
                    Player.STATE_READY -> "READY"
                    Player.STATE_ENDED -> "ENDED"
                    else -> "UNKNOWN"
                }
                android.util.Log.d("LiveScreen", "播放状态变化: $stateName ($playbackState), 当前频道: ${currentChannel.name}")
                
                when (playbackState) {
                    Player.STATE_READY -> {
                        // 播放就绪时更新分辨率，重置缓冲超时
                        bufferingStartTime = 0L
                        exoPlayer.videoFormat?.let { format ->
                            videoWidth = format.width
                            videoHeight = format.height
                            android.util.Log.d("LiveScreen", "视频格式: ${format.sampleMimeType}, 编码: ${format.codecs}, 分辨率: ${format.width}x${format.height}, 码率: ${format.bitrate}")
                        }
                        // 检查音频格式
                        exoPlayer.audioFormat?.let { format ->
                            android.util.Log.d("LiveScreen", "音频格式: ${format.sampleMimeType}, 编码: ${format.codecs}, 声道: ${format.channelCount}, 采样率: ${format.sampleRate}")
                        }
                        // 通知 ViewModel 播放成功，重置错误处理器
                        viewModel.onPlaybackSuccess()
                    }
                    Player.STATE_BUFFERING -> {
                        // 开始缓冲时记录时间
                        if (bufferingStartTime == 0L) {
                            bufferingStartTime = System.currentTimeMillis()
                            android.util.Log.d("LiveScreen", "开始缓冲... 频道: ${currentChannel.name}")
                        }
                    }
                    Player.STATE_IDLE -> {
                        // 空闲状态，重置缓冲时间
                        android.util.Log.d("LiveScreen", "播放器进入IDLE状态")
                        bufferingStartTime = 0L
                    }
                    Player.STATE_ENDED -> {
                        android.util.Log.d("LiveScreen", "播放结束")
                        bufferingStartTime = 0L
                    }
                }
            }
            
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                android.util.Log.d("LiveScreen", "播放状态: isPlaying=$isPlaying, 频道: ${currentChannel.name}")
            }

            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                android.util.Log.e("LiveScreen", "===== 播放器错误 =====")
                android.util.Log.e("LiveScreen", "错误码: ${error.errorCode} (${error.errorCodeName})")
                android.util.Log.e("LiveScreen", "错误信息: ${error.message}")
                android.util.Log.e("LiveScreen", "错误原因: ${error.cause?.message}")
                android.util.Log.e("LiveScreen", "当前频道: ${currentChannel.name}, URL: ${currentChannel.urlList.getOrNull(currentChannelUrlIdx)}")
                android.util.Log.e("LiveScreen", "======================")
                bufferingStartTime = 0L  // 重置缓冲时间
                
                // 构建用户友好的错误信息
                val errorMessage = when (error.errorCode) {
                    androidx.media3.common.PlaybackException.ERROR_CODE_DECODER_INIT_FAILED -> "解码器初始化失败，设备可能不支持此视频格式"
                    androidx.media3.common.PlaybackException.ERROR_CODE_DECODER_QUERY_FAILED -> "解码器查询失败"
                    androidx.media3.common.PlaybackException.ERROR_CODE_DECODING_FAILED -> "视频解码失败，设备可能不支持此视频编码"
                    androidx.media3.common.PlaybackException.ERROR_CODE_IO_UNSPECIFIED -> "网络错误"
                    androidx.media3.common.PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED -> "网络连接失败"
                    androidx.media3.common.PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT -> "网络连接超时"
                    androidx.media3.common.PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED -> "视频文件格式错误"
                    androidx.media3.common.PlaybackException.ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED -> "不支持的视频容器格式"
                    androidx.media3.common.PlaybackException.ERROR_CODE_PARSING_MANIFEST_MALFORMED -> "播放列表格式错误"
                    else -> error.message ?: "播放错误(代码:${error.errorCode})"
                }
                
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
                            viewModel.handlePlayError(errorMessage)
                        }
                    }
                } else {
                    // 其他播放错误，直接通知 ViewModel 处理
                    android.util.Log.e("LiveScreen", "播放错误: $errorMessage，通知 ViewModel 处理")
                    viewModel.handlePlayError(errorMessage)
                }
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
        }
    }

    // 缓冲超时检测 + 解码失败检测（有声音但无画面）
    LaunchedEffect(exoPlayer) {
        var noVideoCount = 0  // 无画面计数器
        
        while (true) {
            delay(1000)
            val currentTime = System.currentTimeMillis()
            
            // 检测缓冲超时
            if (bufferingStartTime > 0) {
                val bufferingDuration = currentTime - bufferingStartTime
                android.util.Log.d("LiveScreen", "缓冲中... 已等待 ${bufferingDuration/1000}秒, 频道: ${currentChannel.name}, 播放状态: ${exoPlayer.playbackState}")
                
                if (bufferingDuration > bufferingTimeoutMs) {
                    android.util.Log.w("LiveScreen", "===== 缓冲超时 =====")
                    android.util.Log.w("LiveScreen", "超时时长: ${bufferingTimeoutMs/1000}秒")
                    android.util.Log.w("LiveScreen", "频道: ${currentChannel.name}")
                    android.util.Log.w("LiveScreen", "URL: ${currentChannel.urlList.getOrNull(currentChannelUrlIdx)}")
                    android.util.Log.w("LiveScreen", "播放器状态: ${exoPlayer.playbackState}")
                    android.util.Log.w("LiveScreen", "====================")
                    bufferingStartTime = 0L  // 重置，避免重复触发
                    viewModel.handlePlayError("视频加载超时(${bufferingTimeoutMs/1000}秒)，设备可能不支持此视频格式")
                }
            }
            
            // 检测解码失败：播放就绪但无视频画面（有声音但黑屏）
            // 检查是否有音频轨道但没有视频轨道，或者视频尺寸为0
            val hasAudio = exoPlayer.audioFormat != null
            val hasVideo = exoPlayer.videoFormat != null && exoPlayer.videoFormat?.width ?: 0 > 0
            
            if (exoPlayer.playbackState == Player.STATE_READY && 
                exoPlayer.isPlaying && 
                hasAudio && !hasVideo) {
                noVideoCount++
                android.util.Log.w("LiveScreen", "警告：有声音但无画面，计数: $noVideoCount, 频道: ${currentChannel.name}")
                
                if (noVideoCount >= 5) {  // 连续5秒有声音但无画面
                    android.util.Log.e("LiveScreen", "===== 视频解码失败（有声音但黑屏）=====")
                    android.util.Log.e("LiveScreen", "频道: ${currentChannel.name}")
                    android.util.Log.e("LiveScreen", "URL: ${currentChannel.urlList.getOrNull(currentChannelUrlIdx)}")
                    android.util.Log.e("LiveScreen", "播放器状态: READY, isPlaying: ${exoPlayer.isPlaying}")
                    android.util.Log.e("LiveScreen", "音频: ${exoPlayer.audioFormat?.codecs}, 视频: ${exoPlayer.videoFormat?.codecs}")
                    android.util.Log.e("LiveScreen", "================================")
                    noVideoCount = 0
                    viewModel.handlePlayError("视频解码失败（有声音但黑屏），设备不支持此视频编码格式")
                }
            } else {
                noVideoCount = 0  // 正常播放，重置计数器
            }
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
                        android.util.Log.d("LiveScreen", "按下上键, isPanelVisible=$isPanelVisible")
                        if (isPanelVisible) {
                            // 频道列表打开时，上下键在列表内处理
                        } else {
                            android.util.Log.d("LiveScreen", "切换到上一个频道")
                            viewModel.changeToPreviousChannel()
                        }
                    },
                    onDown = {
                        android.util.Log.d("LiveScreen", "按下下键, isPanelVisible=$isPanelVisible")
                        if (isPanelVisible) {
                            // 频道列表打开时，上下键在列表内处理
                        } else {
                            android.util.Log.d("LiveScreen", "切换到下一个频道")
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
                        if (!isPanelVisible && !isStatusBarVisible) {
                            // 右键切换线路（只在主界面有效）
                            if (currentChannel.urlList.size > 1) {
                                val nextUrlIdx = if (currentChannelUrlIdx < currentChannel.urlList.size - 1) {
                                    currentChannelUrlIdx + 1
                                } else {
                                    0
                                }
                                viewModel.changeUrlIdx(nextUrlIdx - currentChannelUrlIdx)
                                Toast.makeText(context, "切换到线路${nextUrlIdx + 1}/${currentChannel.urlList.size}", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "当前频道只有一条线路", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    onSelect = {
                        // 确认键功能保留
                    },
                    onSettings = {
                        // 菜单键呼出状态栏
                        if (!isPanelVisible) {
                            isStatusBarVisible = !isStatusBarVisible
                        }
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
                onSwitchRoute = { routeIdx ->
                    // 切换线路逻辑
                    viewModel.changeUrlIdx(routeIdx - currentChannelUrlIdx)
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
        
        // 重试状态提示（新的错误处理器）
        retryStatus?.let { status ->
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    modifier = Modifier
                        .background(
                            color = androidx.compose.ui.graphics.Color(0xCC000000),
                            shape = androidx.compose.material3.MaterialTheme.shapes.medium
                        )
                        .padding(horizontal = 24.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    androidx.compose.material3.CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = androidx.compose.ui.graphics.Color(0xFFfddd0e),
                        strokeWidth = 2.5.dp
                    )
                    androidx.compose.foundation.layout.Column(
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        androidx.compose.material3.Text(
                            text = "视频源连接失败",
                            style = androidx.compose.material3.MaterialTheme.typography.titleSmall,
                            color = androidx.compose.ui.graphics.Color.White,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                        )
                        androidx.compose.material3.Text(
                            text = status,
                            style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                            color = androidx.compose.ui.graphics.Color(0xFFfddd0e)
                        )
                    }
                }
            }
        }
        
        // Toast 错误提示（旧版，保留兼容）
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

package com.lomen.tv.domain.service

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionParameters
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlayerService @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var exoPlayer: ExoPlayer? = null
    private var trackSelector: DefaultTrackSelector? = null

    private val _playerState = MutableStateFlow(PlayerState())
    val playerState: StateFlow<PlayerState> = _playerState.asStateFlow()

    private var videoQualityOptions: List<VideoQualityOption> = emptyList()
    private var selectedQualityIndex: Int = 0
    
    // 字幕和音轨信息
    private val _availableSubtitles = MutableStateFlow<List<TrackInfo>>(emptyList())
    val availableSubtitles: StateFlow<List<TrackInfo>> = _availableSubtitles.asStateFlow()
    
    private val _availableAudioTracks = MutableStateFlow<List<TrackInfo>>(emptyList())
    val availableAudioTracks: StateFlow<List<TrackInfo>> = _availableAudioTracks.asStateFlow()
    
    private val _selectedSubtitleIndex = MutableStateFlow(-1) // -1表示无字幕
    val selectedSubtitleIndex: StateFlow<Int> = _selectedSubtitleIndex.asStateFlow()
    
    private val _selectedAudioTrackIndex = MutableStateFlow(0)
    val selectedAudioTrackIndex: StateFlow<Int> = _selectedAudioTrackIndex.asStateFlow()

    @OptIn(UnstableApi::class)
    fun initializePlayer(): ExoPlayer {
        android.util.Log.d("PlayerService", "initializePlayer called, current exoPlayer=$exoPlayer")
        // 检查播放器是否存在且有效（未被释放）
        if (exoPlayer == null || exoPlayer!!.playbackState == Player.STATE_IDLE && exoPlayer!!.playerError != null) {
            // 如果播放器已被释放或无效，先清理
            if (exoPlayer != null) {
                android.util.Log.d("PlayerService", "Player is invalid, releasing and recreating...")
                try {
                    exoPlayer?.removeListener(playerListener)
                    exoPlayer?.release()
                } catch (e: Exception) {
                    android.util.Log.w("PlayerService", "Error releasing old player: ${e.message}")
                }
                exoPlayer = null
                trackSelector = null
            }
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                .build()

            // 创建TrackSelector以支持自定义轨道选择
            trackSelector = DefaultTrackSelector(context)
            val trackParams = DefaultTrackSelector.Parameters.Builder(context)
                // 支持4K视频
                .setMaxVideoSize(3840, 2160)
                .setMaxVideoBitrate(50_000_000) // 50Mbps
                // 优先选择高质量，但不强制（允许回退到软件解码器）
                .setPreferredVideoMimeType(MimeTypes.VIDEO_H265) // 优先H.265
                .setForceHighestSupportedBitrate(false)
                // 允许选择任何支持的视频格式（包括软件解码器支持的格式）
                .setTunnelingEnabled(false) // 禁用隧道模式，确保可以使用软件解码器
                .build()
            trackSelector!!.parameters = trackParams
            
            // 配置LoadControl以优化缓冲
            val loadControl = DefaultLoadControl.Builder()
                .setBufferDurationsMs(
                    30_000,  // minBufferMs - 最小缓冲30秒
                    60_000,  // maxBufferMs - 最大缓冲60秒
                    2_500,   // bufferForPlaybackMs - 开始播放需要2.5秒
                    5_000    // bufferForPlaybackAfterRebufferMs - 重新缓冲需要5秒
                )
                .build()

            // 配置HttpDataSource以支持自定义请求头
            val httpDataSourceFactory = DefaultHttpDataSource.Factory()
                .setConnectTimeoutMs(30_000)
                .setReadTimeoutMs(30_000)
                .setAllowCrossProtocolRedirects(true)
            
            val mediaSourceFactory = DefaultMediaSourceFactory(context)
                .setDataSourceFactory(httpDataSourceFactory)

            // 创建渲染器工厂，启用软件解码器作为回退
            // 注意：由于 Media3 的 API 限制，我们无法直接重写 buildVideoRenderers
            // 但可以通过 setEnableDecoderFallback(true) 来启用解码器回退
            val renderersFactory = DefaultRenderersFactory(context).apply {
                // 设置扩展渲染器模式为优先使用
                setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER)
                // 启用解码器回退：当硬件解码器不支持时，自动回退到软件解码器
                // 这是关键配置，确保硬件解码失败时能使用软件解码器
                setEnableDecoderFallback(true)
                // 允许视频轨道切换时的缓冲时间
                setAllowedVideoJoiningTimeMs(5000)
            }
            android.util.Log.d("PlayerService", "RenderersFactory configured with decoder fallback enabled")
            
            // 注意：由于硬件和软件解码器都不支持某些 HEVC 格式（如 hvc1.2.4.L153.90），
            // 即使启用了回退机制，也可能无法播放。
            // 如果问题仍然存在，建议考虑转码视频为设备支持的格式（如 H.264 或较低 profile 的 HEVC）

            exoPlayer = ExoPlayer.Builder(context, renderersFactory)
                .setMediaSourceFactory(mediaSourceFactory)
                .setTrackSelector(trackSelector!!)
                .setLoadControl(loadControl)
                .setAudioAttributes(audioAttributes, true)
                .setHandleAudioBecomingNoisy(true)
                .build()
                .apply {
                    addListener(playerListener)
                }
        }
        return exoPlayer!!
    }

    fun releasePlayer() {
        exoPlayer?.apply {
            removeListener(playerListener)
            release()
        }
        exoPlayer = null
        trackSelector = null
    }

    fun getPlayer(): ExoPlayer? {
        android.util.Log.d("PlayerService", "getPlayer called, exoPlayer=$exoPlayer")
        return exoPlayer
    }

    /**
     * 准备媒体播放，支持自定义HTTP头和字幕
     */
    @OptIn(UnstableApi::class)
    fun prepareMedia(
        url: String,
        title: String? = null,
        episodeTitle: String? = null,
        headers: Map<String, String> = emptyMap(),
        subtitles: List<SubtitleInfo> = emptyList(),
        startPositionMs: Long = 0L
    ) {
        android.util.Log.d("PlayerService", "prepareMedia: url=$url, headers=${headers.keys}, startPositionMs=$startPositionMs")
        
        // 确保播放器已初始化且有效
        if (exoPlayer == null) {
            android.util.Log.d("PlayerService", "Player is null, initializing...")
            initializePlayer()
        } else {
            // 检查播放器是否有效（尝试访问其属性，如果抛出异常则说明已被释放）
            try {
                val state = exoPlayer!!.playbackState
                android.util.Log.d("PlayerService", "Player is valid, playbackState=$state")
            } catch (e: Exception) {
                android.util.Log.w("PlayerService", "Player is invalid, reinitializing: ${e.message}")
                exoPlayer = null
                trackSelector = null
                initializePlayer()
            }
        }
        
        // 配置HTTP请求头（如果有）
        val httpDataSourceFactory = if (headers.isNotEmpty()) {
            android.util.Log.d("PlayerService", "Configuring HTTP headers: ${headers.keys}, values: ${headers.mapValues { if (it.key.lowercase().contains("cookie") || it.key.lowercase().contains("authorization")) "***" else it.value }}")
            DefaultHttpDataSource.Factory()
                .setConnectTimeoutMs(30_000)
                .setReadTimeoutMs(30_000)
                .setAllowCrossProtocolRedirects(true)
                .setDefaultRequestProperties(headers)
        } else {
            DefaultHttpDataSource.Factory()
                .setConnectTimeoutMs(30_000)
                .setReadTimeoutMs(30_000)
                .setAllowCrossProtocolRedirects(true)
        }
        
        // 构建MediaItem
        val mediaItemBuilder = MediaItem.Builder()
            .setUri(url)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(title)
                    .setSubtitle(episodeTitle)
                    .build()
            )
        
        // 如果指定了起始位置，使用 ClippingConfiguration 设置起始位置
        if (startPositionMs > 0) {
            mediaItemBuilder.setClippingConfiguration(
                MediaItem.ClippingConfiguration.Builder()
                    .setStartPositionMs(startPositionMs)
                    .build()
            )
        }
        
        // 添加字幕
        if (subtitles.isNotEmpty()) {
            subtitles.forEach { subtitle ->
                mediaItemBuilder.setSubtitleConfigurations(
                    listOf(
                        MediaItem.SubtitleConfiguration.Builder(android.net.Uri.parse(subtitle.url))
                            .setMimeType(subtitle.mimeType)
                            .setLanguage(subtitle.language)
                            .setLabel(subtitle.label)
                            .build()
                    )
                )
            }
        }
        
        val mediaItem = mediaItemBuilder.build()

        exoPlayer?.apply {
            // 如果有 headers，需要使用 setMediaSource 来使用自定义的 DataSourceFactory
            if (headers.isNotEmpty()) {
                val mediaSourceFactory = DefaultMediaSourceFactory(context)
                    .setDataSourceFactory(httpDataSourceFactory)
                val mediaSource = mediaSourceFactory.createMediaSource(mediaItem)
                
                setMediaSource(mediaSource)
                prepare()
                // 如果指定了起始位置，先不自动播放，等待在 PlayerViewModel 中验证位置后再播放
                if (startPositionMs > 0) {
                    android.util.Log.d("PlayerService", "Start position set via ClippingConfiguration: $startPositionMs ms, playWhenReady=false")
                    playWhenReady = false // 先不播放，等待位置验证完成
                } else {
                    playWhenReady = true
                }
            } else {
                // 没有 headers，直接使用 setMediaItem
                // 如果指定了起始位置，已通过 ClippingConfiguration 设置
                setMediaItem(mediaItem)
                prepare()
                // 如果指定了起始位置，先不自动播放，等待在 PlayerViewModel 中验证位置后再播放
                if (startPositionMs > 0) {
                    android.util.Log.d("PlayerService", "Start position set via ClippingConfiguration: $startPositionMs ms, playWhenReady=false")
                    playWhenReady = false // 先不播放，等待位置验证完成
                } else {
                    playWhenReady = true
                }
            }
        }
    }

    fun preparePlaylist(
        urls: List<String>,
        titles: List<String>? = null,
        startIndex: Int = 0
    ) {
        val mediaItems = urls.mapIndexed { index, url ->
            MediaItem.Builder()
                .setUri(url)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(titles?.getOrNull(index))
                        .build()
                )
                .build()
        }

        exoPlayer?.apply {
            setMediaItems(mediaItems, startIndex, 0)
            prepare()
            playWhenReady = true
        }
    }

    fun play() {
        exoPlayer?.play()
    }

    fun pause() {
        exoPlayer?.pause()
    }

    fun seekTo(positionMs: Long) {
        exoPlayer?.seekTo(positionMs)
    }

    fun seekToNext() {
        exoPlayer?.seekToNext()
    }

    fun seekToPrevious() {
        exoPlayer?.seekToPrevious()
    }

    fun seekForward(deltaMs: Long = 10000) {
        exoPlayer?.let { player ->
            val newPosition = (player.currentPosition + deltaMs)
                .coerceAtMost(player.duration)
            player.seekTo(newPosition)
        }
    }

    fun seekBackward(deltaMs: Long = 10000) {
        exoPlayer?.let { player ->
            val newPosition = (player.currentPosition - deltaMs)
                .coerceAtLeast(0)
            player.seekTo(newPosition)
        }
    }

    fun setPlaybackSpeed(speed: Float) {
        exoPlayer?.setPlaybackSpeed(speed)
    }

    fun getAvailableSpeeds(): List<Float> = listOf(0.25f, 0.5f, 0.75f, 1f, 1.25f, 1.5f, 1.75f, 2f)

    fun togglePlayPause() {
        exoPlayer?.let { player ->
            if (player.isPlaying) {
                player.pause()
            } else {
                player.play()
            }
        }
    }

    fun isPlaying(): Boolean = exoPlayer?.isPlaying == true

    fun getCurrentPosition(): Long = exoPlayer?.currentPosition ?: 0

    fun getDuration(): Long = exoPlayer?.duration ?: 0

    fun getBufferedPosition(): Long = exoPlayer?.bufferedPosition ?: 0

    fun getCurrentMediaIndex(): Int = exoPlayer?.currentMediaItemIndex ?: 0

    fun getMediaCount(): Int = exoPlayer?.mediaItemCount ?: 0

    private val playerListener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            val state = when (playbackState) {
                Player.STATE_IDLE -> PlayerState.Type.IDLE
                Player.STATE_BUFFERING -> PlayerState.Type.BUFFERING
                Player.STATE_READY -> PlayerState.Type.READY
                Player.STATE_ENDED -> PlayerState.Type.ENDED
                else -> PlayerState.Type.IDLE
            }
            _playerState.value = _playerState.value.copy(
                type = state,
                isPlaying = exoPlayer?.isPlaying == true
            )
            
            // 当播放器准备好时，更新可用轨道
            if (playbackState == Player.STATE_READY) {
                updateAvailableTracks()
            }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _playerState.value = _playerState.value.copy(isPlaying = isPlaying)
        }

        override fun onPlayerError(error: PlaybackException) {
            android.util.Log.e("PlayerService", "Player error: ${error.message}", error)
            android.util.Log.e("PlayerService", "Error type: ${error.errorCode}, cause: ${error.cause?.message}")
            android.util.Log.e("PlayerService", "Error code details: ${error.errorCodeName}")
            
            // 如果是解码器错误，尝试记录详细信息
            if (error.errorCode == PlaybackException.ERROR_CODE_DECODER_INIT_FAILED ||
                error.errorCode == PlaybackException.ERROR_CODE_DECODER_QUERY_FAILED ||
                error.errorCode == PlaybackException.ERROR_CODE_DECODING_FAILED) {
                android.util.Log.e("PlayerService", "Decoder error detected. This may indicate hardware decoder incompatibility.")
                android.util.Log.e("PlayerService", "Error should trigger fallback to software decoder if enabled.")
                
                // 检查错误原因
                val cause = error.cause
                if (cause != null) {
                    android.util.Log.e("PlayerService", "Error cause: ${cause.javaClass.simpleName}, message: ${cause.message}")
                    cause.printStackTrace()
                }
            }
            
            // 记录播放器当前状态
            exoPlayer?.let { player ->
                android.util.Log.e("PlayerService", "Player state at error: playbackState=${player.playbackState}, isPlaying=${player.isPlaying}")
                android.util.Log.e("PlayerService", "Current media item: ${player.currentMediaItem?.localConfiguration?.uri}")
            }
            
            // 构建更详细的错误信息
            val errorMessage = when (error.errorCode) {
                PlaybackException.ERROR_CODE_DECODER_INIT_FAILED -> "解码器初始化失败"
                PlaybackException.ERROR_CODE_DECODER_QUERY_FAILED -> "解码器查询失败"
                PlaybackException.ERROR_CODE_DECODING_FAILED -> "视频解码失败"
                PlaybackException.ERROR_CODE_IO_UNSPECIFIED -> "网络错误"
                PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED -> "网络连接失败"
                PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT -> "网络连接超时"
                PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED -> "视频文件格式错误"
                PlaybackException.ERROR_CODE_PARSING_MANIFEST_MALFORMED -> "播放列表格式错误"
                else -> {
                    // 对于未知错误，记录详细信息用于调试
                    android.util.Log.e("PlayerService", "Unknown error code: ${error.errorCode}, message: ${error.message}")
                    error.message ?: "播放器运行时错误"
                }
            }
            
            _playerState.value = _playerState.value.copy(
                type = PlayerState.Type.ERROR,
                error = errorMessage
            )
            
            // 对于某些可恢复的错误，尝试自动重试
            if (error.errorCode == PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT ||
                error.errorCode == PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED) {
                android.util.Log.w("PlayerService", "Network error detected, player may auto-retry")
            }
        }

        override fun onPositionDiscontinuity(
            oldPosition: Player.PositionInfo,
            newPosition: Player.PositionInfo,
            reason: Int
        ) {
            updatePositionInfo()
        }
        
        @OptIn(UnstableApi::class)
        override fun onTracksChanged(tracks: Tracks) {
            // 轨道变化时更新可用轨道
            android.util.Log.d("PlayerService", "Tracks changed: ${tracks.groups.size} track groups")
            
            // 检查视频轨道和渲染器信息
            var hasVideoTrack = false
            var hasSelectedVideoTrack = false
            for (trackGroup in tracks.groups) {
                if (trackGroup.type == C.TRACK_TYPE_VIDEO) {
                    hasVideoTrack = true
                    val isSelected = trackGroup.isSelected
                    hasSelectedVideoTrack = isSelected
                    val format = if (trackGroup.length > 0) trackGroup.getTrackFormat(0) else null
                    
                    android.util.Log.d("PlayerService", "Video track found: selected=$isSelected, format=${format?.codecs}, resolution=${format?.width}x${format?.height}")
                    
                    // 检查解码器信息
                    if (format != null) {
                        android.util.Log.d("PlayerService", "Video format details: mimeType=${format.sampleMimeType}, codec=${format.codecs}, colorSpace=${format.colorInfo}")
                    }
                }
            }
            
            if (!hasVideoTrack) {
                android.util.Log.w("PlayerService", "No video track found in media!")
            } else if (!hasSelectedVideoTrack) {
                android.util.Log.w("PlayerService", "Video track found but not selected! This may cause no video display.")
            } else {
                android.util.Log.d("PlayerService", "Video track is selected and should be rendering")
            }
            
            updateAvailableTracks()
        }
    }

    private fun updatePositionInfo() {
        exoPlayer?.let { player ->
            _playerState.value = _playerState.value.copy(
                currentPosition = player.currentPosition,
                duration = player.duration.coerceAtLeast(0),
                bufferedPosition = player.bufferedPosition
            )
        }
    }

    fun updatePosition() {
        updatePositionInfo()
    }

    fun setVolume(volume: Float) {
        exoPlayer?.volume = volume.coerceIn(0f, 1f)
    }

    fun toggleMute() {
        exoPlayer?.let { player ->
            player.volume = if (player.volume > 0) 0f else 1f
        }
    }
    
    /**
     * 清除错误状态
     */
    fun clearError() {
        _playerState.value = _playerState.value.copy(
            error = null,
            type = if (exoPlayer != null && exoPlayer!!.playbackState != Player.STATE_IDLE) {
                PlayerState.Type.READY
            } else {
                PlayerState.Type.IDLE
            }
        )
    }

    fun isMuted(): Boolean = exoPlayer?.volume == 0f
    
    /**
     * 选择字幕轨道
     * @param index 字幕索引，-1表示禁用字幕
     */
    @OptIn(UnstableApi::class)
    fun selectSubtitle(index: Int) {
        trackSelector?.let { selector ->
            val parameters = selector.parameters
                .buildUpon()
                .setPreferredTextLanguage(
                    if (index >= 0 && index < _availableSubtitles.value.size) {
                        _availableSubtitles.value[index].language
                    } else {
                        null
                    }
                )
                .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, index < 0)
                .build()
            
            selector.parameters = parameters
            _selectedSubtitleIndex.value = index
        }
    }
    
    /**
     * 选择音轨
     */
    @OptIn(UnstableApi::class)
    fun selectAudioTrack(index: Int) {
        trackSelector?.let { selector ->
            if (index >= 0 && index < _availableAudioTracks.value.size) {
                val parameters = selector.parameters
                    .buildUpon()
                    .setPreferredAudioLanguage(_availableAudioTracks.value[index].language)
                    .build()
                
                selector.parameters = parameters
                _selectedAudioTrackIndex.value = index
            }
        }
    }
    
    /**
     * 获取当前可用的字幕列表
     */
    @OptIn(UnstableApi::class)
    private fun updateAvailableTracks() {
        exoPlayer?.let { player ->
            val tracks = player.currentTracks
            
            // 更新字幕列表
            val subtitles = mutableListOf<TrackInfo>()
            for (trackGroup in tracks.groups) {
                if (trackGroup.type == C.TRACK_TYPE_TEXT) {
                    for (i in 0 until trackGroup.length) {
                        val format = trackGroup.getTrackFormat(i)
                        subtitles.add(
                            TrackInfo(
                                index = subtitles.size,
                                language = format.language ?: "unknown",
                                label = format.label ?: "字幕 ${subtitles.size + 1}"
                            )
                        )
                    }
                }
            }
            _availableSubtitles.value = subtitles
            
            // 更新音轨列表
            val audioTracks = mutableListOf<TrackInfo>()
            for (trackGroup in tracks.groups) {
                if (trackGroup.type == C.TRACK_TYPE_AUDIO) {
                    for (i in 0 until trackGroup.length) {
                        val format = trackGroup.getTrackFormat(i)
                        audioTracks.add(
                            TrackInfo(
                                index = audioTracks.size,
                                language = format.language ?: "unknown",
                                label = format.label ?: "音轨 ${audioTracks.size + 1}"
                            )
                        )
                    }
                }
            }
            _availableAudioTracks.value = audioTracks
        }
    }
}

data class PlayerState(
    val type: PlayerState.Type = PlayerState.Type.IDLE,
    val isPlaying: Boolean = false,
    val currentPosition: Long = 0,
    val duration: Long = 0,
    val bufferedPosition: Long = 0,
    val error: String? = null
) {
    enum class Type {
        IDLE,
        BUFFERING,
        READY,
        PLAYING,
        ENDED,
        ERROR
    }
}

data class VideoQualityOption(
    val height: Int,
    val label: String,
    val bitrate: Long
)

data class TrackInfo(
    val index: Int,
    val language: String,
    val label: String
)

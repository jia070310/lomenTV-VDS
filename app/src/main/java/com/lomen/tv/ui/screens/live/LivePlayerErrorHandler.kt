package com.lomen.tv.ui.screens.live

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.lomen.tv.data.model.live.LiveChannel

/**
 * 直播播放器错误处理器
 * 
 * 实现四阶段智能重试策略：
 * 1. 第一阶段：重试当前URL（最多3次，间隔3秒）
 * 2. 第二阶段：切换到同频道的下一个URL
 * 3. 第三阶段：切换到下一个频道
 * 4. 第四阶段：连续失败超过阈值，触发直播源切换
 */
class LivePlayerErrorHandler(
    private val coroutineScope: CoroutineScope,
    private val onRetry: (channel: LiveChannel, urlIdx: Int) -> Unit,
    private val onSwitchUrl: (channel: LiveChannel, nextUrlIdx: Int) -> Unit,
    private val onSwitchChannel: (nextChannel: LiveChannel) -> Unit,
    private val onSourceSwitchRequired: (() -> Unit)? = null,
    private val onRetryStatusChanged: ((String?) -> Unit)? = null,
) {
    private val TAG = "LivePlayerErrorHandler"

    companion object {
        /** 单个URL最大重试次数 */
        private const val MAX_RETRY_COUNT = 3
        /** 重试间隔（毫秒） */
        private const val RETRY_INTERVAL_MS = 3000L
        /** 连续切频道失败超过此数则切源 */
        private const val MAX_CHANNEL_FAILURES_BEFORE_SOURCE_SWITCH = 8
    }

    /** 当前重试次数 */
    private var retryCount = 0
    
    /** 当前频道URL切换次数 */
    private var currentChannelUrlAttempts = 0
    
    /** 连续切频道失败次数 */
    private var consecutiveChannelFailures = 0
    
    /** 记录当前源URL已尝试失败的频道 */
    private val failedChannels = mutableSetOf<String>()
    
    /** 重试协程Job */
    private var retryJob: Job? = null
    
    /** 重试状态描述（供UI显示） */
    var retryStatus: String? = null
        private set(value) {
            field = value
            onRetryStatusChanged?.invoke(value)
        }

    /**
     * 处理播放错误
     * 
     * @param currentChannel 当前频道
     * @param currentUrlIdx 当前URL索引
     */
    fun handleError(currentChannel: LiveChannel, currentUrlIdx: Int) {
        // 取消上一个重试任务，避免重叠
        retryJob?.cancel()
        
        if (retryCount < MAX_RETRY_COUNT) {
            // 第一阶段：重试当前URL
            retryCount++
            val countdown = (RETRY_INTERVAL_MS / 1000).toInt()
            Log.w(TAG, "播放失败，${countdown}秒后重试（$retryCount/$MAX_RETRY_COUNT）：${currentChannel.name}")
            
            val retryChannel = currentChannel
            val retryUrlIdx = currentUrlIdx
            
            retryJob = coroutineScope.launch {
                // 倒计时更新提示
                for (remaining in countdown downTo 1) {
                    retryStatus = "正在重试 $retryCount/$MAX_RETRY_COUNT，${remaining}秒后重连..."
                    delay(1000L)
                }
                // 确保仍在播放同一频道同一URL时才重试
                retryStatus = "正在重试 $retryCount/$MAX_RETRY_COUNT..."
                onRetry(retryChannel, retryUrlIdx)
            }
        } else {
            // 重试次数耗尽，重置重试计数
            retryCount = 0
            retryStatus = null
            currentChannelUrlAttempts++
            Log.w(TAG, "重试${MAX_RETRY_COUNT}次均失败（URL ${currentUrlIdx + 1}/${currentChannel.urlList.size}）：${currentChannel.name}")
            
            if (currentUrlIdx < currentChannel.urlList.size - 1) {
                // 第二阶段：还有其他URL，切换到下一个URL
                Log.i(TAG, "切换到下一个URL：${currentChannel.name}")
                onSwitchUrl(currentChannel, currentUrlIdx + 1)
            } else {
                // 第三阶段：当前频道所有URL都失败，切换下一个频道
                currentChannelUrlAttempts = 0
                consecutiveChannelFailures++
                Log.w(TAG, "频道 ${currentChannel.name} 所有URL均失败（连续失败频道数：$consecutiveChannelFailures/$MAX_CHANNEL_FAILURES_BEFORE_SOURCE_SWITCH）")
                failedChannels.add(currentChannel.name)
                
                if (consecutiveChannelFailures >= MAX_CHANNEL_FAILURES_BEFORE_SOURCE_SWITCH) {
                    // 第四阶段：连续失败频道数超限，触发直播源切换
                    Log.w(TAG, "连续 $MAX_CHANNEL_FAILURES_BEFORE_SOURCE_SWITCH 个频道均无法播放，触发自动切换直播源")
                    onSourceSwitchRequired?.invoke()
                } else {
                    // 切换到下一个频道 - 通过回调让ViewModel处理
                    Log.i(TAG, "自动切换到下一个频道")
                    onSwitchChannel(currentChannel)
                }
            }
        }
    }
    
    /**
     * 播放成功回调
     * 重置所有计数器
     */
    fun onPlaybackSuccess() {
        retryCount = 0
        retryJob?.cancel()
        retryJob = null
        currentChannelUrlAttempts = 0
        consecutiveChannelFailures = 0
        retryStatus = null
    }
    
    /**
     * 切换频道时调用
     * 重置重试状态
     */
    fun onChannelChanged() {
        retryJob?.cancel()
        retryJob = null
        retryCount = 0
        retryStatus = null
    }
    
    /**
     * 重置失败记录（切换源后调用）
     */
    fun resetFailureTracking() {
        failedChannels.clear()
        currentChannelUrlAttempts = 0
        retryCount = 0
        consecutiveChannelFailures = 0
        retryJob?.cancel()
        retryJob = null
        retryStatus = null
        Log.i(TAG, "已重置失败跟踪记录")
    }
    
    /**
     * 检查是否需要切换源
     * @return 是否需要切换源
     */
    fun checkAndSwitchSource(totalChannels: Int): Boolean {
        val failedCount = failedChannels.size
        val failureRate = if (totalChannels > 0) failedCount.toFloat() / totalChannels else 0f
        
        Log.i(TAG, "当前源失败统计：${failedCount}/${totalChannels}（${String.format("%.1f%%", failureRate * 100)}）")
        
        // 如果失败频道数>=5个，或失败率>=30%，则认为当前源不可用
        return failedCount >= 5 || failureRate >= 0.3f
    }
    
    /**
     * 获取当前失败频道数
     */
    fun getFailedChannelCount(): Int = failedChannels.size
    
    /**
     * 释放资源
     */
    fun release() {
        retryJob?.cancel()
        retryJob = null
    }
}

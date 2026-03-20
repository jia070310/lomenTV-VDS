package com.lomen.tv.data.repository

import android.util.Log
import com.lomen.tv.data.model.live.ChannelEpgList
import com.lomen.tv.data.model.live.LiveChannel
import com.lomen.tv.data.model.live.LiveChannelGroupList
import com.lomen.tv.data.model.live.LiveChannelGroupList.Companion.allChannels
import com.lomen.tv.data.remote.parser.EpgParser
import com.lomen.tv.data.remote.parser.LiveSourceParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 直播频道仓库
 */
@Singleton
class LiveChannelRepository @Inject constructor() {

    private val sourceParser = LiveSourceParser()
    private val epgParser = EpgParser()
    
    companion object {
        private const val TAG = "LiveChannelRepository"
    }

    private val _channelGroupList = MutableStateFlow(LiveChannelGroupList())
    val channelGroupList: StateFlow<LiveChannelGroupList> = _channelGroupList.asStateFlow()

    private val _epgList = MutableStateFlow(ChannelEpgList())
    val epgList: StateFlow<ChannelEpgList> = _epgList.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    /**
     * 加载直播源
     */
    suspend fun loadSource(url: String): Result<Unit> = withContext(Dispatchers.IO) {
        _isLoading.value = true
        _errorMessage.value = null

        try {
            Log.d("LiveChannelRepository", "开始加载直播源: $url")
            val result = sourceParser.parseFromUrl(url)
            result.fold(
                onSuccess = { groupList ->
                    Log.d("LiveChannelRepository", "直播源加载成功，分组数: ${groupList.size}")
                    _channelGroupList.value = groupList
                    _isLoading.value = false
                    Result.success(Unit)
                },
                onFailure = { error ->
                    Log.e("LiveChannelRepository", "直播源加载失败: ${error.message}", error)
                    _errorMessage.value = "解析源错误，请切换直播源"
                    _isLoading.value = false
                    Result.failure(error)
                }
            )
        } catch (e: Exception) {
            Log.e("LiveChannelRepository", "直播源加载异常: ${e.message}", e)
            _errorMessage.value = e.message
            _isLoading.value = false
            Result.failure(e)
        }
    }

    /**
     * 从文本内容加载直播源
     */
    fun loadSourceFromContent(content: String) {
        val groupList = sourceParser.parseM3uContent(content)
        _channelGroupList.value = groupList
    }

    /**
     * 加载 EPG 节目单（从单个 URL）
     */
    suspend fun loadEpg(url: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val result = epgParser.parseFromUrl(url)
            result.fold(
                onSuccess = { epgList ->
                    _epgList.value = epgList
                    Result.success(Unit)
                },
                onFailure = { error ->
                    Result.failure(error)
                }
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 加载 EPG 节目单（从多个 URL 逐个尝试）
     */
    suspend fun loadEpgFromUrls(urls: List<String>): Result<Unit> = withContext(Dispatchers.IO) {
        if (urls.isEmpty()) {
            return@withContext Result.failure(Exception("EPG URL 列表为空"))
        }
        
        var lastError: Exception? = null
        
        for (url in urls) {
            try {
                Log.d(TAG, "尝试加载 EPG: $url")
                val result = epgParser.parseFromUrl(url)
                result.fold(
                    onSuccess = { epgList ->
                        Log.d(TAG, "EPG 加载成功: $url, 频道数: ${epgList.size}")
                        _epgList.value = epgList
                        return@withContext Result.success(Unit)
                    },
                    onFailure = { error ->
                        Log.w(TAG, "EPG 加载失败: $url, 错误: ${error.message}")
                        lastError = Exception(error)
                    }
                )
            } catch (e: Exception) {
                Log.w(TAG, "EPG 加载异常: $url, 错误: ${e.message}")
                lastError = e
            }
        }
        
        Result.failure(lastError ?: Exception("所有 EPG URL 都加载失败"))
    }

    /**
     * 获取指定频道的下一个频道
     */
    fun getNextChannel(currentChannel: LiveChannel): LiveChannel? {
        val channels = _channelGroupList.value.allChannels()
        // 使用 uniqueId 查找，如果 uniqueId 为空则使用对象引用比较
        val currentIndex = if (currentChannel.uniqueId.isNotEmpty()) {
            channels.indexOfFirst { it.uniqueId == currentChannel.uniqueId }
        } else {
            channels.indexOfFirst { it === currentChannel }
        }
        Log.d(TAG, "getNextChannel: 当前频道=${currentChannel.name}, uniqueId=${currentChannel.uniqueId}, 索引=$currentIndex, 总数=${channels.size}")
        return if (currentIndex >= 0 && currentIndex < channels.size - 1) {
            channels[currentIndex + 1]
        } else if (channels.isNotEmpty()) {
            channels.first() // 循环到第一个
        } else {
            null
        }
    }

    /**
     * 获取指定频道的上一个频道
     */
    fun getPreviousChannel(currentChannel: LiveChannel): LiveChannel? {
        val channels = _channelGroupList.value.allChannels()
        // 使用 uniqueId 查找，如果 uniqueId 为空则使用对象引用比较
        val currentIndex = if (currentChannel.uniqueId.isNotEmpty()) {
            channels.indexOfFirst { it.uniqueId == currentChannel.uniqueId }
        } else {
            channels.indexOfFirst { it === currentChannel }
        }
        Log.d(TAG, "getPreviousChannel: 当前频道=${currentChannel.name}, uniqueId=${currentChannel.uniqueId}, 索引=$currentIndex, 总数=${channels.size}")
        return if (currentIndex > 0) {
            channels[currentIndex - 1]
        } else if (channels.isNotEmpty()) {
            channels.last() // 循环到最后一个
        } else {
            null
        }
    }

    /**
     * 通过频道号获取频道
     */
    fun getChannelByNumber(number: Int): LiveChannel? {
        val channels = _channelGroupList.value.allChannels()
        return if (number in 1..channels.size) {
            channels[number - 1]
        } else {
            null
        }
    }

    /**
     * 清空数据
     */
    fun clear() {
        _channelGroupList.value = LiveChannelGroupList()
        _epgList.value = ChannelEpgList()
        _errorMessage.value = null
    }

    /**
     * 清除缓存
     */
    fun clearCache() {
        // 清除 EPG 缓存
        _epgList.value = ChannelEpgList()
        _errorMessage.value = null
    }
}

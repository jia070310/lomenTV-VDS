package com.lomen.tv.ui.screens.live

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lomen.tv.data.model.live.ChannelEpgList
import com.lomen.tv.data.model.live.LiveChannel
import com.lomen.tv.data.model.live.LiveChannelGroupList
import com.lomen.tv.data.model.live.LiveChannelGroupList.Companion.allChannels
import com.lomen.tv.data.preferences.LiveSettingsPreferences
import com.lomen.tv.data.repository.LiveChannelRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LiveViewModel @Inject constructor(
    private val repository: LiveChannelRepository,
    private val liveSettingsPreferences: LiveSettingsPreferences
) : ViewModel() {

    // 频道分组列表
    val channelGroupList: StateFlow<LiveChannelGroupList> = repository.channelGroupList

    // EPG 列表
    val epgList: StateFlow<ChannelEpgList> = repository.epgList

    // 加载状态
    val isLoading: StateFlow<Boolean> = repository.isLoading

    // 错误信息
    val errorMessage: StateFlow<String?> = repository.errorMessage

    // 当前频道
    private val _currentChannel = MutableStateFlow(LiveChannel())
    val currentChannel: StateFlow<LiveChannel> = _currentChannel.asStateFlow()

    // 当前线路索引
    private val _currentChannelUrlIdx = MutableStateFlow(0)
    val currentChannelUrlIdx: StateFlow<Int> = _currentChannelUrlIdx.asStateFlow()

    // 是否显示面板
    private val _isPanelVisible = MutableStateFlow(false)
    val isPanelVisible: StateFlow<Boolean> = _isPanelVisible.asStateFlow()

    // 是否显示临时信息面板
    private val _isTempPanelVisible = MutableStateFlow(false)
    val isTempPanelVisible: StateFlow<Boolean> = _isTempPanelVisible.asStateFlow()

    // 数字选台输入
    private val _channelInput = MutableStateFlow("")
    val channelInput: StateFlow<String> = _channelInput.asStateFlow()

    // 收藏列表
    private val _favoriteList = MutableStateFlow<List<String>>(emptyList())
    val favoriteList: StateFlow<List<String>> = _favoriteList.asStateFlow()

    // 直播源列表
    val sourceList: StateFlow<List<Pair<String, String>>> = liveSettingsPreferences.liveSourceHistory
        .map { it.toList() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // 当前直播源 URL
    val currentSourceUrl: StateFlow<String> = liveSettingsPreferences.liveSourceUrl
        .stateIn(viewModelScope, SharingStarted.Eagerly, "")

    // 是否显示收藏列表
    private val _favoriteListVisible = MutableStateFlow(false)
    val favoriteListVisible: StateFlow<Boolean> = _favoriteListVisible.asStateFlow()

    // 直播源 URL
    private val _sourceUrl = MutableStateFlow("")
    val sourceUrl: StateFlow<String> = _sourceUrl.asStateFlow()

    // 画面比例
    val videoAspectRatio: StateFlow<LiveSettingsPreferences.Companion.VideoAspectRatio> = 
        liveSettingsPreferences.videoAspectRatio
            .stateIn(viewModelScope, SharingStarted.Eagerly, LiveSettingsPreferences.Companion.VideoAspectRatio.ORIGINAL)

    // 换台方向反转
    val channelChangeFlip: StateFlow<Boolean> = liveSettingsPreferences.channelChangeFlip
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    // 数字选台启用状态
    val channelNoSelectEnable: StateFlow<Boolean> = liveSettingsPreferences.channelNoSelectEnable
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    // 节目单启用状态
    val epgEnable: StateFlow<Boolean> = liveSettingsPreferences.epgEnable
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    // 播放错误计数
    private val _playErrorCount = MutableStateFlow(0)
    val playErrorCount: StateFlow<Int> = _playErrorCount.asStateFlow()
    
    // 当前线路重试计数（用于线路切换逻辑）
    private var routeRetryCount = 0
    private val maxRouteRetries = 3  // 每个线路最多重试3次

    // 错误提示信息
    private val _errorToastMessage = MutableStateFlow<String?>(null)
    val errorToastMessage: StateFlow<String?> = _errorToastMessage.asStateFlow()

    // 是否显示错误对话框
    private val _showErrorDialog = MutableStateFlow(false)
    val showErrorDialog: StateFlow<Boolean> = _showErrorDialog.asStateFlow()

    // 错误对话框消息
    private val _errorDialogMessage = MutableStateFlow("")
    val errorDialogMessage: StateFlow<String> = _errorDialogMessage.asStateFlow()

    init {
        // 加载保存的设置和直播源
        viewModelScope.launch {
            val savedUrl = liveSettingsPreferences.liveSourceUrl.first()
            val savedEpgUrl = liveSettingsPreferences.epgUrl.first()
            val savedFavorites = liveSettingsPreferences.favoriteChannels.first()
            
            if (savedUrl.isNotEmpty()) {
                _sourceUrl.value = savedUrl
                repository.loadSource(savedUrl).fold(
                    onSuccess = {
                        // 加载成功后，自动选择第一个有URL的频道开始播放
                        val firstChannel = repository.channelGroupList.value.allChannels()
                            .firstOrNull { it.urlList.isNotEmpty() }
                        firstChannel?.let { 
                            _currentChannel.value = it
                            _currentChannelUrlIdx.value = 0
                        }
                        
                        // 优先从直播源的 x-tvg-url 加载 EPG
                        // 注意：使用 repository 当前的 channelGroupList
                        val allEpgUrls = repository.channelGroupList.value.allChannels()
                            .flatMap { channel -> channel.epgUrls }
                            .distinct()
                        
                        if (allEpgUrls.isNotEmpty()) {
                            android.util.Log.d("LiveViewModel", "初始化时从直播源加载 EPG: $allEpgUrls")
                            repository.loadEpgFromUrls(allEpgUrls).fold(
                                onSuccess = {
                                    android.util.Log.d("LiveViewModel", "初始化时 EPG 加载成功")
                                },
                                onFailure = { error ->
                                    android.util.Log.w("LiveViewModel", "初始化时 EPG 加载失败: ${error.message}")
                                }
                            )
                        }
                    },
                    onFailure = { /* 错误信息已由 repository 处理 */ }
                )
            }
            
            // 如果直播源没有提供 EPG 或加载失败，尝试使用保存的 EPG URL
            if (repository.epgList.value.isEmpty() && savedEpgUrl.isNotEmpty()) {
                loadEpg(savedEpgUrl)
            }
            
            _favoriteList.value = savedFavorites.toList()
        }
        
        // 启动定时刷新任务
        startPeriodicRefresh()
    }
    
    /**
     * 启动定时刷新任务
     */
    private fun startPeriodicRefresh() {
        viewModelScope.launch {
            liveSettingsPreferences.autoRefreshInterval.collect { intervalHours ->
                if (intervalHours > 0) {
                    // 转换为毫秒
                    val intervalMs = intervalHours * 60 * 60 * 1000L
                    android.util.Log.d("LiveViewModel", "定时刷新已启用，间隔: $intervalHours 小时")
                    
                    while (true) {
                        kotlinx.coroutines.delay(intervalMs)
                        android.util.Log.d("LiveViewModel", "执行定时刷新直播源")
                        refreshAllSources()
                    }
                }
            }
        }
    }
    
    /**
     * 从直播源的 x-tvg-url 加载 EPG
     */
    private suspend fun loadEpgFromSource() {
        // 收集所有频道的 epgUrls（去重）
        val allEpgUrls = repository.channelGroupList.value.allChannels()
            .flatMap { it.epgUrls }
            .distinct()
        
        if (allEpgUrls.isNotEmpty()) {
            android.util.Log.d("LiveViewModel", "从直播源加载 EPG，URL 列表: $allEpgUrls")
            repository.loadEpgFromUrls(allEpgUrls).fold(
                onSuccess = {
                    android.util.Log.d("LiveViewModel", "从直播源加载 EPG 成功")
                },
                onFailure = { error ->
                    android.util.Log.w("LiveViewModel", "从直播源加载 EPG 失败: ${error.message}")
                }
            )
        } else {
            android.util.Log.d("LiveViewModel", "直播源中没有 x-tvg-url")
        }
    }

    /**
     * 加载直播源
     */
    fun loadSource(url: String) {
        _sourceUrl.value = url
        viewModelScope.launch {
            liveSettingsPreferences.setLiveSourceUrl(url)
            repository.loadSource(url).fold(
                onSuccess = {
                    // 加载成功后，自动选择第一个有URL的频道
                    val firstChannel = repository.channelGroupList.value.allChannels()
                        .firstOrNull { it.urlList.isNotEmpty() }
                    firstChannel?.let { changeChannel(it) }
                    
                    // 从直播源加载 EPG
                    loadEpgFromSource()
                },
                onFailure = { }
            )
        }
    }

    /**
     * 加载 EPG
     */
    fun loadEpg(url: String) {
        viewModelScope.launch {
            liveSettingsPreferences.setEpgUrl(url)
            repository.loadEpg(url)
        }
    }

    /**
     * 切换频道
     */
    fun changeChannel(channel: LiveChannel, urlIdx: Int = 0) {
        _currentChannel.value = channel
        _currentChannelUrlIdx.value = urlIdx
        _isTempPanelVisible.value = true
        hidePanel()
        // 重置重试计数
        routeRetryCount = 0
    }

    /**
     * 切换到下一个频道
     * 如果启用了换台方向反转，则切换到上一个频道
     */
    fun changeToNextChannel() {
        val flip = channelChangeFlip.value
        val targetChannel = if (flip) {
            repository.getPreviousChannel(_currentChannel.value)
        } else {
            repository.getNextChannel(_currentChannel.value)
        }
        android.util.Log.d("LiveViewModel", "changeToNextChannel: 当前频道=${_currentChannel.value.name}, 目标频道=${targetChannel?.name}")
        targetChannel?.let { 
            // 重置错误计数
            _playErrorCount.value = 0
            changeChannel(it) 
        }
    }

    /**
     * 切换到上一个频道
     * 如果启用了换台方向反转，则切换到下一个频道
     */
    fun changeToPreviousChannel() {
        val flip = channelChangeFlip.value
        val targetChannel = if (flip) {
            repository.getNextChannel(_currentChannel.value)
        } else {
            repository.getPreviousChannel(_currentChannel.value)
        }
        android.util.Log.d("LiveViewModel", "changeToPreviousChannel: 当前频道=${_currentChannel.value.name}, 目标频道=${targetChannel?.name}")
        targetChannel?.let { 
            // 重置错误计数
            _playErrorCount.value = 0
            changeChannel(it) 
        }
    }

    /**
     * 处理播放错误
     * 智能重试逻辑：
     * 1. 先重试当前线路最多3次
     * 2. 如果当前频道有多线路，切换到下一个线路继续重试
     * 3. 所有线路都试过后，切换到下一个频道
     * 4. 连续错误10次后提示切换直播源
     */
    fun handlePlayError(errorMessage: String? = null) {
        viewModelScope.launch {
            _playErrorCount.value++
            val currentCount = _playErrorCount.value
            val currentChannel = _currentChannel.value
            val currentUrlIdx = _currentChannelUrlIdx.value
            val hasMultipleRoutes = currentChannel.urlList.size > 1
            
            android.util.Log.w("LiveViewModel", "播放错误，当前错误次数: $currentCount, 线路重试: $routeRetryCount/$maxRouteRetries, 当前线路: $currentUrlIdx/${currentChannel.urlList.size}")
            
            if (currentCount >= 10) {
                // 连续错误10次，显示错误对话框并尝试切换直播源
                _errorDialogMessage.value = "连续播放错误 $currentCount 次，正在自动切换直播源..."
                _showErrorDialog.value = true
                
                // 尝试切换到下一个直播源
                switchToNextSource()
            } else {
                // 增加线路重试计数
                routeRetryCount++
                
                if (routeRetryCount < maxRouteRetries) {
                    // 当前线路还没重试够3次，继续重试当前线路
                    _errorToastMessage.value = "播放错误，重试中... (${routeRetryCount}/$maxRouteRetries)"
                    android.util.Log.d("LiveViewModel", "重试当前线路: $currentUrlIdx")
                    // 重试当前线路：触发重新播放（通过切换线路索引到自身来触发）
                    _currentChannelUrlIdx.value = currentUrlIdx
                } else if (hasMultipleRoutes && currentUrlIdx < currentChannel.urlList.size - 1) {
                    // 当前线路已重试3次，且还有其他线路，切换到下一个线路
                    val nextRouteIdx = currentUrlIdx + 1
                    _errorToastMessage.value = "线路${currentUrlIdx + 1}失败，切换到线路${nextRouteIdx + 1}..."
                    android.util.Log.d("LiveViewModel", "切换线路: $currentUrlIdx -> $nextRouteIdx")
                    routeRetryCount = 0  // 重置线路重试计数
                    _currentChannelUrlIdx.value = nextRouteIdx
                } else {
                    // 所有线路都试过了，或者只有一个线路，切换到下一个频道
                    _errorToastMessage.value = "播放错误，自动切换下一个频道 ($currentCount/10)"
                    android.util.Log.d("LiveViewModel", "切换到下一个频道")
                    routeRetryCount = 0  // 重置线路重试计数
                    
                    // 延迟1秒后切换到下一个频道
                    kotlinx.coroutines.delay(1000)
                    changeToNextChannel()
                }
            }
        }
    }

    /**
     * 切换到下一个直播源
     */
    private suspend fun switchToNextSource() {
        val sources = sourceList.value
        val currentUrl = currentSourceUrl.value
        
        if (sources.isEmpty()) {
            _errorDialogMessage.value = "没有可用的直播源"
            return
        }
        
        // 找到当前源的索引
        val currentIndex = sources.indexOfFirst { it.second == currentUrl }
        val nextIndex = if (currentIndex >= 0) (currentIndex + 1) % sources.size else 0
        val nextSource = sources.getOrNull(nextIndex)
        
        if (nextSource != null) {
            android.util.Log.d("LiveViewModel", "切换到下一个直播源: ${nextSource.first}")
            _errorDialogMessage.value = "正在切换到: ${nextSource.first}"
            
            // 加载新源
            repository.loadSource(nextSource.second).fold(
                onSuccess = {
                    _playErrorCount.value = 0
                    _showErrorDialog.value = false
                    _sourceUrl.value = nextSource.second
                    liveSettingsPreferences.setLiveSourceUrl(nextSource.second)
                    
                    // 加载成功后选择第一个频道
                    val firstChannel = repository.channelGroupList.value.allChannels()
                        .firstOrNull { it.urlList.isNotEmpty() }
                    firstChannel?.let { changeChannel(it) }
                },
                onFailure = { error ->
                    _errorDialogMessage.value = "切换直播源失败: ${error.message}"
                    // 3秒后关闭对话框
                    kotlinx.coroutines.delay(3000)
                    _showErrorDialog.value = false
                }
            )
        } else {
            _errorDialogMessage.value = "没有更多可用直播源"
        }
    }

    /**
     * 清除错误提示
     */
    fun clearErrorToast() {
        _errorToastMessage.value = null
    }

    /**
     * 关闭错误对话框
     */
    fun dismissErrorDialog() {
        _showErrorDialog.value = false
    }

    /**
     * 切换线路
     */
    fun changeUrlIdx(delta: Int) {
        val currentIdx = _currentChannelUrlIdx.value
        val channel = _currentChannel.value
        val newIdx = (currentIdx + delta).coerceIn(0, channel.urlList.size - 1)
        android.util.Log.d("LiveViewModel", "切换线路: $currentIdx -> $newIdx (共${channel.urlList.size}个线路)")
        _currentChannelUrlIdx.value = newIdx
        // 重置线路重试计数
        routeRetryCount = 0
    }

    /**
     * 显示面板
     */
    fun showPanel() {
        _isPanelVisible.value = true
    }

    /**
     * 隐藏面板
     */
    fun hidePanel() {
        _isPanelVisible.value = false
    }

    /**
     * 隐藏临时面板
     */
    fun hideTempPanel() {
        _isTempPanelVisible.value = false
    }

    /**
     * 输入数字
     */
    fun inputNumber(number: Int) {
        val currentInput = _channelInput.value
        if (currentInput.length < 3) {
            _channelInput.value = currentInput + number
        }
    }

    /**
     * 确认数字选台
     */
    fun confirmChannelInput() {
        val input = _channelInput.value
        if (input.isNotEmpty()) {
            val channelNo = input.toIntOrNull()
            channelNo?.let { no ->
                val channel = repository.getChannelByNumber(no)
                channel?.let { changeChannel(it) }
            }
        }
        _channelInput.value = ""
    }

    /**
     * 清除数字输入
     */
    fun clearChannelInput() {
        _channelInput.value = ""
    }

    /**
     * 切换收藏
     */
    fun toggleFavorite(channel: LiveChannel) {
        val currentList = _favoriteList.value.toMutableList()
        if (currentList.contains(channel.channelName)) {
            currentList.remove(channel.channelName)
        } else {
            currentList.add(channel.channelName)
        }
        _favoriteList.value = currentList
        
        // 保存到本地
        viewModelScope.launch {
            liveSettingsPreferences.setFavoriteChannels(currentList.toSet())
        }
    }

    /**
     * 检查是否已收藏
     */
    fun isFavorite(channel: LiveChannel): Boolean {
        return _favoriteList.value.contains(channel.channelName)
    }

    /**
     * 切换收藏列表显示
     */
    fun toggleFavoriteListVisible() {
        _favoriteListVisible.value = !_favoriteListVisible.value
    }

    /**
     * 设置收藏列表可见性
     */
    fun setFavoriteListVisible(visible: Boolean) {
        _favoriteListVisible.value = visible
    }

    /**
     * 获取当前频道号
     */
    fun getCurrentChannelNo(): Int {
        return repository.channelGroupList.value.allChannels().indexOfFirst { 
            it.channelName == _currentChannel.value.channelName 
        } + 1
    }

    /**
     * 刷新当前源
     */
    fun refresh() {
        if (_sourceUrl.value.isNotEmpty()) {
            loadSource(_sourceUrl.value)
        }
    }

    /**
     * 清除缓存
     */
    fun clearCache() {
        viewModelScope.launch {
            // 清除 EPG 缓存
            repository.clearCache()
        }
    }

    /**
     * 切换直播源
     */
    fun switchSource(url: String) {
        viewModelScope.launch {
            _sourceUrl.value = url
            loadSource(url)
        }
    }

    /**
     * 切换画面比例
     */
    fun switchAspectRatio() {
        viewModelScope.launch {
            val currentRatio = videoAspectRatio.value
            val ratios = LiveSettingsPreferences.Companion.VideoAspectRatio.entries.toTypedArray()
            val nextIndex = (ratios.indexOf(currentRatio) + 1) % ratios.size
            val nextRatio = ratios[nextIndex]
            liveSettingsPreferences.setVideoAspectRatio(nextRatio)
        }
    }

    /**
     * 刷新全部直播源
     * 遍历所有直播源并重新加载
     */
    fun refreshAllSources() {
        viewModelScope.launch {
            val sources = sourceList.value
            val currentUrl = currentSourceUrl.value
            
            // 重新加载当前源
            if (currentUrl.isNotEmpty()) {
                loadSource(currentUrl)
            }
            
            // 同时也刷新 EPG
            val epgUrls = liveSettingsPreferences.epgUrlHistory.first()
            if (epgUrls.isNotEmpty()) {
                val currentEpgUrl = liveSettingsPreferences.epgUrl.first()
                if (currentEpgUrl.isNotEmpty()) {
                    loadEpg(currentEpgUrl)
                }
            }
            
            // 从直播源加载 EPG
            loadEpgFromSource()
        }
    }
}

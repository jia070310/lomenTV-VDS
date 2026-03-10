package com.lomen.tv.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lomen.tv.data.preferences.TmdbApiPreferences
import com.lomen.tv.data.scraper.TmdbScraper
import com.lomen.tv.service.TmdbApiConfigServer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TmdbApiViewModel @Inject constructor(
    application: Application,
    private val tmdbApiPreferences: TmdbApiPreferences
) : AndroidViewModel(application) {
    
    companion object {
        private const val TAG = "TmdbApiViewModel"
        const val SERVER_PORT = 8893
    }
    
    // API Key 状态
    val apiKey: Flow<String> = tmdbApiPreferences.apiKey
    val apiReadToken: Flow<String> = tmdbApiPreferences.apiReadToken
    val hasCustomApiKey: Flow<Boolean> = tmdbApiPreferences.hasCustomApiKey
    val isApiKeyConfigured: Flow<Boolean> = tmdbApiPreferences.isApiKeyConfigured
    
    // 是否已初始化
    private val _isInitialized = MutableStateFlow(false)
    val isInitialized: StateFlow<Boolean> = _isInitialized.asStateFlow()
    
    // 服务器状态
    private val _isServerRunning = MutableStateFlow(false)
    val isServerRunning: StateFlow<Boolean> = _isServerRunning.asStateFlow()
    
    // 配置服务器
    private var configServer: TmdbApiConfigServer? = null
    
    // 服务器地址
    val serverUrl: String
        get() = "http://${getDeviceIpAddress()}:$SERVER_PORT/tmdb"
    
    init {
        // 应用启动时加载用户的 API Key
        loadApiKey()
    }
    
    /**
     * 加载用户保存的 API Key 到 TmdbScraper
     */
    private fun loadApiKey() {
        viewModelScope.launch {
            try {
                val key = tmdbApiPreferences.apiKey
                val token = tmdbApiPreferences.apiReadToken
                
                key.collect { apiKey ->
                    token.collect { readToken ->
                        TmdbScraper.getInstance().setApiKey(apiKey, readToken)
                        Log.d(TAG, "TMDB API Key 已加载: ${if (apiKey != TmdbApiPreferences.DEFAULT_API_KEY) "自定义" else "默认"}")
                        _isInitialized.value = true
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "加载 TMDB API Key 失败", e)
                _isInitialized.value = true
            }
        }
    }
    
    /**
     * 启动配置服务器
     */
    fun startConfigServer(onConfigReceived: () -> Unit) {
        if (_isServerRunning.value) {
            Log.d(TAG, "Server already running")
            return
        }
        
        try {
            configServer = TmdbApiConfigServer(getApplication(), SERVER_PORT).apply {
                startServer { config ->
                    viewModelScope.launch {
                        saveApiKey(config.apiKey, config.apiReadToken)
                        onConfigReceived()
                    }
                }
            }
            _isServerRunning.value = true
            Log.d(TAG, "Config server started on port $SERVER_PORT")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start config server", e)
            _isServerRunning.value = false
        }
    }
    
    /**
     * 停止配置服务器
     */
    fun stopConfigServer() {
        configServer?.stopServer()
        configServer = null
        _isServerRunning.value = false
        Log.d(TAG, "Config server stopped")
    }
    
    override fun onCleared() {
        super.onCleared()
        stopConfigServer()
    }
    
    /**
     * 获取设备 IP 地址
     */
    private fun getDeviceIpAddress(): String {
        return try {
            val interfaces = java.net.NetworkInterface.getNetworkInterfaces()
            for (networkInterface in interfaces) {
                val addresses = networkInterface.inetAddresses
                for (address in addresses) {
                    if (!address.isLoopbackAddress && address is java.net.Inet4Address) {
                        return address.hostAddress ?: "192.168.0.100"
                    }
                }
            }
            "192.168.0.100"
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get IP address", e)
            "192.168.0.100"
        }
    }
    
    /**
     * 保存用户自定义的 API Key
     */
    fun saveApiKey(apiKey: String, apiReadToken: String) {
        viewModelScope.launch {
            try {
                tmdbApiPreferences.saveApiKey(apiKey)
                tmdbApiPreferences.saveApiReadToken(apiReadToken)
                
                // 立即更新到 TmdbScraper
                TmdbScraper.getInstance().setApiKey(
                    apiKey.takeIf { it.isNotBlank() },
                    apiReadToken.takeIf { it.isNotBlank() }
                )
                
                Log.d(TAG, "TMDB API Key 已保存并更新")
            } catch (e: Exception) {
                Log.e(TAG, "保存 TMDB API Key 失败", e)
            }
        }
    }
    
    /**
     * 重置为默认 API Key
     */
    fun resetToDefault() {
        viewModelScope.launch {
            try {
                tmdbApiPreferences.resetToDefault()
                
                // 立即更新到 TmdbScraper
                TmdbScraper.getInstance().setApiKey(null, null)
                
                Log.d(TAG, "TMDB API Key 已重置为默认")
            } catch (e: Exception) {
                Log.e(TAG, "重置 TMDB API Key 失败", e)
            }
        }
    }
    
    /**
     * 删除 API Key（清空配置）
     */
    fun clearApiKey() {
        viewModelScope.launch {
            try {
                tmdbApiPreferences.saveApiKey("")
                tmdbApiPreferences.saveApiReadToken("")
                
                // 立即更新到 TmdbScraper
                TmdbScraper.getInstance().setApiKey(null, null)
                
                Log.d(TAG, "TMDB API Key 已删除")
            } catch (e: Exception) {
                Log.e(TAG, "删除 TMDB API Key 失败", e)
            }
        }
    }
    
    /**
     * 测试 API Key 是否有效
     */
    suspend fun testApiKey(apiKey: String): Boolean {
        return try {
            // 临时设置测试用的 API Key
            val scraper = TmdbScraper.getInstance()
            val originalKey = scraper.isUsingCustomApiKey()
            
            scraper.setApiKey(apiKey, null)
            
            // 尝试搜索一个已知的电影来测试
            val result = scraper.searchMovie("The Matrix", 1999)
            
            // 恢复原来的设置
            if (!originalKey) {
                scraper.setApiKey(null, null)
            }
            
            result != null
        } catch (e: Exception) {
            Log.e(TAG, "测试 API Key 失败", e)
            false
        }
    }
}

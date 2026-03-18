package com.lomen.tv.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.liveSettingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "live_settings")

@Singleton
class LiveSettingsPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.liveSettingsDataStore

    companion object {
        val LIVE_SOURCE_URL = stringPreferencesKey("live_source_url")
        val EPG_URL = stringPreferencesKey("epg_url")
        val FAVORITE_CHANNELS = stringPreferencesKey("favorite_channels")
        val USER_AGENT = stringPreferencesKey("user_agent")
        val CHANNEL_CHANGE_FLIP = booleanPreferencesKey("channel_change_flip")
        val CHANNEL_NO_SELECT_ENABLE = booleanPreferencesKey("channel_no_select_enable")
        val EPG_ENABLE = booleanPreferencesKey("epg_enable")
        val AUTO_ENTER_LIVE = booleanPreferencesKey("auto_enter_live")
        val BOOT_STARTUP = booleanPreferencesKey("boot_startup")
        val HAS_AUTO_ENTERED_LIVE = booleanPreferencesKey("has_auto_entered_live")
        val LIVE_SOURCE_HISTORY = stringPreferencesKey("live_source_history")
        val EPG_URL_HISTORY = stringPreferencesKey("epg_url_history")
        val USER_AGENT_HISTORY = stringPreferencesKey("user_agent_history")
        val VIDEO_ASPECT_RATIO = intPreferencesKey("video_aspect_ratio")
        val AUTO_REFRESH_INTERVAL = intPreferencesKey("auto_refresh_interval") // 小时，0表示禁用
        
        // 默认直播源
        const val DEFAULT_LIVE_SOURCE_URL = "https://gh-proxy.org/https://github.com/jia070310/lemonTV/blob/main/iptv-fe.m3u"
        // 主默认EPG地址
        const val DEFAULT_EPG_URL = "https://gh-proxy.org/https://raw.githubusercontent.com/plsy1/epg/main/e/seven-days.xml.gz"
        // 备用EPG地址
        const val BACKUP_EPG_URL = "http://epg.51zmt.top:8000/e1.xml.gz"
        const val DEFAULT_USER_AGENT = "AptvPlayer-UA"
        
        // 内置直播源列表
        val BUILT_IN_LIVE_SOURCES = listOf(
            "LEMONTV" to "https://gh-proxy.org/https://github.com/jia070310/lemonTV/blob/main/iptv-fe.m3u",
            "MIGU" to "https://gh-proxy.org/github.com/ioptu/IPTV.txt2m3u.player/raw/refs/heads/main/migu.m3u",
            "APTV" to "https://gh-proxy.org/https://raw.githubusercontent.com/Kimentanm/aptv/master/m3u/iptv.m3u"
        )
        
        // 内置UA列表
        val BUILT_IN_USER_AGENTS = listOf(
            "AptvPlayer-UA",
            "aliplayer",
            "APTV专用 (iPhone UA)"
        )
        
        // APTV 专用 UA（用于 aptv.app 域名的请求）
        const val APTV_SPECIAL_UA = "AptvPlayer/1.4.25 (iPhone; CPU iPhone OS 17_7 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Mobile/15E148"
        
        // 内置EPG节目表列表
        val BUILT_IN_EPG_URLS = listOf(
            "https://gh-proxy.org/https://raw.githubusercontent.com/plsy1/epg/main/e/seven-days.xml.gz",
            "http://epg.51zmt.top:8000/e1.xml.gz",
            "https://ghfast.top/raw.githubusercontent.com/develop202/migu_video/refs/heads/main/playback.xml",
            "https://hk.gh-proxy.org/raw.githubusercontent.com/develop202/migu_video/refs/heads/main/playback.xml"
        )
        
        // 画面比例枚举
        enum class VideoAspectRatio(val value: Int) {
            ORIGINAL(0),      // 原始
            SIXTEEN_NINE(1),  // 16:9
            FOUR_THREE(2),    // 4:3
            AUTO(3);          // 自动拉伸
            
            companion object {
                fun fromValue(value: Int): VideoAspectRatio {
                    return entries.firstOrNull { it.value == value } ?: ORIGINAL
                }
            }
        }
    }

    /**
     * 获取直播源 URL
     */
    val liveSourceUrl: Flow<String> = dataStore.data.map { preferences ->
        preferences[LIVE_SOURCE_URL] ?: DEFAULT_LIVE_SOURCE_URL
    }

    /**
     * 保存直播源 URL
     */
    suspend fun setLiveSourceUrl(url: String) {
        dataStore.edit { preferences ->
            preferences[LIVE_SOURCE_URL] = url
        }
    }

    /**
     * 获取 EPG URL
     */
    val epgUrl: Flow<String> = dataStore.data.map { preferences ->
        preferences[EPG_URL] ?: DEFAULT_EPG_URL
    }

    /**
     * 保存 EPG URL
     */
    suspend fun setEpgUrl(url: String) {
        dataStore.edit { preferences ->
            preferences[EPG_URL] = url
        }
    }

    /**
     * 获取收藏频道列表（逗号分隔的字符串）
     */
    val favoriteChannels: Flow<Set<String>> = dataStore.data.map { preferences ->
        val saved = preferences[FAVORITE_CHANNELS] ?: ""
        if (saved.isEmpty()) emptySet() else saved.split(",").toSet()
    }

    /**
     * 保存收藏频道列表
     */
    suspend fun setFavoriteChannels(channels: Set<String>) {
        dataStore.edit { preferences ->
            preferences[FAVORITE_CHANNELS] = channels.joinToString(",")
        }
    }

    /**
     * 添加收藏频道
     */
    suspend fun addFavoriteChannel(channelName: String) {
        dataStore.edit { preferences ->
            val current = preferences[FAVORITE_CHANNELS] ?: ""
            val set = if (current.isEmpty()) mutableSetOf() else current.split(",").toMutableSet()
            set.add(channelName)
            preferences[FAVORITE_CHANNELS] = set.joinToString(",")
        }
    }

    /**
     * 移除收藏频道
     */
    suspend fun removeFavoriteChannel(channelName: String) {
        dataStore.edit { preferences ->
            val current = preferences[FAVORITE_CHANNELS] ?: ""
            val set = if (current.isEmpty()) mutableSetOf() else current.split(",").toMutableSet()
            set.remove(channelName)
            preferences[FAVORITE_CHANNELS] = set.joinToString(",")
        }
    }

    /**
     * 获取自定义 User-Agent
     */
    val userAgent: Flow<String> = dataStore.data.map { preferences ->
        preferences[USER_AGENT] ?: DEFAULT_USER_AGENT
    }

    /**
     * 保存自定义 User-Agent
     */
    suspend fun setUserAgent(userAgent: String) {
        dataStore.edit { preferences ->
            preferences[USER_AGENT] = userAgent
        }
    }

    /**
     * 获取换台方向反转设置
     */
    val channelChangeFlip: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[CHANNEL_CHANGE_FLIP] ?: false
    }

    /**
     * 保存换台方向反转设置
     */
    suspend fun setChannelChangeFlip(flip: Boolean) {
        dataStore.edit { preferences ->
            preferences[CHANNEL_CHANGE_FLIP] = flip
        }
    }

    /**
     * 获取数字选台启用状态
     */
    val channelNoSelectEnable: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[CHANNEL_NO_SELECT_ENABLE] ?: true
    }

    /**
     * 保存数字选台启用状态
     */
    suspend fun setChannelNoSelectEnable(enable: Boolean) {
        dataStore.edit { preferences ->
            preferences[CHANNEL_NO_SELECT_ENABLE] = enable
        }
    }

    /**
     * 获取节目单启用状态
     */
    val epgEnable: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[EPG_ENABLE] ?: true
    }

    /**
     * 保存节目单启用状态
     */
    suspend fun setEpgEnable(enable: Boolean) {
        dataStore.edit { preferences ->
            preferences[EPG_ENABLE] = enable
        }
    }

    /**
     * 获取打开APP直接进入直播界面设置
     */
    val autoEnterLive: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[AUTO_ENTER_LIVE] ?: false
    }

    /**
     * 保存打开APP直接进入直播界面设置
     */
    suspend fun setAutoEnterLive(enable: Boolean) {
        dataStore.edit { preferences ->
            preferences[AUTO_ENTER_LIVE] = enable
        }
    }

    /**
     * 获取开机启动设置
     */
    val bootStartup: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[BOOT_STARTUP] ?: false
    }

    /**
     * 保存开机启动设置
     */
    suspend fun setBootStartup(enable: Boolean) {
        dataStore.edit { preferences ->
            preferences[BOOT_STARTUP] = enable
        }
    }

    /**
     * 获取本次APP运行已经自动进入过直播的标记
     * 用于防止开启自动进入直播后，从直播返回首页时再次跳转
     */
    val hasAutoEnteredLive: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[HAS_AUTO_ENTERED_LIVE] ?: false
    }

    /**
     * 设置本次APP运行已自动进入直播的标记
     */
    suspend fun setHasAutoEnteredLive(entered: Boolean) {
        dataStore.edit { preferences ->
            preferences[HAS_AUTO_ENTERED_LIVE] = entered
        }
    }

    /**
     * 获取直播源历史记录 (格式: "名称|URL")，包含内置源
     */
    val liveSourceHistory: Flow<Set<Pair<String, String>>> = dataStore.data.map { preferences ->
        val saved = preferences[LIVE_SOURCE_HISTORY] ?: ""
        val savedSet = if (saved.isEmpty()) emptySet() else {
            saved.split("|").mapNotNull { item ->
                val parts = item.split("::", limit = 2)
                if (parts.size == 2) parts[0] to parts[1] else null
            }.toSet()
        }
        // 合并内置源和用户保存的源
        BUILT_IN_LIVE_SOURCES.toSet() + savedSet
    }

    /**
     * 添加直播源到历史记录
     * @param name 直播源名称
     * @param url 直播源URL
     */
    suspend fun addLiveSourceToHistory(name: String, url: String) {
        if (url.isBlank()) return
        dataStore.edit { preferences ->
            val current = preferences[LIVE_SOURCE_HISTORY] ?: ""
            val set = if (current.isEmpty()) mutableSetOf() else current.split("|").toMutableSet()
            // 移除相同URL的旧记录
            set.removeAll { it.split("::", limit = 2).getOrNull(1) == url }
            // 添加新记录
            val displayName = name.takeIf { it.isNotBlank() } ?: "自定义源"
            set.add("$displayName::$url")
            preferences[LIVE_SOURCE_HISTORY] = set.joinToString("|")
        }
    }

    /**
     * 从历史记录删除直播源
     */
    suspend fun removeLiveSourceFromHistory(url: String) {
        dataStore.edit { preferences ->
            val current = preferences[LIVE_SOURCE_HISTORY] ?: ""
            val set = if (current.isEmpty()) mutableSetOf() else current.split("|").toMutableSet()
            set.removeAll { it.split("::", limit = 2).getOrNull(1) == url }
            preferences[LIVE_SOURCE_HISTORY] = set.joinToString("|")
        }
    }

    /**
     * 获取节目单历史记录，包含所有内置EPG
     */
    val epgUrlHistory: Flow<Set<String>> = dataStore.data.map { preferences ->
        val saved = preferences[EPG_URL_HISTORY] ?: ""
        val savedSet = if (saved.isEmpty()) emptySet() else saved.split("|").toSet()
        // 合并内置EPG和用户保存的EPG
        BUILT_IN_EPG_URLS.toSet() + savedSet
    }

    /**
     * 添加节目单URL到历史记录
     */
    suspend fun addEpgUrlToHistory(url: String) {
        if (url.isBlank()) return
        dataStore.edit { preferences ->
            val current = preferences[EPG_URL_HISTORY] ?: ""
            val set = if (current.isEmpty()) mutableSetOf() else current.split("|").toMutableSet()
            set.add(url)
            preferences[EPG_URL_HISTORY] = set.joinToString("|")
        }
    }

    /**
     * 从历史记录删除节目单URL
     */
    suspend fun removeEpgUrlFromHistory(url: String) {
        dataStore.edit { preferences ->
            val current = preferences[EPG_URL_HISTORY] ?: ""
            val set = if (current.isEmpty()) mutableSetOf() else current.split("|").toMutableSet()
            set.remove(url)
            preferences[EPG_URL_HISTORY] = set.joinToString("|")
        }
    }

    /**
     * 获取User-Agent历史记录，包含内置UA
     */
    val userAgentHistory: Flow<Set<String>> = dataStore.data.map { preferences ->
        val saved = preferences[USER_AGENT_HISTORY] ?: ""
        val savedSet = if (saved.isEmpty()) emptySet() else saved.split("|").toSet()
        // 合并内置UA和用户保存的UA
        BUILT_IN_USER_AGENTS.toSet() + savedSet
    }

    /**
     * 添加User-Agent到历史记录
     */
    suspend fun addUserAgentToHistory(ua: String) {
        if (ua.isBlank()) return
        dataStore.edit { preferences ->
            val current = preferences[USER_AGENT_HISTORY] ?: ""
            val set = if (current.isEmpty()) mutableSetOf() else current.split("|").toMutableSet()
            set.add(ua)
            preferences[USER_AGENT_HISTORY] = set.joinToString("|")
        }
    }

    /**
     * 从历史记录删除User-Agent
     */
    suspend fun removeUserAgentFromHistory(ua: String) {
        dataStore.edit { preferences ->
            val current = preferences[USER_AGENT_HISTORY] ?: ""
            val set = if (current.isEmpty()) mutableSetOf() else current.split("|").toMutableSet()
            set.remove(ua)
            preferences[USER_AGENT_HISTORY] = set.joinToString("|")
        }
    }

    /**
     * 获取画面比例
     */
    val videoAspectRatio: Flow<VideoAspectRatio> = dataStore.data.map { preferences ->
        val value = preferences[VIDEO_ASPECT_RATIO] ?: VideoAspectRatio.ORIGINAL.value
        VideoAspectRatio.fromValue(value)
    }

    /**
     * 保存画面比例
     */
    suspend fun setVideoAspectRatio(ratio: VideoAspectRatio) {
        dataStore.edit { preferences ->
            preferences[VIDEO_ASPECT_RATIO] = ratio.value
        }
    }

    /**
     * 获取自动刷新间隔（小时）
     * 0 表示禁用自动刷新
     */
    val autoRefreshInterval: Flow<Int> = dataStore.data.map { preferences ->
        preferences[AUTO_REFRESH_INTERVAL] ?: 0
    }

    /**
     * 保存自动刷新间隔（小时）
     */
    suspend fun setAutoRefreshInterval(hours: Int) {
        dataStore.edit { preferences ->
            preferences[AUTO_REFRESH_INTERVAL] = hours
        }
    }
}

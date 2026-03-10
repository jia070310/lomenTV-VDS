package com.lomen.tv.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.tmdbApiDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "tmdb_api_preferences"
)

/**
 * TMDB API 配置管理
 */
@Singleton
class TmdbApiPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.tmdbApiDataStore
    
    companion object {
        private val API_KEY_KEY = stringPreferencesKey("tmdb_api_key")
        private val API_READ_TOKEN_KEY = stringPreferencesKey("tmdb_api_read_token")
        
        // 不再提供默认 API Key，用户必须自行配置
        const val DEFAULT_API_KEY = ""
        const val DEFAULT_API_READ_TOKEN = ""
    }
    
    /**
     * 获取用户设置的 API Key（可能为空）
     */
    val apiKey: Flow<String> = dataStore.data.map { preferences ->
        preferences[API_KEY_KEY] ?: DEFAULT_API_KEY
    }
    
    /**
     * 获取用户设置的 API Read Token（可能为空）
     */
    val apiReadToken: Flow<String> = dataStore.data.map { preferences ->
        preferences[API_READ_TOKEN_KEY] ?: DEFAULT_API_READ_TOKEN
    }
    
    /**
     * 检查用户是否设置了有效的 API Key
     */
    val hasCustomApiKey: Flow<Boolean> = dataStore.data.map { preferences ->
        !preferences[API_KEY_KEY].isNullOrBlank()
    }
    
    /**
     * 检查 API Key 是否已配置（用于启动检测）
     */
    val isApiKeyConfigured: Flow<Boolean> = dataStore.data.map { preferences ->
        val key = preferences[API_KEY_KEY]
        !key.isNullOrBlank()
    }
    
    /**
     * 保存 API Key
     */
    suspend fun saveApiKey(apiKey: String) {
        dataStore.edit { preferences ->
            if (apiKey.isBlank()) {
                preferences.remove(API_KEY_KEY)
            } else {
                preferences[API_KEY_KEY] = apiKey.trim()
            }
        }
    }
    
    /**
     * 保存 API Read Token
     */
    suspend fun saveApiReadToken(token: String) {
        dataStore.edit { preferences ->
            if (token.isBlank()) {
                preferences.remove(API_READ_TOKEN_KEY)
            } else {
                preferences[API_READ_TOKEN_KEY] = token.trim()
            }
        }
    }
    
    /**
     * 重置为默认 API Key
     */
    suspend fun resetToDefault() {
        dataStore.edit { preferences ->
            preferences.remove(API_KEY_KEY)
            preferences.remove(API_READ_TOKEN_KEY)
        }
    }
}

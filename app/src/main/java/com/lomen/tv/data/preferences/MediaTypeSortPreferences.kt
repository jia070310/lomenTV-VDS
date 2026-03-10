package com.lomen.tv.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.lomen.tv.domain.model.MediaType
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.mediaTypeSortDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "media_type_sort_preferences"
)

/**
 * 媒体类型排序配置管理
 */
@Singleton
class MediaTypeSortPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.mediaTypeSortDataStore
    
    companion object {
        private val SORT_ORDER_KEY = stringPreferencesKey("media_type_sort_order")
        
        // 默认排序：电视剧 → 电影 → 综艺 → 演唱会 → 纪录片 → 其它
        val DEFAULT_SORT_ORDER = listOf(
            MediaType.TV_SHOW,
            MediaType.MOVIE,
            MediaType.VARIETY,
            MediaType.CONCERT,
            MediaType.DOCUMENTARY,
            MediaType.OTHER
        )
    }
    
    /**
     * 获取媒体类型排序顺序
     */
    val sortOrder: Flow<List<MediaType>> = dataStore.data.map { preferences ->
        val orderString = preferences[SORT_ORDER_KEY]
        if (orderString.isNullOrEmpty()) {
            DEFAULT_SORT_ORDER
        } else {
            try {
                orderString.split(",").mapNotNull { name ->
                    try {
                        MediaType.valueOf(name)
                    } catch (e: IllegalArgumentException) {
                        null
                    }
                }
            } catch (e: Exception) {
                DEFAULT_SORT_ORDER
            }
        }
    }
    
    /**
     * 保存媒体类型排序顺序
     */
    suspend fun saveSortOrder(order: List<MediaType>) {
        dataStore.edit { preferences ->
            preferences[SORT_ORDER_KEY] = order.joinToString(",") { it.name }
        }
    }
    
    /**
     * 重置为默认排序
     */
    suspend fun resetToDefault() {
        saveSortOrder(DEFAULT_SORT_ORDER)
    }
}

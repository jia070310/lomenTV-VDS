package com.lomen.tv.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.lomen.tv.domain.model.ResourceLibrary
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ResourceLibraryRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    @ApplicationContext private val context: Context
) {
    companion object {
        private val LIBRARIES_KEY = stringPreferencesKey("resource_libraries")
        private val CURRENT_LIBRARY_ID_KEY = stringPreferencesKey("current_library_id")
    }

    private val gson = Gson()

    // 获取所有资源库
    val libraries: Flow<List<ResourceLibrary>> = dataStore.data.map { preferences ->
        val json = preferences[LIBRARIES_KEY] ?: "[]"
        val type = object : TypeToken<List<ResourceLibrary>>() {}.type
        try {
            gson.fromJson<List<ResourceLibrary>>(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    // 获取当前选中的资源库ID
    val currentLibraryId: Flow<String?> = dataStore.data.map { preferences ->
        preferences[CURRENT_LIBRARY_ID_KEY]
    }

    // 添加资源库
    suspend fun addLibrary(library: ResourceLibrary) {
        dataStore.edit { preferences ->
            val currentJson = preferences[LIBRARIES_KEY] ?: "[]"
            val type = object : TypeToken<List<ResourceLibrary>>() {}.type
            val currentList = try {
                gson.fromJson<List<ResourceLibrary>>(currentJson, type) ?: emptyList()
            } catch (e: Exception) {
                emptyList()
            }
            
            // 检查是否已存在
            val exists = currentList.any { it.id == library.id }
            val newList = if (exists) {
                currentList.map { if (it.id == library.id) library else it }
            } else {
                currentList + library
            }
            
            preferences[LIBRARIES_KEY] = gson.toJson(newList)
        }
    }

    // 删除资源库
    suspend fun removeLibrary(libraryId: String) {
        dataStore.edit { preferences ->
            val currentJson = preferences[LIBRARIES_KEY] ?: "[]"
            val type = object : TypeToken<List<ResourceLibrary>>() {}.type
            val currentList = try {
                gson.fromJson<List<ResourceLibrary>>(currentJson, type) ?: emptyList()
            } catch (e: Exception) {
                emptyList()
            }
            
            val newList = currentList.filter { it.id != libraryId }
            preferences[LIBRARIES_KEY] = gson.toJson(newList)
            
            // 如果删除的是当前选中的，清除当前选中
            if (preferences[CURRENT_LIBRARY_ID_KEY] == libraryId) {
                preferences.remove(CURRENT_LIBRARY_ID_KEY)
            }
        }
    }

    // 设置当前资源库
    suspend fun setCurrentLibrary(libraryId: String?) {
        dataStore.edit { preferences ->
            if (libraryId != null) {
                preferences[CURRENT_LIBRARY_ID_KEY] = libraryId
            } else {
                preferences.remove(CURRENT_LIBRARY_ID_KEY)
            }
        }
    }

    // 更新资源库
    suspend fun updateLibrary(library: ResourceLibrary) {
        dataStore.edit { preferences ->
            val currentJson = preferences[LIBRARIES_KEY] ?: "[]"
            val type = object : TypeToken<List<ResourceLibrary>>() {}.type
            val currentList = try {
                gson.fromJson<List<ResourceLibrary>>(currentJson, type) ?: emptyList()
            } catch (e: Exception) {
                emptyList()
            }
            
            val newList = currentList.map { 
                if (it.id == library.id) library else it 
            }
            preferences[LIBRARIES_KEY] = gson.toJson(newList)
        }
    }
}

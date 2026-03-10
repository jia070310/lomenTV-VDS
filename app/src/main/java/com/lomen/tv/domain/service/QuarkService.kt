package com.lomen.tv.domain.service

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.lomen.tv.data.remote.api.QuarkApi
import com.lomen.tv.data.remote.model.QuarkFileItem
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class QuarkService @Inject constructor(
    private val quarkApi: QuarkApi,
    private val dataStore: DataStore<Preferences>,
    @ApplicationContext private val context: Context
) {
    companion object {
        // 配置的文件夹ID
        const val ROOT_FOLDER_ID = "83dc504807784ccb8e0097e142ce0c72"
        
        // Cookie存储Key
        private val COOKIE_KEY = stringPreferencesKey("quark_cookie")
    }

    // Cookie管理
    val cookie: Flow<String?> = dataStore.data.map { preferences ->
        preferences[COOKIE_KEY]
    }

    suspend fun saveCookie(cookie: String) {
        dataStore.edit { preferences ->
            preferences[COOKIE_KEY] = cookie
        }
    }

    suspend fun getCurrentCookie(): String? {
        return cookie.first()
    }

    // 删除 Cookie
    suspend fun clearCookie() {
        dataStore.edit { preferences ->
            preferences.remove(COOKIE_KEY)
        }
    }

    // 检查是否已登录（有Cookie即可）
    suspend fun isLoggedIn(): Boolean {
        return getCurrentCookie()?.isNotBlank() == true
    }

    // 获取文件列表（从配置的根目录）
    suspend fun getFileList(
        parentId: String = ROOT_FOLDER_ID,
        page: Int = 1,
        limit: Int = 100
    ): Result<List<QuarkFileItem>> {
        return try {
            val cookie = getCurrentCookie()
            if (cookie == null) {
                return Result.failure(Exception("Cookie not configured. Please set Quark cookie in settings."))
            }
            
            val response = quarkApi.getFileList(
                cookie = cookie,
                parentId = parentId,
                page = page,
                limit = limit
            )

            if (response.isSuccessful) {
                val fileResponse = response.body()
                val files = fileResponse?.data?.list ?: emptyList()
                Result.success(files)
            } else {
                Result.failure(Exception("Failed to get file list: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // 搜索文件
    suspend fun searchFiles(
        query: String,
        page: Int = 1,
        limit: Int = 100
    ): Result<List<QuarkFileItem>> {
        return try {
            val cookie = getCurrentCookie()
            if (cookie == null) {
                return Result.failure(Exception("Cookie not configured. Please set Quark cookie in settings."))
            }
            
            val response = quarkApi.searchFiles(
                cookie = cookie,
                query = query,
                page = page,
                limit = limit
            )

            if (response.isSuccessful) {
                val fileResponse = response.body()
                val files = fileResponse?.data?.list ?: emptyList()
                Result.success(files)
            } else {
                Result.failure(Exception("Failed to search files: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // 获取视频下载链接
    suspend fun getVideoDownloadUrl(fileId: String): Result<String> {
        return try {
            val cookie = getCurrentCookie()
            if (cookie == null) {
                return Result.failure(Exception("Cookie not configured. Please set Quark cookie in settings."))
            }
            
            val response = quarkApi.getDownloadUrl(
                cookie = cookie,
                fileId = fileId
            )

            if (response.isSuccessful) {
                val downloadData = response.body()
                val url = downloadData?.get("url") as? String
                if (url != null) {
                    Result.success(url)
                } else {
                    Result.failure(Exception("No download URL in response"))
                }
            } else {
                Result.failure(Exception("Failed to get download URL: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // 获取指定文件夹下的所有视频文件（不递归子文件夹）
    suspend fun getVideoFilesInFolder(
        folderId: String = ROOT_FOLDER_ID,
        onProgress: ((Int) -> Unit)? = null
    ): Result<List<QuarkFileItem>> {
        val videoExtensions = listOf("mp4", "mkv", "avi", "mov", "wmv", "flv", "webm", "m4v", "ts")
        
        return try {
            val result = getFileList(parentId = folderId)
            
            result.onSuccess { files ->
                val videoFiles = files.filter { file ->
                    file.fileType == "file" && videoExtensions.any { 
                        file.fileName.lowercase().endsWith(".$it") 
                    }
                }
                onProgress?.invoke(videoFiles.size)
                Result.success(videoFiles)
            }.onFailure {
                Result.failure<List<QuarkFileItem>>(it)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // 递归获取所有视频文件（如果需要）
    suspend fun getAllVideoFiles(
        parentId: String = ROOT_FOLDER_ID,
        onProgress: ((Int) -> Unit)? = null
    ): Result<List<QuarkFileItem>> {
        val allFiles = mutableListOf<QuarkFileItem>()
        val videoExtensions = listOf("mp4", "mkv", "avi", "mov", "wmv", "flv", "webm", "m4v", "ts")

        suspend fun fetchFiles(folderId: String, depth: Int = 0) {
            val result = getFileList(parentId = folderId)
            result.onSuccess { files ->
                files.forEach { file ->
                    when {
                        // 视频文件
                        file.fileType == "file" && videoExtensions.any { 
                            file.fileName.lowercase().endsWith(".$it") 
                        } -> {
                            allFiles.add(file)
                            onProgress?.invoke(allFiles.size)
                        }
                        // 文件夹，递归获取（限制深度）
                        file.fileType == "folder" && depth < 3 -> {
                            fetchFiles(file.fileId, depth + 1)
                        }
                    }
                }
            }
        }

        return try {
            fetchFiles(parentId)
            Result.success(allFiles)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

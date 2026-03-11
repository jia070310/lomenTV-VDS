package com.lomen.tv.domain.service

import android.util.Log
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 夸克网盘服务
 */
@Singleton
class QuarkCloudService @Inject constructor() {
    companion object {
        private const val TAG = "QuarkCloudService"
        private const val QUARK_API_URL = "https://drive-pc.quark.cn"
    }
    
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()
    
    /**
     * 获取夸克网盘文件下载链接
     */
    suspend fun getDownloadUrl(cookie: String, fileId: String, fileName: String): Result<String> {
        return try {
            Log.d(TAG, "Getting download URL for file ID: $fileId, name: $fileName")
            
            // 构建API请求
            val apiUrl = "$QUARK_API_URL/1/clouddrive/file/download"
            val jsonBody = JSONObject()
                .put("file_id", fileId)
                .put("target_path", "/Downloads/$fileName")
                .toString()
            
            val request = Request.Builder()
                .url(apiUrl)
                .post(jsonBody.toRequestBody("application/json".toMediaType()))
                .header("Cookie", cookie)
                .header("Referer", "https://pan.quark.cn/")
                .header("Origin", "https://pan.quark.cn")
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .header("Accept", "application/json, text/plain, */*")
                .header("Accept-Language", "zh-CN,zh;q=0.9")
                .header("Accept-Encoding", "gzip, deflate, br")
                .header("Connection", "keep-alive")
                .build()
            
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""
            
            Log.d(TAG, "Quark API response: statusCode=${response.code}, body=${responseBody.take(200)}")
            
            if (response.isSuccessful) {
                val json = JSONObject(responseBody)
                if (json.optInt("code") == 200) {
                    val data = json.optJSONObject("data")
                    val downloadUrl = data?.optString("url")
                    
                    if (!downloadUrl.isNullOrBlank()) {
                        Log.d(TAG, "Got download URL: ${downloadUrl.take(100)}...")
                        return Result.success(downloadUrl)
                    } else {
                        Log.e(TAG, "Download URL is null or blank")
                        return Result.failure(Exception("下载链接为空"))
                    }
                } else {
                    val message = json.optString("message", "未知错误")
                    Log.e(TAG, "API error: $message")
                    return Result.failure(Exception(message))
                }
            } else {
                Log.e(TAG, "HTTP error: ${response.code}")
                return Result.failure(Exception("HTTP错误: ${response.code}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get download URL", e)
            Result.failure(e)
        }
    }
}
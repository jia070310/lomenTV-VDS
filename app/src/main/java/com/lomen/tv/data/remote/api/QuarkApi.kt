package com.lomen.tv.data.remote.api

import com.lomen.tv.data.remote.model.QuarkFileResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

interface QuarkApi {

    companion object {
        const val BASE_URL = "https://drive.quark.cn/"
    }

    // 获取文件列表（使用Cookie认证）
    @GET("1/clouddrive/file/sort")
    suspend fun getFileList(
        @Header("Cookie") cookie: String,
        @Query("pr") pr: String = "ucpro",
        @Query("fr") fr: String = "pc",
        @Query("pdir_fid") parentId: String? = null,
        @Query("_page") page: Int = 1,
        @Query("_size") limit: Int = 100,
        @Query("_fetch_total") fetchTotal: Int = 1,
        @Query("_sort") sort: String = "file_type:asc,updated_at:desc"
    ): Response<QuarkFileResponse>

    // 搜索文件（使用Cookie认证）
    @GET("1/clouddrive/file/search")
    suspend fun searchFiles(
        @Header("Cookie") cookie: String,
        @Query("pr") pr: String = "ucpro",
        @Query("fr") fr: String = "pc",
        @Query("q") query: String,
        @Query("_page") page: Int = 1,
        @Query("_size") limit: Int = 100
    ): Response<QuarkFileResponse>

    // 获取文件下载链接（使用Cookie认证）
    @GET("1/clouddrive/file/download")
    suspend fun getDownloadUrl(
        @Header("Cookie") cookie: String,
        @Query("pr") pr: String = "ucpro",
        @Query("fr") fr: String = "pc",
        @Query("fid") fileId: String
    ): Response<Map<String, Any>>
}

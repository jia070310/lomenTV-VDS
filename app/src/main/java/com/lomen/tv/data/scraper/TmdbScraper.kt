package com.lomen.tv.data.scraper

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URLEncoder
import okhttp3.Dns
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.InetAddress
import java.util.concurrent.TimeUnit

data class ScrapeResult(
    val id: String,
    val title: String,
    val originalTitle: String? = null,
    val overview: String? = null,
    val posterUrl: String? = null,
    val backdropUrl: String? = null,
    val year: Int? = null,
    val rating: Float? = null,
    val genres: List<String> = emptyList(),
    val isMovie: Boolean = true,
    val seasonNumber: Int? = null,
    val episodeNumber: Int? = null
)

/**
 * 剧集信息数据类
 */
data class EpisodeInfo(
    val episodeNumber: Int,
    val name: String,           // 剧集标题/副标题
    val overview: String?,      // 剧集简介
    val stillUrl: String?,      // 剧集封面/剧照
    val airDate: String?,       // 播出日期
    val runtime: Int = 0        // 时长(分钟)
)

class TmdbScraper private constructor() {
    companion object {
        private const val TAG = "TmdbScraper"
        
        // 默认 API 密钥（个人免费版，仅用于演示）
        private const val DEFAULT_API_KEY = "cd1660fdecd8066874f593beab890967"
        private const val DEFAULT_API_READ_TOKEN = "eyJhbGciOiJIUzI1NiJ9.eyJhdWQiOiJjZDE2NjBmZGVjZDgwNjY4NzRmNTkzYmVhYjg5MDk2NyIsIm5iZiI6MTcyMzgxMDI1NC45MjYsInN1YiI6IjY2YmY0MWNlODBhMjIzZTNkZDUwZWQxOSIsInNjb3BlcyI6WyJhcGlfcmVhZCJdLCJ2ZXJzaW9uIjoxfQ.PG_TLHful_sxjJPXbYr1VrdD-YFRBlfwPniPVz0ZgwE"
        
        private const val BASE_URL = "https://api.themoviedb.org/3"
        private const val IMAGE_BASE_URL = "https://image.tmdb.org/t/p/w500"
        private const val BACKDROP_BASE_URL = "https://image.tmdb.org/t/p/w1280"
        
        @Volatile
        private var instance: TmdbScraper? = null
        
        fun getInstance(): TmdbScraper {
            return instance ?: synchronized(this) {
                instance ?: TmdbScraper().also { instance = it }
            }
        }
    }
    
    // 用户自定义的 API Key
    private var customApiKey: String? = null
    private var customApiReadToken: String? = null
    
    /**
     * 设置用户自定义的 API Key
     */
    fun setApiKey(apiKey: String?, apiReadToken: String?) {
        customApiKey = apiKey?.takeIf { it.isNotBlank() }
        customApiReadToken = apiReadToken?.takeIf { it.isNotBlank() }
        Log.d(TAG, "API Key 已更新: ${if (customApiKey != null) "自定义" else "默认"}")
    }
    
    /**
     * 获取当前使用的 API Key
     */
    private fun getApiKey(): String = customApiKey ?: DEFAULT_API_KEY
    
    /**
     * 获取当前使用的 API Read Token
     */
    private fun getApiReadToken(): String = customApiReadToken ?: DEFAULT_API_READ_TOKEN
    
    /**
     * 检查是否使用了自定义 API Key
     */
    fun isUsingCustomApiKey(): Boolean = customApiKey != null

    /**
     * 某些网络环境下 DNS/直连会失败。
     * 这里用 OkHttp + 自定义 DNS：对 `api.themoviedb.org` 返回一组可用 IP，但 **URL 仍保持域名**
     * 以保证 TLS SNI/证书校验正确（相比 “把域名替换为 IP” 更可靠）。
     */
    private val tmdbApiIps = listOf(
        "3.169.231.119",
        "108.156.91.119",
        "108.156.91.128"
    )

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .dns(object : Dns {
            override fun lookup(hostname: String): List<InetAddress> {
                return if (hostname == "api.themoviedb.org") {
                    tmdbApiIps.mapNotNull { ip ->
                        runCatching { InetAddress.getByName(ip) }.getOrNull()
                    }.ifEmpty { Dns.SYSTEM.lookup(hostname) }
                } else {
                    Dns.SYSTEM.lookup(hostname)
                }
            }
        })
        .build()

    /**
     * 仅当标题中的4位数字看起来像“附加年份”时，才允许移除后重搜。
     * 例如：
     * - "Inception 2010" / "盗梦空间(2010)" -> true
     * - "你好1983"（数字是正式片名一部分）-> false
     */
    private fun shouldStripYearFromTitle(title: String, year: Int?): Boolean {
        if (year != null) return true
        return Regex("""(?:^|[.\s_\-()])((?:19|20)\d{2})(?:$|[.\s_\-()])""").containsMatchIn(title)
    }

    suspend fun searchMovie(title: String, year: Int? = null): ScrapeResult? = withContext(Dispatchers.IO) {
        try {
            // 渐进式搜索策略
            // 1. 先用完整标题 + 年份搜索
            if (year != null) {
                val result1 = searchMovieInternal(title, year)
                if (result1 != null) return@withContext result1
                
                Log.d(TAG, "Search with year failed, trying without year...")
            }
            
            // 2. 用完整标题（不带年份）搜索
            val result2 = searchMovieInternal(title, null)
            if (result2 != null) return@withContext result2
            
            // 3. 仅在“疑似年份后缀”场景才移除4位年份重试，避免误删正式片名中的数字
            val titleWithoutYear = title.replace(Regex("\\d{4}"), "").replace(Regex("\\s+"), " ").trim()
            if (shouldStripYearFromTitle(title, year) && titleWithoutYear != title && titleWithoutYear.isNotEmpty()) {
                Log.d(TAG, "Search with cleaned title: $titleWithoutYear")
                val result3 = searchMovieInternal(titleWithoutYear, null)
                if (result3 != null) return@withContext result3
            }
            
            Log.w(TAG, "All search attempts failed for: $title")
            null
        } catch (e: Exception) {
            Log.e(TAG, "Search movie failed: ${e.message}", e)
            null
        }
    }
    
    /**
     * 内部搜索方法（单次尝试）
     */
    private suspend fun searchMovieInternal(title: String, year: Int?): ScrapeResult? {
        try {
            val query = URLEncoder.encode(title, "UTF-8")
            var url = "$BASE_URL/search/movie?query=$query&language=zh-CN"
            if (year != null) {
                url += "&year=$year"
            }

            val result = makeRequest(url)
            val searchResult = if (result == null) {
                // 尝试用英文搜索
                val enUrl = "$BASE_URL/search/movie?query=$query&language=en-US"
                val enResult = makeRequest(enUrl)
                parseMovieResult(enResult)
            } else {
                parseMovieResult(result)
            }
            
            // 如果搜索成功但没有封面，尝试获取详情
            if (searchResult != null && searchResult.posterUrl == null) {
                val detailResult = getMovieDetails(searchResult.id)
                if (detailResult != null) {
                    return detailResult
                }
            }
            
            return searchResult
        } catch (e: Exception) {
            Log.e(TAG, "Failed to search movie: $title", e)
            return null
        }
    }

    /**
     * 使用TMDB的multi搜索接口，让TMDB自己判断类型
     * 这是最推荐的方式，因为TMDB会根据media_type字段明确告诉你是movie还是tv
     */
    suspend fun searchMulti(title: String, year: Int? = null): Pair<ScrapeResult?, String?> = withContext(Dispatchers.IO) {
        try {
            // 渐进式搜索策略
            // 1. 先用完整标题 + 年份搜索
            if (year != null) {
                val result1 = searchMultiInternal(title, year)
                if (result1.first != null) return@withContext result1
                
                Log.d(TAG, "Multi search with year failed, trying without year...")
            }
            
            // 2. 用完整标题（不带年份）搜索
            val result2 = searchMultiInternal(title, null)
            if (result2.first != null) return@withContext result2
            
            // 3. 仅在“疑似年份后缀”场景才移除4位年份重试，避免误删正式片名中的数字
            val titleWithoutYear = title.replace(Regex("\\d{4}"), "").replace(Regex("\\s+"), " ").trim()
            if (shouldStripYearFromTitle(title, year) && titleWithoutYear != title && titleWithoutYear.isNotEmpty()) {
                Log.d(TAG, "Multi search with cleaned title: $titleWithoutYear")
                val result3 = searchMultiInternal(titleWithoutYear, null)
                if (result3.first != null) return@withContext result3
            }
            
            Log.w(TAG, "All multi search attempts failed for: $title")
            Pair(null, null)
        } catch (e: Exception) {
            Log.e(TAG, "Search multi failed: ${e.message}", e)
            Pair(null, null)
        }
    }
    
    /**
     * 内部multi搜索方法（单次尝试）
     */
    private suspend fun searchMultiInternal(title: String, year: Int?): Pair<ScrapeResult?, String?> {
        try {
            val query = URLEncoder.encode(title, "UTF-8")
            var url = "$BASE_URL/search/multi?query=$query&language=zh-CN"
            if (year != null) {
                url += "&year=$year"
            }

            val result = makeRequest(url)
            if (result == null) {
                // 尝试用英文搜索
                val enUrl = "$BASE_URL/search/multi?query=$query&language=en-US"
                val enResult = makeRequest(enUrl)
                return parseMultiResult(enResult)
            }
            return parseMultiResult(result)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to search multi internal: $title", e)
            return Pair(null, null)
        }
    }
    
    private fun parseMultiResult(json: JSONObject?): Pair<ScrapeResult?, String?> {
        if (json == null) return Pair(null, null)
        
        val results = json.optJSONArray("results") ?: return Pair(null, null)
        if (results.length() == 0) return Pair(null, null)
        
        // 找到第一个有效结果
        for (i in 0 until results.length()) {
            val item = results.getJSONObject(i)
            val mediaType = item.optString("media_type", "")
            
            if (mediaType == "movie") {
                val movieResult = parseMovieResultFromItem(item)
                if (movieResult != null) {
                    return Pair(movieResult, "movie")
                }
            } else if (mediaType == "tv") {
                val tvResult = parseTvResultFromItem(item)
                if (tvResult != null) {
                    return Pair(tvResult, "tv")
                }
            }
        }
        
        return Pair(null, null)
    }
    
    private fun parseMovieResultFromItem(item: JSONObject): ScrapeResult? {
        return try {
            val posterPath = item.optString("poster_path", "")
            val backdropPath = item.optString("backdrop_path", "")
            
            ScrapeResult(
                id = item.getInt("id").toString(),
                title = item.optString("title", ""),
                originalTitle = item.optString("original_title", null),
                overview = item.optString("overview", null),
                posterUrl = if (posterPath.isNotEmpty()) IMAGE_BASE_URL + posterPath else null,
                backdropUrl = if (backdropPath.isNotEmpty()) BACKDROP_BASE_URL + backdropPath else null,
                year = item.optString("release_date", "").take(4).toIntOrNull(),
                rating = item.optDouble("vote_average", 0.0).toFloat(),
                genres = emptyList(),
                isMovie = true
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse movie result from item", e)
            null
        }
    }
    
    private fun parseTvResultFromItem(item: JSONObject): ScrapeResult? {
        return try {
            val posterPath = item.optString("poster_path", "")
            val backdropPath = item.optString("backdrop_path", "")
            
            ScrapeResult(
                id = item.getInt("id").toString(),
                title = item.optString("name", ""),
                originalTitle = item.optString("original_name", null),
                overview = item.optString("overview", null),
                posterUrl = if (posterPath.isNotEmpty()) IMAGE_BASE_URL + posterPath else null,
                backdropUrl = if (backdropPath.isNotEmpty()) BACKDROP_BASE_URL + backdropPath else null,
                year = item.optString("first_air_date", "").take(4).toIntOrNull(),
                rating = item.optDouble("vote_average", 0.0).toFloat(),
                genres = emptyList(),
                isMovie = false
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse TV result from item", e)
            null
        }
    }

    suspend fun searchTv(title: String, year: Int? = null): ScrapeResult? = withContext(Dispatchers.IO) {
        try {
            // 渐进式搜索策略
            // 1. 先用完整标题 + 年份搜索
            if (year != null) {
                val result1 = searchTvInternal(title, year)
                if (result1 != null) return@withContext result1
                
                Log.d(TAG, "TV search with year failed, trying without year...")
            }
            
            // 2. 用完整标题（不带年份）搜索
            val result2 = searchTvInternal(title, null)
            if (result2 != null) return@withContext result2
            
            // 3. 仅在“疑似年份后缀”场景才移除4位年份重试，避免误删正式片名中的数字
            val titleWithoutYear = title.replace(Regex("\\d{4}"), "").replace(Regex("\\s+"), " ").trim()
            if (shouldStripYearFromTitle(title, year) && titleWithoutYear != title && titleWithoutYear.isNotEmpty()) {
                Log.d(TAG, "TV search with cleaned title: $titleWithoutYear")
                val result3 = searchTvInternal(titleWithoutYear, null)
                if (result3 != null) return@withContext result3
            }
            
            Log.w(TAG, "All TV search attempts failed for: $title")
            null
        } catch (e: Exception) {
            Log.e(TAG, "Search TV failed: ${e.message}", e)
            null
        }
    }
    
    /**
     * 内部TV搜索方法（单次尝试）
     */
    private suspend fun searchTvInternal(title: String, year: Int?): ScrapeResult? {
        try {
            val query = URLEncoder.encode(title, "UTF-8")
            var url = "$BASE_URL/search/tv?query=$query&language=zh-CN"
            if (year != null) {
                url += "&first_air_date_year=$year"
            }

            val result = makeRequest(url)
            val searchResult = if (result == null) {
                // 尝试用英文搜索
                val enUrl = "$BASE_URL/search/tv?query=$query&language=en-US"
                val enResult = makeRequest(enUrl)
                parseTvResult(enResult)
            } else {
                parseTvResult(result)
            }
            
            // 如果搜索成功但没有封面，尝试获取详情
            if (searchResult != null && searchResult.posterUrl == null) {
                val detailResult = getTvDetails(searchResult.id)
                if (detailResult != null) {
                    return detailResult
                }
            }
            
            return searchResult
        } catch (e: Exception) {
            Log.e(TAG, "Failed to search TV internal: $title", e)
            return null
        }
    }
    
    /**
     * 获取电影详情（用于获取完整封面信息）
     */
    suspend fun getMovieDetails(movieId: String): ScrapeResult? = withContext(Dispatchers.IO) {
        try {
            val url = "$BASE_URL/movie/$movieId?language=zh-CN"
            val result = makeRequest(url)
            if (result != null) {
                parseMovieDetails(result)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get movie details: $movieId", e)
            null
        }
    }
    
    /**
     * 获取电视剧详情（用于获取完整封面信息）
     */
    suspend fun getTvDetails(tvId: String): ScrapeResult? = withContext(Dispatchers.IO) {
        try {
            val url = "$BASE_URL/tv/$tvId?language=zh-CN"
            val result = makeRequest(url)
            if (result != null) {
                parseTvDetails(result)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get TV details: $tvId", e)
            null
        }
    }
    
    /**
     * 获取电视剧特定季的信息
     */
    suspend fun getTvSeasonDetails(tvId: String, seasonNumber: Int): ScrapeResult? = withContext(Dispatchers.IO) {
        try {
            val url = "$BASE_URL/tv/$tvId/season/$seasonNumber?language=zh-CN"
            val result = makeRequest(url)
            if (result != null) {
                parseTvSeasonDetails(result)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get TV season details: $tvId season $seasonNumber", e)
            null
        }
    }
    
    /**
     * 获取电视剧某一季的所有剧集信息
     * 返回每一集的标题、简介、封面等
     */
    suspend fun getTvSeasonEpisodes(tvId: String, seasonNumber: Int): List<EpisodeInfo>? = withContext(Dispatchers.IO) {
        try {
            val url = "$BASE_URL/tv/$tvId/season/$seasonNumber?language=zh-CN"
            val result = makeRequest(url)
            if (result != null) {
                parseSeasonEpisodes(result)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get TV season episodes: $tvId season $seasonNumber", e)
            null
        }
    }
    
    /**
     * 解析季中的所有剧集信息
     */
    private fun parseSeasonEpisodes(json: JSONObject): List<EpisodeInfo> {
        val episodes = mutableListOf<EpisodeInfo>()
        val episodesArray = json.optJSONArray("episodes") ?: return episodes
        
        Log.d(TAG, "parseSeasonEpisodes: 找到 ${episodesArray.length()} 集")
        
        for (i in 0 until episodesArray.length()) {
            val ep = episodesArray.getJSONObject(i)
            val stillPath = ep.optString("still_path", "")
            val episodeName = ep.optString("name", "")
            val episodeNumber = ep.optInt("episode_number", i + 1)
            
            Log.d(TAG, "parseSeasonEpisodes: 第${episodeNumber}集, 标题='$episodeName', still_path='$stillPath'")
            
            episodes.add(EpisodeInfo(
                episodeNumber = episodeNumber,
                name = episodeName,
                overview = ep.optStringOrNull("overview"),
                stillUrl = if (stillPath.isNotEmpty()) BACKDROP_BASE_URL + stillPath else null,
                airDate = ep.optStringOrNull("air_date"),
                runtime = ep.optInt("runtime", 0)
            ))
        }
        
        return episodes
    }
    
    /**
     * 根据tmdbid获取媒体信息
     */
    suspend fun getMediaByTmdbId(tmdbId: String, mediaType: String): ScrapeResult? = withContext(Dispatchers.IO) {
        try {
            if (mediaType == "tv") {
                getTvDetails(tmdbId)
            } else {
                getMovieDetails(tmdbId)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get media by tmdbid: $tmdbId type $mediaType", e)
            null
        }
    }
    
    /**
     * 解析电视剧季的详细信息
     */
    private fun parseTvSeasonDetails(json: JSONObject): ScrapeResult? {
        return try {
            val posterPath = json.optString("poster_path", "")
            val backdropPath = json.optString("backdrop_path", "")
            
            ScrapeResult(
                id = json.getInt("id").toString(),
                title = json.optString("name", ""),
                originalTitle = null,
                overview = json.optStringOrNull("overview"),
                posterUrl = if (posterPath.isNotEmpty()) IMAGE_BASE_URL + posterPath else null,
                backdropUrl = if (backdropPath.isNotEmpty()) BACKDROP_BASE_URL + backdropPath else null,
                year = json.optString("air_date", "").take(4).toIntOrNull(),
                rating = json.optDouble("vote_average", 0.0).toFloat(),
                genres = emptyList(),
                isMovie = false
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse TV season details", e)
            null
        }
    }
    
    private fun parseMovieDetails(json: JSONObject): ScrapeResult? {
        return try {
            val posterPath = json.optString("poster_path", "")
            val backdropPath = json.optString("backdrop_path", "")
            val genresArray = json.optJSONArray("genres")
            val genres = mutableListOf<String>()
            if (genresArray != null) {
                for (i in 0 until genresArray.length()) {
                    val genre = genresArray.getJSONObject(i)
                    genres.add(genre.optString("name", ""))
                }
            }
            
            ScrapeResult(
                id = json.getInt("id").toString(),
                title = json.optString("title", ""),
                originalTitle = json.optStringOrNull("original_title"),
                overview = json.optStringOrNull("overview"),
                posterUrl = if (posterPath.isNotEmpty()) IMAGE_BASE_URL + posterPath else null,
                backdropUrl = if (backdropPath.isNotEmpty()) BACKDROP_BASE_URL + backdropPath else null,
                year = json.optString("release_date", "").take(4).toIntOrNull(),
                rating = json.optDouble("vote_average", 0.0).toFloat(),
                genres = genres,
                isMovie = true
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse movie details", e)
            null
        }
    }
    
    private fun parseTvDetails(json: JSONObject): ScrapeResult? {
        return try {
            val posterPath = json.optString("poster_path", "")
            val backdropPath = json.optString("backdrop_path", "")
            val genresArray = json.optJSONArray("genres")
            val genres = mutableListOf<String>()
            if (genresArray != null) {
                for (i in 0 until genresArray.length()) {
                    val genre = genresArray.getJSONObject(i)
                    genres.add(genre.optString("name", ""))
                }
            }
            
            ScrapeResult(
                id = json.getInt("id").toString(),
                title = json.optString("name", ""),
                originalTitle = json.optStringOrNull("original_name"),
                overview = json.optStringOrNull("overview"),
                posterUrl = if (posterPath.isNotEmpty()) IMAGE_BASE_URL + posterPath else null,
                backdropUrl = if (backdropPath.isNotEmpty()) BACKDROP_BASE_URL + backdropPath else null,
                year = json.optString("first_air_date", "").take(4).toIntOrNull(),
                rating = json.optDouble("vote_average", 0.0).toFloat(),
                genres = genres,
                isMovie = false
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse TV details", e)
            null
        }
    }

    // TMDB API备用域名列表
    private val TMDB_API_HOSTS = listOf(
        "api.themoviedb.org",
        "api.tmdb.org"
    )

    private fun makeRequest(urlString: String): JSONObject? {
        // 首先尝试使用域名（正常方式）；client 内置对 api.themoviedb.org 的 DNS 兜底
        for (host in TMDB_API_HOSTS) {
            val modifiedUrl = urlString.replace("api.themoviedb.org", host)
            Log.d(TAG, "Trying host: $host")

            // 尝试使用Bearer Token方式
            var result = makeRequestWithBearer(modifiedUrl)
            if (result != null) {
                Log.d(TAG, "Success with host: $host")
                return result
            }

            // 如果Bearer Token失败，尝试使用API Key方式
            result = makeRequestWithApiKey(modifiedUrl)
            if (result != null) {
                Log.d(TAG, "Success with API Key on host: $host")
                return result
            }
        }

        Log.e(TAG, "All methods failed for: $urlString")
        return null
    }

    private fun makeRequestWithBearer(urlString: String): JSONObject? {
        return try {
            val request = Request.Builder()
                .url(urlString)
                .get()
                .header("Authorization", "Bearer ${getApiReadToken()}")
                .header("Accept", "application/json")
                .build()

            client.newCall(request).execute().use { resp ->
                if (!resp.isSuccessful) {
                    Log.w(TAG, "Bearer Token HTTP ${resp.code} for $urlString")
                    return null
                }
                val body = resp.body?.string() ?: return null
                JSONObject(body)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Bearer Token request failed: ${e.message}")
            null
        }
    }

    private fun makeRequestWithApiKey(urlString: String): JSONObject? {
        return try {
            // 将Bearer Token URL转换为API Key URL
            val apiKey = getApiKey()
            val apiKeyUrl = if (urlString.contains("?")) {
                "$urlString&api_key=$apiKey"
            } else {
                "$urlString?api_key=$apiKey"
            }

            Log.d(TAG, "Trying API Key URL: ${apiKeyUrl.replace(apiKey, "***")}")

            val request = Request.Builder()
                .url(apiKeyUrl)
                .get()
                .header("Accept", "application/json")
                .build()

            client.newCall(request).execute().use { resp ->
                if (!resp.isSuccessful) {
                    Log.w(TAG, "API Key HTTP ${resp.code} for $urlString")
                    return null
                }
                val body = resp.body?.string() ?: return null
                Log.d(TAG, "API Key method succeeded")
                JSONObject(body)
            }
        } catch (e: Exception) {
            Log.e(TAG, "API Key request failed: ${e.message}")
            null
        }
    }

    private fun parseMovieResult(json: JSONObject?): ScrapeResult? {
        if (json == null) return null

        val results = json.optJSONArray("results") ?: return null
        if (results.length() == 0) return null

        val first = results.getJSONObject(0)
        val posterPath = first.optString("poster_path", "")
        val backdropPath = first.optString("backdrop_path", "")

        return ScrapeResult(
            id = first.getInt("id").toString(),
            title = first.optString("title", ""),
            originalTitle = first.optStringOrNull("original_title"),
            overview = first.optStringOrNull("overview"),
            posterUrl = if (posterPath.isNotEmpty()) IMAGE_BASE_URL + posterPath else null,
            backdropUrl = if (backdropPath.isNotEmpty()) BACKDROP_BASE_URL + backdropPath else null,
            year = first.optString("release_date", "").take(4).toIntOrNull(),
            rating = first.optDouble("vote_average", 0.0).toFloat(),
            genres = emptyList(),
            isMovie = true
        )
    }

    private fun parseTvResult(json: JSONObject?): ScrapeResult? {
        if (json == null) return null

        val results = json.optJSONArray("results") ?: return null
        if (results.length() == 0) return null

        val first = results.getJSONObject(0)
        val posterPath = first.optString("poster_path", "")
        val backdropPath = first.optString("backdrop_path", "")

        return ScrapeResult(
            id = first.getInt("id").toString(),
            title = first.optString("name", ""),
            originalTitle = first.optStringOrNull("original_name"),
            overview = first.optStringOrNull("overview"),
            posterUrl = if (posterPath.isNotEmpty()) IMAGE_BASE_URL + posterPath else null,
            backdropUrl = if (backdropPath.isNotEmpty()) BACKDROP_BASE_URL + backdropPath else null,
            year = first.optString("first_air_date", "").take(4).toIntOrNull(),
            rating = first.optDouble("vote_average", 0.0).toFloat(),
            genres = emptyList(),
            isMovie = false
        )
    }

    private fun JSONObject.optStringOrNull(key: String): String? {
        val v = optString(key, "")
        return v.takeIf { it.isNotBlank() }
    }

    /**
     * 搜索艺人信息
     */
    suspend fun searchPerson(name: String): PersonResult? = withContext(Dispatchers.IO) {
        try {
            val query = URLEncoder.encode(name, "UTF-8")
            val url = "$BASE_URL/search/person?query=$query&language=zh-CN"

            val result = makeRequest(url)
            if (result == null) {
                // 尝试用英文搜索
                val enUrl = "$BASE_URL/search/person?query=$query&language=en-US"
                val enResult = makeRequest(enUrl)
                return@withContext parsePersonResult(enResult)
            }
            parsePersonResult(result)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to search person: $name", e)
            null
        }
    }

    private fun parsePersonResult(json: JSONObject?): PersonResult? {
        if (json == null) return null

        val results = json.optJSONArray("results") ?: return null
        if (results.length() == 0) return null

        val first = results.getJSONObject(0)
        val profilePath = first.optString("profile_path", "")

        return PersonResult(
            id = first.getInt("id").toString(),
            name = first.optString("name", ""),
            originalName = first.optString("original_name", null),
            profileUrl = if (profilePath.isNotEmpty()) IMAGE_BASE_URL + profilePath else null,
            knownFor = first.optString("known_for_department", null)
        )
    }
}

data class PersonResult(
    val id: String,
    val name: String,
    val originalName: String?,
    val profileUrl: String?,
    val knownFor: String?
)

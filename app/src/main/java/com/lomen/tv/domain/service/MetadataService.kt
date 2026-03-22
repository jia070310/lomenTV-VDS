package com.lomen.tv.domain.service

import android.util.Log
import com.lomen.tv.data.remote.api.TmdbApi
import com.lomen.tv.data.remote.model.TmdbCast
import com.lomen.tv.data.remote.model.TmdbCrew
import com.lomen.tv.data.remote.model.TmdbMovieResponse
import com.lomen.tv.data.remote.model.TmdbTvResponse
import com.lomen.tv.domain.model.MediaItem
import com.lomen.tv.domain.model.MediaType
import com.lomen.tv.utils.FileNameParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MetadataService @Inject constructor(
    private val tmdbApi: TmdbApi
) {
    companion object {
        private const val TAG = "MetadataService"
        // TMDb API Key
        private const val API_KEY = "cd1660fdecd8066874f593beab890967"
        private const val IMAGE_BASE_URL = "https://image.tmdb.org/t/p/"
    }

    /**
     * 根据文件名搜索影视信息
     */
    suspend fun searchByFileName(fileName: String): Result<MediaItem> {
        return withContext(Dispatchers.IO) {
            try {
                val parseResult = FileNameParser.parse(fileName)
                val searchQuery = buildSearchQuery(parseResult.title, parseResult.year)
                
                // 根据类型选择搜索API
                if (parseResult.type == MediaType.TV_SHOW) {
                    val searchResponse = tmdbApi.searchTvShows(
                        apiKey = API_KEY,
                        query = searchQuery,
                        year = parseResult.year
                    )
                    
                    if (searchResponse.isSuccessful) {
                        val results = searchResponse.body()?.results ?: emptyList()
                        val bestMatch = selectBestMatchTv(results, parseResult)
                        
                        if (bestMatch != null) {
                            val detailsResponse = tmdbApi.getTvDetails(
                                tvId = bestMatch.id,
                                apiKey = API_KEY
                            )
                            
                            if (detailsResponse.isSuccessful) {
                                val details = detailsResponse.body()
                                if (details != null) {
                                    val mediaItem = convertTvToMediaItem(details)
                                    Result.success(mediaItem)
                                } else {
                                    Result.failure(Exception("Empty details response"))
                                }
                            } else {
                                Result.failure(Exception("Failed to get details: ${detailsResponse.code()}"))
                            }
                        } else {
                            Result.success(createFallbackItem(parseResult))
                        }
                    } else {
                        Result.failure(Exception("Search failed: ${searchResponse.code()}"))
                    }
                } else {
                    val searchResponse = tmdbApi.searchMovies(
                        apiKey = API_KEY,
                        query = searchQuery,
                        year = parseResult.year
                    )
                    
                    if (searchResponse.isSuccessful) {
                        val results = searchResponse.body()?.results ?: emptyList()
                        val bestMatch = selectBestMatchMovie(results, parseResult)
                        
                        if (bestMatch != null) {
                            val detailsResponse = tmdbApi.getMovieDetails(
                                movieId = bestMatch.id,
                                apiKey = API_KEY
                            )
                            
                            if (detailsResponse.isSuccessful) {
                                val details = detailsResponse.body()
                                if (details != null) {
                                    val mediaItem = convertMovieToMediaItem(details)
                                    Result.success(mediaItem)
                                } else {
                                    Result.failure(Exception("Empty details response"))
                                }
                            } else {
                                Result.failure(Exception("Failed to get details: ${detailsResponse.code()}"))
                            }
                        } else {
                            Result.success(createFallbackItem(parseResult))
                        }
                    } else {
                        Result.failure(Exception("Search failed: ${searchResponse.code()}"))
                    }
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * 根据TMDb ID获取详情
     */
    suspend fun getDetailsByTmdbId(tmdbId: Int, type: MediaType): Result<MediaItem> {
        return withContext(Dispatchers.IO) {
            try {
                if (type == MediaType.TV_SHOW) {
                    val response = tmdbApi.getTvDetails(tvId = tmdbId, apiKey = API_KEY)
                    if (response.isSuccessful) {
                        response.body()?.let { details ->
                            Result.success(convertTvToMediaItem(details))
                        } ?: Result.failure(Exception("Empty response"))
                    } else {
                        Result.failure(Exception("Failed to get details: ${response.code()}"))
                    }
                } else {
                    val response = tmdbApi.getMovieDetails(movieId = tmdbId, apiKey = API_KEY)
                    if (response.isSuccessful) {
                        response.body()?.let { details ->
                            Result.success(convertMovieToMediaItem(details))
                        } ?: Result.failure(Exception("Empty response"))
                    } else {
                        Result.failure(Exception("Failed to get details: ${response.code()}"))
                    }
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * 批量搜索
     */
    suspend fun searchBatch(fileNames: List<String>): List<Result<MediaItem>> {
        return fileNames.map { searchByFileName(it) }
    }

    /**
     * 构建搜索查询
     */
    private fun buildSearchQuery(title: String, year: Int?): String {
        var query = title.trim()
        // 移除一些常见干扰词
        query = query.replace(Regex("""\b(the|a|an)\b""", RegexOption.IGNORE_CASE), "")
        return query.trim()
    }

    /**
     * 选择最佳匹配结果（电影）
     */
    private fun selectBestMatchMovie(
        results: List<TmdbMovieResponse>,
        parseResult: FileNameParser.ParseResult
    ): TmdbMovieResponse? {
        if (results.isEmpty()) return null
        
        val filtered = results.filter { result ->
            val releaseYear = parseYear(result.releaseDate ?: "")
            parseResult.year?.let { year -> releaseYear == year } ?: true
        }
        
        return filtered.firstOrNull()
    }

    /**
     * 选择最佳匹配结果（电视剧）
     */
    private fun selectBestMatchTv(
        results: List<TmdbTvResponse>,
        parseResult: FileNameParser.ParseResult
    ): TmdbTvResponse? {
        if (results.isEmpty()) return null
        
        val filtered = results.filter { result ->
            val releaseYear = parseYear(result.firstAirDate ?: "")
            parseResult.year?.let { year -> releaseYear == year } ?: true
        }
        
        return filtered.firstOrNull()
    }

    /**
     * 转换电影TMDb响应为MediaItem
     */
    private fun convertMovieToMediaItem(details: TmdbMovieResponse): MediaItem {
        return MediaItem(
            id = "movie_${details.id}",
            title = details.title,
            originalTitle = details.originalTitle ?: details.title,
            overview = details.overview,
            posterUrl = details.posterPath?.let { "$IMAGE_BASE_URL/w500$it" },
            backdropUrl = details.backdropPath?.let { "$IMAGE_BASE_URL/w780$it" },
            releaseDate = details.releaseDate,
            rating = details.voteAverage,
            genres = details.genres?.map { it.name } ?: emptyList(),
            type = MediaType.MOVIE,
            tmdbId = details.id
        )
    }

    /**
     * 转换电视剧TMDb响应为MediaItem
     */
    private fun convertTvToMediaItem(details: TmdbTvResponse): MediaItem {
        return MediaItem(
            id = "tv_${details.id}",
            title = details.name,
            originalTitle = details.originalName ?: details.name,
            overview = details.overview,
            posterUrl = details.posterPath?.let { "$IMAGE_BASE_URL/w500$it" },
            backdropUrl = details.backdropPath?.let { "$IMAGE_BASE_URL/w780$it" },
            releaseDate = details.firstAirDate,
            rating = details.voteAverage,
            genres = details.genres?.map { it.name } ?: emptyList(),
            type = MediaType.TV_SHOW,
            tmdbId = details.id,
            seasonCount = details.numberOfSeasons ?: 0,
            episodeCount = details.numberOfEpisodes ?: 0
        )
    }

    /**
     * 创建备用MediaItem（当没有匹配到时）
     */
    private fun createFallbackItem(parseResult: FileNameParser.ParseResult): MediaItem {
        return MediaItem(
            id = "local_${System.currentTimeMillis()}",
            title = parseResult.title,
            overview = null,
            type = parseResult.type,
            releaseDate = parseResult.year?.toString()
        )
    }

    /**
     * 解析年份
     */
    private fun parseYear(dateString: String): Int? {
        return try {
            dateString.take(4).toIntOrNull()
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 获取演职人员列表（导演+演员）
     * 使用旧版API (api.tmdb.org) 访问更稳定
     */
    suspend fun getCastAndCrew(
        tmdbId: Int,
        type: MediaType,
        actorLimit: Int = 10,
        directorLimit: Int = 5
    ): List<CastMember> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "getCastAndCrew: tmdbId=$tmdbId, type=$type")
                
                // 使用旧版API（api.tmdb.org）获取演职人员
                val endpoint = if (
                    type == MediaType.TV_SHOW ||
                    type == MediaType.VARIETY ||
                    type == MediaType.ANIME
                ) {
                    "tv"
                } else {
                    "movie"
                }
                val url = "https://api.tmdb.org/3/$endpoint/$tmdbId/credits?api_key=$API_KEY&language=zh-CN"
                Log.d(TAG, "getCastAndCrew: 请求URL: $url")
                
                val connection = java.net.URL(url).openConnection() as java.net.HttpURLConnection
                connection.requestMethod = "GET"
                connection.setRequestProperty("Accept", "application/json")
                connection.connectTimeout = 15000
                connection.readTimeout = 15000
                
                val responseCode = connection.responseCode
                if (responseCode !in 200..299) {
                    Log.e(TAG, "getCastAndCrew HTTP错误: $responseCode")
                    return@withContext emptyList()
                }
                
                val responseText = connection.inputStream.bufferedReader().use { it.readText() }
                connection.disconnect()
                
                val jsonObj = org.json.JSONObject(responseText)
                val rawCastArray = jsonObj.optJSONArray("cast")
                val rawCrewArray = jsonObj.optJSONArray("crew")
                
                Log.d(TAG, "getCastAndCrew: cast size=${rawCastArray?.length()}, crew size=${rawCrewArray?.length()}")
                
                val result = mutableListOf<CastMember>()
                
                // 添加导演
                if (rawCrewArray != null) {
                    var directorCount = 0
                    for (i in 0 until rawCrewArray.length()) {
                        if (directorCount >= directorLimit) break
                        val crewObj = rawCrewArray.getJSONObject(i)
                        val job = crewObj.optString("job", "")
                        if (job == "Director") {
                            result.add(CastMember(
                                id = crewObj.optInt("id", 0),
                                name = crewObj.optString("name", ""),
                                role = "导演",
                                profileUrl = crewObj.optString("profile_path", null)?.let { "$IMAGE_BASE_URL/w185$it" }
                            ))
                            directorCount++
                        }
                    }
                    Log.d(TAG, "getCastAndCrew: 找到 $directorCount 位导演")
                }
                
                // 添加演员（按order排序）
                if (rawCastArray != null) {
                    val actors = mutableListOf<Pair<Int, org.json.JSONObject>>()
                    for (i in 0 until rawCastArray.length()) {
                        val castObj = rawCastArray.getJSONObject(i)
                        val order = castObj.optInt("order", 999)
                        actors.add(Pair(order, castObj))
                    }
                    actors.sortBy { it.first }
                    
                    actors.take(actorLimit).forEach { (_, castObj) ->
                        result.add(CastMember(
                            id = castObj.optInt("id", 0),
                            name = castObj.optString("name", ""),
                            role = castObj.optString("character", null),
                            profileUrl = castObj.optString("profile_path", null)?.let { "$IMAGE_BASE_URL/w185$it" }
                        ))
                    }
                    Log.d(TAG, "getCastAndCrew: 找到 ${minOf(actors.size, actorLimit)} 位演员")
                }
                
                Log.d(TAG, "getCastAndCrew: 总计 ${result.size} 位演职人员")
                result
            } catch (e: Exception) {
                Log.e(TAG, "getCastAndCrew failed", e)
                emptyList()
            }
        }
    }
    
    /**
     * 获取演员列表（兼容旧接口）
     */
    @Deprecated("使用getCastAndCrew代替")
    suspend fun getCastMembers(
        tmdbId: Int,
        type: MediaType,
        limit: Int = 10
    ): List<CastMember> {
        return getCastAndCrew(tmdbId, type, actorLimit = limit, directorLimit = 0)
    }

    /**
     * 演职人员数据类
     */
    data class CastMember(
        val id: Int,
        val name: String,
        val role: String?,  // 导演/角色名称
        val profileUrl: String?
    )
}

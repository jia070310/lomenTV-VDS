package com.lomen.tv.data.scraper

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class DoubanScraper {
    companion object {
        private const val TAG = "DoubanScraper"
        // 使用豆瓣公开的API代理
        private const val SEARCH_URL = "https://movie.douban.com/j/subject_suggest"
        private const val DETAIL_URL = "https://movie.douban.com/j/subject"
        private const val API_BASE = "https://frodo.douban.com/api/v2"
    }
    
    /**
     * 演职人员数据类
     */
    data class CastInfo(
        val id: String,
        val name: String,
        val role: String?,  // 导演/角色
        val avatarUrl: String?
    )

    /**
     * 获取演职人员信息（解析JSON-LD结构化数据）
     * @param doubanId 豆瓣ID
     * @param isMovie 是否是电影（false表示电视剧）
     */
    suspend fun getCastAndCrew(doubanId: String, isMovie: Boolean = true): List<CastInfo> = withContext(Dispatchers.IO) {
        try {
            // 使用豆瓣主页面获取演职人员
            val url = "https://movie.douban.com/subject/$doubanId/"
            
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
            connection.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8")
            connection.setRequestProperty("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8")
            connection.setRequestProperty("Accept-Encoding", "gzip, deflate, br")
            connection.setRequestProperty("Referer", "https://www.douban.com/")
            connection.setRequestProperty("Connection", "keep-alive")
            connection.setRequestProperty("Upgrade-Insecure-Requests", "1")
            connection.connectTimeout = 10000
            connection.readTimeout = 10000

            val responseCode = connection.responseCode
            if (responseCode !in 200..299) {
                Log.w(TAG, "HTTP $responseCode for $url")
                return@withContext emptyList()
            }

            val html = connection.inputStream.bufferedReader().use { it.readText() }
            connection.disconnect()
            
            // 调试：输出HTML的前500字符
            Log.d(TAG, "HTML preview: ${html.take(500)}")
            
            val castList = mutableListOf<CastInfo>()
            
            // 提取JSON-LD结构化数据 - 使用更宽松的匹配
            val jsonLdPattern = Regex("""<script[^>]*type=["']application/ld\+json["'][^>]*>([\s\S]*?)</script>""", RegexOption.IGNORE_CASE)
            val jsonLdMatch = jsonLdPattern.find(html)
            
            if (jsonLdMatch != null) {
                val jsonStr = jsonLdMatch.groupValues[1]
                val jsonObj = JSONObject(jsonStr)
                
                // 解析导演
                val directors = jsonObj.optJSONArray("director")
                if (directors != null) {
                    for (i in 0 until minOf(directors.length(), 3)) {
                        val director = directors.getJSONObject(i)
                        val name = director.optString("name", "")
                            .split(" ")[0]  // 只取中文名，去掉英文名
                            .trim()
                        if (name.isNotBlank()) {
                            castList.add(CastInfo(
                                id = director.optString("url", "").substringAfterLast("/").substringBefore("/"),
                                name = name,
                                role = "导演",
                                avatarUrl = null
                            ))
                            Log.d(TAG, "Found director from JSON-LD: $name")
                        }
                    }
                }
                
                // 解析演员
                val actors = jsonObj.optJSONArray("actor")
                if (actors != null) {
                    for (i in 0 until minOf(actors.length(), 10)) {
                        val actor = actors.getJSONObject(i)
                        val name = actor.optString("name", "")
                            .split(" ")[0]  // 只取中文名，去掉英文名
                            .trim()
                        if (name.isNotBlank()) {
                            castList.add(CastInfo(
                                id = actor.optString("url", "").substringAfterLast("/").substringBefore("/"),
                                name = name,
                                role = "演员",
                                avatarUrl = null
                            ))
                            Log.d(TAG, "Found actor from JSON-LD: $name")
                        }
                    }
                }
                
                Log.d(TAG, "Got ${castList.size} cast members for $doubanId from JSON-LD")
            } else {
                Log.w(TAG, "No JSON-LD found in page")
            }
            
            castList
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get cast info: $doubanId", e)
            emptyList()
        }
    }
    
    private fun makeApiRequest(urlString: String): JSONObject? {
        try {
            val url = URL(urlString)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("User-Agent", "api-client/1 com.douban.frodo/6.42.2(194)")
            connection.setRequestProperty("Referer", "https://movie.douban.com/")
            connection.connectTimeout = 10000
            connection.readTimeout = 10000

            val responseCode = connection.responseCode
            if (responseCode !in 200..299) {
                Log.w(TAG, "HTTP $responseCode for $urlString")
                return null
            }

            val response = connection.inputStream.bufferedReader().use { it.readText() }
            connection.disconnect()

            return JSONObject(response)
        } catch (e: Exception) {
            Log.e(TAG, "API request failed", e)
            return null
        }
    }

    suspend fun search(title: String, year: Int? = null): ScrapeResult? = withContext(Dispatchers.IO) {
        try {
            val query = URLEncoder.encode(title, "UTF-8")
            val url = "$SEARCH_URL?q=$query"

            val result = makeRequest(url)
            if (result == null) {
                Log.w(TAG, "No results for: $title")
                return@withContext null
            }

            // 解析搜索结果
            val items = result.optJSONArray("items") ?: return@withContext null
            if (items.length() == 0) return@withContext null

            // 找到最匹配的结果（考虑年份）
            var bestMatch: JSONObject? = null
            for (i in 0 until items.length()) {
                val item = items.getJSONObject(i)
                val itemYear = item.optString("year", "").toIntOrNull()
                
                if (year != null && itemYear == year) {
                    bestMatch = item
                    break
                }
                if (bestMatch == null) {
                    bestMatch = item
                }
            }

            bestMatch?.let { parseResult(it) }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to search: $title", e)
            null
        }
    }

    private fun makeRequest(urlString: String): JSONObject? {
        val url = URL(urlString)
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
        connection.setRequestProperty("Referer", "https://movie.douban.com/")
        connection.connectTimeout = 10000
        connection.readTimeout = 10000

        val responseCode = connection.responseCode
        if (responseCode !in 200..299) {
            Log.w(TAG, "HTTP $responseCode for $urlString")
            return null
        }

        val response = connection.inputStream.bufferedReader().use { it.readText() }
        connection.disconnect()

        // 豆瓣返回的是JSON数组
        return try {
            JSONObject().put("items", org.json.JSONArray(response))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse response", e)
            null
        }
    }

    private fun parseResult(json: JSONObject): ScrapeResult {
        val type = json.optString("type", "movie")
        val isMovie = type == "movie"
        
        return ScrapeResult(
            id = json.optString("id", ""),
            title = json.optString("title", ""),
            originalTitle = json.optString("original_title", null),
            overview = null, // 搜索结果没有简介，需要单独获取详情
            posterUrl = json.optString("img", null),
            backdropUrl = null,
            year = json.optString("year", "").toIntOrNull(),
            rating = json.optString("rating", "").toFloatOrNull(),
            genres = emptyList(),
            isMovie = isMovie
        )
    }
}

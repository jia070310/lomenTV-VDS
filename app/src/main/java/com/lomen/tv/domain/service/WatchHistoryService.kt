package com.lomen.tv.domain.service

import com.lomen.tv.data.local.database.dao.EpisodeDao
import com.lomen.tv.data.local.database.dao.MovieDao
import com.lomen.tv.data.local.database.dao.WebDavMediaDao
import com.lomen.tv.data.local.database.dao.WatchHistoryDao
import com.lomen.tv.data.local.database.entity.WatchHistoryEntity
import com.lomen.tv.domain.model.isLocalEpisodicSeries
import com.lomen.tv.data.scraper.TmdbScraper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WatchHistoryService @Inject constructor(
    private val watchHistoryDao: WatchHistoryDao,
    private val movieDao: MovieDao,
    private val webDavMediaDao: WebDavMediaDao,
    private val episodeDao: EpisodeDao
) {
    private val tmdbScraper = TmdbScraper.getInstance()
    
    // 内存缓存
    private var cachedWatchHistory: List<WatchHistoryItem>? = null
    private var cacheTimestamp: Long = 0
    private val CACHE_DURATION_MS = 5 * 60 * 1000 // 缓存 5 分钟
    private val MAX_CACHE_SIZE = 50 // 最多缓存 50 条记录

    /**
     * 获取最近观看记录 - 支持 Movie 和 WebDAV 媒体
     * 对于连续剧，只显示最新播放的那一集
     */
    fun getRecentWatchHistory(limit: Int = 30): Flow<List<WatchHistoryItem>> {
        return watchHistoryDao.getRecentWatchHistory(limit * 2).flatMapLatest { entities ->
            flow {
                // 检查缓存是否有效
                val currentTime = System.currentTimeMillis()
                val cached = cachedWatchHistory
                if (cached != null && (currentTime - cacheTimestamp) < CACHE_DURATION_MS) {
                    android.util.Log.d("WatchHistoryService", "Returning cached watch history: ${cached.size} items")
                    emit(cached.take(limit))
                    return@flow
                }
                
                android.util.Log.d("WatchHistoryService", "getRecentWatchHistory: Found ${entities.size} history entities")
                // 先按 lastWatchedAt 排序，确保最新的记录在前面
                val sortedEntities = entities.sortedByDescending { it.lastWatchedAt }
                android.util.Log.d("WatchHistoryService", "Sorted entities by lastWatchedAt (newest first)")
                
                val items = mutableListOf<WatchHistoryItem>()
                val tvShowMap = mutableMapOf<String, WatchHistoryItem>() // 用于去重连续剧：key = seriesKey, value = latest episode
                
                for (entity in sortedEntities) {
                    android.util.Log.d("WatchHistoryService", "Processing history entity: id=${entity.id}, mediaId=${entity.movieId}, episodeId=${entity.episodeId}, lastWatchedAt=${entity.lastWatchedAt}")
                    
                    // 首先尝试从 MovieDao 获取
                    val movie = try {
                        movieDao.getMovieById(entity.movieId)
                    } catch (e: Exception) {
                        android.util.Log.e("WatchHistoryService", "Error getting movie by id: ${entity.movieId}", e)
                        null
                    }
                    android.util.Log.d("WatchHistoryService", "Movie lookup: ${if (movie != null) "Found: ${movie.title}" else "Not found"}")
                    
                    // 如果找不到，尝试从 WebDavMediaDao 获取
                    val webDavMedia = if (movie == null) {
                        try {
                            val media = webDavMediaDao.getById(entity.movieId)
                            android.util.Log.d("WatchHistoryService", "WebDAV media lookup: ${if (media != null) "Found: ${media.title}" else "Not found"}")
                            media
                        } catch (e: Exception) {
                            android.util.Log.e("WatchHistoryService", "Error getting WebDAV media by id: ${entity.movieId}", e)
                            null
                        }
                    } else null
                    
                    // 如果都找不到，跳过这条记录
                    if (movie == null && webDavMedia == null) {
                        android.util.Log.w("WatchHistoryService", "Skipping history entity ${entity.id}: media not found (mediaId=${entity.movieId})")
                        continue
                    }
                    
                    // 对于 WebDAV 媒体的电视剧，episodeId 可能是具体集数的 WebDAV 媒体 ID
                    // 优先尝试从 episodeDao 获取（MovieEntity 类型的电视剧）
                    val episode = entity.episodeId?.let { 
                        try {
                            val ep = episodeDao.getEpisodeById(it)
                            android.util.Log.d("WatchHistoryService", "Episode lookup: ${if (ep != null) "Found: ${ep.title} (episode ${ep.episodeNumber})" else "Not found"}")
                            ep
                        } catch (e: Exception) {
                            android.util.Log.e("WatchHistoryService", "Error getting episode by id: $it", e)
                            null
                        }
                    }
                    
                    // 对于 WebDAV 媒体，如果 episode 为 null 但 episodeId 不为 null，尝试从 WebDavMediaDao 获取
                    // 这表示 episodeId 是另一个 WebDAV 媒体的 ID（具体集数的 ID）
                    val episodeWebDavMedia = if (episode == null && entity.episodeId != null && webDavMedia != null) {
                        try {
                            val epMedia = webDavMediaDao.getById(entity.episodeId)
                            android.util.Log.d("WatchHistoryService", "Episode WebDAV media lookup: ${if (epMedia != null) "Found: ${epMedia.title} (episode ${epMedia.episodeNumber})" else "Not found"}")
                            epMedia
                        } catch (e: Exception) {
                            android.util.Log.e("WatchHistoryService", "Error getting episode WebDAV media by id: ${entity.episodeId}", e)
                            null
                        }
                    } else null

                    // 使用具体集数的信息（如果有），否则使用主媒体信息
                    val targetWebDavMedia = episodeWebDavMedia ?: webDavMedia
                    val rawTitle = movie?.title ?: targetWebDavMedia?.title ?: ""
                    val episodeNumber = episode?.episodeNumber ?: episodeWebDavMedia?.episodeNumber ?: targetWebDavMedia?.episodeNumber
                    
                    // 对于 WebDAV 媒体，如果标题包含集数信息，需要提取基础标题
                    // 例如："枭起青壤 第1集" -> "枭起青壤"
                    val title = if (targetWebDavMedia != null && episodeNumber != null) {
                        // 去除标题中的集数信息
                        rawTitle.replace(Regex("第\\d+集.*"), "")
                            .replace(Regex("\\s+"), " ")
                            .trim()
                    } else {
                        rawTitle
                    }
                    
                    val posterUrl = movie?.posterUrl ?: targetWebDavMedia?.posterUrl
                    val backdropUrl = movie?.backdropUrl ?: targetWebDavMedia?.backdropUrl
                    val isTvShow = movie?.type == com.lomen.tv.domain.model.MediaType.TV_SHOW || 
                                   (targetWebDavMedia != null && (targetWebDavMedia.type == com.lomen.tv.domain.model.MediaType.TV_SHOW || 
                                    targetWebDavMedia.episodeNumber != null || entity.episodeId != null))

                    // 生成 episodeTitle：仅使用本地数据，不调用 TMDB 网络请求
                    val episodeTitle: String? = if (episode != null) {
                        // MovieEntity 类型的电视剧，直接使用 episode.title
                        episode.title
                    } else if (episodeWebDavMedia != null && episodeNumber != null) {
                        // WebDAV 媒体，仅从标题中提取副标题（不调用 TMDB）
                        var episodeTitleText: String? = null
                        
                        if (episodeWebDavMedia.title.isNotBlank()) {
                            // 去除剧集名和"第X集"部分，提取集数标题
                            val episodeTitlePattern = Regex("第\\d+集\\s*(.+)$")
                            val match = episodeTitlePattern.find(episodeWebDavMedia.title)
                            if (match != null && match.groupValues.size > 1) {
                                val extractedTitle = match.groupValues[1].trim()
                                // 如果提取的标题与基础标题不同，且不是空字符串，使用提取的标题
                                if (extractedTitle.isNotBlank() && extractedTitle != title) {
                                    episodeTitleText = extractedTitle
                                }
                            }
                        }
                        
                        episodeTitleText
                    } else {
                        null
                    }

                    val item = WatchHistoryItem(
                        id = entity.id,
                        mediaId = entity.movieId,
                        title = title,
                        posterUrl = posterUrl,
                        backdropUrl = backdropUrl,
                        episodeTitle = episodeTitle,
                        episodeNumber = episodeNumber,
                        seasonNumber = episode?.seasonNumber ?: episodeWebDavMedia?.seasonNumber ?: targetWebDavMedia?.seasonNumber,
                        episodeId = entity.episodeId,
                        progress = entity.progress,
                        duration = entity.duration,
                        progressPercent = if (entity.duration > 0) {
                            (entity.progress * 100 / entity.duration).toInt()
                        } else 0,
                        lastWatchedAt = entity.lastWatchedAt,
                        isWatched = entity.progress > entity.duration * 0.9
                    )
                    
                    // 如果是连续剧，需要去重
                    if (isTvShow) {
                        // 生成剧集的唯一标识：对于 MovieEntity 使用 mediaId，对于 WebDAV 使用 tmdbId（如果存在）
                        val seriesKey = if (movie != null) {
                            // MovieEntity 的 TV_SHOW，使用 mediaId 作为分组键
                            "movie_${entity.movieId}"
                        } else {
                            // WebDAV 媒体，使用 tmdbId 作为分组键（如果存在），否则使用去除集数信息后的标题
                            // 使用主媒体（webDavMedia）的 tmdbId，而不是集数媒体（episodeWebDavMedia）的
                            val tmdbId = webDavMedia?.tmdbId
                            if (tmdbId != null && tmdbId.isNotBlank()) {
                                "webdav_tmdb_$tmdbId"
                            } else {
                                // 如果没有 tmdbId，使用基础标题作为分组键
                                // title 已经是去除集数信息后的基础标题
                                val libraryId = webDavMedia?.libraryId ?: ""
                                // 使用 libraryId + title 作为分组键，确保不同资源库的同名剧集不会混淆
                                "webdav_${libraryId}_${title}"
                            }
                        }
                        
                        // 如果该剧集还没有记录，或者当前记录更新，则更新
                        val existingItem = tvShowMap[seriesKey]
                        if (existingItem == null) {
                            tvShowMap[seriesKey] = item
                            android.util.Log.d("WatchHistoryService", "Added new TV show entry: $seriesKey -> ${item.title} 第${item.episodeNumber}集 (lastWatchedAt=${item.lastWatchedAt})")
                        } else {
                            // 比较 lastWatchedAt，只保留最新的
                            if (item.lastWatchedAt > existingItem.lastWatchedAt) {
                                tvShowMap[seriesKey] = item
                                android.util.Log.d("WatchHistoryService", "Updated TV show entry: $seriesKey -> ${item.title} 第${item.episodeNumber}集 (lastWatchedAt=${item.lastWatchedAt}, old=${existingItem.episodeNumber}, oldTime=${existingItem.lastWatchedAt})")
                            } else {
                                android.util.Log.d("WatchHistoryService", "Skipped older TV show entry: $seriesKey -> ${item.title} 第${item.episodeNumber}集 (lastWatchedAt=${item.lastWatchedAt} < ${existingItem.lastWatchedAt})")
                            }
                        }
                    } else {
                        // 电影直接添加
                        android.util.Log.d("WatchHistoryService", "Added movie item: ${item.title} (lastWatchedAt=${item.lastWatchedAt})")
                        items.add(item)
                    }
                }
                
                // 将去重后的连续剧添加到结果列表
                items.addAll(tvShowMap.values)
                
                // 按最后观看时间排序（最新的在前）
                items.sortByDescending { it.lastWatchedAt }
                
                // 限制返回数量
                val result = items.take(limit)
                
                // 更新缓存（限制缓存大小）
                cachedWatchHistory = items.take(MAX_CACHE_SIZE)
                cacheTimestamp = System.currentTimeMillis()
                android.util.Log.d("WatchHistoryService", "Cache updated with ${cachedWatchHistory?.size} items")
                
                android.util.Log.d("WatchHistoryService", "getRecentWatchHistory: Returning ${result.size} items")
                result.forEachIndexed { index, item ->
                    android.util.Log.d("WatchHistoryService", "  [$index] ${item.title}${if (item.episodeNumber != null) " 第${item.episodeNumber}集" else ""} (lastWatchedAt=${item.lastWatchedAt})")
                }
                emit(result)
            }
        }
    }

    /**
     * 获取全部观看记录累计播放时长（毫秒）
     */
    fun getTotalWatchTimeMs(): Flow<Long> = watchHistoryDao.getTotalWatchTimeMs()

    /**
     * 清除缓存（当观看历史变化时调用）
     */
    fun clearCache() {
        cachedWatchHistory = null
        cacheTimestamp = 0
        android.util.Log.d("WatchHistoryService", "Cache cleared")
    }

    /**
     * 保存观看进度
     */
    suspend fun saveWatchProgress(
        mediaId: String,
        episodeId: String? = null,
        progress: Long,
        duration: Long
    ) {
        try {
            android.util.Log.d("WatchHistoryService", "Saving watch progress: mediaId=$mediaId, episodeId=$episodeId, progress=$progress, duration=$duration")
            
            val existingHistory = try {
                watchHistoryDao.getWatchHistory(mediaId, episodeId)
            } catch (e: Exception) {
                android.util.Log.e("WatchHistoryService", "Error getting existing history", e)
                null
            }

            val historyEntity = if (existingHistory != null) {
                android.util.Log.d("WatchHistoryService", "Updating existing history: ${existingHistory.id}")
                existingHistory.copy(
                    progress = progress,
                    duration = duration,
                    lastWatchedAt = System.currentTimeMillis(),
                    watchCount = existingHistory.watchCount + 1
                )
            } else {
                val newId = generateHistoryId(mediaId, episodeId)
                android.util.Log.d("WatchHistoryService", "Creating new history: $newId")
                WatchHistoryEntity(
                    id = newId,
                    movieId = mediaId,
                    episodeId = episodeId,
                    progress = progress,
                    duration = duration,
                    lastWatchedAt = System.currentTimeMillis()
                )
            }

            try {
                watchHistoryDao.insertWatchHistory(historyEntity)
                android.util.Log.d("WatchHistoryService", "Watch history saved successfully: id=${historyEntity.id}")
                // 清除缓存，下次获取时会重新加载
                clearCache()
            } catch (e: Exception) {
                android.util.Log.e("WatchHistoryService", "Error inserting watch history", e)
                throw e
            }

            // 更新剧集观看进度
            episodeId?.let {
                try {
                    val isWatched = progress > duration * 0.9
                    episodeDao.updateWatchProgress(it, progress, isWatched)
                } catch (e: Exception) {
                    android.util.Log.e("WatchHistoryService", "Error updating episode progress", e)
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("WatchHistoryService", "Fatal error in saveWatchProgress", e)
            throw e
        }
    }

    /**
     * 获取媒体的上次观看位置
     */
    suspend fun getLastWatchPosition(mediaId: String, episodeId: String? = null): WatchPosition? {
        val history = if (episodeId != null) {
            watchHistoryDao.getWatchHistory(mediaId, episodeId)
        } else {
            watchHistoryDao.getLatestWatchHistoryByMovieId(mediaId)
        }

        return history?.let {
            WatchPosition(
                progress = it.progress,
                duration = it.duration,
                episodeId = it.episodeId
            )
        }
    }

    /**
     * 获取播放路径和标题信息（用于继续播放）
     */
    suspend fun getPlaybackInfo(mediaId: String, episodeId: String?): PlaybackInfo? {
        // 首先尝试从 MovieDao 获取
        val movie = movieDao.getMovieById(mediaId)
        
        // 如果找不到，尝试从 WebDavMediaDao 获取
        val webDavMedia = if (movie == null) {
            webDavMediaDao.getById(mediaId)
        } else null
        
        if (movie == null && webDavMedia == null) {
            return null
        }
        
        val videoPath: String?
        val title: String
        val episodeTitle: String?
        
        if (movie != null) {
            // 如果是电视剧且有剧集ID，获取剧集路径
            if (episodeId != null && movie.type.isLocalEpisodicSeries()) {
                val episode = episodeDao.getEpisodeById(episodeId)
                videoPath = episode?.quarkPath ?: movie.quarkPath
                title = movie.title
                episodeTitle = episode?.let {
                    val localSubtitle = it.title?.trim()?.takeIf { subtitle -> subtitle.isNotBlank() }
                    val tmdbSubtitle = if (localSubtitle == null) {
                        resolveTmdbEpisodeSubtitle(
                            tmdbId = movie.tmdbId?.toString(),
                            seasonNumber = it.seasonNumber,
                            episodeNumber = it.episodeNumber
                        )
                    } else {
                        null
                    }
                    buildEpisodeTitle(it.episodeNumber, localSubtitle ?: tmdbSubtitle)
                }
            } else {
                videoPath = movie.quarkPath
                title = movie.title
                episodeTitle = null
            }
        } else {
            // WebDAV 媒体
            // 对于 WebDAV 媒体，如果 episodeId 不为 null，说明是通过剧集导航切换的
            // 此时 episodeId 就是下一集的 ID，应该用 episodeId 来获取下一集的信息
            val targetMediaId = if (episodeId != null) {
                // 通过剧集导航时，episodeId 就是下一集的 mediaId
                episodeId
            } else {
                // 直接播放时，使用 mediaId
                mediaId
            }
            
            // 获取目标集数的 WebDAV 媒体信息
            val targetWebDavMedia: com.lomen.tv.data.local.database.entity.WebDavMediaEntity = if (targetMediaId != mediaId) {
                webDavMediaDao.getById(targetMediaId) ?: webDavMedia!!
            } else {
                webDavMedia!!
            }
            
            videoPath = targetWebDavMedia.filePath
            
            // 获取集数信息（优先从数据库，如果不存在则从文件名解析）
            var episodeNumber = targetWebDavMedia.episodeNumber
            var seasonNumber = targetWebDavMedia.seasonNumber ?: 1
            if (episodeNumber == null && targetWebDavMedia.fileName != null) {
                val parseResult = com.lomen.tv.utils.FileNameParser.parse(targetWebDavMedia.fileName)
                episodeNumber = parseResult.episode
                seasonNumber = parseResult.season ?: 1
            }
            
            // 对于 WebDAV 媒体，如果标题包含集数信息，需要提取基础标题
            val baseTitle = if (episodeNumber != null) {
                // 去除标题中的集数信息，例如："绿色星球 第1集" -> "绿色星球"
                targetWebDavMedia.title
                    .replace(Regex("第\\d+集.*"), "")
                    .replace(Regex("\\s+"), " ")
                    .trim()
            } else {
                targetWebDavMedia.title
            }
            title = baseTitle
            
            // 只使用本地数据，不调用网络请求
            episodeTitle = if (episodeNumber != null) {
                val localSubtitle = extractWebDavEpisodeSubtitle(targetWebDavMedia.title)
                val tmdbSubtitle = if (localSubtitle == null) {
                    resolveTmdbEpisodeSubtitle(
                        tmdbId = targetWebDavMedia.tmdbId,
                        seasonNumber = seasonNumber,
                        episodeNumber = episodeNumber
                    )
                } else {
                    null
                }
                buildEpisodeTitle(episodeNumber, localSubtitle ?: tmdbSubtitle)
            } else null
            
            android.util.Log.d("WatchHistoryService", "getPlaybackInfo for WebDAV: mediaId=$mediaId, episodeId=$episodeId, targetMediaId=$targetMediaId, episodeNumber=$episodeNumber, seasonNumber=$seasonNumber, title=$title, episodeTitle=$episodeTitle")
        }
        
        // 获取播放位置
        val position = getLastWatchPosition(mediaId, episodeId)
        
        return PlaybackInfo(
            videoPath = videoPath,
            title = title,
            episodeTitle = episodeTitle,
            mediaId = mediaId,
            episodeId = episodeId,
            startPosition = position?.progress ?: 0
        )
    }

    /**
     * 删除观看记录
     */
    suspend fun deleteWatchHistory(historyId: String) {
        watchHistoryDao.deleteWatchHistory(historyId)
    }

    /**
     * 清除所有观看记录
     */
    suspend fun clearAllWatchHistory() {
        watchHistoryDao.clearAllWatchHistory()
        clearCache()
        android.util.Log.d("WatchHistoryService", "All watch history cleared")
    }

    /**
     * 获取媒体的观看进度百分比
     */
    suspend fun getWatchProgressPercent(mediaId: String): Int {
        val history = watchHistoryDao.getLatestWatchHistoryByMovieId(mediaId) ?: return 0
        return if (history.duration > 0) {
            (history.progress * 100 / history.duration).toInt()
        } else 0
    }

    /**
     * 检查是否已看完
     */
    suspend fun isWatched(mediaId: String, episodeId: String? = null): Boolean {
        val history = watchHistoryDao.getWatchHistory(mediaId, episodeId) ?: return false
        return history.progress > history.duration * 0.9
    }

    /**
     * 继续观看 - 获取应该播放的下一集
     */
    suspend fun getContinueWatching(mediaId: String): ContinueWatching? {
        val movie = movieDao.getMovieById(mediaId) ?: return null

        return if (movie.type == com.lomen.tv.domain.model.MediaType.TV_SHOW) {
            // 电影直接返回
            val history = watchHistoryDao.getLatestWatchHistoryByMovieId(mediaId)
            ContinueWatching(
                mediaId = mediaId,
                episodeId = null,
                episodeNumber = null,
                seasonNumber = null,
                title = movie.title,
                progress = history?.progress ?: 0,
                isNewEpisode = history == null
            )
        } else {
            // 电影直接返回
            val history = watchHistoryDao.getLatestWatchHistoryByMovieId(mediaId)
            ContinueWatching(
                mediaId = mediaId,
                episodeId = null,
                episodeNumber = null,
                seasonNumber = null,
                title = movie.title,
                progress = history?.progress ?: 0,
                isNewEpisode = history == null
            )
        }
    }

    private fun generateHistoryId(mediaId: String, episodeId: String?): String {
        return if (episodeId != null) {
            "history_${mediaId}_${episodeId}"
        } else {
            "history_${mediaId}"
        }
    }

    private fun buildEpisodeTitle(episodeNumber: Int, subtitle: String?): String {
        val normalizedSubtitle = subtitle
            ?.trim()
            ?.takeIf { it.isNotBlank() }
        return if (normalizedSubtitle != null) {
            "第${episodeNumber}集 $normalizedSubtitle"
        } else {
            "第${episodeNumber}集"
        }
    }

    private fun extractWebDavEpisodeSubtitle(mediaTitle: String): String? {
        if (mediaTitle.isBlank()) return null
        val match = Regex("第\\d+集\\s*(.+)$").find(mediaTitle) ?: return null
        val subtitle = match.groupValues.getOrNull(1)?.trim()
        return subtitle?.takeIf { it.isNotBlank() }
    }

    private suspend fun resolveTmdbEpisodeSubtitle(
        tmdbId: String?,
        seasonNumber: Int?,
        episodeNumber: Int?
    ): String? {
        if (tmdbId.isNullOrBlank() || seasonNumber == null || episodeNumber == null) return null
        return runCatching {
            tmdbScraper
                .getTvSeasonEpisodes(tmdbId, seasonNumber)
                ?.firstOrNull { it.episodeNumber == episodeNumber }
                ?.name
                ?.trim()
                ?.takeIf { it.isNotBlank() }
        }.getOrNull()
    }
}

// 数据类
data class WatchHistoryItem(
    val id: String,
    val mediaId: String,
    val title: String,
    val posterUrl: String?,
    val backdropUrl: String?,
    val episodeTitle: String?,
    val episodeNumber: Int?,
    val seasonNumber: Int?,
    val episodeId: String?,
    val progress: Long,
    val duration: Long,
    val progressPercent: Int,
    val lastWatchedAt: Long,
    val isWatched: Boolean
)

data class WatchPosition(
    val progress: Long,
    val duration: Long,
    val episodeId: String?
)

data class ContinueWatching(
    val mediaId: String,
    val episodeId: String?,
    val episodeNumber: Int?,
    val seasonNumber: Int?,
    val title: String,
    val progress: Long,
    val isNewEpisode: Boolean
)

data class PlaybackInfo(
    val videoPath: String?,
    val title: String,
    val episodeTitle: String?,
    val mediaId: String,
    val episodeId: String?,
    val startPosition: Long
)

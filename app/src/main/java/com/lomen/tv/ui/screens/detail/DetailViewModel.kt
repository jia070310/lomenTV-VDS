package com.lomen.tv.ui.screens.detail

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lomen.tv.data.local.database.dao.EpisodeDao
import com.lomen.tv.data.local.database.dao.MovieDao
import com.lomen.tv.data.local.database.dao.WebDavMediaDao
import com.lomen.tv.data.local.database.dao.WatchHistoryDao
import com.lomen.tv.data.local.database.entity.WatchHistoryEntity
import com.lomen.tv.data.local.database.entity.WebDavMediaEntity
import com.lomen.tv.data.scraper.DoubanScraper
import com.lomen.tv.data.scraper.FolderSeriesNameParser
import com.lomen.tv.data.scraper.TmdbScraper
import com.lomen.tv.domain.model.MediaType
import com.lomen.tv.domain.model.isLocalEpisodicSeries
import com.lomen.tv.domain.service.MetadataService
import com.lomen.tv.utils.FileNameParser
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject

@HiltViewModel
class DetailViewModel @Inject constructor(
    private val movieDao: MovieDao,
    private val episodeDao: EpisodeDao,
    private val webDavMediaDao: WebDavMediaDao,
    private val watchHistoryDao: WatchHistoryDao,
    private val metadataService: MetadataService
) : ViewModel() {

    private val tmdbScraper = TmdbScraper.getInstance()
    private val doubanScraper = DoubanScraper()  // 添加豆瓣Scraper

    companion object {
        private const val TAG = "DetailViewModel"

        /** 综艺详情页标题用节目名（季在 Hero 区切换）；与首页合并卡片标题一致 */
        fun varietyDisplayTitleForDetail(media: WebDavMediaEntity): String {
            if (media.type != MediaType.VARIETY && media.type != MediaType.DOCUMENTARY) return media.title
            return FolderSeriesNameParser.stripAllSeasonMarkers(media.title.trim())
                .ifBlank { media.title.trim() }
        }

        /** 同系列搜索：用剥离季标记后的剧名，避免库内是「圆桌派」而当前条是「圆桌派 第八季」时搜不到 */
        fun titleForSeriesSearch(media: WebDavMediaEntity): String {
            if (media.type != MediaType.VARIETY &&
                media.type != MediaType.TV_SHOW &&
                media.type != MediaType.ANIME &&
                media.type != MediaType.DOCUMENTARY
            ) {
                return media.title
            }
            return FolderSeriesNameParser.stripAllSeasonMarkers(media.title.trim())
                .ifBlank { media.title.trim() }
        }
    }

    private val _uiState = MutableStateFlow<DetailUiState>(DetailUiState.Loading)
    val uiState: StateFlow<DetailUiState> = _uiState
    
    // 当前选中的季数
    private val _selectedSeason = MutableStateFlow(1)
    val selectedSeason: StateFlow<Int> = _selectedSeason
    
    // 可用的季数列表
    private val _availableSeasons = MutableStateFlow<List<Int>>(listOf(1))
    val availableSeasons: StateFlow<List<Int>> = _availableSeasons
    
    // 所有剧集按季分组
    private var allEpisodesBySeason: Map<Int, List<EpisodeItem>> = emptyMap()
    
    // 当前媒体 ID
    private var currentMediaId: String? = null
    
    /**
     * 切换选中的季数
     */
    fun selectSeason(season: Int) {
        Log.d(TAG, "选择季数: $season")
        _selectedSeason.value = season
        
        // 更新UI状态中的剧集列表
        val currentState = _uiState.value
        if (currentState is DetailUiState.Success) {
            val filteredEpisodes = allEpisodesBySeason[season] ?: emptyList()
            Log.d(TAG, "筛选后的剧集数量: ${filteredEpisodes.size}")
            _uiState.value = currentState.copy(
                episodes = filteredEpisodes,
                media = currentState.media.copy(seasonCount = season)
            )
        }
    }

    fun loadMediaDetail(mediaId: String) {
        viewModelScope.launch {
            _uiState.value = DetailUiState.Loading
            currentMediaId = null

            try {
                // 首先尝试从WebDavMediaDao获取数据
                var webDavMedia = webDavMediaDao.getById(mediaId)
                
                // 如果通过id获取不到数据，尝试通过标题搜索
                if (webDavMedia == null) {
                    val searchResults = webDavMediaDao.search(mediaId)
                    if (searchResults.isNotEmpty()) {
                        webDavMedia = searchResults.first()
                    }
                }
                
                if (webDavMedia != null) {
                    // 记录真实媒体ID（避免把标题等非ID写入观看历史）
                    currentMediaId = webDavMedia.id
                    // 获取TMDB总集数（如果有tmdbId） - 使用协程异步获取
                    var totalEpisodes: Int? = null
                    val tmdbIdStr = webDavMedia.tmdbId
                    val isWebDavEpisodic = webDavMedia.type == MediaType.TV_SHOW ||
                        webDavMedia.type == MediaType.VARIETY ||
                        webDavMedia.type == MediaType.ANIME ||
                        webDavMedia.type == MediaType.DOCUMENTARY
                    if (isWebDavEpisodic && tmdbIdStr != null) {
                        totalEpisodes = withContext(Dispatchers.IO) {
                            try {
                                val tmdbIdInt = tmdbIdStr.toIntOrNull()
                                if (tmdbIdInt != null) {
                                    Log.d(TAG, "\u5f00\u59cb\u83b7\u53d6TMDB\u603b\u96c6\u6570: tmdbId=$tmdbIdInt")
                                    // \u4f7f\u7528\u65e7\u7248API\u83b7\u53d6TV\u8be6\u60c5
                                    val url = java.net.URL("https://api.tmdb.org/3/tv/$tmdbIdInt?api_key=cd1660fdecd8066874f593beab890967&language=zh-CN")
                                    val connection = url.openConnection() as java.net.HttpURLConnection
                                    connection.requestMethod = "GET"
                                    connection.setRequestProperty("Accept", "application/json")
                                    connection.connectTimeout = 10000  // \u589e\u52a0\u523010\u79d2
                                    connection.readTimeout = 10000
                                                    
                                    val responseCode = connection.responseCode
                                    Log.d(TAG, "TMDB\u603b\u96c6\u6570\u8bf7\u6c42\u54cd\u5e94: HTTP $responseCode")
                                    if (responseCode in 200..299) {
                                        val responseText = connection.inputStream.bufferedReader().use { it.readText() }
                                        connection.disconnect()
                                        Log.d(TAG, "TMDB\u54cd\u5e94\u524d100\u5b57\u7b26: ${responseText.take(100)}")
                                        val jsonObj = org.json.JSONObject(responseText)
                                        val episodes = jsonObj.optInt("number_of_episodes", 0).takeIf { it > 0 }
                                        Log.d(TAG, "TMDB\u603b\u96c6\u6570: $episodes")
                                        episodes
                                    } else {
                                        Log.w(TAG, "TMDB\u603b\u96c6\u6570\u8bf7\u6c42\u5931\u8d25: HTTP $responseCode")
                                        null
                                    }
                                } else {
                                    null
                                }
                            } catch (e: Exception) {
                                Log.w(TAG, "\u83b7\u53d6TMDB\u603b\u96c6\u6570\u5931\u8d25: ${e.message}", e)
                                null
                            }
                        }
                    }
                    
                    // 转换为UI模型（综艺详情标题与首页卡片一致，不直接用 TMDB 里可能错误的「第八季」）
                    val displayTitle = varietyDisplayTitleForDetail(webDavMedia)
                    val mediaDetail = MediaDetail(
                        id = webDavMedia.id,
                        title = displayTitle,
                        originalTitle = webDavMedia.originalTitle,
                        overview = webDavMedia.overview,
                        posterUrl = webDavMedia.posterUrl,
                        backdropUrl = webDavMedia.backdropUrl,
                        rating = webDavMedia.rating,
                        year = webDavMedia.year?.toString(),
                        genres = webDavMedia.genres?.split(",")?.map { it.trim() } ?: emptyList(),
                        type = webDavMedia.type,
                        seasonCount = webDavMedia.seasonNumber ?: 1, // 使用从文件名中提取的真实季数
                        totalEpisodes = totalEpisodes,  // 添加总集数
                        path = webDavMedia.filePath
                    )

                    // 对于电视剧，获取同系列的其他剧集
                    val episodes: List<EpisodeItem>
                    val seasons: List<Int>
                    val initialSeason: Int
                    
                    if (isWebDavEpisodic) {
                        // 按剥离季标记后的剧名搜索同系列（综艺/纪录片多季合并详情，在页内切换季）
                        val rawSeries = webDavMediaDao.search(titleForSeriesSearch(webDavMedia))
                        val seriesEpisodes = rawSeries.filter {
                            it.libraryId == webDavMedia.libraryId && it.type == webDavMedia.type
                        }
                        Log.d(TAG, "找到同系列剧集: ${seriesEpisodes.size} 个")
                        
                        // 获取TMDB剧集详情（如果有tmdbId）
                        val tmdbEpisodesMap = mutableMapOf<Pair<Int, Int>, com.lomen.tv.data.scraper.EpisodeInfo>()
                        val tmdbId = webDavMedia.tmdbId
                        if (tmdbId != null) {
                            // 获取所有涉及的季的剧集信息
                            val involvedSeasons = seriesEpisodes.mapNotNull { it.seasonNumber }.distinct()
                            for (season in involvedSeasons) {
                                try {
                                    val tmdbEpisodes = tmdbScraper.getTvSeasonEpisodes(tmdbId, season)
                                    tmdbEpisodes?.forEach { ep ->
                                        tmdbEpisodesMap[Pair(season, ep.episodeNumber)] = ep
                                    }
                                } catch (e: Exception) {
                                    // 静默失败，不影响加载速度
                                }
                            }
                        }
                        
                        // 将所有剧集转换为EpisodeItem，并使用TMDB信息丰富
                        val allEpisodes = seriesEpisodes.map { episode ->
                            val parsed = episode.fileName?.let { FileNameParser.parse(it) }
                            val parsedEp = parsed?.episode
                            // 库内集号常误为 1：综艺/本地命名优先采用文件名解析的集号
                            val episodeNumber = parsedEp ?: episode.episodeNumber ?: 1
                            val seasonNumber = episode.seasonNumber ?: 1
                            val tmdbEpisode = tmdbEpisodesMap[Pair(seasonNumber, episodeNumber)]
                            
                            // 优先使用TMDB的副标题，否则用文件名
                            val episodeTitle = tmdbEpisode?.name?.takeIf { it.isNotBlank() }
                                ?: episode.fileName.substringBeforeLast('.')
                            
                            // 优先使用TMDB的剧照，否则用主封面
                            val stillUrl = tmdbEpisode?.stillUrl ?: episode.posterUrl ?: webDavMedia.posterUrl
                            
                            // 时长（分钟转毫秒）
                            val duration = (tmdbEpisode?.runtime ?: 0) * 60 * 1000L
                            
                            EpisodeItem(
                                id = episode.id,
                                episodeNumber = episodeNumber,
                                seasonNumber = seasonNumber,
                                title = episodeTitle,
                                stillUrl = stillUrl,
                                progress = 0,
                                duration = duration,
                                isWatched = false,
                                path = episode.filePath
                            )
                        }
                        
                        // 按季分组
                        allEpisodesBySeason = allEpisodes.groupBy { it.seasonNumber }
                        Log.d(TAG, "按季分组: ${allEpisodesBySeason.keys}")
                        
                        // 获取可用季数列表并排序
                        seasons = allEpisodesBySeason.keys.sorted()
                        _availableSeasons.value = seasons
                        
                        // 设置初始选中的季数（当前剧集的季数，或第一季）
                        initialSeason = webDavMedia.seasonNumber ?: seasons.firstOrNull() ?: 1
                        _selectedSeason.value = initialSeason
                        
                        // 筛选当前季的剧集
                        episodes = allEpisodesBySeason[initialSeason] ?: emptyList()
                        Log.d(TAG, "当前季 $initialSeason 的剧集数量：${episodes.size}")
                                                                
                        // 加载观看历史，更新剧集进度信息
                        updateEpisodesWithWatchHistory(episodes, webDavMedia.id)
                    } else {
                        episodes = emptyList()
                        seasons = emptyList()
                        initialSeason = 1
                    }
                    
                    // 先快速显示页面，演职人员稍后加载
                    _uiState.value = DetailUiState.Success(
                        media = mediaDetail,
                        episodes = episodes,
                        cast = emptyList()
                    )
                    
                    // 后台异步加载演职人员（使用TMDB API，增加超时和重试）
                    viewModelScope.launch {
                        val tmdbIdStr = webDavMedia.tmdbId
                        Log.d(TAG, "[Cast] 开始加载演职人员: tmdbId=$tmdbIdStr")
                        val cast = if (tmdbIdStr != null) {
                            try {
                                withTimeout(30000L) {  // 增加到30秒超时
                                    val tmdbIdInt = tmdbIdStr.toIntOrNull()
                                    if (tmdbIdInt != null) {
                                        Log.d(TAG, "[Cast] 调用TMDB getCastAndCrew: tmdbId=$tmdbIdInt")
                                        val result = metadataService.getCastAndCrew(
                                            tmdbId = tmdbIdInt,
                                            type = webDavMedia.type,
                                            actorLimit = 10,
                                            directorLimit = 5
                                        ).map { castMember ->
                                            CastItem(
                                                id = castMember.id,
                                                name = castMember.name,
                                                role = castMember.role,
                                                profileUrl = castMember.profileUrl
                                            )
                                        }
                                        Log.d(TAG, "[Cast] 成功加载 ${result.size} 位演职人员")
                                        result
                                    } else {
                                        Log.w(TAG, "[Cast] tmdbId格式错误: $tmdbIdStr")
                                        emptyList()
                                    }
                                }
                            } catch (e: TimeoutCancellationException) {
                                Log.w(TAG, "[Cast] 加载超时（30秒），请检查网络")
                                emptyList()
                            } catch (e: Exception) {
                                Log.e(TAG, "[Cast] 加载失败: ${e.message}", e)
                                emptyList()
                            }
                        } else {
                            Log.w(TAG, "[Cast] tmdbId为null")
                            emptyList()
                        }
                        
                        // 更新UI添加演职人员
                        val currentState = _uiState.value
                        if (currentState is DetailUiState.Success) {
                            if (cast.isNotEmpty()) {
                                Log.d(TAG, "[Cast] 更新UI，显示 ${cast.size} 位演职人员")
                                _uiState.value = currentState.copy(cast = cast)
                            } else {
                                Log.d(TAG, "[Cast] 演职人员为空，不更新UI")
                            }
                        }
                    }
                    return@launch
                }

                // 如果WebDavMediaDao中没有数据，尝试从MovieDao获取
                val movieEntity = movieDao.getMovieById(mediaId)

                // 如果数据库中没有数据，显示错误信息
                if (movieEntity == null) {
                    _uiState.value = DetailUiState.Error("未找到媒体信息")
                    return@launch
                }
                
                // MovieEntity 场景下，使用真实 movieId
                currentMediaId = movieEntity.id

                // 转换为UI模型
                val mediaDetail = MediaDetail(
                    id = movieEntity.id,
                    title = movieEntity.title,
                    originalTitle = movieEntity.originalTitle,
                    overview = movieEntity.overview,
                    posterUrl = movieEntity.posterUrl,
                    backdropUrl = movieEntity.backdropUrl,
                    rating = movieEntity.rating,
                    year = movieEntity.releaseDate?.take(4),
                    genres = movieEntity.genre?.split(",")?.map { it.trim() } ?: emptyList(),
                    type = movieEntity.type,
                    seasonCount = movieEntity.seasonCount,
                    totalEpisodes = null,  // MovieEntity路径暂不支持
                    path = movieEntity.quarkPath
                )

                // 获取剧集列表（电视剧 / 综艺 / 动漫 / 纪录片等多集类型）
                val episodes = if (movieEntity.type.isLocalEpisodicSeries()) {
                    episodeDao.getEpisodesByMovieId(mediaId).first().map { entity ->
                        EpisodeItem(
                            id = entity.id,
                            episodeNumber = entity.episodeNumber,
                            seasonNumber = entity.seasonNumber,
                            title = entity.title,
                            stillUrl = entity.stillUrl,
                            progress = entity.watchProgress,
                            duration = entity.duration ?: 0,
                            isWatched = entity.isWatched,
                            path = entity.quarkPath
                        )
                    }
                } else {
                    emptyList()
                }
                
                // 多集类型，按季分组
                if (movieEntity.type.isLocalEpisodicSeries() && episodes.isNotEmpty()) {
                    allEpisodesBySeason = episodes.groupBy { it.seasonNumber }
                    val seasons = allEpisodesBySeason.keys.sorted()
                    _availableSeasons.value = seasons
                    _selectedSeason.value = seasons.firstOrNull() ?: 1
                }

                // 获取演职人员列表（导演+演员）
                Log.d(TAG, "开始获取演职人员: tmdbId=${movieEntity.tmdbId}, type=${movieEntity.type}")
                val cast = if (movieEntity.tmdbId != null) {
                    Log.d(TAG, "调用getCastAndCrew: tmdbId=${movieEntity.tmdbId}")
                    metadataService.getCastAndCrew(
                        tmdbId = movieEntity.tmdbId,
                        type = movieEntity.type,
                        actorLimit = 10,
                        directorLimit = 5
                    ).map { castMember ->
                        CastItem(
                            id = castMember.id,
                            name = castMember.name,
                            role = castMember.role,
                            profileUrl = castMember.profileUrl
                        )
                    }
                } else {
                    Log.w(TAG, "tmdbId为null，无法获取演职人员")
                    emptyList()
                }
                Log.d(TAG, "演职人员获取完成: 总计 ${cast.size} 位")

                _uiState.value = DetailUiState.Success(
                    media = mediaDetail,
                    episodes = episodes,
                    cast = cast
                )

            } catch (e: Exception) {
                _uiState.value = DetailUiState.Error(e.message ?: "加载失败")
            }
        }
    }

    /**
     * 根据观看历史更新剧集进度信息
     */
    private suspend fun updateEpisodesWithWatchHistory(episodes: List<EpisodeItem>, mediaId: String) {
        // 对于电视剧，需要获取同系列的所有剧集的观看历史
        val currentState = _uiState.value
        if (currentState !is DetailUiState.Success) return
        
        val media = currentState.media
        val episodic = media.type == MediaType.TV_SHOW ||
            media.type == MediaType.VARIETY ||
            media.type == MediaType.ANIME ||
            media.type == MediaType.DOCUMENTARY
        if (!episodic) return
        
        // 获取同系列的所有剧集 ID
        val allEpisodeIds = mutableListOf<String>()
        for ((_, episodeList) in allEpisodesBySeason) {
            allEpisodeIds.addAll(episodeList.map { it.id })
        }
        
        // 获取这些剧集的观看历史
        val updatedEpisodes = episodes.map { episode ->
            val history = watchHistoryDao.getWatchHistory(mediaId, episode.id)
            if (history != null) {
                episode.copy(
                    progress = history.progress,
                    duration = history.duration,
                    isWatched = history.progress > history.duration * 0.9
                )
            } else {
                episode
            }
        }
        
        // 更新 UI 状态中的剧集列表
        _uiState.value = (currentState as DetailUiState.Success).copy(
            episodes = updatedEpisodes
        )
    }
    
    /**
     * 获取应该播放的起始位置（用于立即播放按钮）
     */
    suspend fun getResumePlaybackInfo(): ResumePlaybackInfo? {
        val mediaId = currentMediaId ?: return null
        val currentState = _uiState.value as? DetailUiState.Success ?: return null
        val media = currentState.media
        
        if (media.type == MediaType.MOVIE) {
            // 电影：直接返回电影本身的播放历史
            val history = watchHistoryDao.getLatestWatchHistoryByMovieId(mediaId)
            return if (history != null && history.progress > 0) {
                ResumePlaybackInfo(
                    videoUrl = media.path ?: "",
                    title = media.title,
                    episodeTitle = null,
                    mediaId = mediaId,
                    episodeId = null,
                    startPosition = history.progress
                )
            } else {
                // 没有观看历史，从头开始播放
                ResumePlaybackInfo(
                    videoUrl = media.path ?: "",
                    title = media.title,
                    episodeTitle = null,
                    mediaId = mediaId,
                    episodeId = null,
                    startPosition = 0
                )
            }
        } else {
            // 电视剧 / 综艺 / 动漫 / 纪录片：查找最近观看的剧集
            // 获取所有剧集
            val allEpisodes = allEpisodesBySeason.values.flatten()
            if (allEpisodes.isEmpty()) {
                // 未合并到分集表时（或仅单文件）：按当前条目路径播放，与电影一致
                val path = media.path?.takeIf { it.isNotBlank() } ?: return null
                val history = watchHistoryDao.getLatestWatchHistoryByMovieId(mediaId)
                val start = if (history != null && history.progress > 0) history.progress else 0L
                return ResumePlaybackInfo(
                    videoUrl = path,
                    title = media.title,
                    episodeTitle = null,
                    mediaId = mediaId,
                    episodeId = null,
                    startPosition = start
                )
            }
            
            // 查找有观看历史的剧集中最新的一集
            var latestHistory: WatchHistoryEntity? = null
            var targetEpisode: EpisodeItem? = null
            
            for (episode in allEpisodes) {
                val history = watchHistoryDao.getWatchHistory(mediaId, episode.id)
                if (history != null) {
                    if (latestHistory == null || history.lastWatchedAt > latestHistory.lastWatchedAt) {
                        latestHistory = history
                        targetEpisode = episode
                    }
                }
            }
            
            return if (targetEpisode != null && latestHistory != null) {
                // 找到了有观看历史的剧集，继续观看
                ResumePlaybackInfo(
                    videoUrl = targetEpisode.path ?: "",
                    title = media.title,
                    episodeTitle = "第${targetEpisode.episodeNumber}集 ${targetEpisode.title ?: ""}",
                    mediaId = mediaId,
                    episodeId = targetEpisode.id,
                    startPosition = latestHistory.progress
                )
            } else {
                // 没有任何观看历史，播放第一集
                val firstEpisode = allEpisodes.minByOrNull { it.episodeNumber }
                if (firstEpisode != null) {
                    ResumePlaybackInfo(
                        videoUrl = firstEpisode.path ?: "",
                        title = media.title,
                        episodeTitle = "第${firstEpisode.episodeNumber}集 ${firstEpisode.title ?: ""}",
                        mediaId = mediaId,
                        episodeId = firstEpisode.id,
                        startPosition = 0
                    )
                } else {
                    null
                }
            }
        }
    }
    
    /**
     * 获取指定剧集的播放信息（包含观看历史）
     */
    suspend fun getEpisodePlaybackInfo(episodeId: String): EpisodePlaybackInfo? {
        val mediaId = currentMediaId ?: return null
        val currentState = _uiState.value as? DetailUiState.Success ?: return null
        
        // 找到对应的剧集
        val episode = allEpisodesBySeason.values.flatten().find { it.id == episodeId }
            ?: return null
        
        // 获取观看历史
        val history = watchHistoryDao.getWatchHistory(mediaId, episodeId)
        
        return EpisodePlaybackInfo(
            videoUrl = episode.path ?: "",
            title = currentState.media.title,
            episodeTitle = "第${episode.episodeNumber}集 ${episode.title ?: ""}",
            mediaId = mediaId,
            episodeId = episodeId,
            startPosition = history?.progress ?: 0
        )
    }

    fun refreshMetadata(mediaId: String) {
        viewModelScope.launch {
            try {
                val movieEntity = movieDao.getMovieById(mediaId) ?: return@launch

                // 如果TMDb ID存在，刷新元数据
                movieEntity.tmdbId?.let { tmdbId ->
                    val result = metadataService.getDetailsByTmdbId(tmdbId, movieEntity.type)

                    result.onSuccess { mediaItem ->
                        // 更新数据库
                        val updatedEntity = movieEntity.copy(
                            title = mediaItem.title,
                            originalTitle = mediaItem.originalTitle,
                            overview = mediaItem.overview,
                            posterUrl = mediaItem.posterUrl,
                            backdropUrl = mediaItem.backdropUrl,
                            rating = mediaItem.rating,
                            genre = mediaItem.genres.joinToString(","),
                            updatedAt = System.currentTimeMillis()
                        )
                        movieDao.updateMovie(updatedEntity)

                        // 重新加载
                        loadMediaDetail(mediaId)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}

// 数据类：用于立即播放按钮的播放信息
data class ResumePlaybackInfo(
    val videoUrl: String,
    val title: String,
    val episodeTitle: String?,
    val mediaId: String,
    val episodeId: String?,
    val startPosition: Long
)

// 数据类：用于指定剧集播放的信息
data class EpisodePlaybackInfo(
    val videoUrl: String,
    val title: String,
    val episodeTitle: String?,
    val mediaId: String,
    val episodeId: String?,
    val startPosition: Long
)

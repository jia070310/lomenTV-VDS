package com.lomen.tv.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lomen.tv.data.local.database.dao.WebDavMediaDao
import com.lomen.tv.data.local.database.entity.WebDavMediaEntity
import com.lomen.tv.data.repository.ResourceLibraryRepository
import com.lomen.tv.domain.model.ResourceLibrary
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

// 剧集系列数据类（合并同一部剧的集数）
data class TvShowSeries(
    val id: String,  // 使用剧集名称作为ID
    val title: String,
    val posterUrl: String?,
    val year: Int?,
    val rating: Float?,
    val overview: String?,
    val episodes: List<WebDavMediaEntity>,  // 该剧的所有集数
    val episodeCount: Int
)

@HiltViewModel
class ResourceLibraryViewModel @Inject constructor(
    private val webDavMediaDao: WebDavMediaDao,
    private val repository: ResourceLibraryRepository,
    private val mediaTypeSortPreferences: com.lomen.tv.data.preferences.MediaTypeSortPreferences,
    private val watchHistoryService: com.lomen.tv.domain.service.WatchHistoryService
) : ViewModel() {

    companion object {
        private const val TAG = "ResourceLibraryVM"
    }

    private val _currentLibraryMedia = MutableStateFlow<List<WebDavMediaEntity>>(emptyList())
    val currentLibraryMedia: StateFlow<List<WebDavMediaEntity>> = _currentLibraryMedia.asStateFlow()

    private val _movies = MutableStateFlow<List<WebDavMediaEntity>>(emptyList())
    val movies: StateFlow<List<WebDavMediaEntity>> = _movies.asStateFlow()

    private val _tvShows = MutableStateFlow<List<TvShowSeries>>(emptyList())
    val tvShows: StateFlow<List<TvShowSeries>> = _tvShows.asStateFlow()

    private val _concerts = MutableStateFlow<List<WebDavMediaEntity>>(emptyList())
    val concerts: StateFlow<List<WebDavMediaEntity>> = _concerts.asStateFlow()

    private val _variety = MutableStateFlow<List<WebDavMediaEntity>>(emptyList())
    val variety: StateFlow<List<WebDavMediaEntity>> = _variety.asStateFlow()

    private val _documentaries = MutableStateFlow<List<WebDavMediaEntity>>(emptyList())
    val documentaries: StateFlow<List<WebDavMediaEntity>> = _documentaries.asStateFlow()

    private val _others = MutableStateFlow<List<WebDavMediaEntity>>(emptyList())
    val others: StateFlow<List<WebDavMediaEntity>> = _others.asStateFlow()

    // 媒体类型排序顺序（从DataStore读取）
    val mediaSortOrder: StateFlow<List<com.lomen.tv.domain.model.MediaType>> = 
        mediaTypeSortPreferences.sortOrder.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = com.lomen.tv.data.preferences.MediaTypeSortPreferences.DEFAULT_SORT_ORDER
        )

    private val _resourceLibraries = MutableStateFlow<List<ResourceLibrary>>(emptyList())
    val resourceLibraries: StateFlow<List<ResourceLibrary>> = _resourceLibraries.asStateFlow()

    private val _currentLibraryId = MutableStateFlow<String?>(null)
    val currentLibraryId: StateFlow<String?> = _currentLibraryId.asStateFlow()

    init {
        Log.d(TAG, "ViewModel initialized")
        
        // 从DataStore加载资源库列表
        viewModelScope.launch {
            repository.libraries.collect { libraries ->
                Log.d(TAG, "Loaded ${libraries.size} libraries from DataStore")
                _resourceLibraries.value = libraries
            }
        }

        // 从DataStore加载当前选中的资源库ID
        viewModelScope.launch {
            repository.currentLibraryId.collect { id ->
                Log.d(TAG, "Current library ID from DataStore: $id")
                _currentLibraryId.value = id
            }
        }

        // 监听当前资源库的媒体变化
        viewModelScope.launch {
            currentLibraryId.collect { libraryId ->
                Log.d(TAG, "Library ID changed: $libraryId")
                if (libraryId != null) {
                    // 加载该资源库的所有媒体
                    launch {
                        Log.d(TAG, "Loading media for library: $libraryId")
                        webDavMediaDao.getByLibraryId(libraryId).collect { media ->
                            Log.d(TAG, "Loaded ${media.size} media items from database")
                            _currentLibraryMedia.value = media
                            
                            // 不做去重，直接使用原始媒体数据进行分类
                            // 剧集应该保留所有集数，不能去重
                            val categorized = media.map { mediaItem ->
                                val titleLower = mediaItem.title.lowercase()
                                val genresLower = (mediaItem.genres ?: "").lowercase()
                                val hasEpisode = mediaItem.seasonNumber != null || mediaItem.episodeNumber != null

                                val category = when {
                                    // 有季/集信息：优先视为TV相关
                                    hasEpisode || mediaItem.type == com.lomen.tv.domain.model.MediaType.TV_SHOW -> {
                                        when {
                                            // 综艺/真人秀
                                            titleLower.contains("综艺") ||
                                                titleLower.contains("真人秀") ||
                                                titleLower.contains("脱口秀") ||
                                                titleLower.contains("选秀") ||
                                                genresLower.contains("variety") ||
                                                genresLower.contains("reality") -> {
                                                com.lomen.tv.domain.model.MediaType.VARIETY
                                            }
                                            // 纪录片剧集
                                            titleLower.contains("纪录片") ||
                                                titleLower.contains("纪录") ||
                                                genresLower.contains("documentary") -> {
                                                com.lomen.tv.domain.model.MediaType.DOCUMENTARY
                                            }
                                            else -> com.lomen.tv.domain.model.MediaType.TV_SHOW
                                        }
                                    }
                                    // 无季/集信息：电影/演唱会/纪录片电影
                                    else -> {
                                        when {
                                            // 演唱会 / 音乐会
                                            titleLower.contains("演唱会") ||
                                                titleLower.contains("巡回演唱") ||
                                                (genresLower.contains("music") &&
                                                    (titleLower.contains("live") || titleLower.contains("concert"))) -> {
                                                com.lomen.tv.domain.model.MediaType.CONCERT
                                            }
                                            // 纪录片电影
                                            titleLower.contains("纪录片") ||
                                                titleLower.contains("纪录") ||
                                                genresLower.contains("documentary") -> {
                                                com.lomen.tv.domain.model.MediaType.DOCUMENTARY
                                            }
                                            // 普通电影
                                            mediaItem.type == com.lomen.tv.domain.model.MediaType.MOVIE -> {
                                                com.lomen.tv.domain.model.MediaType.MOVIE
                                            }
                                            else -> com.lomen.tv.domain.model.MediaType.OTHER
                                        }
                                    }
                                }

                                mediaItem to category
                            }

                            // 按分类填充不同列表
                            _movies.value = categorized
                                .filter { it.second == com.lomen.tv.domain.model.MediaType.MOVIE }
                                .map { it.first }
                            _variety.value = categorized
                                .filter { it.second == com.lomen.tv.domain.model.MediaType.VARIETY }
                                .map { it.first }
                            _concerts.value = categorized
                                .filter { it.second == com.lomen.tv.domain.model.MediaType.CONCERT }
                                .map { it.first }
                            _documentaries.value = categorized
                                .filter { it.second == com.lomen.tv.domain.model.MediaType.DOCUMENTARY }
                                .map { it.first }
                            _others.value = categorized
                                .filter { it.second == com.lomen.tv.domain.model.MediaType.OTHER }
                                .map { it.first }
                            
                            // 电视剧：合并同一部剧的集数（仅 TV_SHOW 分类）
                            val tvShowList = categorized
                                .filter { it.second == com.lomen.tv.domain.model.MediaType.TV_SHOW }
                                .map { it.first }
                            _tvShows.value = groupTvShowsBySeries(tvShowList)
                            
                            Log.d(TAG, "Movies: ${_movies.value.size}, TV Shows: ${_tvShows.value.size}, " +
                                    "Concerts: ${_concerts.value.size}, Variety: ${_variety.value.size}, " +
                                    "Documentaries: ${_documentaries.value.size}, Others: ${_others.value.size}")
                        }
                    }
                } else {
                    Log.d(TAG, "No library selected, clearing media")
                    _currentLibraryMedia.value = emptyList()
                    _movies.value = emptyList()
                    _tvShows.value = emptyList()
                    _concerts.value = emptyList()
                    _variety.value = emptyList()
                    _documentaries.value = emptyList()
                    _others.value = emptyList()
                }
            }
        }
    }

    fun addLibrary(library: ResourceLibrary) {
        viewModelScope.launch {
            repository.addLibrary(library)
        }
    }

    fun removeLibrary(libraryId: String) {
        viewModelScope.launch {
            // 先删除该资源库的所有媒体数据
            webDavMediaDao.deleteByLibraryId(libraryId)
            Log.d(TAG, "Deleted all media for library: $libraryId")
            // 再删除资源库本身
            repository.removeLibrary(libraryId)
            Log.d(TAG, "Deleted library: $libraryId")
            // 清空播放记录
            try {
                watchHistoryService.clearAllWatchHistory()
                Log.d(TAG, "Cleared all watch history")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to clear watch history", e)
            }
        }
    }

    fun setCurrentLibrary(libraryId: String?) {
        viewModelScope.launch {
            repository.setCurrentLibrary(libraryId)
        }
    }

    fun getLibraryById(libraryId: String): ResourceLibrary? {
        return resourceLibraries.value.find { it.id == libraryId }
    }

    fun getCurrentLibrary(): ResourceLibrary? {
        val currentId = currentLibraryId.value ?: return null
        return getLibraryById(currentId)
    }

    /**
     * 清除当前资源库的媒体缓存（用于设置页"一键清除缓存"）
     * 删除数据库中的webdav_media记录，同时清空播放记录
     */
    fun clearAllCache() {
        viewModelScope.launch {
            val libraryId = currentLibraryId.value
            if (libraryId != null) {
                webDavMediaDao.deleteByLibraryId(libraryId)
                Log.d(TAG, "Cleared media cache for library: $libraryId")
            } else {
                Log.d(TAG, "No current library, skip clearAllCache")
            }
            // 清空播放记录
            try {
                watchHistoryService.clearAllWatchHistory()
                Log.d(TAG, "Cleared all watch history")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to clear watch history", e)
            }
        }
    }

    // 将剧集按系列分组（按标题分组）
    private fun groupTvShowsBySeries(tvShows: List<WebDavMediaEntity>): List<TvShowSeries> {
        // 按标题分组
        val grouped = tvShows.groupBy { it.title }
        
        return grouped.map { (title, episodes) ->
            // 使用第一集的信息作为系列信息
            val firstEpisode = episodes.first()
            TvShowSeries(
                id = title,  // 使用标题作为ID
                title = title,
                posterUrl = firstEpisode.posterUrl,
                year = firstEpisode.year,
                rating = firstEpisode.rating,
                overview = firstEpisode.overview,
                episodes = episodes.sortedBy { it.episodeNumber },  // 按集数排序
                episodeCount = episodes.size
            )
        }.sortedByDescending { it.episodeCount }  // 按集数排序，集数多的在前
    }
}

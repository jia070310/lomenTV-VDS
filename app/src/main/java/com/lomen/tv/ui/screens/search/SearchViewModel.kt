package com.lomen.tv.ui.screens.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lomen.tv.data.local.database.dao.MovieDao
import com.lomen.tv.domain.model.MediaType
import com.lomen.tv.domain.service.MetadataService
import com.lomen.tv.utils.PinyinUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(FlowPreview::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val movieDao: MovieDao,
    private val metadataService: MetadataService
) : ViewModel() {

    private val _uiState = MutableStateFlow<SearchUiState>(SearchUiState.Initial)
    val uiState: StateFlow<SearchUiState> = _uiState

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _searchHistory = MutableStateFlow<List<String>>(emptyList())
    val searchHistory: StateFlow<List<String>> = _searchHistory

    init {
        loadSearchHistory()
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
        if (query.isEmpty()) {
            _uiState.value = SearchUiState.Initial
        }
    }

    fun search() {
        val query = _searchQuery.value.trim()
        if (query.isEmpty()) return

        viewModelScope.launch {
            _uiState.value = SearchUiState.Loading
            addToHistory(query)

            try {
                // 本地搜索
                val localResults = searchLocal(query)

                // 如果本地结果不足，进行在线搜索
                val results = if (localResults.size < 5) {
                    val onlineResults = searchOnline(query)
                    (localResults + onlineResults).distinctBy { it.id }
                } else {
                    localResults
                }

                _uiState.value = if (results.isEmpty()) {
                    SearchUiState.Empty
                } else {
                    SearchUiState.Success(results)
                }
            } catch (e: Exception) {
                _uiState.value = SearchUiState.Error(e.message ?: "搜索失败")
            }
        }
    }

    private suspend fun searchLocal(query: String): List<SearchResultItem> {
        val movies = movieDao.searchMovies("%$query%").first()

        return movies.map { entity ->
            SearchResultItem(
                id = entity.id,
                title = entity.title,
                originalTitle = entity.originalTitle,
                overview = entity.overview,
                posterUrl = entity.posterUrl,
                rating = entity.rating,
                year = entity.releaseDate?.take(4),
                genres = entity.genre?.split(",")?.map { it.trim() } ?: emptyList(),
                type = entity.type
            )
        }
    }

    private suspend fun searchOnline(query: String): List<SearchResultItem> {
        return try {
            // 使用TMDb搜索
            val result = metadataService.searchByFileName("$query.mp4")

            if (result.isSuccess) {
                val mediaItem = result.getOrThrow()
                listOf(
                    SearchResultItem(
                        id = mediaItem.id,
                        title = mediaItem.title,
                        originalTitle = mediaItem.originalTitle,
                        overview = mediaItem.overview,
                        posterUrl = mediaItem.posterUrl,
                        rating = mediaItem.rating,
                        year = mediaItem.releaseDate?.take(4),
                        genres = mediaItem.genres,
                        type = mediaItem.type
                    )
                )
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun clearSearch() {
        _searchQuery.value = ""
        _uiState.value = SearchUiState.Initial
    }

    private fun loadSearchHistory() {
        viewModelScope.launch {
            // 从DataStore加载搜索历史
            // TODO: 实现搜索历史持久化
            _searchHistory.value = emptyList()
        }
    }

    private fun addToHistory(query: String) {
        val currentHistory = _searchHistory.value.toMutableList()
        currentHistory.remove(query)
        currentHistory.add(0, query)
        if (currentHistory.size > 20) {
            currentHistory.removeAt(currentHistory.size - 1)
        }
        _searchHistory.value = currentHistory
        // TODO: 保存到DataStore
    }

    fun clearHistory() {
        _searchHistory.value = emptyList()
        // TODO: 清除DataStore
    }
}

// UI状态密封类
sealed class SearchUiState {
    object Initial : SearchUiState()
    object Loading : SearchUiState()
    data class Success(val results: List<SearchResultItem>) : SearchUiState()
    object Empty : SearchUiState()
    data class Error(val message: String) : SearchUiState()
}

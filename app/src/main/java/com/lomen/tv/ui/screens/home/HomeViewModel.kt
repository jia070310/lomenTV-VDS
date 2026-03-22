package com.lomen.tv.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lomen.tv.data.local.database.dao.MovieDao
import com.lomen.tv.domain.service.NotificationService
import com.lomen.tv.domain.service.PlaybackStatsService
import com.lomen.tv.domain.service.WatchHistoryService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val movieDao: MovieDao,
    val watchHistoryService: WatchHistoryService,
    private val notificationService: NotificationService,
    val playbackStatsService: PlaybackStatsService
) : ViewModel() {

    val recentWatchHistory = watchHistoryService.getRecentWatchHistory(30)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val allMovies = movieDao.getAllMovies()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    
    // 通知状态
    val currentNotification = notificationService.currentNotification
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,  // 立即开始收集，确保不错过更新
            initialValue = null
        )
    
    init {
        // 初始化时获取通知
        refreshNotifications()
    }
    
    fun refreshNotifications() {
        viewModelScope.launch {
            notificationService.refreshNotifications()
        }
    }
    
    suspend fun getPlaybackInfo(mediaId: String, episodeId: String?) = 
        watchHistoryService.getPlaybackInfo(mediaId, episodeId)
}

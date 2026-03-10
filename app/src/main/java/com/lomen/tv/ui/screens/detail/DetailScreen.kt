package com.lomen.tv.ui.screens.detail

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.tv.foundation.PivotOffsets
import androidx.tv.foundation.lazy.list.TvLazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.tv.material3.Border
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.IconButton
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Tab
import androidx.tv.material3.TabDefaults
import androidx.tv.material3.TabRow
import androidx.tv.material3.TabRowDefaults
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import com.lomen.tv.domain.model.MediaType
import com.lomen.tv.ui.theme.BackgroundDark
import com.lomen.tv.ui.theme.GlassBackground
import com.lomen.tv.ui.theme.PrimaryYellow
import com.lomen.tv.ui.theme.RatingGold
import com.lomen.tv.ui.theme.SurfaceDark
import com.lomen.tv.ui.theme.TextMuted
import com.lomen.tv.ui.theme.TextPrimary
import com.lomen.tv.ui.theme.TextSecondary

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun DetailScreen(
    mediaId: String,
    onNavigateBack: () -> Unit,
    onPlayClick: (videoUrl: String, title: String, episodeTitle: String?, mediaId: String, episodeId: String?) -> Unit,
    viewModel: DetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val selectedSeason by viewModel.selectedSeason.collectAsState()
    val availableSeasons by viewModel.availableSeasons.collectAsState()

    LaunchedEffect(mediaId) {
        viewModel.loadMediaDetail(mediaId)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
    ) {
        when (val state = uiState) {
            is DetailUiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "加载中...",
                        style = MaterialTheme.typography.headlineMedium,
                        color = TextPrimary
                    )
                }
            }
            is DetailUiState.Error -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "加载失败: ${state.message}",
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextPrimary
                    )
                }
            }
            is DetailUiState.Success -> {
                DetailContent(
                    media = state.media,
                    episodes = state.episodes,
                    cast = state.cast,
                    selectedSeason = selectedSeason,
                    availableSeasons = availableSeasons,
                    onSeasonSelected = { viewModel.selectSeason(it) },
                    onNavigateBack = onNavigateBack,
                    onPlayClick = onPlayClick
                )
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun DetailContent(
    media: MediaDetail,
    episodes: List<EpisodeItem>,
    cast: List<CastItem>,
    selectedSeason: Int,
    availableSeasons: List<Int>,
    onSeasonSelected: (Int) -> Unit,
    onNavigateBack: () -> Unit,
    onPlayClick: (videoUrl: String, title: String, episodeTitle: String?, mediaId: String, episodeId: String?) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = if (media.type == MediaType.TV_SHOW) {
        listOf("剧集", "简介", "演职人员")
    } else {
        listOf("简介", "演职人员")
    }

    TvLazyColumn(
        modifier = Modifier.fillMaxSize()
    ) {
        // Hero Section with Backdrop
        item {
            HeroSection(
                media = media,
                episodes = episodes,
                selectedSeason = selectedSeason,
                availableSeasons = availableSeasons,
                onSeasonSelected = onSeasonSelected,
                onNavigateBack = onNavigateBack,
                onPlayClick = {
                    // 立即播放按钮：播放第一集或影片本身
                    val firstEpisode = episodes.minByOrNull { it.episodeNumber }
                    val videoUrl = firstEpisode?.path ?: media.path ?: ""
                    val episodeTitle = if (media.type == MediaType.TV_SHOW && firstEpisode != null) {
                        "第${firstEpisode.episodeNumber}集 ${firstEpisode.title ?: ""}"
                    } else null
                    // 对于 WebDAV 媒体的剧集，使用集数的 id 作为 mediaId
                    val playMediaId = if (media.type == MediaType.TV_SHOW && firstEpisode != null) {
                        firstEpisode.id
                    } else {
                        media.id
                    }
                    onPlayClick(videoUrl, media.title, episodeTitle, playMediaId, null)
                }
            )
        }

        // Tabs
        item {
            TabRow(
                selectedTabIndex = selectedTab,
                modifier = Modifier.padding(horizontal = 48.dp, vertical = 4.dp),
                containerColor = Color.Transparent,
                indicator = { tabPositions, doesTabRowHaveFocus ->
                    tabPositions.getOrNull(selectedTab)?.let {
                        TabRowDefaults.PillIndicator(
                            currentTabPosition = it,
                            activeColor = PrimaryYellow,
                            inactiveColor = Color.White,
                            doesTabRowHaveFocus = doesTabRowHaveFocus
                        )
                    }
                }
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onFocus = { selectedTab = index }
                    ) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (selectedTab == index) BackgroundDark else TextSecondary,
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        }

        // Content based on selected tab
        when (selectedTab) {
            0 -> {
                if (media.type == MediaType.TV_SHOW) {
                    // Episodes
                    item {
                        EpisodesSection(
                            episodes = episodes,
                            totalEpisodes = media.totalEpisodes,
                            onEpisodeClick = { episode ->
                                val videoUrl = episode.path ?: ""
                                // 确保集数信息正确显示
                                // 如果 episode.title 包含剧集名，只提取集数相关的标题部分
                                val episodeTitle = if (episode.episodeNumber != null) {
                                    val episodeTitleText = episode.title?.trim() ?: ""
                                    // 如果 episode.title 包含剧集名（和 media.title 相同），则只显示集数
                                    if (episodeTitleText == media.title || episodeTitleText.isEmpty()) {
                                        "第${episode.episodeNumber}集"
                                    } else {
                                        // 否则显示集数和标题
                                        "第${episode.episodeNumber}集 $episodeTitleText"
                                    }
                                } else {
                                    // 如果没有集数，使用 episode.title（去除可能的剧集名）
                                    val episodeTitleText = episode.title?.trim() ?: ""
                                    if (episodeTitleText == media.title) {
                                        "" // 如果标题和剧集名相同，返回空字符串
                                    } else {
                                        episodeTitleText
                                    }
                                }
                                // 对于 WebDAV 媒体的剧集，使用集数的 id 作为 mediaId
                                val playMediaId = if (media.type == MediaType.TV_SHOW) {
                                    episode.id
                                } else {
                                    media.id
                                }
                                android.util.Log.d("DetailScreen", "Playing episode: title=$episodeTitle, media.title=${media.title}, episode.title=${episode.title}")
                                onPlayClick(videoUrl, media.title, episodeTitle, playMediaId, null)
                            }
                        )
                    }
                } else {
                    // Overview for movie
                    item {
                        OverviewSection(overview = media.overview)
                    }
                }
            }
            1 -> {
                if (media.type == MediaType.TV_SHOW) {
                    // Overview for TV show
                    item {
                        OverviewSection(overview = media.overview)
                    }
                } else {
                    // Cast for movie
                    item {
                        CastSection(cast = cast)
                    }
                }
            }
            2 -> {
                // Cast for TV show
                item {
                    CastSection(cast = cast)
                }
            }
        }

        // Path Information Section
        item {
            PathInformationSection(
                mediaPath = media.path
            )
        }

        // Bottom padding
        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun HeroSection(
    media: MediaDetail,
    episodes: List<EpisodeItem>,
    selectedSeason: Int,
    availableSeasons: List<Int>,
    onSeasonSelected: (Int) -> Unit,
    onNavigateBack: () -> Unit,
    onPlayClick: () -> Unit
) {
    // 季数下拉菜单状态
    var showSeasonDropdown by remember { mutableStateOf(false) }

    // Backdrop Section with Back Button overlay
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(500.dp)
    ) {
        // Backdrop Image
        AsyncImage(
            model = media.backdropUrl ?: media.posterUrl,
            contentDescription = media.title,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // Gradient Overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            BackgroundDark.copy(alpha = 0.95f),
                            BackgroundDark.copy(alpha = 0.4f),
                            BackgroundDark.copy(alpha = 0.8f)
                        )
                    )
                )
        )
        
        // Back Button - 直接叠加在图片上层
        IconButton(
            onClick = onNavigateBack,
            modifier = Modifier
                .padding(16.dp)
                .align(Alignment.TopStart),
            colors = androidx.tv.material3.IconButtonDefaults.colors(
                containerColor = Color.Black.copy(alpha = 0.5f),
                contentColor = TextPrimary,
                focusedContainerColor = PrimaryYellow,
                focusedContentColor = BackgroundDark
            )
        ) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "Back",
                modifier = Modifier.size(28.dp)
            )
        }

        // Content
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 48.dp, vertical = 60.dp)
        ) {
            Column(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .width(600.dp)
            ) {
                // Title
                Text(
                    text = media.title,
                    style = MaterialTheme.typography.displayLarge,
                    color = TextPrimary,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Rating and Info
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    media.rating?.let {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFF10b981))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = "Rating",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "%.1f".format(it),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color.White
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                    }

                    media.year?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextMuted
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                    }

                    Text(
                        text = media.genres.take(3).joinToString(" / "),
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMuted
                    )
                    Spacer(modifier = Modifier.width(16.dp))

                    if (media.type == MediaType.TV_SHOW) {
                        // 优先显示TMDB总集数，否则显示当前加载的集数
                        val episodeCount = media.totalEpisodes ?: episodes.size
                        Text(
                            text = "共${episodeCount}集",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White
                        )
                    }
                }

                // Overview
                Text(
                    text = media.overview ?: "暂无简介",
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextSecondary,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(bottom = 24.dp)
                )

                // Action Buttons
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Button(
                        onClick = onPlayClick,
                        colors = ButtonDefaults.colors(
                            containerColor = SurfaceDark.copy(alpha = 0.8f),
                            contentColor = TextPrimary,
                            focusedContainerColor = PrimaryYellow,
                            focusedContentColor = Color.Black,
                            pressedContainerColor = PrimaryYellow,
                            pressedContentColor = Color.Black
                        ),
                        shape = ButtonDefaults.shape(shape = RoundedCornerShape(24.dp)),
                        modifier = Modifier
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Play",
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "立即播放",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                color = Color.Unspecified
                            )
                        )
                    }

                    if (media.type == MediaType.TV_SHOW && availableSeasons.size > 1) {
                        // 多季显示下拉选择器
                        Box {
                            Button(
                                onClick = { showSeasonDropdown = !showSeasonDropdown },
                                colors = ButtonDefaults.colors(
                                    containerColor = SurfaceDark.copy(alpha = 0.8f),
                                    contentColor = TextPrimary,
                                    focusedContainerColor = PrimaryYellow,
                                    focusedContentColor = Color.Black,
                                    pressedContainerColor = PrimaryYellow,
                                    pressedContentColor = Color.Black
                                ),
                                shape = ButtonDefaults.shape(shape = RoundedCornerShape(24.dp)),
                                modifier = Modifier
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = "第${selectedSeason}季",
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        color = Color.Unspecified
                                    )
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = "选择季数",
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            
                            // 下拉菜单
                            if (showSeasonDropdown) {
                                SeasonDropdownMenu(
                                    availableSeasons = availableSeasons,
                                    selectedSeason = selectedSeason,
                                    onSeasonSelected = { season ->
                                        onSeasonSelected(season)
                                        showSeasonDropdown = false
                                    },
                                    onDismiss = { showSeasonDropdown = false }
                                )
                            }
                        }
                    } else if (media.type == MediaType.TV_SHOW) {
                        // 单季显示固定标签
                        Button(
                            onClick = {},
                            colors = ButtonDefaults.colors(
                                containerColor = SurfaceDark.copy(alpha = 0.8f),
                                contentColor = TextPrimary,
                                focusedContainerColor = PrimaryYellow,
                                focusedContentColor = Color.Black,
                                pressedContainerColor = PrimaryYellow,
                                pressedContentColor = Color.Black
                            ),
                            shape = ButtonDefaults.shape(shape = RoundedCornerShape(24.dp)),
                            modifier = Modifier
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = "第${selectedSeason}季",
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    color = Color.Unspecified
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun EpisodesSection(
    episodes: List<EpisodeItem>,
    totalEpisodes: Int?,  // 总集数
    onEpisodeClick: (EpisodeItem) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp, start = 32.dp)
    ) {
        // Section Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "剧集播放",
                style = MaterialTheme.typography.headlineSmall,
                color = TextPrimary
            )
            val episodeText = if (totalEpisodes != null) {
                "更新至 ${episodes.size} 集 / 全集 $totalEpisodes 集"
            } else {
                "更新至 ${episodes.size} 集 / 全集"
            }
            Text(
                text = episodeText,
                style = MaterialTheme.typography.bodyMedium,
                color = TextMuted
            )
        }

        // Episodes Horizontal List - sorted by episode number
        val sortedEpisodes = episodes.sortedBy { it.episodeNumber }
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(28.dp),
            contentPadding = PaddingValues(start = 48.dp, end = 64.dp, top = 16.dp, bottom = 16.dp)
        ) {
            items(sortedEpisodes) {
                EpisodeCard(
                    episode = it,
                    onClick = { onEpisodeClick(it) }
                )
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun EpisodeCard(
    episode: EpisodeItem,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    
    Column(
        modifier = Modifier.width(300.dp)
    ) {
        Card(
            onClick = onClick,
            modifier = Modifier
                .width(300.dp)
                .height(168.dp)
                .onFocusChanged { isFocused = it.isFocused },
            colors = CardDefaults.colors(
                containerColor = SurfaceDark,
                focusedContainerColor = PrimaryYellow.copy(alpha = 0.2f),
                pressedContainerColor = PrimaryYellow.copy(alpha = 0.2f)
            ),
            scale = CardDefaults.scale(
                scale = 1.0f,
                focusedScale = 1.05f,
                pressedScale = 1.0f
            ),
            border = CardDefaults.border(
                focusedBorder = Border(
                    border = BorderStroke(width = 2.dp, color = Color.White.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(8.dp)
                )
            )
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Thumbnail
                AsyncImage(
                    model = episode.stillUrl,
                    contentDescription = episode.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                // Overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.2f))
                )

                // Duration
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.Black.copy(alpha = 0.8f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    // 显示真实时长（如果有）或默认时长
                    val durationText = if (episode.duration > 0) {
                        val minutes = episode.duration / 60000
                        "${minutes}分钟"
                    } else {
                        "--:--"
                    }
                    Text(
                        text = durationText,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White
                    )
                }

                // Play Button Overlay - 只在聚焦时显示
                if (isFocused) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0f))
                            .align(Alignment.Center)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Play",
                            tint = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier
                                .size(64.dp)
                                .align(Alignment.Center)
                        )
                    }
                }
            }
        }

        // Episode Title (副标题)
        Text(
            text = "第${episode.episodeNumber}集 ${episode.title ?: ""}",
            style = MaterialTheme.typography.bodyMedium,
            color = TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .padding(top = 8.dp)
                .width(300.dp)
        )

        // Episode Path (显示文件名)
        episode.path?.let {
            val fileName = it.substringAfterLast("/")
            Text(
                text = fileName,
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                modifier = Modifier
                    .padding(top = 4.dp)
                    .width(300.dp)
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun OverviewSection(overview: String?) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp)
    ) {
        // Section Header
        Column(
            modifier = Modifier
                .padding(horizontal = 48.dp, vertical = 8.dp)
        ) {
            Text(
                text = "简介",
                style = MaterialTheme.typography.headlineSmall,
                color = TextPrimary
            )
        }

        // Overview Content
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 48.dp, vertical = 4.dp)
        ) {
            Text(
                text = overview ?: "暂无简介",
                style = MaterialTheme.typography.bodyLarge,
                color = TextSecondary,
                lineHeight = MaterialTheme.typography.bodyLarge.lineHeight * 1.5f
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun CastSection(cast: List<CastItem>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp)
    ) {
        // Section Header
        Column(
            modifier = Modifier
                .padding(horizontal = 48.dp, vertical = 8.dp)
        ) {
            Text(
                text = "演职人员",
                style = MaterialTheme.typography.headlineSmall,
                color = TextPrimary
            )
        }

        // Cast Grid
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 48.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            items(cast) {
                CastCard(castItem = it)
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun CastCard(castItem: CastItem) {
    var isFocused by remember { androidx.compose.runtime.mutableStateOf(false) }
    
    Card(
        onClick = {},
        modifier = Modifier
            .width(96.dp)
            .onFocusChanged { isFocused = it.isFocused },
        colors = CardDefaults.colors(
            containerColor = Color.Transparent,
            focusedContainerColor = PrimaryYellow.copy(alpha = 0.2f),
            pressedContainerColor = PrimaryYellow.copy(alpha = 0.4f)
        ),
        scale = CardDefaults.scale(
            scale = 1.0f,
            focusedScale = 1.05f,
            pressedScale = 1.0f
        )
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(96.dp)
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(SurfaceDark)
                    .padding(4.dp)
            ) {
            AsyncImage(
                model = castItem.profileUrl,
                contentDescription = castItem.name,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = castItem.name,
            style = MaterialTheme.typography.bodyMedium,
            color = if (isFocused) BackgroundDark else TextPrimary,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )

        castItem.role?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.labelSmall,
                color = if (isFocused) BackgroundDark else TextMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
        }
    }
}

/**
 * 季数下拉选择菜单
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun SeasonDropdownMenu(
    availableSeasons: List<Int>,
    selectedSeason: Int,
    onSeasonSelected: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    // 计算菜单高度：每个选项约56dp，最多显示4个
    val itemHeight = 56.dp
    val maxVisibleItems = 4
    val menuHeight = if (availableSeasons.size <= maxVisibleItems) {
        itemHeight * availableSeasons.size + 16.dp // 加上上下padding
    } else {
        itemHeight * maxVisibleItems + 16.dp
    }
    
    Box(
        modifier = Modifier
            .padding(top = 56.dp) // 在按钮下方显示
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceDark)
            .width(140.dp)
            .height(menuHeight) // 使用固定高度
    ) {
        // 使用TvLazyColumn支持滚动
        androidx.tv.foundation.lazy.list.TvLazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 8.dp),
            pivotOffsets = PivotOffsets(0.5f, 0.5f)
        ) {
            items(availableSeasons.size) { index ->
                val season = availableSeasons[index]
                val isSelected = season == selectedSeason
                Button(
                    onClick = { onSeasonSelected(season) },
                    colors = ButtonDefaults.colors(
                        containerColor = if (isSelected) PrimaryYellow.copy(alpha = 0.2f) else Color.Transparent,
                        contentColor = if (isSelected) PrimaryYellow else TextPrimary,
                        focusedContainerColor = PrimaryYellow,
                        focusedContentColor = Color.Black,
                        pressedContainerColor = PrimaryYellow,
                        pressedContentColor = Color.Black
                    ),
                    shape = ButtonDefaults.shape(shape = RoundedCornerShape(8.dp)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "第${season}季",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = Color.Unspecified
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        }
    }
}

// Data classes for UI
data class MediaDetail(
    val id: String,
    val title: String,
    val originalTitle: String?,
    val overview: String?,
    val posterUrl: String?,
    val backdropUrl: String?,
    val rating: Float?,
    val year: String?,
    val genres: List<String>,
    val type: MediaType,
    val seasonCount: Int = 0,
    val totalEpisodes: Int? = null,  // 总集数
    val path: String? = null
)

data class EpisodeItem(
    val id: String,
    val episodeNumber: Int,
    val seasonNumber: Int = 1,  // 新增季数字段
    val title: String?,
    val stillUrl: String?,
    val progress: Long = 0,
    val duration: Long = 0,
    val isWatched: Boolean = false,
    val path: String? = null
)

/**
 * 演职人员数据类
 */
data class CastItem(
    val id: Int,
    val name: String,
    val role: String?,  // 导演/角色名称
    val profileUrl: String?
)

// UI State
sealed class DetailUiState {
    object Loading : DetailUiState()
    data class Success(
        val media: MediaDetail,
        val episodes: List<EpisodeItem>,
        val cast: List<CastItem>
    ) : DetailUiState()
    data class Error(val message: String) : DetailUiState()
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun PathInformationSection(
    mediaPath: String?
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp)
    ) {
        // Section Header
        Column(
            modifier = Modifier
                .padding(horizontal = 48.dp, vertical = 8.dp)
        ) {
            Text(
                text = "文件路径",
                style = MaterialTheme.typography.headlineSmall,
                color = TextPrimary
            )
        }

        // Path Content
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 48.dp, vertical = 4.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(SurfaceDark)
                .padding(16.dp)
        ) {
            Text(
                text = mediaPath ?: "暂无路径信息",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
            )
        }
    }
}

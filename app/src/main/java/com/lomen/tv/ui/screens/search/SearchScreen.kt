package com.lomen.tv.ui.screens.search

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.IconButton
import androidx.tv.material3.IconButtonDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import com.lomen.tv.domain.model.MediaType
import com.lomen.tv.ui.theme.BackgroundDark
import com.lomen.tv.ui.theme.GlassBackground
import com.lomen.tv.ui.theme.PrimaryYellow
import com.lomen.tv.ui.theme.SurfaceDark
import com.lomen.tv.ui.theme.TextMuted
import com.lomen.tv.ui.theme.TextPrimary
import com.lomen.tv.ui.theme.TextSecondary

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun SearchScreen(
    onNavigateBack: () -> Unit,
    onNavigateToDetail: (String) -> Unit,
    viewModel: SearchViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchHistory by viewModel.searchHistory.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // 搜索栏
            SearchBar(
                query = searchQuery,
                onQueryChange = viewModel::onSearchQueryChange,
                onSearch = viewModel::search,
                onClear = viewModel::clearSearch,
                onNavigateBack = onNavigateBack
            )

            // 搜索内容
            when (val state = uiState) {
                is SearchUiState.Initial -> {
                    // 显示搜索历史和热门搜索
                    SearchInitialContent(
                        searchHistory = searchHistory,
                        onHistoryClick = { query ->
                            viewModel.onSearchQueryChange(query)
                            viewModel.search()
                        },
                        onClearHistory = viewModel::clearHistory
                    )
                }
                is SearchUiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "搜索中...",
                            style = MaterialTheme.typography.bodyLarge,
                            color = TextSecondary
                        )
                    }
                }
                is SearchUiState.Success -> {
                    SearchResults(
                        results = state.results,
                        onItemClick = onNavigateToDetail
                    )
                }
                is SearchUiState.Empty -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "未找到相关结果",
                            style = MaterialTheme.typography.bodyLarge,
                            color = TextSecondary
                        )
                    }
                }
                is SearchUiState.Error -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "搜索出错: ${state.message}",
                            style = MaterialTheme.typography.bodyLarge,
                            color = TextSecondary
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onClear: () -> Unit,
    onNavigateBack: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 48.dp, vertical = 24.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 返回按钮
        IconButton(
            onClick = onNavigateBack,
            colors = IconButtonDefaults.colors(
                containerColor = androidx.compose.ui.graphics.Color.Transparent,
                contentColor = TextPrimary,
                focusedContainerColor = PrimaryYellow,
                focusedContentColor = BackgroundDark
            )
        ) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "返回",
                modifier = Modifier.size(28.dp)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        // 搜索输入框
        Box(
            modifier = Modifier
                .weight(1f)
                .height(56.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(SurfaceDark)
                .padding(horizontal = 20.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = TextMuted,
                    modifier = Modifier.size(24.dp)
                )

                Spacer(modifier = Modifier.width(12.dp))

                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    modifier = Modifier.weight(1f),
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        color = TextPrimary
                    ),
                    cursorBrush = SolidColor(PrimaryYellow),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Search
                    ),
                    keyboardActions = KeyboardActions(
                        onSearch = { onSearch() }
                    ),
                    decorationBox = { innerTextField ->
                        if (query.isEmpty()) {
                            Text(
                                text = "搜索电影、电视剧、综艺...",
                                style = MaterialTheme.typography.bodyLarge,
                                color = TextMuted
                            )
                        }
                        innerTextField()
                    }
                )

                if (query.isNotEmpty()) {
                    IconButton(
                        onClick = onClear,
                        colors = IconButtonDefaults.colors(
                            containerColor = androidx.compose.ui.graphics.Color.Transparent,
                            contentColor = TextMuted,
                            focusedContainerColor = PrimaryYellow,
                            focusedContentColor = BackgroundDark
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "清除",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        // 搜索按钮
        IconButton(
            onClick = onSearch,
            colors = IconButtonDefaults.colors(
                containerColor = androidx.compose.ui.graphics.Color.Transparent,
                contentColor = PrimaryYellow,
                focusedContainerColor = PrimaryYellow,
                focusedContentColor = BackgroundDark
            )
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "搜索",
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun SearchInitialContent(
    searchHistory: List<String>,
    onHistoryClick: (String) -> Unit,
    onClearHistory: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 48.dp, vertical = 16.dp)
    ) {
        // 搜索历史
        if (searchHistory.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "搜索历史",
                        style = MaterialTheme.typography.headlineSmall,
                        color = TextPrimary
                    )

                    IconButton(onClick = onClearHistory) {
                        Text(
                            text = "清除",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextMuted
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            items(searchHistory) { history ->
                SearchHistoryItem(
                    query = history,
                    onClick = { onHistoryClick(history) }
                )
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }

        // 热门搜索
        item {
            Text(
                text = "热门搜索",
                style = MaterialTheme.typography.headlineSmall,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(16.dp))
        }

        items(getHotSearches()) { hotSearch ->
            HotSearchItem(
                rank = hotSearch.rank,
                title = hotSearch.title,
                onClick = { onHistoryClick(hotSearch.title) }
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun SearchHistoryItem(
    query: String,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .onFocusChanged { isFocused = it.isFocused },
        colors = CardDefaults.colors(
            containerColor = SurfaceDark,
            focusedContainerColor = PrimaryYellow,
            pressedContainerColor = PrimaryYellow.copy(alpha = 0.8f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.History,
                contentDescription = null,
                tint = if (isFocused) BackgroundDark else TextMuted,
                modifier = Modifier.size(20.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = query,
                style = MaterialTheme.typography.bodyLarge,
                color = if (isFocused) BackgroundDark else TextPrimary,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun HotSearchItem(
    rank: Int,
    title: String,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .onFocusChanged { isFocused = it.isFocused },
        colors = CardDefaults.colors(
            containerColor = SurfaceDark,
            focusedContainerColor = PrimaryYellow,
            pressedContainerColor = PrimaryYellow.copy(alpha = 0.8f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 排名
            val rankColor = if (isFocused) {
                BackgroundDark
            } else {
                when (rank) {
                    1 -> PrimaryYellow
                    2 -> PrimaryYellow.copy(alpha = 0.8f)
                    3 -> PrimaryYellow.copy(alpha = 0.6f)
                    else -> TextMuted
                }
            }

            Text(
                text = rank.toString(),
                style = MaterialTheme.typography.bodyLarge,
                color = rankColor,
                modifier = Modifier.width(32.dp)
            )

            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (isFocused) BackgroundDark else TextPrimary,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun SearchResults(
    results: List<SearchResultItem>,
    onItemClick: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 48.dp, vertical = 16.dp)
    ) {
        items(results) { result ->
            SearchResultCard(
                result = result,
                onClick = { onItemClick(result.id) }
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun SearchResultCard(
    result: SearchResultItem,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.colors(
            containerColor = SurfaceDark,
            focusedContainerColor = PrimaryYellow,
            pressedContainerColor = PrimaryYellow.copy(alpha = 0.8f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // 海报
            Card(
                onClick = {},
                modifier = Modifier
                    .width(120.dp)
                    .height(180.dp),
                colors = CardDefaults.colors(containerColor = BackgroundDark)
            ) {
                AsyncImage(
                    model = result.posterUrl,
                    contentDescription = result.title,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // 信息
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = result.title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = TextPrimary
                )

                if (result.originalTitle != null && result.originalTitle != result.title) {
                    Text(
                        text = result.originalTitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMuted,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    result.rating?.let { rating ->
                        Text(
                            text = "%.1f".format(rating),
                            style = MaterialTheme.typography.bodyMedium,
                            color = PrimaryYellow
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    result.year?.let { year ->
                        Text(
                            text = year,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    val typeText = when (result.type) {
                        MediaType.MOVIE -> "电影"
                        MediaType.TV_SHOW -> "电视剧"
                        MediaType.CONCERT -> "演唱会"
                        MediaType.VARIETY -> "综艺"
                        MediaType.DOCUMENTARY -> "纪录片"
                        MediaType.OTHER -> "其它"
                    }
                    Text(
                        text = typeText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }

                // 类型标签
                if (result.genres.isNotEmpty()) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(result.genres.take(3)) { genre ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(GlassBackground)
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = genre,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = result.overview ?: "暂无简介",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMuted,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// 数据类
data class SearchResultItem(
    val id: String,
    val title: String,
    val originalTitle: String?,
    val overview: String?,
    val posterUrl: String?,
    val rating: Float?,
    val year: String?,
    val genres: List<String>,
    val type: MediaType
)

data class HotSearchItem(
    val rank: Int,
    val title: String
)

// 模拟热门搜索数据
private fun getHotSearches(): List<HotSearchItem> {
    return listOf(
        HotSearchItem(1, "狂飙"),
        HotSearchItem(2, "三体"),
        HotSearchItem(3, "流浪地球2"),
        HotSearchItem(4, "满江红"),
        HotSearchItem(5, "阿凡达2"),
        HotSearchItem(6, "黑豹2"),
        HotSearchItem(7, "蚁人3"),
        HotSearchItem(8, "雷霆沙赞2"),
        HotSearchItem(9, "龙与地下城"),
        HotSearchItem(10, "铃芽之旅")
    )
}

package com.lomen.tv.ui.screens.category

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.tv.foundation.PivotOffsets
import androidx.tv.foundation.lazy.list.TvLazyColumn
import androidx.tv.foundation.lazy.list.TvLazyRow
import androidx.tv.foundation.lazy.list.items
import androidx.tv.material3.Border
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.IconButton
import androidx.tv.material3.IconButtonDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.lomen.tv.domain.model.MediaType
import com.lomen.tv.ui.theme.BackgroundDark
import com.lomen.tv.ui.theme.PrimaryYellow
import com.lomen.tv.ui.theme.SurfaceDark
import com.lomen.tv.ui.theme.TextMuted
import com.lomen.tv.ui.theme.TextPrimary
import com.lomen.tv.ui.viewmodel.ResourceLibraryViewModel

private const val CATEGORY_COLUMNS = 6

private fun com.lomen.tv.data.local.database.entity.WebDavMediaEntity.activityAt(): Long =
    maxOf(createdAt, updatedAt)

private fun com.lomen.tv.ui.viewmodel.TvShowSeries.activityAt(): Long {
    val latestCreated = episodes.maxOfOrNull { it.createdAt } ?: 0L
    val latestUpdated = episodes.maxOfOrNull { it.updatedAt } ?: 0L
    return maxOf(latestCreated, latestUpdated)
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun CategoryScreen(
    mediaType: MediaType,
    onNavigateBack: () -> Unit,
    onNavigateToDetail: (String) -> Unit,
    resourceLibraryViewModel: ResourceLibraryViewModel = hiltViewModel()
) {
    val focusRequester = remember { FocusRequester() }
    
    // 根据媒体类型获取对应的数据，并按“最近活动优先”排序（新加入/最近更新）
    val tvShowsRaw by resourceLibraryViewModel.tvShows.collectAsState()
    val animeRaw by resourceLibraryViewModel.anime.collectAsState()
    val moviesRaw by resourceLibraryViewModel.movies.collectAsState()
    val varietyRaw by resourceLibraryViewModel.variety.collectAsState()
    val concertsRaw by resourceLibraryViewModel.concerts.collectAsState()
    val documentariesRaw by resourceLibraryViewModel.documentaries.collectAsState()
    val othersRaw by resourceLibraryViewModel.others.collectAsState()
    
    val tvShows = tvShowsRaw.sortedByDescending { it.activityAt() }
    val anime = animeRaw.sortedByDescending { it.activityAt() }
    val movies = moviesRaw.sortedByDescending { it.activityAt() }
    val variety = varietyRaw.sortedByDescending { it.activityAt() }
    val concerts = concertsRaw.sortedByDescending { it.activityAt() }
    val documentaries = documentariesRaw.sortedByDescending { it.activityAt() }
    val others = othersRaw.sortedByDescending { it.activityAt() }
    
    // 获取标题和颜色
    val (title, accentColor, itemsCount) = when (mediaType) {
        MediaType.TV_SHOW -> Triple("电视剧", Color(0xFF10b981), tvShows.size)
        MediaType.ANIME -> Triple("动漫", Color(0xFFf59e0b), anime.size)
        MediaType.MOVIE -> Triple("电影", PrimaryYellow, movies.size)
        MediaType.VARIETY -> Triple("综艺", Color(0xFFec4899), variety.size) // 按节目部数（多季合并）
        MediaType.CONCERT -> Triple("演唱会", Color(0xFFa855f7), concerts.size)
        MediaType.DOCUMENTARY -> Triple("纪录片", Color(0xFF3b82f6), documentaries.size)
        MediaType.OTHER -> Triple("其它", Color(0xFF6b7280), others.size)
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
    ) {
        TvLazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {
            // Header with back button
            item {
                    CategoryHeader(
                        title = "$title ($itemsCount)",
                    accentColor = accentColor,
                    onBackClick = onNavigateBack,
                    modifier = Modifier.focusRequester(focusRequester)
                )
            }
            
            item { Spacer(modifier = Modifier.height(32.dp)) }
            
            // Media grid - render rows directly in TvLazyColumn
            if (itemsCount == 0) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "暂无$title",
                            style = MaterialTheme.typography.titleLarge,
                            color = TextMuted
                        )
                    }
                }
            } else {
                when (mediaType) {
                    MediaType.TV_SHOW -> {
                        val columns = CATEGORY_COLUMNS
                        val rowCount = (tvShows.size + columns - 1) / columns
                        items(rowCount) { rowIndex ->
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(24.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 48.dp)
                            ) {
                                for (column in 0 until columns) {
                                    val index = rowIndex * columns + column
                                    if (index < tvShows.size) {
                                        val series = tvShows[index]
                                        TvShowSeriesCard(
                                            series = series,
                                            onClick = { onNavigateToDetail(series.id) }
                                        )
                                    } else {
                                        Spacer(modifier = Modifier.width(160.dp))
                                    }
                                }
                            }
                        }
                    }
                    MediaType.ANIME -> {
                        val columns = CATEGORY_COLUMNS
                        val rowCount = (anime.size + columns - 1) / columns
                        items(rowCount) { rowIndex ->
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(24.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 48.dp)
                            ) {
                                for (column in 0 until columns) {
                                    val index = rowIndex * columns + column
                                    if (index < anime.size) {
                                        val series = anime[index]
                                        TvShowSeriesCard(
                                            series = series,
                                            onClick = { onNavigateToDetail(series.id) }
                                        )
                                    } else {
                                        Spacer(modifier = Modifier.width(160.dp))
                                    }
                                }
                            }
                        }
                    }
                    MediaType.VARIETY -> {
                        val columns = CATEGORY_COLUMNS
                        val rowCount = (variety.size + columns - 1) / columns
                        items(rowCount) { rowIndex ->
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(24.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 48.dp)
                            ) {
                                for (column in 0 until columns) {
                                    val index = rowIndex * columns + column
                                    if (index < variety.size) {
                                        val series = variety[index]
                                        TvShowSeriesCard(
                                            series = series,
                                            onClick = { onNavigateToDetail(series.id) }
                                        )
                                    } else {
                                        Spacer(modifier = Modifier.width(160.dp))
                                    }
                                }
                            }
                        }
                    }
                    else -> {
                        val mediaList = when (mediaType) {
                            MediaType.MOVIE -> movies
                            MediaType.CONCERT -> concerts
                            MediaType.DOCUMENTARY -> documentaries
                            MediaType.OTHER -> others
                            else -> emptyList()
                        }
                        val columns = CATEGORY_COLUMNS
                        val rowCount = (mediaList.size + columns - 1) / columns
                        items(rowCount) { rowIndex ->
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(24.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 48.dp)
                            ) {
                                for (column in 0 until columns) {
                                    val index = rowIndex * columns + column
                                    if (index < mediaList.size) {
                                        val media = mediaList[index]
                                        MediaCard(
                                            media = media,
                                            onClick = { onNavigateToDetail(media.id) }
                                        )
                                    } else {
                                        Spacer(modifier = Modifier.width(160.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun CategoryHeader(
    title: String,
    accentColor: Color,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 48.dp, vertical = 24.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onBackClick,
            colors = IconButtonDefaults.colors(
                containerColor = Color.Transparent,
                contentColor = TextPrimary,
                focusedContainerColor = PrimaryYellow,
                focusedContentColor = BackgroundDark
            )
        ) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "返回",
                modifier = Modifier.size(24.dp)
            )
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(24.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(accentColor)
        )
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            color = TextPrimary
        )
    }
}


@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun TvShowSeriesCard(
    series: com.lomen.tv.ui.viewmodel.TvShowSeries,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier.width(160.dp)
    ) {
        Card(
            onClick = onClick,
            modifier = Modifier
                .width(160.dp)
                .height(240.dp),
            colors = CardDefaults.colors(
                containerColor = SurfaceDark,
                focusedContainerColor = PrimaryYellow,
                pressedContainerColor = PrimaryYellow.copy(alpha = 0.8f)
            ),
            border = CardDefaults.border(
                focusedBorder = Border(
                    border = BorderStroke(
                        width = 2.dp,
                        color = Color.White.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(8.dp)
                )
            )
        ) {
            Box {
                if (series.posterUrl != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(series.posterUrl)
                            .size(320, 480)
                            .crossfade(false)
                            .build(),
                        contentDescription = series.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(SurfaceDark),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = TextMuted
                        )
                    }
                }
                
                if (series.rating != null && series.rating > 0) {
                    Box(
                        modifier = Modifier
                            .padding(8.dp)
                            .align(Alignment.TopEnd)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color.Black.copy(alpha = 0.6f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "%.1f".format(series.rating),
                            style = MaterialTheme.typography.labelMedium,
                            color = Color(0xFF10b981)
                        )
                    }
                }
                
                if (series.year != null) {
                    Box(
                        modifier = Modifier
                            .padding(8.dp)
                            .align(Alignment.BottomStart)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color.Black.copy(alpha = 0.6f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = series.year.toString(),
                            style = MaterialTheme.typography.labelSmall,
                            color = TextPrimary
                        )
                    }
                }
                
                Box(
                    modifier = Modifier
                        .padding(8.dp)
                        .align(Alignment.TopStart)
                        .clip(RoundedCornerShape(4.dp))
                        .background(PrimaryYellow.copy(alpha = 0.9f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "${series.episodeCount}集",
                        style = MaterialTheme.typography.labelSmall,
                        color = BackgroundDark
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = series.title,
            style = MaterialTheme.typography.bodyMedium,
            color = TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        
        if (series.overview != null) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = series.overview,
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun MediaCard(
    media: com.lomen.tv.data.local.database.entity.WebDavMediaEntity,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier.width(160.dp)
    ) {
        Card(
            onClick = onClick,
            modifier = Modifier
                .width(160.dp)
                .height(240.dp),
            colors = CardDefaults.colors(
                containerColor = SurfaceDark,
                focusedContainerColor = PrimaryYellow,
                pressedContainerColor = PrimaryYellow.copy(alpha = 0.8f)
            ),
            border = CardDefaults.border(
                focusedBorder = Border(
                    border = BorderStroke(
                        width = 2.dp,
                        color = Color.White.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(8.dp)
                )
            )
        ) {
            Box {
                if (media.posterUrl != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(media.posterUrl)
                            .size(320, 480)
                            .crossfade(false)
                            .build(),
                        contentDescription = media.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(SurfaceDark),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = TextMuted
                        )
                    }
                }
                
                if (media.rating != null && media.rating > 0) {
                    Box(
                        modifier = Modifier
                            .padding(8.dp)
                            .align(Alignment.TopEnd)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color.Black.copy(alpha = 0.6f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "%.1f".format(media.rating),
                            style = MaterialTheme.typography.labelMedium,
                            color = Color(0xFF10b981)
                        )
                    }
                }
                
                if (media.year != null) {
                    Box(
                        modifier = Modifier
                            .padding(8.dp)
                            .align(Alignment.BottomStart)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color.Black.copy(alpha = 0.6f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = media.year.toString(),
                            style = MaterialTheme.typography.labelSmall,
                            color = TextPrimary
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = media.title,
            style = MaterialTheme.typography.bodyMedium,
            color = TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        
        if (media.overview != null) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = media.overview,
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// Sealed class for category items
private sealed class CategoryItem {
    data class Media(val media: com.lomen.tv.data.local.database.entity.WebDavMediaEntity) : CategoryItem()
    data class TvShowSeries(val series: com.lomen.tv.ui.viewmodel.TvShowSeries) : CategoryItem()
}


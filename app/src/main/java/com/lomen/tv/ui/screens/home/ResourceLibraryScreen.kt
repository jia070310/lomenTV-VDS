package com.lomen.tv.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storage
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.foundation.lazy.list.TvLazyColumn
import androidx.tv.foundation.lazy.list.items
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.IconButton
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.lomen.tv.domain.model.ResourceLibrary
import com.lomen.tv.ui.theme.BackgroundDark
import com.lomen.tv.ui.theme.PrimaryYellow
import com.lomen.tv.ui.theme.SurfaceDark
import com.lomen.tv.ui.theme.TextMuted
import com.lomen.tv.ui.theme.TextPrimary
import com.lomen.tv.ui.theme.TextSecondary
import com.lomen.tv.ui.viewmodel.MediaSyncViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// V2 设计颜色
private val BackgroundV2 = Color(0xFF050506)
private val SidebarBackground = Color(0xFF0A0A0B)
private val GlassCardBackground = Color(0xFF18181B).copy(alpha = 0.4f)
private val BorderColor = Color.White.copy(alpha = 0.05f)
private val AccentYellow = Color(0xFFF59E0B)
private val FocusYellow = Color(0xFFFEDD0E) // 焦点高亮黄色
private val StatusGreen = Color(0xFF10B981)
private val StatusRed = Color(0xFFEF4444)
private val TextZinc400 = Color(0xFFA1A1AA)
private val TextZinc500 = Color(0xFF71717A)
private val TextZinc600 = Color(0xFF52525B)

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun ResourceLibraryScreen(
    libraries: List<ResourceLibrary>,
    currentLibraryId: String?,
    syncState: MediaSyncViewModel.SyncState = MediaSyncViewModel.SyncState.Idle,
    syncProgress: Pair<Int, Int> = 0 to 0,
    onNavigateBack: () -> Unit,
    onLibrarySelected: (ResourceLibrary) -> Unit,
    onSyncComplete: () -> Unit = {},
    onAddLibrary: () -> Unit,
    onDeleteLibrary: (ResourceLibrary) -> Unit,
    onEditLibrary: (ResourceLibrary) -> Unit = {},
    onNavigateToSettings: () -> Unit = {}
) {
    val listFocusRequester = remember { FocusRequester() }
    var checkingLibraryId by remember { mutableStateOf<String?>(null) }
    var checkResult by remember { mutableStateOf<Map<String, Boolean>>(emptyMap()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    // 计算统计数据
    val webDavCount = libraries.count { it.type == ResourceLibrary.LibraryType.WEBDAV }
    val quarkCount = libraries.count { it.type == ResourceLibrary.LibraryType.QUARK }
    val activeCount = libraries.count { it.isActive }

    // 延迟请求焦点，确保Compose已完成布局
    LaunchedEffect(libraries.size) {
        if (libraries.isNotEmpty()) {
            kotlinx.coroutines.delay(100)
            try {
                listFocusRequester.requestFocus()
            } catch (e: Exception) {
                // 焦点请求失败时忽略错误
            }
        }
    }

    // 监听同步状态
    LaunchedEffect(syncState) {
        if (syncState is MediaSyncViewModel.SyncState.Completed) {
            kotlinx.coroutines.delay(1000)
            onSyncComplete()
        }
    }

    // 错误提示自动消失
    LaunchedEffect(errorMessage) {
        if (errorMessage != null) {
            kotlinx.coroutines.delay(3000)
            errorMessage = null
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // 主布局 - 左右分栏
        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundV2)
        ) {
            // 左侧导航栏
            Sidebar(
                onNavigateBack = onNavigateBack,
                onNavigateToSettings = onNavigateToSettings,
                modifier = Modifier.width(280.dp)
            )

            // 右侧主内容区
            MainContent(
                libraries = libraries,
                currentLibraryId = currentLibraryId,
                webDavCount = webDavCount,
                quarkCount = quarkCount,
                activeCount = activeCount,
                syncState = syncState,
                syncProgress = syncProgress,
                checkingLibraryId = checkingLibraryId,
                checkResult = checkResult,
                listFocusRequester = listFocusRequester,
                onLibrarySelected = onLibrarySelected,
                onNavigateBack = onNavigateBack,
                onAddLibrary = onAddLibrary,
                onDeleteLibrary = onDeleteLibrary,
                onEditLibrary = onEditLibrary,
                scope = scope,
                onCheckingLibrary = { checkingLibraryId = it },
                onCheckResult = { id, result ->
                    checkResult = checkResult + (id to result)
                    checkingLibraryId = null
                },
                onError = { msg -> errorMessage = msg }
            )
        }

        // 连接失败错误提示
        if (errorMessage != null) {
            androidx.compose.foundation.layout.Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 48.dp)
            ) {
                androidx.compose.foundation.layout.Row(
                    modifier = Modifier
                        .background(
                            color = Color(0xFFEF4444).copy(alpha = 0.92f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 24.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Error,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = errorMessage!!,
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun Sidebar(
    onNavigateBack: () -> Unit,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dateFormat = SimpleDateFormat("yyyy年MM月dd日", Locale.getDefault())
    val currentDate = dateFormat.format(Date())

    Column(
        modifier = modifier
            .fillMaxHeight()
            .background(SidebarBackground)
            .padding(32.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // 顶部标题区域
        Column {
            Text(
                text = "柠檬TV",
                style = MaterialTheme.typography.headlineLarge,
                color = Color.White,
                fontWeight = FontWeight.Black
            )
            Text(
                text = currentDate,
                style = MaterialTheme.typography.bodyMedium,
                color = TextZinc500,
                modifier = Modifier.padding(top = 8.dp)
            )

            Spacer(modifier = Modifier.height(48.dp))

            // 导航菜单
            NavigationItem(
                icon = Icons.Default.Menu,
                label = "资源库",
                isSelected = true,
                onClick = { }
            )

            NavigationItem(
                icon = Icons.Default.Home,
                label = "首页",
                isSelected = false,
                onClick = onNavigateBack
            )

            NavigationItem(
                icon = Icons.Default.Settings,
                label = "设置",
                isSelected = false,
                onClick = onNavigateToSettings
            )
        }

        // 底部时间卡片 - 动态显示当前时间
        var currentTime by remember { mutableStateOf("") }
        LaunchedEffect(Unit) {
            while (true) {
                val sdf = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())
                currentTime = sdf.format(Date())
                kotlinx.coroutines.delay(60000) // 每分钟更新一次
            }
        }
        GlassCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = null,
                    tint = AccentYellow,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = currentTime,
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun NavigationItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }

    Button(
        onClick = onClick,
        colors = ButtonDefaults.colors(
            containerColor = if (isFocused) FocusYellow else Color.Transparent,
            contentColor = if (isSelected) AccentYellow else TextZinc500,
            focusedContainerColor = FocusYellow,
            focusedContentColor = BackgroundDark
        ),
        shape = ButtonDefaults.shape(shape = RoundedCornerShape(16.dp)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .onFocusChanged { isFocused = it.isFocused }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = if (isFocused) BackgroundDark else if (isSelected) AccentYellow else TextZinc500
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = if (isFocused) BackgroundDark else if (isSelected) AccentYellow else TextZinc500,
                fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun MainContent(
    libraries: List<ResourceLibrary>,
    currentLibraryId: String?,
    webDavCount: Int,
    quarkCount: Int,
    activeCount: Int,
    syncState: MediaSyncViewModel.SyncState,
    syncProgress: Pair<Int, Int>,
    checkingLibraryId: String?,
    checkResult: Map<String, Boolean>,
    listFocusRequester: FocusRequester,
    onLibrarySelected: (ResourceLibrary) -> Unit,
    onNavigateBack: () -> Unit,
    onAddLibrary: () -> Unit,
    onDeleteLibrary: (ResourceLibrary) -> Unit,
    onEditLibrary: (ResourceLibrary) -> Unit,
    scope: kotlinx.coroutines.CoroutineScope,
    onCheckingLibrary: (String?) -> Unit,
    onCheckResult: (String, Boolean) -> Unit,
    onError: (String) -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        SidebarBackground,
                        BackgroundV2
                    )
                )
            )
            .padding(48.dp)
    ) {
        // 顶部状态摘要
        HeaderSection(
            libraryCount = libraries.size,
            webDavCount = webDavCount,
            quarkCount = quarkCount,
            activeCount = activeCount
        )

        Spacer(modifier = Modifier.height(32.dp))

        // 同步状态显示
        when (syncState) {
            is MediaSyncViewModel.SyncState.Scanning -> {
                SyncStatusCardV2("正在扫描文件...", syncProgress)
                Spacer(modifier = Modifier.height(16.dp))
            }
            is MediaSyncViewModel.SyncState.Scraping -> {
                SyncStatusCardV2("正在刮削信息 (${syncProgress.first}/${syncProgress.second})...", syncProgress)
                Spacer(modifier = Modifier.height(16.dp))
            }
            is MediaSyncViewModel.SyncState.Error -> {
                SyncStatusCardV2("同步失败", syncProgress, isError = true)
                Spacer(modifier = Modifier.height(16.dp))
            }
            else -> {}
        }

        val isSyncing = syncState is MediaSyncViewModel.SyncState.Scanning ||
                syncState is MediaSyncViewModel.SyncState.Scraping

        if (libraries.isEmpty()) {
            EmptyLibraryViewV2(onAddLibrary = onAddLibrary)
        } else {
            // 左右两栏布局
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // 左侧 - 网盘区（始终显示）
                val quarkLibraries = libraries.filter { it.type == ResourceLibrary.LibraryType.QUARK }
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    SectionTitle(title = "网盘区", accentColor = AccentYellow)
                    Spacer(modifier = Modifier.height(16.dp))
                    if (quarkLibraries.isNotEmpty()) {
                        TvLazyColumn(
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(quarkLibraries) { library ->
                                val isChecking = checkingLibraryId == library.id
                                val isConnected = checkResult[library.id]
                                LibraryCardV2(
                                    library = library,
                                    isSelected = library.id == currentLibraryId,
                                    isChecking = isChecking,
                                    isConnected = isConnected,
                                    enabled = !isSyncing,
                                    onClick = {
                                        handleLibraryClick(
                                            library = library,
                                            scope = scope,
                                            onCheckingLibrary = onCheckingLibrary,
                                            onCheckResult = onCheckResult,
                                            onLibrarySelected = onLibrarySelected,
                                            onNavigateBack = onNavigateBack,
                                            onError = onError
                                        )
                                    },
                                    onDelete = { onDeleteLibrary(library) },
                                    onEdit = { onEditLibrary(library) },
                                    modifier = if (quarkLibraries.indexOf(library) == 0) {
                                        Modifier.focusRequester(listFocusRequester)
                                    } else {
                                        Modifier
                                    }
                                )
                            }
                        }
                    } else {
                        // 空状态提示
                        EmptyQuarkLibraryView()
                    }
                }

                // 右侧 - WEBDAV本地区（始终显示）
                val webDavLibraries = libraries.filter { it.type == ResourceLibrary.LibraryType.WEBDAV }
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    SectionTitle(title = "WEBDAV本地区", accentColor = TextZinc600)
                    Spacer(modifier = Modifier.height(16.dp))
                    if (webDavLibraries.isNotEmpty()) {
                        TvLazyColumn(
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(webDavLibraries) { library ->
                                val isChecking = checkingLibraryId == library.id
                                val isConnected = checkResult[library.id]
                                LibraryListItemV2(
                                    library = library,
                                    isSelected = library.id == currentLibraryId,
                                    isChecking = isChecking,
                                    isConnected = isConnected,
                                    enabled = !isSyncing,
                                    onClick = {
                                        handleLibraryClick(
                                            library = library,
                                            scope = scope,
                                            onCheckingLibrary = onCheckingLibrary,
                                            onCheckResult = onCheckResult,
                                            onLibrarySelected = onLibrarySelected,
                                            onNavigateBack = onNavigateBack,
                                            onError = onError
                                        )
                                    },
                                    onDelete = { onDeleteLibrary(library) },
                                    onEdit = { onEditLibrary(library) }
                                )
                            }

                            // 添加按钮
                            item {
                                Spacer(modifier = Modifier.height(16.dp))
                                AddLibraryButtonV2(onClick = onAddLibrary)
                            }
                        }
                    } else {
                        // 空状态提示
                        EmptyWebDavLibraryView(onAddLibrary = onAddLibrary)
                    }
                }
            }
        }
    }
}

private fun handleLibraryClick(
    library: ResourceLibrary,
    scope: kotlinx.coroutines.CoroutineScope,
    onCheckingLibrary: (String?) -> Unit,
    onCheckResult: (String, Boolean) -> Unit,
    onLibrarySelected: (ResourceLibrary) -> Unit,
    onNavigateBack: () -> Unit,
    onError: (String) -> Unit = {}
) {
    if (library.type == ResourceLibrary.LibraryType.WEBDAV) {
        onCheckingLibrary(library.id)
        scope.launch {
            val connected = checkWebDavConnection(library)
            onCheckResult(library.id, connected)
            if (connected) {
                withContext(Dispatchers.Main) {
                    // 触发同步，留在当前页面让用户看到扫描进度，不自动跳转
                    onLibrarySelected(library)
                }
            } else {
                withContext(Dispatchers.Main) {
                    onError("无法连接到 ${library.host}:${library.port}，请检查服务器是否在线")
                }
            }
        }
    } else {
        onLibrarySelected(library)
        onNavigateBack()
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun HeaderSection(
    libraryCount: Int,
    webDavCount: Int,
    quarkCount: Int,
    activeCount: Int
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        Column {
            Text(
                text = "资源库列表",
                style = MaterialTheme.typography.headlineLarge,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "当前有 $libraryCount 个资源库",
                style = MaterialTheme.typography.bodyLarge,
                color = TextZinc400,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // 统计卡片
            StatCard(
                label = "WEBDAV",
                value = "$webDavCount",
                borderColor = BorderColor
            )
            StatCard(
                label = "网盘",
                value = "$quarkCount",
                borderColor = AccentYellow.copy(alpha = 0.3f)
            )
            StatCard(
                label = "在线",
                value = "$activeCount",
                borderColor = StatusGreen.copy(alpha = 0.3f)
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun StatCard(
    label: String,
    value: String,
    borderColor: Color
) {
    GlassCard(
        modifier = Modifier,
        borderColor = borderColor
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = TextZinc500,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                color = if (borderColor == StatusGreen.copy(alpha = 0.3f)) StatusGreen else Color.White,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun SectionTitle(
    title: String,
    accentColor: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(bottom = 12.dp)
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(20.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(accentColor)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = TextZinc400,
            fontWeight = FontWeight.Bold
        )
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun LibraryCardV2(
    library: ResourceLibrary,
    isSelected: Boolean,
    isChecking: Boolean,
    isConnected: Boolean?,
    enabled: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }

    Card(
        onClick = { if (enabled) onClick() },
        colors = CardDefaults.colors(
            containerColor = if (isSelected) AccentYellow.copy(alpha = 0.15f) else GlassCardBackground,
            focusedContainerColor = AccentYellow.copy(alpha = 0.25f)
        ),
        modifier = modifier
            .fillMaxWidth()
            .height(120.dp)
            .onFocusChanged { isFocused = it.isFocused }
            .then(
                if (isFocused || isSelected) {
                    Modifier.drawBehind {
                        drawRoundRect(
                            color = AccentYellow,
                            cornerRadius = CornerRadius(24.dp.toPx(), 24.dp.toPx()),
                            style = Stroke(width = 2.dp.toPx())
                        )
                    }
                } else Modifier
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 图标
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                AccentYellow.copy(alpha = 0.8f),
                                AccentYellow
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Cloud,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.width(24.dp))

            // 信息
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = library.name,
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Black
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 状态指示器
                    StatusIndicator(
                        isChecking = isChecking,
                        isConnected = isConnected
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Text(
                        text = if (isChecking) "检测中..."
                        else if (isConnected == true) "在线"
                        else if (isConnected == false) "离线"
                        else "待检测",
                        style = MaterialTheme.typography.bodyMedium,
                        color = when {
                            isChecking -> AccentYellow
                            isConnected == true -> StatusGreen
                            isConnected == false -> StatusRed
                            else -> TextZinc500
                        },
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Text(
                        text = "|",
                        color = TextZinc600
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Text(
                        text = library.getDisplayUrl(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextZinc400
                    )
                }
            }

            // 操作按钮
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(
                    onClick = { if (enabled) onEdit() },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "编辑",
                        tint = if (enabled) AccentYellow else TextZinc500,
                        modifier = Modifier.size(20.dp)
                    )
                }

                IconButton(
                    onClick = { if (enabled) onDelete() },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "删除",
                        tint = if (enabled) StatusRed else TextZinc500,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun LibraryListItemV2(
    library: ResourceLibrary,
    isSelected: Boolean,
    isChecking: Boolean,
    isConnected: Boolean?,
    enabled: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit
) {
    var isCardFocused by remember { mutableStateOf(false) }
    var isEditFocused by remember { mutableStateOf(false) }
    var isDeleteFocused by remember { mutableStateOf(false) }

    // 使用Row将卡片和按钮并排显示，按钮独立在卡片外部
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // 资源库信息卡片
        Card(
            onClick = { if (enabled) onClick() },
            colors = CardDefaults.colors(
                containerColor = if (isCardFocused) FocusYellow else if (isSelected) GlassCardBackground else GlassCardBackground,
                focusedContainerColor = FocusYellow
            ),
            border = CardDefaults.border(
                focusedBorder = androidx.tv.material3.Border(
                    BorderStroke(2.dp, FocusYellow),
                    shape = RoundedCornerShape(16.dp)
                )
            ),
            scale = CardDefaults.scale(focusedScale = 1.02f),
            modifier = Modifier
                .weight(1f)
                .height(80.dp)
                .onFocusChanged { isCardFocused = it.isFocused }
                .then(
                    if (isSelected && !isCardFocused) {
                        Modifier.drawBehind {
                            drawRoundRect(
                                color = AccentYellow.copy(alpha = 0.6f),
                                cornerRadius = CornerRadius(16.dp.toPx(), 16.dp.toPx()),
                                style = Stroke(width = 2.dp.toPx())
                            )
                        }
                    } else Modifier
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 图标
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isCardFocused) FocusYellow.copy(alpha = 0.3f) else SurfaceDark),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Storage,
                            contentDescription = null,
                            tint = if (isCardFocused) BackgroundDark else TextZinc400,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    // 信息
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = library.name,
                            style = MaterialTheme.typography.titleMedium,
                            color = if (isCardFocused) BackgroundDark else if (isSelected) AccentYellow else Color.White,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = library.getDisplayUrl(),
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isCardFocused) BackgroundDark.copy(alpha = 0.7f) else TextZinc500,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    // 右侧状态
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (isSelected) {
                            Text(
                                text = "当前",
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isCardFocused) BackgroundDark else AccentYellow,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        StatusIndicator(
                            isChecking = isChecking,
                            isConnected = isConnected
                        )
                    }
                }
            }
        }

        // 独立的编辑按钮
        Card(
            onClick = { if (enabled) onEdit() },
            colors = CardDefaults.colors(
                containerColor = if (isEditFocused) FocusYellow else GlassCardBackground,
                focusedContainerColor = FocusYellow
            ),
            border = CardDefaults.border(
                focusedBorder = androidx.tv.material3.Border(
                    BorderStroke(2.dp, FocusYellow),
                    shape = RoundedCornerShape(12.dp)
                )
            ),
            scale = CardDefaults.scale(focusedScale = 1.05f),
            modifier = Modifier
                .size(56.dp)
                .onFocusChanged { isEditFocused = it.isFocused }
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "编辑",
                    tint = if (isEditFocused) BackgroundDark else if (enabled) AccentYellow else TextZinc500,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        // 独立的删除按钮
        Card(
            onClick = { if (enabled) onDelete() },
            colors = CardDefaults.colors(
                containerColor = if (isDeleteFocused) FocusYellow else GlassCardBackground,
                focusedContainerColor = FocusYellow
            ),
            border = CardDefaults.border(
                focusedBorder = androidx.tv.material3.Border(
                    BorderStroke(2.dp, FocusYellow),
                    shape = RoundedCornerShape(12.dp)
                )
            ),
            scale = CardDefaults.scale(focusedScale = 1.05f),
            modifier = Modifier
                .size(56.dp)
                .onFocusChanged { isDeleteFocused = it.isFocused }
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "删除",
                    tint = if (isDeleteFocused) BackgroundDark else if (enabled) StatusRed else TextZinc500,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

@Composable
private fun StatusIndicator(
    isChecking: Boolean,
    isConnected: Boolean?
) {
    Box(
        modifier = Modifier.size(12.dp),
        contentAlignment = Alignment.Center
    ) {
        when {
            isChecking -> {
                // 脉冲动画效果
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(AccentYellow)
                )
            }
            isConnected == true -> {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(StatusGreen)
                )
            }
            isConnected == false -> {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(StatusRed)
                )
            }
            else -> {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(TextZinc600)
                )
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun AddLibraryButtonV2(
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }

    Card(
        onClick = onClick,
        colors = CardDefaults.colors(
            containerColor = Color.Transparent,
            focusedContainerColor = AccentYellow.copy(alpha = 0.1f)
        ),
        border = CardDefaults.border(
            focusedBorder = androidx.tv.material3.Border(
                BorderStroke(2.dp, AccentYellow),
                shape = RoundedCornerShape(16.dp)
            )
        ),
        scale = CardDefaults.scale(focusedScale = 1.02f),
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .onFocusChanged { isFocused = it.isFocused }
            .then(
                if (!isFocused) {
                    Modifier.drawBehind {
                        drawRoundRect(
                            color = BorderColor,
                            cornerRadius = CornerRadius(16.dp.toPx(), 16.dp.toPx()),
                            style = Stroke(width = 2.dp.toPx(), pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f))
                        )
                    }
                } else Modifier
            )
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "+ 添加新资源库",
                style = MaterialTheme.typography.bodyLarge,
                color = if (isFocused) Color.White else TextZinc500,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun EmptyQuarkLibraryView() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(32.dp))
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(TextZinc600.copy(alpha = 0.3f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Cloud,
                contentDescription = null,
                tint = TextZinc500,
                modifier = Modifier.size(32.dp)
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "暂无网盘",
            style = MaterialTheme.typography.bodyLarge,
            color = TextZinc500
        )
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun EmptyWebDavLibraryView(
    onAddLibrary: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(32.dp))
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(TextZinc600.copy(alpha = 0.3f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Storage,
                contentDescription = null,
                tint = TextZinc500,
                modifier = Modifier.size(32.dp)
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "暂无WEBDAV",
            style = MaterialTheme.typography.bodyLarge,
            color = TextZinc500
        )
        Spacer(modifier = Modifier.height(24.dp))
        AddLibraryButtonV2(onClick = onAddLibrary)
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun EmptyLibraryViewV2(
    onAddLibrary: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        GlassCard {
            Column(
                modifier = Modifier.padding(48.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(TextZinc600.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Storage,
                        contentDescription = null,
                        tint = TextZinc500,
                        modifier = Modifier.size(40.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "暂无资源库",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "添加WebDAV网盘或网盘",
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextZinc500
                )

                Spacer(modifier = Modifier.height(32.dp))

                var isButtonFocused by remember { mutableStateOf(false) }
                Button(
                    onClick = onAddLibrary,
                    colors = ButtonDefaults.colors(
                        containerColor = SurfaceDark,      // 未选中：浅灰色底
                        contentColor = TextPrimary,         // 未选中：白色字
                        focusedContainerColor = PrimaryYellow, // 选中：亮黄色底
                        focusedContentColor = BackgroundDark    // 选中：黑色字
                    ),
                    shape = ButtonDefaults.shape(shape = RoundedCornerShape(12.dp)),
                    modifier = Modifier.onFocusChanged { isButtonFocused = it.isFocused }
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        tint = if (isButtonFocused) BackgroundDark else TextPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "添加资源库",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (isButtonFocused) BackgroundDark else TextPrimary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun GlassCard(
    modifier: Modifier = Modifier,
    borderColor: Color = BorderColor,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(GlassCardBackground)
            .drawBehind {
                drawRoundRect(
                    color = borderColor,
                    cornerRadius = CornerRadius(16.dp.toPx(), 16.dp.toPx()),
                    style = Stroke(width = 1.dp.toPx())
                )
            }
            .padding(1.dp)
    ) {
        content()
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun SyncStatusCardV2(
    message: String,
    progress: Pair<Int, Int>,
    isError: Boolean = false
) {
    GlassCard(
        borderColor = if (isError) StatusRed.copy(alpha = 0.3f) else AccentYellow.copy(alpha = 0.3f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isError) {
                    Icon(
                        imageVector = Icons.Default.Error,
                        contentDescription = null,
                        tint = StatusRed,
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        tint = AccentYellow,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (isError) StatusRed else Color.White
                )
            }

            if (!isError && progress.second > 0) {
                Text(
                    text = "${progress.first}/${progress.second}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = AccentYellow,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

private suspend fun checkWebDavConnection(library: ResourceLibrary): Boolean {
    return withContext(Dispatchers.IO) {
        // 连接测试只需测根路径，不测具体存储路径（避免子目录 404/权限问题）
        val rootUrl = "${library.protocol}://${library.host}:${library.port}/"

        fun buildAuthHeader(): String? {
            return if (library.username.isNotEmpty() && library.password.isNotEmpty()) {
                val auth = "${library.username}:${library.password}"
                "Basic ${android.util.Base64.encodeToString(auth.toByteArray(), android.util.Base64.NO_WRAP)}"
            } else null
        }

        // 策略：只要服务器有任何 HTTP 响应就说明网络可达，连接成功。
        // 真正的失败是超时、拒绝连接等网络层异常。

        // 1. 尝试 OPTIONS（最轻量，不需要认证，WebDAV 标准支持）
        try {
            val url = URL(rootUrl)
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "OPTIONS"
            conn.connectTimeout = 8000
            conn.readTimeout = 8000
            conn.instanceFollowRedirects = false
            buildAuthHeader()?.let { conn.setRequestProperty("Authorization", it) }

            val code = conn.responseCode
            conn.disconnect()
            android.util.Log.d("WebDAV", "OPTIONS response: $code for $rootUrl")

            // 任何 HTTP 响应都说明服务器可达
            if (code in 100..599) return@withContext true
        } catch (e: java.net.ProtocolException) {
            android.util.Log.d("WebDAV", "OPTIONS not supported by HttpURLConnection: ${e.message}")
        } catch (e: Exception) {
            android.util.Log.d("WebDAV", "OPTIONS failed: ${e.message}, trying GET...")
        }

        // 2. 降级尝试 GET 根路径
        try {
            val url = URL(rootUrl)
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.connectTimeout = 8000
            conn.readTimeout = 8000
            conn.instanceFollowRedirects = true
            buildAuthHeader()?.let { conn.setRequestProperty("Authorization", it) }

            val code = conn.responseCode
            conn.disconnect()
            android.util.Log.d("WebDAV", "GET response: $code for $rootUrl")

            // 任何 HTTP 响应都代表服务器可达
            code in 100..599
        } catch (e: Exception) {
            android.util.Log.e("WebDAV", "Connection failed: ${e.message}")
            false
        }
    }
}

package com.lomen.tv.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.IconButton
import androidx.tv.material3.IconButtonDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Switch
import androidx.tv.material3.SwitchDefaults
import androidx.tv.material3.Text
import com.lomen.tv.ui.theme.BackgroundDark
import com.lomen.tv.ui.theme.GlassBackground
import androidx.compose.ui.graphics.Brush
import com.lomen.tv.ui.theme.PrimaryYellow
import com.lomen.tv.ui.theme.SurfaceDark
import com.lomen.tv.ui.theme.TextMuted
import com.lomen.tv.ui.theme.TextPrimary
import com.lomen.tv.ui.theme.TextSecondary
import com.lomen.tv.domain.model.ResourceLibrary
import com.lomen.tv.ui.viewmodel.VersionUpdateViewModel
import com.lomen.tv.domain.model.VersionInfo
import com.lomen.tv.domain.service.DownloadService
import com.lomen.tv.ui.components.VersionUpdateDialog
import com.lomen.tv.ui.components.DownloadProgressToast
import java.util.UUID
import kotlinx.coroutines.launch
import com.lomen.tv.data.preferences.TmdbApiPreferences
import com.lomen.tv.ui.viewmodel.ResourceLibraryViewModel

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    sharedResourceLibraryViewModel: ResourceLibraryViewModel,
    sharedMediaSyncViewModel: com.lomen.tv.ui.viewmodel.MediaSyncViewModel,
    libraryToEdit: ResourceLibrary? = null
) {
    var selectedCategory by remember { mutableIntStateOf(0) }
    var showWebDavDialog by remember { mutableStateOf(false) }
    var showEditWebDavDialog by remember { mutableStateOf(libraryToEdit != null) }
    
    // 版本更新相关状态
    val versionUpdateViewModel: VersionUpdateViewModel = hiltViewModel()
    val versionInfo by versionUpdateViewModel.versionInfo.collectAsState()
    val hasUpdate by versionUpdateViewModel.hasUpdate.collectAsState()
    var showVersionUpdateDialog by remember { mutableStateOf(false) }
    
    // 首页设置对话框状态
    var showSortDialog by remember { mutableStateOf(false) }
    var showClearCacheDialog by remember { mutableStateOf(false) }
    var showForceRescrapeDialog by remember { mutableStateOf(false) }
    var showClearWatchHistoryDialog by remember { mutableStateOf(false) }
    var showTmdbApiDialog by remember { mutableStateOf(false) }
    var showSuccessMessage by remember { mutableStateOf<String?>(null) }
    
    // TMDB API 状态 - 提前声明以便在 UI 中使用
    val tmdbApiViewModel: com.lomen.tv.ui.viewmodel.TmdbApiViewModel = hiltViewModel()
    val hasCustomTmdbKey by tmdbApiViewModel.hasCustomApiKey.collectAsState(initial = false)
    val currentApiKey by tmdbApiViewModel.apiKey.collectAsState(initial = TmdbApiPreferences.DEFAULT_API_KEY)
    
    val categories = listOf(
        SettingCategory("资源管理", Icons.Default.CloudUpload),
        SettingCategory("首页设置", Icons.Default.Settings),
        SettingCategory("播放设置", Icons.Default.PlayArrow),
        SettingCategory("关于应用", Icons.Default.Info)
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
    ) {
        Row(
            modifier = Modifier.fillMaxSize()
        ) {
            // 左侧导航栏
            SettingsSidebar(
                categories = categories,
                selectedIndex = selectedCategory,
                onCategorySelected = { selectedCategory = it },
                onNavigateBack = onNavigateBack,
                hasUpdate = hasUpdate
            )

            // 右侧内容区
            SettingsContent(
                selectedCategory = selectedCategory,
                onShowWebDavDialog = { showWebDavDialog = true },
                onShowSortDialog = { showSortDialog = true },
                onShowClearCacheDialog = { showClearCacheDialog = true },
                onShowForceRescrapeDialog = { showForceRescrapeDialog = true },
                onShowClearWatchHistoryDialog = { showClearWatchHistoryDialog = true },
                onShowTmdbApiDialog = { showTmdbApiDialog = true },
                onShowVersionUpdateDialog = { showVersionUpdateDialog = true },
                hasUpdate = hasUpdate,
                versionInfo = versionInfo,
                hasCustomTmdbKey = hasCustomTmdbKey,
                currentApiKey = currentApiKey,
                modifier = Modifier.weight(1f)
            )
        }
        
        // WebDAV配置弹窗 - 添加新资源库（放在Box内部确保全屏覆盖）
        if (showWebDavDialog) {
            WebDavConfigDialog(
                onDismiss = { showWebDavDialog = false },
                onConfigReceived = { config ->
                    // 将WebDAV配置转换为ResourceLibrary并添加到ViewModel
                    val newLibrary = ResourceLibrary(
                        id = UUID.randomUUID().toString(),
                        name = "WebDAV-${config.host}",
                        type = ResourceLibrary.LibraryType.WEBDAV,
                        protocol = config.protocol,
                        host = config.host,
                        port = config.port,
                        path = config.path,
                        username = config.username,
                        password = config.password,
                        isActive = true
                    )
                    sharedResourceLibraryViewModel.addLibrary(newLibrary)
                    showWebDavDialog = false
                }
            )
        }

        // WebDAV编辑弹窗 - 编辑现有资源库（放在Box内部确保全屏覆盖）
        if (showEditWebDavDialog && libraryToEdit != null) {
            WebDavConfigDialog(
                onDismiss = { 
                    showEditWebDavDialog = false
                    // 如果用户取消编辑，返回上一页
                    onNavigateBack()
                },
                onConfigReceived = { config ->
                    // 更新现有资源库
                    val updatedLibrary = libraryToEdit.copy(
                        name = "WebDAV-${config.host}",
                        protocol = config.protocol,
                        host = config.host,
                        port = config.port,
                        path = config.path,
                        username = config.username,
                        password = config.password
                    )
                    sharedResourceLibraryViewModel.addLibrary(updatedLibrary)
                    showEditWebDavDialog = false
                    // 编辑完成后返回上一页
                    onNavigateBack()
                },
                existingLibrary = libraryToEdit
            )
        }
    }
    
    // 首页设置对话框
    val resourceLibraryViewModel: ResourceLibraryViewModel = sharedResourceLibraryViewModel
    val mediaSyncViewModel: com.lomen.tv.ui.viewmodel.MediaSyncViewModel = sharedMediaSyncViewModel
    val context = androidx.compose.ui.platform.LocalContext.current
    val sortPreferences = remember {
        com.lomen.tv.data.preferences.MediaTypeSortPreferences(context)
    }
    val currentLibrary = resourceLibraryViewModel.getCurrentLibrary()
    
    // 对话框回调
    val onShowSortDialog = { showSortDialog = true }
    val onShowClearCacheDialog = { showClearCacheDialog = true }
    val onShowForceRescrapeDialog = { showForceRescrapeDialog = true }
    val onShowClearWatchHistoryDialog = { showClearWatchHistoryDialog = true }
    val onShowTmdbApiDialog = { showTmdbApiDialog = true }
    
    // 检查版本更新
    LaunchedEffect(Unit) {
        val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        val currentVersionCode = packageInfo.versionCode
        versionUpdateViewModel.checkForUpdates(currentVersionCode)
    }
    
    // 监听刮削状态
    val syncState by mediaSyncViewModel.syncState.collectAsState()
    val syncProgress by mediaSyncViewModel.syncProgress.collectAsState()
    
    // 刮削完成后显示提示
    LaunchedEffect(syncState) {
        if (syncState is com.lomen.tv.ui.viewmodel.MediaSyncViewModel.SyncState.Completed) {
            showSuccessMessage = "刮削完成"
            mediaSyncViewModel.resetState()
        } else if (syncState is com.lomen.tv.ui.viewmodel.MediaSyncViewModel.SyncState.Error) {
            val error = (syncState as com.lomen.tv.ui.viewmodel.MediaSyncViewModel.SyncState.Error).message
            showSuccessMessage = "刮削失败: $error"
            mediaSyncViewModel.resetState()
        }
    }
    
    // 成功提示
    if (showSuccessMessage != null) {
        SuccessToast(message = showSuccessMessage!!)
        androidx.compose.runtime.LaunchedEffect(showSuccessMessage) {
            kotlinx.coroutines.delay(2000)
            showSuccessMessage = null
        }
    }
    
    // 刮削进度提示
    when (syncState) {
        is com.lomen.tv.ui.viewmodel.MediaSyncViewModel.SyncState.Scanning -> {
            ScrapeProgressToast(
                message = "正在扫描文件...",
                progress = null
            )
        }
        is com.lomen.tv.ui.viewmodel.MediaSyncViewModel.SyncState.Scraping -> {
            val (current, total) = syncProgress
            ScrapeProgressToast(
                message = "正在刮削媒体信息...",
                progress = if (total > 0) (current to total) else null
            )
        }
        else -> { /* 不显示 */ }
    }
    
    // 媒体分类排序对话框
    if (showSortDialog) {
        MediaTypeSortDialog(
            sortPreferences = sortPreferences,
            onDismiss = { showSortDialog = false },
            onSuccess = {
                showSortDialog = false
                showSuccessMessage = "排序设置已保存"
            }
        )
    }
    
    // 强制全量刮削确认对话框
    if (showForceRescrapeDialog && currentLibrary != null) {
        ConfirmDialog(
            title = "强制全量刮削",
            message = "确定要重新刮削所有媒体文件吗？\n这将花费较长时间，建议在空闲时执行。",
            onConfirm = {
                mediaSyncViewModel.syncLibrary(currentLibrary)
                showForceRescrapeDialog = false
                showSuccessMessage = "已开始全量刮削"
            },
            onDismiss = { showForceRescrapeDialog = false }
        )
    }
    
    // 清除缓存确认对话框
    if (showClearCacheDialog) {
        ConfirmDialog(
            title = "清除缓存数据",
            message = "确定要清除所有媒体元数据缓存吗？\n服务器列表将被保留。",
            onConfirm = {
                resourceLibraryViewModel.clearAllCache()
                showClearCacheDialog = false
                showSuccessMessage = "缓存已清除"
            },
            onDismiss = { showClearCacheDialog = false }
        )
    }
    
    // TMDB API 设置对话框
    if (showTmdbApiDialog) {
        TmdbApiSettingsDialog(
            onDismiss = { showTmdbApiDialog = false }
        )
    }
    
    // 版本更新对话框
    if (showVersionUpdateDialog && versionInfo != null) {
        val context = LocalContext.current
        val scope = rememberCoroutineScope()
        val currentVersionInfo = versionInfo
        if (currentVersionInfo != null) {
            VersionUpdateDialog(
                versionInfo = currentVersionInfo,
                onUpdate = {
                    showVersionUpdateDialog = false
                    // 开始下载
                    versionUpdateViewModel.startDownloadProgress()
                    val downloadService = DownloadService(context)
                    scope.launch {
                        downloadService.downloadApk(
                            versionInfo = currentVersionInfo,
                            onProgress = { progress ->
                                versionUpdateViewModel.updateDownloadProgress(progress)
                            },
                            onComplete = { apkFile ->
                                versionUpdateViewModel.completeDownload()
                                if (apkFile != null) {
                                    downloadService.installApk(apkFile)
                                }
                            }
                        )
                    }
                },
                onCancel = {
                    showVersionUpdateDialog = false
                }
            )
        }
    }
    
    // 下载进度提示
    val isDownloading by versionUpdateViewModel.isDownloading.collectAsState()
    val downloadProgress by versionUpdateViewModel.downloadProgress.collectAsState()
    if (isDownloading) {
        DownloadProgressToast(progress = downloadProgress)
    }
    
    // 清空最近播放记录确认对话框
    if (showClearWatchHistoryDialog) {
        val homeViewModel: com.lomen.tv.ui.screens.home.HomeViewModel = androidx.hilt.navigation.compose.hiltViewModel()
        val coroutineScope = rememberCoroutineScope()
        ConfirmDialog(
            title = "清空最近播放记录",
            message = "确定要清空所有最近播放记录吗？\n此操作不可恢复。",
            onConfirm = {
                coroutineScope.launch {
                    homeViewModel.watchHistoryService.clearAllWatchHistory()
                    showClearWatchHistoryDialog = false
                    showSuccessMessage = "最近播放记录已清空"
                }
            },
            onDismiss = { showClearWatchHistoryDialog = false }
        )
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun SettingsSidebar(
    categories: List<SettingCategory>,
    selectedIndex: Int,
    onCategorySelected: (Int) -> Unit,
    onNavigateBack: () -> Unit,
    hasUpdate: Boolean
) {
    Column(
        modifier = Modifier
            .width(240.dp)
            .fillMaxHeight()
            .background(SurfaceDark)
            .padding(24.dp)
    ) {
        // 返回按钮和标题
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 32.dp)
        ) {
            IconButton(
                onClick = onNavigateBack,
                colors = IconButtonDefaults.colors(
                    containerColor = Color.Transparent,
                    contentColor = TextPrimary,
                    focusedContainerColor = PrimaryYellow,
                    focusedContentColor = BackgroundDark
                ),
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "返回",
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Logo
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(PrimaryYellow),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = BackgroundDark,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = "设置中心",
                style = MaterialTheme.typography.headlineMedium,
                color = TextPrimary
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 导航菜单
        categories.forEachIndexed { index, category ->
            val isSelected = index == selectedIndex
            var isFocused by remember { mutableStateOf(false) }
            Button(
                onClick = { onCategorySelected(index) },
                colors = ButtonDefaults.colors(
                    containerColor = if (isSelected) Color.Transparent else Color.Transparent,
                    contentColor = if (isSelected) PrimaryYellow else TextPrimary,
                    focusedContainerColor = PrimaryYellow,
                    focusedContentColor = BackgroundDark
                ),
                shape = ButtonDefaults.shape(shape = RoundedCornerShape(16.dp)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .onFocusChanged { isFocused = it.isFocused }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 12.dp)
                ) {
                    Icon(
                        imageVector = category.icon,
                        contentDescription = null,
                        tint = when {
                            isFocused -> BackgroundDark
                            isSelected -> PrimaryYellow
                            else -> TextPrimary
                        },
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = category.name,
                        style = MaterialTheme.typography.bodyLarge,
                        color = when {
                            isFocused -> BackgroundDark
                            isSelected -> PrimaryYellow
                            else -> TextPrimary
                        }
                    )
                    // 版本更新红点提示
                    if (index == 3 && hasUpdate) { // 3 是"关于应用"的索引
                        Spacer(modifier = Modifier.weight(1f))
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color.Red)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // 系统时间 - 动态更新
        var currentTime by remember { mutableStateOf("") }
        LaunchedEffect(Unit) {
            while (true) {
                val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                currentTime = sdf.format(Date())
                delay(60000) // 每分钟更新一次
            }
        }
        Text(
            text = "系统时间：$currentTime",
            style = MaterialTheme.typography.bodySmall,
            color = TextMuted
        )
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun SettingsContent(
    selectedCategory: Int,
    onShowWebDavDialog: () -> Unit,
    onShowSortDialog: () -> Unit,
    onShowClearCacheDialog: () -> Unit,
    onShowForceRescrapeDialog: () -> Unit,
    onShowClearWatchHistoryDialog: () -> Unit,
    onShowTmdbApiDialog: () -> Unit,
    onShowVersionUpdateDialog: () -> Unit,
    hasUpdate: Boolean,
    versionInfo: VersionInfo?,
    hasCustomTmdbKey: Boolean = false,
    currentApiKey: String = "",
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        BackgroundDark,
                        SurfaceDark.copy(alpha = 0.5f)
                    )
                )
            )
            .padding(48.dp)
    ) {
        androidx.tv.foundation.lazy.list.TvLazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 80.dp),
            pivotOffsets = androidx.tv.foundation.PivotOffsets(parentFraction = 0.1f)
        ) {
            when (selectedCategory) {
                0 -> { // 资源管理
                    item {
                        SectionTitle(title = "资源导入", accentColor = PrimaryYellow)
                        Spacer(modifier = Modifier.height(16.dp))
                        ResourceImportSection(
                            onShowWebDavDialog = onShowWebDavDialog
                        )
                    }
                }
                1 -> { // 首页设置
                    item {
                        SectionTitle(title = "首页设置", accentColor = PrimaryYellow)
                        Spacer(modifier = Modifier.height(16.dp))
                        HomeSettingsSection(
                            onShowSortDialog = onShowSortDialog,
                            onShowClearCacheDialog = onShowClearCacheDialog,
                            onShowForceRescrapeDialog = onShowForceRescrapeDialog,
                            onShowTmdbApiDialog = onShowTmdbApiDialog,
                            hasCustomTmdbKey = hasCustomTmdbKey,
                            currentApiKey = currentApiKey
                        )
                    }
                }
                2 -> { // 播放设置
                    item {
                        SectionTitle(title = "播放设置", accentColor = PrimaryYellow)
                        Spacer(modifier = Modifier.height(16.dp))
                        PlaybackSettingsSection(
                            onShowClearWatchHistoryDialog = onShowClearWatchHistoryDialog
                        )
                    }
                }
                3 -> { // 关于应用
                    item {
                        SectionTitle(title = "关于应用", accentColor = PrimaryYellow)
                        Spacer(modifier = Modifier.height(16.dp))
                        AboutSection(
                            hasUpdate = hasUpdate,
                            versionInfo = versionInfo,
                            onVersionUpdateClick = onShowVersionUpdateDialog
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun SectionTitle(title: String, accentColor: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
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
            style = MaterialTheme.typography.headlineSmall,
            color = TextPrimary
        )
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun ResourceImportSection(
    onShowWebDavDialog: () -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // 连通网盘
        SettingCard(
            icon = Icons.Default.CloudUpload,
            iconBackgroundColor = Color.White.copy(alpha = 0.4f),
            iconTint = Color(0xFF60a5fa),
            title = "连通网盘",
            subtitle = "支持二级文件夹扫描与极速预览",
            onClick = { /* TODO: 网盘配置 */ },
            modifier = Modifier.weight(1f)
        )

        // 添加WebDAV网盘
        SettingCard(
            icon = Icons.Default.Folder,
            iconBackgroundColor = Color.White.copy(alpha = 0.4f),
            iconTint = Color(0xFF34d399),
            title = "添加WebDAV网盘",
            subtitle = "通过局域网或外部存储进行挂载",
            onClick = onShowWebDavDialog,
            modifier = Modifier.weight(1f)
        )
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun HomeSettingsSection(
    onShowSortDialog: () -> Unit,
    onShowClearCacheDialog: () -> Unit,
    onShowForceRescrapeDialog: () -> Unit,
    onShowTmdbApiDialog: () -> Unit,
    hasCustomTmdbKey: Boolean = false,
    currentApiKey: String = ""
) {
    val resourceLibraryViewModel: com.lomen.tv.ui.viewmodel.ResourceLibraryViewModel = hiltViewModel()
    val currentLibrary = resourceLibraryViewModel.getCurrentLibrary()
    
    Column {
        // 媒体分类排序
        SettingCard(
            icon = Icons.Default.Settings,
            iconBackgroundColor = Color.White.copy(alpha = 0.4f),
            iconTint = Color(0xFF34d399),
            title = "媒体分类排序",
            subtitle = "自定义首页显示顺序（电视剧、电影、综艺等）",
            onClick = onShowSortDialog,
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // TMDB API 设置 - 显示当前状态
        // 判断是否有自定义API Key：有自定义Key且不等于默认值
        val isCustomKey = hasCustomTmdbKey && 
                          currentApiKey.isNotBlank() && 
                          currentApiKey != TmdbApiPreferences.DEFAULT_API_KEY
        
        val apiStatusText = if (isCustomKey) {
            // 显示API Key的前8位和后4位，中间用...代替
            val keyLength = currentApiKey.length
            if (keyLength > 12) {
                "${currentApiKey.take(8)}...${currentApiKey.takeLast(4)}"
            } else {
                currentApiKey
            }
        } else {
            "API为空 请尽快配置"
        }
        SettingCard(
            icon = Icons.Default.CloudUpload,
            iconBackgroundColor = Color.White.copy(alpha = 0.4f),
            iconTint = Color(0xFF3b82f6),
            title = "TMDB API 设置",
            subtitle = apiStatusText,
            onClick = onShowTmdbApiDialog,
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // 强制全量刮削
        SettingCard(
            icon = Icons.Default.Refresh,
            iconBackgroundColor = Color.White.copy(alpha = 0.4f),
            iconTint = Color(0xFFa78bfa),
            title = "强制全量刮削",
            subtitle = if (currentLibrary != null) "重新刮削所有媒体文件（忽略缓存）" else "请先选择资源库",
            onClick = { if (currentLibrary != null) onShowForceRescrapeDialog() },
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // 清除缓存
        SettingCard(
            icon = Icons.Default.Refresh,
            iconBackgroundColor = Color.White.copy(alpha = 0.4f),
            iconTint = Color(0xFFfb923c),
            title = "清除缓存数据",
            subtitle = "清除所有媒体元数据缓存，保留服务器列表",
            onClick = onShowClearCacheDialog,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun PlaybackSettingsSection(
    onShowClearWatchHistoryDialog: () -> Unit = {}
) {
    Column {
        // 快进快退时长
        SettingListItem(
            title = "快进快退时长",
            subtitle = "设置遥控器左右键跳转的秒数",
            trailing = { itemFocused ->
                Text(
                    text = "15s",
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (itemFocused) Color.Black else PrimaryYellow,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                )
            }
        )

        Spacer(modifier = Modifier.height(2.dp))

        // 自动跳过片头片尾
        var autoSkipEnabled by remember { mutableStateOf(true) }
        var autoSkipFocused by remember { mutableStateOf(false) }
        SettingListItem(
            title = "自动跳过片头片尾",
            subtitle = "智能识别影视内容，自动跳转至正片",
            trailing = { itemFocused ->
                Box(
                    modifier = Modifier
                        .border(
                            width = 2.dp,
                            color = if (itemFocused || autoSkipFocused) Color.White else Color(0xFF1a1a1e),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .padding(2.dp)
                ) {
                    Switch(
                        checked = autoSkipEnabled,
                        onCheckedChange = { autoSkipEnabled = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = if ((itemFocused || autoSkipFocused) && autoSkipEnabled) {
                                Color(0xFFF59E0B) // 橙黄色 - 开启+聚焦
                            } else if (autoSkipEnabled) {
                                com.lomen.tv.ui.theme.SuccessGreen // 绿色 - 开启
                            } else {
                                Color(0xFF444444) // 深灰 - 关闭
                            },
                            uncheckedThumbColor = Color.White,
                            uncheckedTrackColor = Color(0xFF444444) // 深灰 - 关闭
                        ),
                        modifier = Modifier.onFocusChanged { autoSkipFocused = it.isFocused }
                    )
                }
            }
        )

        Spacer(modifier = Modifier.height(2.dp))

        // 记忆续播功能
        var rememberPlaybackEnabled by remember { mutableStateOf(true) }
        var rememberPlaybackFocused by remember { mutableStateOf(false) }
        SettingListItem(
            title = "记忆续播功能",
            subtitle = "自动记录播放进度，下次打开即刻续播",
            trailing = { itemFocused ->
                Box(
                    modifier = Modifier
                        .border(
                            width = 2.dp,
                            color = if (itemFocused || rememberPlaybackFocused) Color.White else Color(0xFF1a1a1e),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .padding(2.dp)
                ) {
                    Switch(
                        checked = rememberPlaybackEnabled,
                        onCheckedChange = { rememberPlaybackEnabled = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = if ((itemFocused || rememberPlaybackFocused) && rememberPlaybackEnabled) {
                                Color(0xFFF59E0B) // 橙黄色 - 开启+聚焦
                            } else if (rememberPlaybackEnabled) {
                                com.lomen.tv.ui.theme.SuccessGreen // 绿色 - 开启
                            } else {
                                Color(0xFF444444) // 深灰 - 关闭
                            },
                            uncheckedThumbColor = Color.White,
                            uncheckedTrackColor = Color(0xFF444444) // 深灰 - 关闭
                        ),
                        modifier = Modifier.onFocusChanged { rememberPlaybackFocused = it.isFocused }
                    )
                }
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 清空最近播放记录
        SettingCard(
            icon = Icons.Default.Refresh,
            iconBackgroundColor = Color.White.copy(alpha = 0.4f),
            iconTint = Color(0xFFef4444),
            title = "清空最近播放记录",
            subtitle = "清除所有最近播放记录，此操作不可恢复",
            onClick = onShowClearWatchHistoryDialog,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun AboutSection(
    hasUpdate: Boolean,
    versionInfo: VersionInfo?,
    onVersionUpdateClick: () -> Unit
) {
    Column {
        Row(
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // 版本更新
            val currentVersion = "1.0.2"
            val versionSubtitle = if (hasUpdate && versionInfo != null) {
                "当前版本: v$currentVersion | 最新版本: v${versionInfo.versionName}"
            } else {
                "当前版本: v$currentVersion (稳定版)"
            }
            InfoCard(
                icon = Icons.Default.Refresh,
                iconBackgroundColor = Color.White.copy(alpha = 0.4f),
                iconTint = Color(0xFFc084fc),
                title = "版本更新",
                subtitle = versionSubtitle,
                badge = if (hasUpdate) "New" else null,
                onClick = onVersionUpdateClick,
                modifier = Modifier.weight(1f)
            )

            // 应用统计
            InfoCard(
                icon = Icons.Default.Timer,
                iconBackgroundColor = Color.White.copy(alpha = 0.4f),
                iconTint = TextMuted,
                title = "应用统计",
                subtitle = "累计播放时长: 128小时",
                badge = null,
                onClick = {},
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // 应用信息
        var isFocused by remember { mutableStateOf(false) }
        Card(
            onClick = {},
            colors = CardDefaults.colors(
                containerColor = SurfaceDark,
                focusedContainerColor = PrimaryYellow
            ),
            scale = CardDefaults.scale(
                scale = 1.0f,
                focusedScale = 1.02f,
                pressedScale = 1.0f
            ),
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { isFocused = it.isFocused }
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                Text(
                    text = "柠檬TV",
                    style = MaterialTheme.typography.headlineSmall,
                    color = if (isFocused) Color.Black else TextPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "一款专为Android TV设计的视频播放器，支持网盘资源导入、智能跳过片头片尾、记忆续播等功能。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isFocused) Color.Black.copy(alpha = 0.8f) else TextSecondary
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "© 2026 柠檬TV 版权所有",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isFocused) Color.Black.copy(alpha = 0.7f) else TextMuted
                )
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
private fun SettingCard(
    icon: ImageVector,
    iconBackgroundColor: Color,
    iconTint: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }
    
    Card(
        onClick = onClick,
        colors = CardDefaults.colors(
            containerColor = SurfaceDark,
            focusedContainerColor = PrimaryYellow
        ),
        scale = CardDefaults.scale(
            scale = 1.0f,
            focusedScale = 1.02f,
            pressedScale = 1.0f
        ),
        modifier = modifier
            .height(120.dp)
            .onFocusChanged { isFocused = it.isFocused }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(iconBackgroundColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isFocused) Color.Black else iconTint,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = if (isFocused) Color.Black else TextPrimary
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = if (isFocused) Color.Black.copy(alpha = 0.8f) else TextSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun SettingListItem(
    title: String,
    subtitle: String,
    trailing: @Composable (Boolean) -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    
    Card(
        onClick = {},
        colors = CardDefaults.colors(
            containerColor = SurfaceDark,
            focusedContainerColor = PrimaryYellow
        ),
        scale = CardDefaults.scale(
            scale = 1.0f,
            focusedScale = 1.02f,
            pressedScale = 1.0f
        ),
        modifier = Modifier
            .fillMaxWidth()
            .onFocusChanged { isFocused = it.isFocused }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isFocused) Color.Black else TextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isFocused) Color.Black.copy(alpha = 0.7f) else TextMuted
                )
            }
            trailing(isFocused)
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun InfoCard(
    icon: ImageVector,
    iconBackgroundColor: Color,
    iconTint: Color,
    title: String,
    subtitle: String,
    badge: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }
    
    Card(
        onClick = onClick,
        colors = CardDefaults.colors(
            containerColor = SurfaceDark,
            focusedContainerColor = PrimaryYellow
        ),
        scale = CardDefaults.scale(
            scale = 1.0f,
            focusedScale = 1.02f,
            pressedScale = 1.0f
        ),
        modifier = modifier
            .height(100.dp)
            .onFocusChanged { isFocused = it.isFocused }
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(iconBackgroundColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isFocused) Color.Black else iconTint,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (isFocused) Color.Black else TextPrimary,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isFocused) Color.Black.copy(alpha = 0.7f) else TextMuted
                )
            }

            badge?.let {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isFocused) Color.Black.copy(alpha = 0.2f) else PrimaryYellow.copy(alpha = 0.2f))
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isFocused) Color.Black else PrimaryYellow,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun SettingsBottomBar(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .padding(bottom = 24.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(GlassBackground)
            .padding(horizontal = 24.dp, vertical = 12.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(32.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 上下键提示
            KeyHint(key = "↑↓", description = "切换选项")
            // OK键提示
            KeyHint(key = "OK", description = "确定")
            // 返回键提示
            KeyHint(key = "Back", description = "返回")
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun KeyHint(key: String, description: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .border(1.dp, TextMuted.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                text = key,
                style = MaterialTheme.typography.labelSmall,
                color = TextPrimary,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
            )
        }
        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = TextMuted
        )
    }
}

// Data class for setting categories
private data class SettingCategory(
    val name: String,
    val icon: ImageVector
)

/**
 * 确认对话框
 */
@OptIn(ExperimentalTvMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
private fun ConfirmDialog(
    title: String,
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f))
            .onPreviewKeyEvent { keyEvent ->
                // 处理返回键
                if (keyEvent.key == Key.Back && keyEvent.type == KeyEventType.KeyUp) {
                    onDismiss()
                    true
                } else {
                    false
                }
            }
            .clickable(
                onClick = { },
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ),
        contentAlignment = Alignment.Center
    ) {
        Card(
            onClick = {},
            colors = CardDefaults.colors(
                containerColor = SurfaceDark,
                focusedContainerColor = SurfaceDark
            ),
            modifier = Modifier
                .width(600.dp)
                .padding(32.dp)
                .focusProperties {
                    // 禁止焦点向外移动，实现焦点陷阱
                    canFocus = true
                }
        ) {
            Column(
                modifier = Modifier.padding(32.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = TextPrimary
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 取消按钮 - 未选中时灰色，高亮时黄色底黑色字
                    var cancelButtonFocused by remember { mutableStateOf(false) }
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.colors(
                            containerColor = Color.Transparent,
                            contentColor = TextMuted,
                            focusedContainerColor = PrimaryYellow,
                            focusedContentColor = BackgroundDark
                        ),
                        shape = ButtonDefaults.shape(shape = RoundedCornerShape(12.dp)),
                        modifier = Modifier
                            .focusRequester(focusRequester)
                            .onFocusChanged { cancelButtonFocused = it.isFocused }
                            .focusProperties {
                                // 锁定左键和上键，防止光标移出窗口
                                left = FocusRequester.Cancel
                                up = FocusRequester.Cancel
                            }
                    ) {
                        Text(
                            text = "取消",
                            color = if (cancelButtonFocused) BackgroundDark else TextMuted
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    // 确定按钮 - 未选中时灰色，高亮时黄色底黑色字
                    var confirmButtonFocused by remember { mutableStateOf(false) }
                    Button(
                        onClick = onConfirm,
                        colors = ButtonDefaults.colors(
                            containerColor = Color.Transparent,
                            contentColor = TextMuted,
                            focusedContainerColor = PrimaryYellow,
                            focusedContentColor = BackgroundDark
                        ),
                        shape = ButtonDefaults.shape(shape = RoundedCornerShape(12.dp)),
                        modifier = Modifier
                            .onFocusChanged { confirmButtonFocused = it.isFocused }
                            .focusProperties {
                                // 锁定上键，防止光标移出窗口
                                up = FocusRequester.Cancel
                            }
                    ) {
                        Text(
                            text = "确定",
                            color = if (confirmButtonFocused) BackgroundDark else TextMuted
                        )
                    }
                }
            }
        }
    }
    
    // 对话框显示时，默认将焦点给"取消"按钮（安全操作），避免误触"确定"
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
}

/**
 * 媒体分类排序对话框
 */
@OptIn(ExperimentalTvMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
private fun MediaTypeSortDialog(
    sortPreferences: com.lomen.tv.data.preferences.MediaTypeSortPreferences,
    onDismiss: () -> Unit,
    onSuccess: () -> Unit
) {
    val currentOrder by sortPreferences.sortOrder.collectAsState(
        initial = com.lomen.tv.data.preferences.MediaTypeSortPreferences.DEFAULT_SORT_ORDER
    )
    var editableOrder by remember(currentOrder) { mutableStateOf(currentOrder) }
    
    val mediaTypeLabels = mapOf(
        com.lomen.tv.domain.model.MediaType.TV_SHOW to "电视剧",
        com.lomen.tv.domain.model.MediaType.MOVIE to "电影",
        com.lomen.tv.domain.model.MediaType.VARIETY to "综艺",
        com.lomen.tv.domain.model.MediaType.CONCERT to "演唱会",
        com.lomen.tv.domain.model.MediaType.DOCUMENTARY to "纪录片",
        com.lomen.tv.domain.model.MediaType.OTHER to "其它"
    )
    
    val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f))
            .onPreviewKeyEvent { keyEvent ->
                // 处理返回键
                if (keyEvent.key == Key.Back && keyEvent.type == KeyEventType.KeyUp) {
                    onDismiss()
                    true
                } else {
                    false
                }
            }
            .clickable(
                onClick = { },
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ),
        contentAlignment = Alignment.Center
    ) {
        Card(
            onClick = {},
            colors = CardDefaults.colors(
                containerColor = SurfaceDark,
                focusedContainerColor = SurfaceDark
            ),
            modifier = Modifier
                .width(700.dp)
                .height(600.dp)
                .padding(32.dp)
                .focusProperties {
                    // 禁止焦点向外移动，实现焦点陷阱
                    canFocus = true
                }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp)
            ) {
                // 标题栏（固定）
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "媒体分类排序",
                        style = MaterialTheme.typography.headlineSmall,
                        color = TextPrimary
                    )
                    
                    // 恢复默认按钮 - 未选中时灰色，高亮时黄色底黑色字
                    var restoreButtonFocused by remember { mutableStateOf(false) }
                    Button(
                        onClick = {
                            editableOrder = com.lomen.tv.data.preferences.MediaTypeSortPreferences.DEFAULT_SORT_ORDER
                        },
                        colors = ButtonDefaults.colors(
                            containerColor = Color.Transparent,
                            contentColor = TextMuted,
                            focusedContainerColor = PrimaryYellow,
                            focusedContentColor = BackgroundDark
                        ),
                        shape = ButtonDefaults.shape(shape = RoundedCornerShape(12.dp)),
                        modifier = Modifier
                            .focusRequester(focusRequester)
                            .onFocusChanged { restoreButtonFocused = it.isFocused }
                            .focusProperties {
                                // 锁定上键，防止光标移出窗口
                                up = FocusRequester.Cancel
                            }
                    ) {
                        Text(
                            text = "恢复默认",
                            color = if (restoreButtonFocused) BackgroundDark else TextMuted
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 媒体类型列表（可滚动）
                androidx.tv.foundation.lazy.list.TvLazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    items(editableOrder.size) { index ->
                        val mediaType = editableOrder[index]
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(BackgroundDark)
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${index + 1}",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = PrimaryYellow
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Text(
                                    text = mediaTypeLabels[mediaType] ?: mediaType.name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = TextPrimary
                                )
                            }
                            
                            Row {
                                if (index > 0) {
                                    IconButton(
                                        onClick = {
                                            val newList = editableOrder.toMutableList()
                                            newList[index] = newList[index - 1].also { newList[index - 1] = newList[index] }
                                            editableOrder = newList
                                        },
                                        colors = IconButtonDefaults.colors(
                                            containerColor = Color.Transparent,
                                            contentColor = TextMuted,
                                            focusedContainerColor = PrimaryYellow,
                                            focusedContentColor = BackgroundDark
                                        )
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PlayArrow,
                                            contentDescription = "上移",
                                            modifier = Modifier.size(20.dp).rotate(-90f)
                                        )
                                    }
                                }
                                if (index < editableOrder.size - 1) {
                                    IconButton(
                                        onClick = {
                                            val newList = editableOrder.toMutableList()
                                            newList[index] = newList[index + 1].also { newList[index + 1] = newList[index] }
                                            editableOrder = newList
                                        },
                                        colors = IconButtonDefaults.colors(
                                            containerColor = Color.Transparent,
                                            contentColor = TextMuted,
                                            focusedContainerColor = PrimaryYellow,
                                            focusedContentColor = BackgroundDark
                                        )
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PlayArrow,
                                            contentDescription = "下移",
                                            modifier = Modifier.size(20.dp).rotate(90f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 底部按钮（固定）
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 取消按钮 - 未选中时灰色，高亮时黄色底黑色字
                    var cancelButtonFocused by remember { mutableStateOf(false) }
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.colors(
                            containerColor = Color.Transparent,
                            contentColor = TextMuted,
                            focusedContainerColor = PrimaryYellow,
                            focusedContentColor = BackgroundDark
                        ),
                        shape = ButtonDefaults.shape(shape = RoundedCornerShape(12.dp)),
                        modifier = Modifier
                            .onFocusChanged { cancelButtonFocused = it.isFocused }
                            .focusProperties {
                                // 锁定左键，防止光标移出窗口
                                left = FocusRequester.Cancel
                            }
                    ) {
                        Text(
                            text = "取消",
                            color = if (cancelButtonFocused) BackgroundDark else TextMuted
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    // 保存按钮 - 未选中时灰色，高亮时黄色底黑色字
                    var saveButtonFocused by remember { mutableStateOf(false) }
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                sortPreferences.saveSortOrder(editableOrder)
                                onSuccess()
                            }
                        },
                        colors = ButtonDefaults.colors(
                            containerColor = Color.Transparent,
                            contentColor = TextMuted,
                            focusedContainerColor = PrimaryYellow,
                            focusedContentColor = BackgroundDark
                        ),
                        shape = ButtonDefaults.shape(shape = RoundedCornerShape(12.dp)),
                        modifier = Modifier.onFocusChanged { saveButtonFocused = it.isFocused }
                    ) {
                        Text(
                            text = "保存",
                            color = if (saveButtonFocused) BackgroundDark else TextMuted
                        )
                    }
                }
            }
        }
    }
    
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
}

/**
 * 成功提示Toast
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun SuccessToast(message: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomCenter
    ) {
        Card(
            onClick = {},
            colors = CardDefaults.colors(
                containerColor = Color(0xFF10b981),
                focusedContainerColor = Color(0xFF10b981)
            ),
            modifier = Modifier.padding(bottom = 100.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White
                )
            }
        }
    }
}

/**
 * 刮削进度提示Toast
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun ScrapeProgressToast(
    message: String,
    progress: Pair<Int, Int>? = null
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomCenter
    ) {
        Card(
            onClick = {},
            colors = CardDefaults.colors(
                containerColor = Color(0xFF3b82f6),
                focusedContainerColor = Color(0xFF3b82f6)
            ),
            modifier = Modifier.padding(bottom = 100.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White
                    )
                    if (progress != null) {
                        val (current, total) = progress
                        Text(
                            text = "$current / $total",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }
    }
}

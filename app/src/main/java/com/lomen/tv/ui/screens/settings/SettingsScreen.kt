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
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import com.lomen.tv.service.WebDavConfigServer
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
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import com.lomen.tv.data.preferences.LiveSettingsPreferences
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
        SettingCategory("直播设置", Icons.Default.LiveTv),
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
                    if (index == 4 && hasUpdate) { // 4 是"关于应用"的索引
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
            color = TextMuted,
            modifier = Modifier.padding(bottom = 16.dp) // 增加底部内边距，避免被截断
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
                3 -> { // 直播设置
                    item {
                        SectionTitle(title = "直播设置", accentColor = PrimaryYellow)
                        Spacer(modifier = Modifier.height(16.dp))
                        LiveSettingsSection()
                    }
                }
                4 -> { // 关于应用
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
    val playerSettingsPreferences = androidx.hilt.navigation.compose.hiltViewModel<com.lomen.tv.ui.viewmodel.PlayerSettingsViewModel>()
    val autoSkipEnabled by playerSettingsPreferences.autoSkipIntroOutro.collectAsState(initial = true)
    val rememberPlaybackEnabled by playerSettingsPreferences.rememberPlaybackPosition.collectAsState(initial = true)
    val coroutineScope = rememberCoroutineScope()
    
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
        var autoSkipFocused by remember { mutableStateOf(false) }
        val autoSkipEnabledState by rememberUpdatedState(autoSkipEnabled)
        SettingListItem(
            title = "自动跳过片头片尾",
            subtitle = "智能识别影视内容，自动跳转至正片",
            onClick = {
                // 点击整行也切换开关
                val newValue = !autoSkipEnabledState
                android.util.Log.d("SettingsScreen", "Auto skip intro outro row clicked, new value: $newValue")
                coroutineScope.launch {
                    playerSettingsPreferences.setAutoSkipIntroOutro(newValue)
                }
            },
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
                        onCheckedChange = null, // TV 上使用 onClick 处理
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
        var rememberPlaybackFocused by remember { mutableStateOf(false) }
        val rememberPlaybackEnabledState by rememberUpdatedState(rememberPlaybackEnabled)
        SettingListItem(
            title = "记忆续播功能",
            subtitle = "自动记录播放进度，下次打开即刻续播",
            onClick = {
                // 点击整行也切换开关
                val newValue = !rememberPlaybackEnabledState
                android.util.Log.d("SettingsScreen", "Remember playback position row clicked, new value: $newValue")
                coroutineScope.launch {
                    playerSettingsPreferences.setRememberPlaybackPosition(newValue)
                }
            },
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
                        onCheckedChange = null, // TV 上使用 onClick 处理
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
            val currentVersion = "1.0.3"
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
    trailing: @Composable (Boolean) -> Unit,
    onClick: (() -> Unit)? = null
) {
    var isFocused by remember { mutableStateOf(false) }
    
    Card(
        onClick = { onClick?.invoke() },
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

/**
 * 直播设置区域 - 列表卡片样式
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun LiveSettingsSection() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val liveSettingsPreferences = remember { com.lomen.tv.data.preferences.LiveSettingsPreferences(context) }
    
    // 状态
    var showLiveSourceHistory by remember { mutableStateOf(false) }
    var showEpgHistory by remember { mutableStateOf(false) }
    var showUaHistory by remember { mutableStateOf(false) }
    var showWebConfigQr by remember { mutableStateOf(false) }
    
    // 收集当前设置值
    val liveSourceUrl by liveSettingsPreferences.liveSourceUrl.collectAsState(initial = "")
    val epgUrl by liveSettingsPreferences.epgUrl.collectAsState(initial = "")
    val userAgent by liveSettingsPreferences.userAgent.collectAsState(initial = "")
    val channelChangeFlip by liveSettingsPreferences.channelChangeFlip.collectAsState(initial = false)
    val channelNoSelectEnable by liveSettingsPreferences.channelNoSelectEnable.collectAsState(initial = true)
    val epgEnable by liveSettingsPreferences.epgEnable.collectAsState(initial = true)
    val autoEnterLive by liveSettingsPreferences.autoEnterLive.collectAsState(initial = false)
    val bootStartup by liveSettingsPreferences.bootStartup.collectAsState(initial = false)
    val autoRefreshInterval by liveSettingsPreferences.autoRefreshInterval.collectAsState(initial = 0)
    val liveSourceHistory by liveSettingsPreferences.liveSourceHistory.collectAsState(initial = emptySet())
    val epgUrlHistory by liveSettingsPreferences.epgUrlHistory.collectAsState(initial = emptySet())
    val userAgentHistory by liveSettingsPreferences.userAgentHistory.collectAsState(initial = emptySet())
    
    // 获取WiFi IP
    val wifiIpAddress = remember { getWifiIpAddress(context) }
    val serverUrl = "http://$wifiIpAddress:8893/live"
    
    // 启动 WebDav 配置服务器用于直播设置
    val webDavServer = remember { WebDavConfigServer(context, 8893) }
    DisposableEffect(showWebConfigQr) {
        if (showWebConfigQr) {
            webDavServer.startServerWithLiveConfig(
                onWebDavConfig = { /* 在直播设置页面不处理 WebDAV 配置 */ },
                onLiveConfig = { config ->
                    scope.launch {
                        if (config.liveSourceUrl.isNotBlank()) {
                            liveSettingsPreferences.setLiveSourceUrl(config.liveSourceUrl)
                            liveSettingsPreferences.addLiveSourceToHistory(
                                config.liveSourceName.takeIf { it.isNotBlank() } ?: "自定义源",
                                config.liveSourceUrl
                            )
                        }
                        if (config.epgUrl.isNotBlank()) {
                            liveSettingsPreferences.setEpgUrl(config.epgUrl)
                            liveSettingsPreferences.addEpgUrlToHistory(config.epgUrl)
                        }
                        if (config.userAgent.isNotBlank()) {
                            liveSettingsPreferences.setUserAgent(config.userAgent)
                            liveSettingsPreferences.addUserAgentToHistory(config.userAgent)
                        }
                    }
                }
            )
        }
        onDispose {
            if (showWebConfigQr) {
                webDavServer.stopServer()
            }
        }
    }
    
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 直播源配置卡片
        LiveSettingListCard(
            icon = Icons.Default.LiveTv,
            iconBackgroundColor = Color(0xFFf59e0b).copy(alpha = 0.2f),
            iconTint = Color(0xFFf59e0b),
            title = "直播源配置",
            onClick = { showLiveSourceHistory = true }
        )
        
        // 节目单配置卡片
        LiveSettingListCard(
            icon = Icons.Default.Timer,
            iconBackgroundColor = Color(0xFF60a5fa).copy(alpha = 0.2f),
            iconTint = Color(0xFF60a5fa),
            title = "节目单配置",
            onClick = { showEpgHistory = true }
        )
        
        // 自定义 User-Agent 卡片
        LiveSettingListCard(
            icon = Icons.Default.Settings,
            iconBackgroundColor = Color(0xFF34d399).copy(alpha = 0.2f),
            iconTint = Color(0xFF34d399),
            title = "自定义 User-Agent",
            onClick = { showUaHistory = true }
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // 开关设置 - 使用卡片样式
        // 换台反转
        SettingListItem(
            title = "换台方向反转",
            subtitle = "上下键换台方向反转",
            trailing = { itemFocused ->
                Switch(
                    checked = channelChangeFlip,
                    onCheckedChange = null,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = com.lomen.tv.ui.theme.SuccessGreen,
                        uncheckedThumbColor = Color.White,
                        uncheckedTrackColor = Color(0xFF444444)
                    )
                )
            },
            onClick = {
                scope.launch { liveSettingsPreferences.setChannelChangeFlip(!channelChangeFlip) }
            }
        )

        Spacer(modifier = Modifier.height(2.dp))

        // 数字选台
        SettingListItem(
            title = "启用数字选台",
            subtitle = "使用数字键快速输入频道号",
            trailing = { itemFocused ->
                Switch(
                    checked = channelNoSelectEnable,
                    onCheckedChange = null,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = com.lomen.tv.ui.theme.SuccessGreen,
                        uncheckedThumbColor = Color.White,
                        uncheckedTrackColor = Color(0xFF444444)
                    )
                )
            },
            onClick = {
                scope.launch { liveSettingsPreferences.setChannelNoSelectEnable(!channelNoSelectEnable) }
            }
        )

        Spacer(modifier = Modifier.height(2.dp))

        // 启用节目单
        SettingListItem(
            title = "启用节目单",
            subtitle = "显示频道节目信息",
            trailing = { itemFocused ->
                Switch(
                    checked = epgEnable,
                    onCheckedChange = null,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = com.lomen.tv.ui.theme.SuccessGreen,
                        uncheckedThumbColor = Color.White,
                        uncheckedTrackColor = Color(0xFF444444)
                    )
                )
            },
            onClick = {
                scope.launch { liveSettingsPreferences.setEpgEnable(!epgEnable) }
            }
        )

        Spacer(modifier = Modifier.height(2.dp))

        // 打开APP直接进入直播界面
        SettingListItem(
            title = "打开直接进入直播",
            subtitle = "启动APP后自动进入直播界面",
            trailing = { itemFocused ->
                Switch(
                    checked = autoEnterLive,
                    onCheckedChange = null,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = com.lomen.tv.ui.theme.SuccessGreen,
                        uncheckedThumbColor = Color.White,
                        uncheckedTrackColor = Color(0xFF444444)
                    )
                )
            },
            onClick = {
                scope.launch { liveSettingsPreferences.setAutoEnterLive(!autoEnterLive) }
            }
        )

        Spacer(modifier = Modifier.height(2.dp))

        // 开机启动
        SettingListItem(
            title = "开机启动",
            subtitle = "设备开机后自动启动APP并进入直播",
            trailing = { itemFocused ->
                Switch(
                    checked = bootStartup,
                    onCheckedChange = null,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = com.lomen.tv.ui.theme.SuccessGreen,
                        uncheckedThumbColor = Color.White,
                        uncheckedTrackColor = Color(0xFF444444)
                    )
                )
            },
            onClick = {
                scope.launch { liveSettingsPreferences.setBootStartup(!bootStartup) }
            }
        )

        Spacer(modifier = Modifier.height(2.dp))

        // 定时刷新直播源
        SettingListItem(
            title = "定时刷新直播源",
            subtitle = "每隔指定时间自动刷新直播源（当前：${if (autoRefreshInterval > 0) "${autoRefreshInterval}小时" else "已关闭"}）",
            trailing = { itemFocused ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 选项按钮：关闭、1小时、2小时、4小时、6小时、12小时
                    listOf(0, 1, 2, 4, 6, 12).forEach { hours ->
                        val label = if (hours == 0) "关闭" else "${hours}h"
                        val isSelected = autoRefreshInterval == hours
                        androidx.tv.material3.Button(
                            onClick = { scope.launch { liveSettingsPreferences.setAutoRefreshInterval(hours) } },
                            colors = androidx.tv.material3.ButtonDefaults.colors(
                                containerColor = if (isSelected) com.lomen.tv.ui.theme.PrimaryYellow else Color(0xFF333333),
                                contentColor = if (isSelected) Color.Black else Color.White,
                            ),
                            shape = androidx.tv.material3.ButtonDefaults.shape(
                                shape = RoundedCornerShape(8.dp)
                            ),
                            modifier = Modifier
                                .height(36.dp)
                                .then(
                                    if (isSelected) {
                                        Modifier.border(
                                            width = 2.dp,
                                            color = Color(0xFFB8860B), // 深金色描边
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                    } else {
                                        Modifier
                                    }
                                )
                        ) {
                            Text(label, fontSize = 12.sp)
                        }
                    }
                }
            },
            onClick = {
                // 循环切换：0 -> 1 -> 2 -> 4 -> 6 -> 12 -> 0
                val options = listOf(0, 1, 2, 4, 6, 12)
                val currentIndex = options.indexOf(autoRefreshInterval)
                val nextIndex = (currentIndex + 1) % options.size
                scope.launch { liveSettingsPreferences.setAutoRefreshInterval(options[nextIndex]) }
            }
        )
    }
    
    // 直播源历史列表弹窗
    val currentLiveSourceItem = liveSourceHistory.find { it.second == liveSourceUrl }
    HistoryListDialog(
        title = "历史直播源",
        showDialogProvider = { showLiveSourceHistory },
        onDismissRequest = { showLiveSourceHistory = false },
        items = liveSourceHistory.toList(),
        currentItem = currentLiveSourceItem,
        onSelected = { item ->
            val (name, url) = item
            scope.launch { 
                liveSettingsPreferences.setLiveSourceUrl(url)
                liveSettingsPreferences.addLiveSourceToHistory(name, url)
            }
            showLiveSourceHistory = false
        },
        onDeleted = { item ->
            val (_, url) = item
            scope.launch { liveSettingsPreferences.removeLiveSourceFromHistory(url) }
        },
        onAddNew = {
            showLiveSourceHistory = false
            showWebConfigQr = true
        },
        itemContent = { item, isSelected, isFocused ->
            val (name, url) = item
            Pair(name, url)
        },
        isBuiltInItem = { item ->
            LiveSettingsPreferences.BUILT_IN_LIVE_SOURCES.any { it.second == item.second }
        },
        addNewText = "添加其他直播源"
    )
    
    // 节目单历史列表弹窗
    HistoryListDialog(
        title = "历史节目单",
        showDialogProvider = { showEpgHistory },
        onDismissRequest = { showEpgHistory = false },
        items = epgUrlHistory.toList(),
        currentItem = epgUrl.takeIf { it.isNotBlank() },
        onSelected = { url ->
            scope.launch { 
                liveSettingsPreferences.setEpgUrl(url)
                liveSettingsPreferences.addEpgUrlToHistory(url)
            }
            showEpgHistory = false
        },
        onDeleted = { url ->
            scope.launch { liveSettingsPreferences.removeEpgUrlFromHistory(url) }
        },
        onAddNew = {
            showEpgHistory = false
            showWebConfigQr = true
        },
        itemContent = { item, isSelected, isFocused ->
            Pair(
                if (item.contains("51zmt")) "默认节目单" else "自定义节目单",
                item
            )
        },
        isBuiltInItem = { item ->
            LiveSettingsPreferences.BUILT_IN_EPG_URLS.contains(item)
        },
        addNewText = "添加其他节目单"
    )
    
    // User-Agent 历史列表弹窗
    HistoryListDialog(
        title = "历史 User-Agent",
        showDialogProvider = { showUaHistory },
        onDismissRequest = { showUaHistory = false },
        items = userAgentHistory.toList(),
        currentItem = userAgent.takeIf { it.isNotBlank() },
        onSelected = { ua ->
            scope.launch { 
                liveSettingsPreferences.setUserAgent(ua)
                liveSettingsPreferences.addUserAgentToHistory(ua)
            }
            showUaHistory = false
        },
        onDeleted = { ua ->
            scope.launch { liveSettingsPreferences.removeUserAgentFromHistory(ua) }
        },
        onAddNew = {
            showUaHistory = false
            showWebConfigQr = true
        },
        itemContent = { item, isSelected, isFocused ->
            Pair(
                if (item.contains("ExoPlayer")) "默认UA" else "自定义UA",
                item
            )
        },
        isBuiltInItem = { item ->
            LiveSettingsPreferences.BUILT_IN_USER_AGENTS.contains(item)
        },
        addNewText = "添加其他 User-Agent"
    )
    
    // 网页配置二维码弹窗 - 在历史列表中通过"添加其他"触发
    QrCodeDialog(
        text = serverUrl,
        title = "网页配置",
        description = "使用手机扫描二维码访问网页配置界面\n可批量添加直播源、节目单和UA",
        showDialogProvider = { showWebConfigQr },
        onDismissRequest = { showWebConfigQr = false }
    )
}

/**
 * 直播源配置对话框
 */
@OptIn(ExperimentalTvMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
private fun LiveSourceSettingsDialog(
    currentUrl: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var url by remember { mutableStateOf(currentUrl) }
    val focusRequester = remember { FocusRequester() }
    
    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            onClick = {},
            colors = CardDefaults.colors(containerColor = SurfaceDark),
            modifier = Modifier
                .width(600.dp)
                .padding(24.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                Text(
                    text = "配置直播源",
                    style = MaterialTheme.typography.headlineSmall,
                    color = TextPrimary
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "支持 M3U/M3U8 格式的直播源链接",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMuted
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // URL 输入框
                OutlinedTextField(
                    value = url,
                    onValueChange = { newValue: String -> url = newValue },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                    placeholder = { Text("请输入直播源 URL") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = SurfaceDark,
                        unfocusedContainerColor = Color.Transparent,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextMuted,
                        focusedBorderColor = PrimaryYellow,
                        unfocusedBorderColor = TextMuted.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(8.dp)
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // 按钮行
                Row(
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.colors(
                            containerColor = Color.Transparent,
                            contentColor = TextMuted
                        )
                    ) {
                        Text("取消")
                    }
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    Button(
                        onClick = { onSave(url) },
                        colors = ButtonDefaults.colors(
                            containerColor = PrimaryYellow,
                            contentColor = BackgroundDark
                        ),
                        enabled = url.isNotBlank()
                    ) {
                        Text("保存")
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
 * 节目单配置对话框
 */
@OptIn(ExperimentalTvMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
private fun EpgUrlSettingsDialog(
    currentUrl: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var url by remember { mutableStateOf(currentUrl) }
    val focusRequester = remember { FocusRequester() }
    
    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            onClick = {},
            colors = CardDefaults.colors(containerColor = SurfaceDark),
            modifier = Modifier
                .width(600.dp)
                .padding(24.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                Text(
                    text = "配置节目单",
                    style = MaterialTheme.typography.headlineSmall,
                    color = TextPrimary
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "支持 XMLTV 格式的 EPG 节目单链接",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMuted
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // URL 输入框
                OutlinedTextField(
                    value = url,
                    onValueChange = { newValue: String -> url = newValue },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                    placeholder = { Text("请输入 EPG 节目单 URL") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = SurfaceDark,
                        unfocusedContainerColor = Color.Transparent,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextMuted,
                        focusedBorderColor = PrimaryYellow,
                        unfocusedBorderColor = TextMuted.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(8.dp)
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // 按钮行
                Row(
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.colors(
                            containerColor = Color.Transparent,
                            contentColor = TextMuted
                        )
                    ) {
                        Text("取消")
                    }
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    Button(
                        onClick = { onSave(url) },
                        colors = ButtonDefaults.colors(
                            containerColor = PrimaryYellow,
                            contentColor = BackgroundDark
                        ),
                        enabled = url.isNotBlank()
                    ) {
                        Text("保存")
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
 * User-Agent 配置对话框
 */
@OptIn(ExperimentalTvMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
private fun UserAgentSettingsDialog(
    currentUserAgent: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var userAgent by remember { mutableStateOf(currentUserAgent) }
    val focusRequester = remember { FocusRequester() }
    
    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            onClick = {},
            colors = CardDefaults.colors(containerColor = SurfaceDark),
            modifier = Modifier
                .width(600.dp)
                .padding(24.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                Text(
                    text = "配置 User-Agent",
                    style = MaterialTheme.typography.headlineSmall,
                    color = TextPrimary
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "自定义播放器请求时使用的 User-Agent",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMuted
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // UA 输入框
                OutlinedTextField(
                    value = userAgent,
                    onValueChange = { newValue: String -> userAgent = newValue },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                    placeholder = { Text("请输入 User-Agent（留空使用默认）") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = SurfaceDark,
                        unfocusedContainerColor = Color.Transparent,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextMuted,
                        focusedBorderColor = PrimaryYellow,
                        unfocusedBorderColor = TextMuted.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(8.dp)
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // 按钮行
                Row(
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.colors(
                            containerColor = Color.Transparent,
                            contentColor = TextMuted
                        )
                    ) {
                        Text("取消")
                    }
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    Button(
                        onClick = { onSave(userAgent) },
                        colors = ButtonDefaults.colors(
                            containerColor = PrimaryYellow,
                            contentColor = BackgroundDark
                        )
                    ) {
                        Text("保存")
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
 * 网页配置提示对话框
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun WebConfigInfoDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val wifiIpAddress = remember { getWifiIpAddress(context) }
    
    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            onClick = {},
            colors = CardDefaults.colors(containerColor = SurfaceDark),
            modifier = Modifier
                .width(600.dp)
                .padding(24.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                Text(
                    text = "网页端配置",
                    style = MaterialTheme.typography.headlineSmall,
                    color = TextPrimary
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "请在电脑或手机浏览器中访问以下地址：",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMuted
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Card(
                    onClick = {},
                    colors = CardDefaults.colors(containerColor = BackgroundDark),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "http://$wifiIpAddress:8893",
                        style = MaterialTheme.typography.bodyLarge,
                        color = PrimaryYellow,
                        modifier = Modifier.padding(16.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "网页端支持配置：\n• 直播源 URL\n• 节目单 URL\n• 自定义 User-Agent\n• WebDAV 网盘",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.colors(
                        containerColor = PrimaryYellow,
                        contentColor = BackgroundDark
                    ),
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("知道了")
                }
            }
        }
    }
}

/**
 * 获取 WiFi IP 地址
 */
private fun getWifiIpAddress(context: android.content.Context): String {
    // 方法1: 通过 WiFiManager 获取
    try {
        val wifiManager = context.applicationContext.getSystemService(android.content.Context.WIFI_SERVICE) as android.net.wifi.WifiManager
        val ipInt = wifiManager.connectionInfo.ipAddress
        if (ipInt != 0) {
            val ip = String.format(
                "%d.%d.%d.%d",
                ipInt and 0xff,
                ipInt shr 8 and 0xff,
                ipInt shr 16 and 0xff,
                ipInt shr 24 and 0xff
            )
            if (ip != "0.0.0.0") return ip
        }
    } catch (e: Exception) {
        // 忽略错误，尝试其他方法
    }
    
    // 方法2: 通过网络接口获取
    try {
        val interfaces = java.net.NetworkInterface.getNetworkInterfaces()
        while (interfaces.hasMoreElements()) {
            val networkInterface = interfaces.nextElement()
            val addresses = networkInterface.inetAddresses
            while (addresses.hasMoreElements()) {
                val address = addresses.nextElement()
                if (!address.isLoopbackAddress && address is java.net.Inet4Address) {
                    val ip = address.hostAddress
                    if (ip != null && !ip.startsWith("0.")) {
                        return ip
                    }
                }
            }
        }
    } catch (e: Exception) {
        // 忽略错误
    }
    
    // 方法3: 返回常见局域网 IP 作为备选
    return "192.168.1.100"
}

package com.lomen.tv.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storage
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.IconButton
import androidx.tv.material3.IconButtonDefaults
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
import com.lomen.tv.ui.LocalCompactUiScale
import com.lomen.tv.ui.computeCompactUiScale
import com.lomen.tv.ui.scale
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// V2 设计颜色（与设置中心保持一致）
private val BackgroundV2 = BackgroundDark
private val SidebarBackground = SurfaceDark
private val GlassCardBackground = Color(0xFF18181B).copy(alpha = 0.4f)
private val BorderColor = Color.White.copy(alpha = 0.05f)
private val AccentYellow = Color(0xFFF59E0B)
private val FocusYellow = Color(0xFFFEDD0E) // 焦点高亮黄色
private val StatusGreen = Color(0xFF10B981)
private val StatusRed = Color(0xFFEF4444)
private val TextZinc400 = Color(0xFFA1A1AA)
private val TextZinc500 = Color(0xFF71717A)
private val TextZinc600 = Color(0xFF52525B)

/** 资源库列表单行高度基准（再乘紧凑缩放）；与设置中心主按钮高度保持一致 */
private val LibraryListRowHeightBase = 88.dp
private val LibraryListVerticalGapBase = 8.dp

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
    val lastLibraryRowFocusRequester = remember { FocusRequester() }
    val addLibraryFocusRequester = remember { FocusRequester() }
    val backButtonFocusRequester = remember { FocusRequester() }
    var checkingLibraryId by remember { mutableStateOf<String?>(null) }
    var checkResult by remember { mutableStateOf<Map<String, Boolean>>(emptyMap()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    // 计算统计数据
    val webDavCount = libraries.count { it.type == ResourceLibrary.LibraryType.WEBDAV }
    val quarkCount = libraries.count { it.type == ResourceLibrary.LibraryType.QUARK }
    val activeCount = libraries.count { checkResult[it.id] == true }

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

    // 进入页面后自动检测所有资源库连通状态
    LaunchedEffect(libraries) {
        if (libraries.isEmpty()) {
            checkResult = emptyMap()
            checkingLibraryId = null
            return@LaunchedEffect
        }

        val newResult = mutableMapOf<String, Boolean>()
        libraries.forEach { library ->
            checkingLibraryId = library.id
            val connected = checkLibraryConnection(library)
            newResult[library.id] = connected
            checkResult = newResult.toMap()
        }
        checkingLibraryId = null
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

    val configuration = LocalConfiguration.current
    val compactScale = remember(configuration.screenHeightDp, configuration.screenWidthDp) {
        computeCompactUiScale(configuration.screenHeightDp, configuration.screenWidthDp)
    }
    // 资源库主界面整体放大：高分屏更易读，低分屏保持紧凑不溢出
    val resourceLibraryScale = remember(compactScale) {
        if (compactScale >= 1f) 1.12f else (compactScale * 1.08f).coerceAtMost(1f)
    }

    CompositionLocalProvider(LocalCompactUiScale provides resourceLibraryScale) {
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
                backButtonFocusRequester = backButtonFocusRequester,
                addLibraryFocusRequester = addLibraryFocusRequester,
                listFocusRequester = listFocusRequester,
                modifier = Modifier.width(280.dp.scale(compactScale))
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
                lastLibraryRowFocusRequester = lastLibraryRowFocusRequester,
                addLibraryFocusRequester = addLibraryFocusRequester,
                backButtonFocusRequester = backButtonFocusRequester,
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
                    .padding(bottom = 48.dp.scale(compactScale))
            ) {
                androidx.compose.foundation.layout.Row(
                    modifier = Modifier
                        .background(
                            color = Color(0xFFEF4444).copy(alpha = 0.92f),
                            shape = RoundedCornerShape(12.dp.scale(compactScale))
                        )
                        .padding(
                            horizontal = 24.dp.scale(compactScale),
                            vertical = 14.dp.scale(compactScale)
                        ),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp.scale(compactScale))
                ) {
                    Icon(
                        imageVector = Icons.Default.Error,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp.scale(compactScale))
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
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun Sidebar(
    onNavigateBack: () -> Unit,
    onNavigateToSettings: () -> Unit,
    backButtonFocusRequester: FocusRequester,
    addLibraryFocusRequester: FocusRequester,
    listFocusRequester: FocusRequester,
    modifier: Modifier = Modifier
) {
    val s = LocalCompactUiScale.current
    Column(
        modifier = modifier
            .fillMaxHeight()
            .background(SidebarBackground)
            .padding(24.dp.scale(s))
    ) {
        // 返回按钮和标题（与设置中心左栏一致）
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 32.dp.scale(s))
        ) {
            IconButton(
                onClick = onNavigateBack,
                colors = IconButtonDefaults.colors(
                    containerColor = Color.Transparent,
                    contentColor = TextPrimary,
                    focusedContainerColor = PrimaryYellow,
                    focusedContentColor = BackgroundDark
                ),
                modifier = Modifier
                    .size(48.dp.scale(s))
                    .focusRequester(backButtonFocusRequester)
                    .focusProperties {
                        right = listFocusRequester
                    }
                    .onPreviewKeyEvent { e ->
                        if (e.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                        when (e.key) {
                            Key.DirectionLeft -> {
                                // 返回按钮上不允许左键跳转到“添加新资源库”
                                true
                            }
                            Key.DirectionRight -> {
                                listFocusRequester.requestFocus()
                                true
                            }
                            else -> false
                        }
                    }
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "返回",
                    modifier = Modifier.size(28.dp.scale(s))
                )
            }

            Spacer(modifier = Modifier.width(16.dp.scale(s)))

            Box(
                modifier = Modifier
                    .size(40.dp.scale(s))
                    .clip(RoundedCornerShape(12.dp.scale(s)))
                    .background(PrimaryYellow),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = BackgroundDark,
                    modifier = Modifier.size(24.dp.scale(s))
                )
            }

            Spacer(modifier = Modifier.width(12.dp.scale(s)))

            Text(
                text = "资源库",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontSize = (MaterialTheme.typography.headlineMedium.fontSize.value * s + 2f).sp
                ),
                color = TextPrimary
            )
        }

        Spacer(modifier = Modifier.height(16.dp.scale(s)))

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

        Spacer(modifier = Modifier.weight(1f))

        // 底部时间（与设置中心左栏一致）
        var currentTime by remember { mutableStateOf("") }
        LaunchedEffect(Unit) {
            while (true) {
                val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                currentTime = sdf.format(Date())
                kotlinx.coroutines.delay(60000) // 每分钟更新一次
            }
        }
        Text(
            text = "系统时间：$currentTime",
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = (MaterialTheme.typography.bodySmall.fontSize.value * s + 2f).sp
            ),
            color = TextMuted,
            modifier = Modifier.padding(bottom = 16.dp.scale(s))
        )
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
    val s = LocalCompactUiScale.current
    var isFocused by remember { mutableStateOf(false) }

    Button(
        onClick = onClick,
        colors = ButtonDefaults.colors(
            containerColor = Color.Transparent,
            contentColor = if (isSelected) PrimaryYellow else TextPrimary,
            focusedContainerColor = PrimaryYellow,
            focusedContentColor = BackgroundDark
        ),
        shape = ButtonDefaults.shape(shape = RoundedCornerShape(16.dp.scale(s))),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp.scale(s))
            .onFocusChanged { isFocused = it.isFocused }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp.scale(s), vertical = 13.dp.scale(s)),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp.scale(s)),
                tint = if (isFocused) BackgroundDark else if (isSelected) PrimaryYellow else TextPrimary
            )
            Spacer(modifier = Modifier.width(16.dp.scale(s)))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = (MaterialTheme.typography.bodyLarge.fontSize.value * s + 2f).sp
                ),
                color = if (isFocused) BackgroundDark else if (isSelected) PrimaryYellow else TextPrimary,
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
    lastLibraryRowFocusRequester: FocusRequester,
    addLibraryFocusRequester: FocusRequester,
    backButtonFocusRequester: FocusRequester,
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
    val s = LocalCompactUiScale.current
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
            .padding(48.dp.scale(s))
    ) {
        // 顶部状态摘要
        HeaderSection(
            libraryCount = libraries.size,
            webDavCount = webDavCount,
            quarkCount = quarkCount,
            activeCount = activeCount
        )

        Spacer(modifier = Modifier.height(32.dp.scale(s)))

        // 同步状态显示
        when (syncState) {
            is MediaSyncViewModel.SyncState.Scanning -> {
                SyncStatusCardV2("正在扫描文件...", syncProgress)
                Spacer(modifier = Modifier.height(16.dp.scale(s)))
            }
            is MediaSyncViewModel.SyncState.Scraping -> {
                SyncStatusCardV2("正在刮削信息 (${syncProgress.first}/${syncProgress.second})...", syncProgress)
                Spacer(modifier = Modifier.height(16.dp.scale(s)))
            }
            is MediaSyncViewModel.SyncState.Error -> {
                SyncStatusCardV2(syncState.message, syncProgress, isError = true)
                Spacer(modifier = Modifier.height(16.dp.scale(s)))
            }
            else -> {}
        }

        val isSyncing = syncState is MediaSyncViewModel.SyncState.Scanning ||
                syncState is MediaSyncViewModel.SyncState.Scraping

        if (libraries.isEmpty()) {
            EmptyLibraryViewV2(onAddLibrary = onAddLibrary)
        } else {
            val webDavLibraries = libraries.filter { it.type == ResourceLibrary.LibraryType.WEBDAV }
            val quarkLibraries = libraries.filter { it.type == ResourceLibrary.LibraryType.QUARK }
            val firstFocusableLibraryId = webDavLibraries.firstOrNull()?.id ?: quarkLibraries.firstOrNull()?.id
            val lastLibraryId: String? =
                if (quarkLibraries.isNotEmpty()) quarkLibraries.last().id else webDavLibraries.lastOrNull()?.id
            val addButtonUpTarget: FocusRequester =
                if (firstFocusableLibraryId != null && firstFocusableLibraryId == lastLibraryId) {
                    listFocusRequester
                } else {
                    lastLibraryRowFocusRequester
                }

            val libRowH = LibraryListRowHeightBase.scale(s)
            val libGap = LibraryListVerticalGapBase.scale(s)
            val libListViewportH = libRowH + libGap + libRowH
            val scrollState = rememberScrollState()

            // 整页竖向滚动，便于看到全部列表与底部「添加」；焦点链：末行下→添加，添加上→末行，添加右→侧栏返回，返回左→添加
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    SectionTitle(title = "WEBDAV本地区", accentColor = AccentYellow)
                    Spacer(modifier = Modifier.height(10.dp.scale(s)))
                    if (webDavLibraries.isNotEmpty()) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(libGap)
                        ) {
                            webDavLibraries.forEach { library ->
                                val isChecking = checkingLibraryId == library.id
                                val isConnected = checkResult[library.id]
                                val isLastRow = library.id == lastLibraryId
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
                                    onEdit = { onEditLibrary(library) },
                                    isLastLibraryRow = isLastRow,
                                    addLibraryDownTarget = addLibraryFocusRequester,
                                    modifier = when {
                                        library.id == firstFocusableLibraryId &&
                                            firstFocusableLibraryId == lastLibraryId ->
                                            Modifier.focusRequester(listFocusRequester)
                                        library.id == firstFocusableLibraryId ->
                                            Modifier.focusRequester(listFocusRequester)
                                        library.id == lastLibraryId ->
                                            Modifier.focusRequester(lastLibraryRowFocusRequester)
                                        else -> Modifier
                                    }
                                )
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(libListViewportH),
                            contentAlignment = Alignment.Center
                        ) {
                            EmptyWebDavLibraryView(
                                onAddLibrary = onAddLibrary,
                                compact = true
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(44.dp.scale(s)))

                Column(modifier = Modifier.fillMaxWidth()) {
                    SectionTitle(title = "网盘区", accentColor = TextZinc600)
                    Spacer(modifier = Modifier.height(10.dp.scale(s)))
                    if (quarkLibraries.isNotEmpty()) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(libGap)
                        ) {
                            quarkLibraries.forEach { library ->
                                val isChecking = checkingLibraryId == library.id
                                val isConnected = checkResult[library.id]
                                val isLastRow = library.id == lastLibraryId
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
                                    isLastLibraryRow = isLastRow,
                                    addLibraryDownTarget = addLibraryFocusRequester,
                                    modifier = when {
                                        library.id == firstFocusableLibraryId &&
                                            firstFocusableLibraryId == lastLibraryId ->
                                            Modifier.focusRequester(listFocusRequester)
                                        library.id == firstFocusableLibraryId ->
                                            Modifier.focusRequester(listFocusRequester)
                                        library.id == lastLibraryId ->
                                            Modifier.focusRequester(lastLibraryRowFocusRequester)
                                        else -> Modifier
                                    }
                                )
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(libListViewportH),
                            contentAlignment = Alignment.Center
                        ) {
                            EmptyQuarkLibraryView(compact = true)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp.scale(s)))

                // 底部留白避免焦点缩放/描边被父级裁切
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp.scale(s), vertical = 8.dp.scale(s))
                ) {
                    AddLibraryButtonV2(
                        onClick = onAddLibrary,
                        focusRequester = addLibraryFocusRequester,
                        upFocus = addButtonUpTarget,
                        leftFocus = backButtonFocusRequester
                    )
                }
                Spacer(modifier = Modifier.height(24.dp.scale(s)))
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
    val s = LocalCompactUiScale.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        Column {
            Text(
                text = "资源库列表",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontSize = (MaterialTheme.typography.headlineLarge.fontSize.value * s).sp
                ),
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "当前有 $libraryCount 个资源库",
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = (MaterialTheme.typography.bodyLarge.fontSize.value * s).sp
                ),
                color = TextZinc400,
                modifier = Modifier.padding(top = 8.dp.scale(s))
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(24.dp.scale(s))
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
    val s = LocalCompactUiScale.current
    GlassCard(
        modifier = Modifier,
        borderColor = borderColor
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 24.dp.scale(s), vertical = 16.dp.scale(s)),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = (MaterialTheme.typography.labelSmall.fontSize.value * s).sp
                ),
                color = TextZinc500,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp.scale(s)))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontSize = (MaterialTheme.typography.headlineSmall.fontSize.value * s).sp
                ),
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
    val s = LocalCompactUiScale.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
    ) {
        Box(
            modifier = Modifier
                .width(4.dp.scale(s))
                .height(24.dp.scale(s))
                .clip(RoundedCornerShape(2.dp.scale(s)))
                .background(accentColor)
        )
        Spacer(modifier = Modifier.width(12.dp.scale(s)))
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall.copy(
                fontSize = (MaterialTheme.typography.headlineSmall.fontSize.value * s).sp
            ),
            color = TextPrimary
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
    modifier: Modifier = Modifier,
    isLastLibraryRow: Boolean = false,
    addLibraryDownTarget: FocusRequester? = null
) {
    LibraryListItemV2(
        library = library,
        isSelected = isSelected,
        isChecking = isChecking,
        isConnected = isConnected,
        enabled = enabled,
        onClick = onClick,
        onDelete = onDelete,
        onEdit = onEdit,
        modifier = modifier,
        isLastLibraryRow = isLastLibraryRow,
        addLibraryDownTarget = addLibraryDownTarget
    )
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
    onEdit: () -> Unit,
    modifier: Modifier = Modifier,
    isLastLibraryRow: Boolean = false,
    addLibraryDownTarget: FocusRequester? = null
) {
    val s = LocalCompactUiScale.current
    val rowH = LibraryListRowHeightBase.scale(s)
    var isFocused by remember { mutableStateOf(false) }
    var isEditFocused by remember { mutableStateOf(false) }
    var isDeleteFocused by remember { mutableStateOf(false) }
    val downToAdd =
        if (isLastLibraryRow && addLibraryDownTarget != null) {
            Modifier.focusProperties { down = addLibraryDownTarget }
        } else {
            Modifier
        }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 2.dp.scale(s), vertical = 2.dp.scale(s)),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp.scale(s))
    ) {
        Card(
            onClick = { if (enabled) onClick() },
            colors = CardDefaults.colors(
                containerColor = SurfaceDark,
                focusedContainerColor = PrimaryYellow
            ),
            scale = CardDefaults.scale(
                scale = 1.0f,
                focusedScale = 1.01f,
                pressedScale = 1.0f
            ),
            modifier = Modifier
                .weight(1f)
                .height(rowH)
                .then(downToAdd)
                .onFocusChanged { isFocused = it.isFocused }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp.scale(s), vertical = 8.dp.scale(s)),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp.scale(s))
                        .clip(RoundedCornerShape(10.dp.scale(s)))
                        .background(Color.White.copy(alpha = if (isFocused) 0.35f else 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (library.type == ResourceLibrary.LibraryType.WEBDAV) Icons.Default.Storage else Icons.Default.Cloud,
                        contentDescription = null,
                        tint = if (isFocused) Color.Black else if (library.type == ResourceLibrary.LibraryType.WEBDAV) Color(0xFF60a5fa) else Color(0xFF34d399),
                        modifier = Modifier.size(18.dp.scale(s))
                    )
                }

                Spacer(modifier = Modifier.width(10.dp.scale(s)))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = library.name,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = (MaterialTheme.typography.titleMedium.fontSize.value * s * 0.95f).sp
                        ),
                        color = if (isFocused) Color.Black else TextPrimary,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp.scale(s)))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        StatusIndicator(
                            isChecking = isChecking,
                            isConnected = isConnected
                        )
                        Spacer(modifier = Modifier.width(8.dp.scale(s)))
                        Text(
                            text = library.getDisplayUrl(),
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = (MaterialTheme.typography.bodySmall.fontSize.value * s * 0.92f).sp
                            ),
                            color = if (isFocused) Color.Black.copy(alpha = 0.75f) else TextMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                if (isSelected) {
                    Spacer(modifier = Modifier.width(8.dp.scale(s)))
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "当前选中",
                        tint = if (isFocused) Color.Black else AccentYellow,
                        modifier = Modifier.size(22.dp.scale(s))
                    )
                }
            }
        }

        Card(
            onClick = { if (enabled) onEdit() },
            colors = CardDefaults.colors(
                containerColor = SurfaceDark,
                focusedContainerColor = PrimaryYellow
            ),
            scale = CardDefaults.scale(
                scale = 1.0f,
                focusedScale = 1.01f,
                pressedScale = 1.0f
            ),
            modifier = Modifier
                .width(72.dp.scale(s))
                .height(rowH)
                .then(downToAdd)
                .onFocusChanged { isEditFocused = it.isFocused }
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "编辑",
                    tint = if (isEditFocused) Color.Black else AccentYellow,
                    modifier = Modifier.size(18.dp.scale(s))
                )
            }
        }

        Card(
            onClick = { if (enabled) onDelete() },
            colors = CardDefaults.colors(
                containerColor = SurfaceDark,
                focusedContainerColor = PrimaryYellow
            ),
            scale = CardDefaults.scale(
                scale = 1.0f,
                focusedScale = 1.01f,
                pressedScale = 1.0f
            ),
            modifier = Modifier
                .width(72.dp.scale(s))
                .height(rowH)
                .then(downToAdd)
                .onFocusChanged { isDeleteFocused = it.isFocused }
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "删除",
                    tint = if (isDeleteFocused) Color.Black else StatusRed,
                    modifier = Modifier.size(18.dp.scale(s))
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
    val s = LocalCompactUiScale.current
    Box(
        modifier = Modifier.size(12.dp.scale(s)),
        contentAlignment = Alignment.Center
    ) {
        when {
            isChecking -> {
                // 脉冲动画效果
                Box(
                    modifier = Modifier
                        .size(8.dp.scale(s))
                        .clip(CircleShape)
                        .background(AccentYellow)
                )
            }
            isConnected == true -> {
                Box(
                    modifier = Modifier
                        .size(8.dp.scale(s))
                        .clip(CircleShape)
                        .background(StatusGreen)
                )
            }
            isConnected == false -> {
                Box(
                    modifier = Modifier
                        .size(8.dp.scale(s))
                        .clip(CircleShape)
                        .background(StatusRed)
                )
            }
            else -> {
                Box(
                    modifier = Modifier
                        .size(8.dp.scale(s))
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
    onClick: () -> Unit,
    focusRequester: FocusRequester? = null,
    upFocus: FocusRequester? = null,
    leftFocus: FocusRequester? = null
) {
    val s = LocalCompactUiScale.current
    var isFocused by remember { mutableStateOf(false) }

    Card(
        onClick = onClick,
        colors = CardDefaults.colors(
            containerColor = Color.Transparent,
            focusedContainerColor = AccentYellow.copy(alpha = 0.1f)
        ),
        border = CardDefaults.border(
            focusedBorder = androidx.tv.material3.Border(
                BorderStroke(2.dp.scale(s), AccentYellow),
                shape = RoundedCornerShape(16.dp.scale(s))
            )
        ),
        // 不放大，避免在滚动区域内被裁切
        scale = CardDefaults.scale(
            scale = 1f,
            focusedScale = 1f,
            pressedScale = 1f
        ),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp.scale(s))
            .then(focusRequester?.let { Modifier.focusRequester(it) } ?: Modifier)
            .focusProperties {
                upFocus?.let { up = it }
            }
            .onPreviewKeyEvent { e ->
                if (e.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when (e.key) {
                    // 底部已在“添加新资源库”，按右键不做任何跳转
                    Key.DirectionRight -> true
                    // 从“添加新资源库”左键回到左侧功能区（返回按钮）
                    Key.DirectionLeft -> {
                        leftFocus?.requestFocus()
                        true
                    }
                    else -> false
                }
            }
            .onFocusChanged { isFocused = it.isFocused }
            .then(
                if (!isFocused) {
                    Modifier.drawBehind {
                        val r = 16.dp.scale(s).toPx()
                        drawRoundRect(
                            color = BorderColor,
                            cornerRadius = CornerRadius(r, r),
                            style = Stroke(width = 2.dp.scale(s).toPx(), pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f))
                        )
                    }
                } else Modifier
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp.scale(s), vertical = 14.dp.scale(s)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "+ 添加新资源库",
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = (MaterialTheme.typography.bodyLarge.fontSize.value * s).sp
                ),
                color = if (isFocused) Color.White else TextZinc500,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun EmptyQuarkLibraryView(compact: Boolean = false) {
    val s = LocalCompactUiScale.current
    if (compact) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp.scale(s))
                    .clip(CircleShape)
                    .background(TextZinc600.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Cloud,
                    contentDescription = null,
                    tint = TextZinc500,
                    modifier = Modifier.size(22.dp.scale(s))
                )
            }
            Spacer(modifier = Modifier.height(6.dp.scale(s)))
            Text(
                text = "暂无网盘",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = (MaterialTheme.typography.bodySmall.fontSize.value * s).sp
                ),
                color = TextZinc500
            )
        }
    } else {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp.scale(s)))
            Box(
                modifier = Modifier
                    .size(64.dp.scale(s))
                    .clip(CircleShape)
                    .background(TextZinc600.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Cloud,
                    contentDescription = null,
                    tint = TextZinc500,
                    modifier = Modifier.size(32.dp.scale(s))
                )
            }
            Spacer(modifier = Modifier.height(16.dp.scale(s)))
            Text(
                text = "暂无网盘",
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = (MaterialTheme.typography.bodyLarge.fontSize.value * s).sp
                ),
                color = TextZinc500
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun EmptyWebDavLibraryView(
    onAddLibrary: () -> Unit,
    compact: Boolean = false
) {
    val s = LocalCompactUiScale.current
    if (compact) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp.scale(s))
                    .clip(CircleShape)
                    .background(TextZinc600.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Storage,
                    contentDescription = null,
                    tint = TextZinc500,
                    modifier = Modifier.size(22.dp.scale(s))
                )
            }
            Spacer(modifier = Modifier.height(6.dp.scale(s)))
            Text(
                text = "暂无WEBDAV",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = (MaterialTheme.typography.bodySmall.fontSize.value * s).sp
                ),
                color = TextZinc500
            )
            Spacer(modifier = Modifier.height(8.dp.scale(s)))
            Text(
                text = "请使用底部「添加新资源库」",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = (MaterialTheme.typography.bodySmall.fontSize.value * s * 0.9f).sp
                ),
                color = TextZinc600
            )
        }
    } else {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp.scale(s)))
            Box(
                modifier = Modifier
                    .size(64.dp.scale(s))
                    .clip(CircleShape)
                    .background(TextZinc600.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Storage,
                    contentDescription = null,
                    tint = TextZinc500,
                    modifier = Modifier.size(32.dp.scale(s))
                )
            }
            Spacer(modifier = Modifier.height(16.dp.scale(s)))
            Text(
                text = "暂无WEBDAV",
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = (MaterialTheme.typography.bodyLarge.fontSize.value * s).sp
                ),
                color = TextZinc500
            )
            Spacer(modifier = Modifier.height(24.dp.scale(s)))
            AddLibraryButtonV2(onClick = onAddLibrary)
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun EmptyLibraryViewV2(
    onAddLibrary: () -> Unit
) {
    val s = LocalCompactUiScale.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        GlassCard {
            Column(
                modifier = Modifier.padding(48.dp.scale(s)),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp.scale(s))
                        .clip(CircleShape)
                        .background(TextZinc600.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Storage,
                        contentDescription = null,
                        tint = TextZinc500,
                        modifier = Modifier.size(40.dp.scale(s))
                    )
                }

                Spacer(modifier = Modifier.height(24.dp.scale(s)))

                Text(
                    text = "暂无资源库",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontSize = (MaterialTheme.typography.headlineSmall.fontSize.value * s).sp
                    ),
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp.scale(s)))

                Text(
                    text = "添加WebDAV网盘或网盘",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = (MaterialTheme.typography.bodyLarge.fontSize.value * s).sp
                    ),
                    color = TextZinc500
                )

                Spacer(modifier = Modifier.height(32.dp.scale(s)))

                var isButtonFocused by remember { mutableStateOf(false) }
                Button(
                    onClick = onAddLibrary,
                    colors = ButtonDefaults.colors(
                        containerColor = SurfaceDark,      // 未选中：浅灰色底
                        contentColor = TextPrimary,         // 未选中：白色字
                        focusedContainerColor = PrimaryYellow, // 选中：亮黄色底
                        focusedContentColor = BackgroundDark    // 选中：黑色字
                    ),
                    shape = ButtonDefaults.shape(shape = RoundedCornerShape(12.dp.scale(s))),
                    modifier = Modifier.onFocusChanged { isButtonFocused = it.isFocused }
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        tint = if (isButtonFocused) BackgroundDark else TextPrimary,
                        modifier = Modifier.size(20.dp.scale(s))
                    )
                    Spacer(modifier = Modifier.width(8.dp.scale(s)))
                    Text(
                        text = "添加资源库",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontSize = (MaterialTheme.typography.bodyLarge.fontSize.value * s).sp
                        ),
                        fontWeight = FontWeight.Bold,
                        color = if (isButtonFocused) BackgroundDark else TextPrimary,
                        modifier = Modifier.padding(horizontal = 8.dp.scale(s), vertical = 4.dp.scale(s))
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
    val s = LocalCompactUiScale.current
    GlassCard(
        borderColor = if (isError) StatusRed.copy(alpha = 0.3f) else AccentYellow.copy(alpha = 0.3f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp.scale(s)),
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
                        modifier = Modifier.size(24.dp.scale(s))
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        tint = AccentYellow,
                        modifier = Modifier.size(24.dp.scale(s))
                    )
                }
                Spacer(modifier = Modifier.width(12.dp.scale(s)))
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = (MaterialTheme.typography.bodyLarge.fontSize.value * s).sp
                    ),
                    color = if (isError) StatusRed else Color.White
                )
            }

            if (!isError && progress.second > 0) {
                Text(
                    text = "${progress.first}/${progress.second}",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = (MaterialTheme.typography.bodyMedium.fontSize.value * s).sp
                    ),
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

private suspend fun checkLibraryConnection(library: ResourceLibrary): Boolean {
    return when (library.type) {
        ResourceLibrary.LibraryType.WEBDAV -> checkWebDavConnection(library)
        ResourceLibrary.LibraryType.QUARK -> checkQuarkConnection()
    }
}

private suspend fun checkQuarkConnection(): Boolean {
    return withContext(Dispatchers.IO) {
        try {
            val conn = URL("https://drive.quark.cn/").openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.connectTimeout = 8000
            conn.readTimeout = 8000
            conn.instanceFollowRedirects = true
            val code = conn.responseCode
            conn.disconnect()
            code in 100..599
        } catch (e: Exception) {
            android.util.Log.e("Quark", "Connection failed: ${e.message}")
            false
        }
    }
}

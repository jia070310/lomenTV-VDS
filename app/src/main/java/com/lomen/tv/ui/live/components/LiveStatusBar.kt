package com.lomen.tv.ui.live.components

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.tv.foundation.lazy.list.TvLazyColumn
import androidx.tv.foundation.lazy.list.itemsIndexed
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.ListItem
import androidx.tv.material3.ListItemDefaults
import androidx.tv.material3.Surface
import com.lomen.tv.data.model.live.CurrentProgramme
import com.lomen.tv.data.model.live.LiveChannel
import com.lomen.tv.data.preferences.LiveSettingsPreferences
import com.lomen.tv.ui.live.utils.handleLiveKeyEvents
import com.lomen.tv.ui.screens.settings.QrCodeDialog
import com.lomen.tv.ui.theme.LomenTVTheme
import com.lomen.tv.ui.theme.PrimaryYellow

/**
 * 状态栏 - 显示在屏幕顶部，包含频道信息和操作按钮
 * 右键呼出，返回键关闭
 * 样式完全参考 mytv-android-main 的 QuickPanelScreen
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun LiveStatusBar(
    modifier: Modifier = Modifier,
    channelProvider: () -> LiveChannel = { LiveChannel() },
    channelNoProvider: () -> Int = { 0 },
    currentChannelUrlIdxProvider: () -> Int = { 0 },
    currentProgrammesProvider: () -> CurrentProgramme? = { null },
    videoWidthProvider: () -> Int = { 0 },
    videoHeightProvider: () -> Int = { 0 },
    networkSpeedProvider: () -> Long = { 0L },
    videoAspectRatioProvider: () -> LiveSettingsPreferences.Companion.VideoAspectRatio = { 
        LiveSettingsPreferences.Companion.VideoAspectRatio.ORIGINAL 
    },
    sourceListProvider: () -> List<Pair<String, String>> = { emptyList() },
    currentSourceUrlProvider: () -> String = { "" },
    onChangeVideoAspectRatio: () -> Unit = {},
    onClearCache: () -> Unit = {},
    onSwitchSource: (String) -> Unit = {},
    onRefreshAllSources: () -> Unit = {},
    onClose: () -> Unit = {},
) {
    val context = LocalContext.current
    val focusRequester = remember { FocusRequester() }
    
    // 对话框状态
    var showSourceDialog by remember { mutableStateOf(false) }
    var showQrcodeDialog by remember { mutableStateOf(false) }
    var showToast by remember { mutableStateOf(false) }
    var toastMessage by remember { mutableStateOf("") }
    
    // 自动关闭计时器
    var lastInteractionTime by remember { mutableStateOf(System.currentTimeMillis()) }
    
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
    
    // 自动关闭逻辑：5秒无操作后关闭
    LaunchedEffect(lastInteractionTime, showSourceDialog, showQrcodeDialog) {
        if (!showSourceDialog && !showQrcodeDialog) { // 如果源选择对话框和二维码对话框都没有打开
            delay(5000) // 5秒延迟
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastInteractionTime >= 5000) {
                onClose()
            }
        }
    }
    
    // 更新最后交互时间的函数
    val updateInteractionTime = {
        lastInteractionTime = System.currentTimeMillis()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .focusRequester(focusRequester)
            .handleLiveKeyEvents(
                onSelect = { 
                    updateInteractionTime()
                    /* 确认键不做任何事，只保持焦点 */ 
                }
            )
            .pointerInput(Unit) { 
                detectTapGestures(onTap = { 
                    updateInteractionTime()
                    onClose() 
                }) 
            },
    ) {
        // 阻止点击事件冒泡的内部容器
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) { detectTapGestures(onTap = { }) }
                .padding(start = 40.dp, bottom = 40.dp, end = 40.dp, top = 20.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // 顶部：只有频道号和时间在右上角
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    LiveStatusBarTopRight(
                        channelNoProvider = channelNoProvider
                    )
                }

                // 底部区域：所有信息都在左下角
                Column(
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // 左下角：频道信息（名称、标签、节目）
                    LiveStatusBarChannelInfo(
                        channelProvider = channelProvider,
                        currentChannelUrlIdxProvider = currentChannelUrlIdxProvider,
                        currentProgrammesProvider = currentProgrammesProvider,
                    )

                    // 左下角：分辨率和网速信息
                    LiveStatusBarPlayerInfo(
                        videoWidthProvider = videoWidthProvider,
                        videoHeightProvider = videoHeightProvider,
                        networkSpeedProvider = networkSpeedProvider,
                    )

                    // 底部操作按钮
                    LiveStatusBarActions(
                        context = context,
                        channelProvider = channelProvider,
                        currentChannelUrlIdxProvider = currentChannelUrlIdxProvider,
                        videoAspectRatioProvider = videoAspectRatioProvider,
                        onChangeVideoAspectRatio = onChangeVideoAspectRatio,
                        onClearCache = onClearCache,
                        onSwitchSource = onSwitchSource,
                        showSourceDialogProvider = { showSourceDialog },
                        onShowSourceDialogChange = { showSourceDialog = it },
                        showToastProvider = { showToast },
                        toastMessageProvider = { toastMessage },
                        onShowToastChange = { showToast = it },
                        onRefreshAllSources = onRefreshAllSources,
                        onUserAction = updateInteractionTime,
                    )
                }
            }
        }

        // 切换源对话框（右上角）
        LiveSourceDialog(
            showDialogProvider = { showSourceDialog },
            sourceListProvider = sourceListProvider,
            currentSourceUrlProvider = currentSourceUrlProvider,
            onDismissRequest = { showSourceDialog = false },
            onSourceSelected = { url ->
                onSwitchSource(url)
                showSourceDialog = false
            },
            onShowQrcodeDialog = { showQrcodeDialog = true }
        )
        
        // 二维码对话框 - 使用直播设置中的组件
        QrCodeDialog(
            text = "http://192.168.0.3:8893/live",
            title = "网页配置",
            description = "使用手机扫描二维码访问网页配置界面\n可批量添加直播源、节目单和UA",
            showDialogProvider = { showQrcodeDialog },
            onDismissRequest = { showQrcodeDialog = false }
        )
    }
}

@Composable
private fun LiveStatusBarChannelInfo(
    modifier: Modifier = Modifier,
    channelProvider: () -> LiveChannel = { LiveChannel() },
    currentChannelUrlIdxProvider: () -> Int = { 0 },
    currentProgrammesProvider: () -> CurrentProgramme? = { null },
) {
    val channel = channelProvider()
    val channelUrlIdx = currentChannelUrlIdxProvider()
    val currentProgrammes = currentProgrammesProvider()

    Column(modifier = modifier) {
        // 频道名称 + 标签
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = channel.name,
                style = MaterialTheme.typography.headlineLarge,
                color = Color.White,
                modifier = Modifier.alignByBaseline(),
                maxLines = 1,
            )

            Spacer(modifier = Modifier.width(6.dp))

            Row(
                modifier = Modifier.padding(bottom = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                CompositionLocalProvider(
                    LocalTextStyle provides MaterialTheme.typography.labelMedium,
                    LocalContentColor provides Color.White.copy(alpha = 0.8f),
                ) {
                    val textModifier = Modifier
                        .background(
                            Color.White.copy(alpha = 0.3f),
                            MaterialTheme.shapes.extraSmall,
                        )
                        .padding(vertical = 2.dp, horizontal = 4.dp)

                    // 多线路标识
                    if (channel.urlList.size > 1) {
                        Text(
                            text = "${channelUrlIdx + 1}/${channel.urlList.size}",
                            modifier = textModifier,
                        )
                    }

                    // IPV4/IPV6 标识
                    if (channel.urlList.isNotEmpty() && channelUrlIdx < channel.urlList.size) {
                        val url = channel.urlList[channelUrlIdx]
                        val isIPv6 = url.contains("[") && url.contains("]")
                        Text(
                            text = if (isIPv6) "IPV6" else "IPV4",
                            modifier = textModifier,
                        )
                    }
                }
            }
        }

        // 节目信息
        CompositionLocalProvider(
            LocalTextStyle provides MaterialTheme.typography.bodyLarge,
            LocalContentColor provides Color.White.copy(alpha = 0.8f),
        ) {
            Text(
                text = "正在播放：${currentProgrammes?.now?.title ?: "无节目"}",
                maxLines = 1,
            )
            Text(
                text = "稍后播放：${currentProgrammes?.next?.title ?: "无节目"}",
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun LiveStatusBarTopRight(
    modifier: Modifier = Modifier,
    channelNoProvider: () -> Int = { 0 },
) {
    val channelNo = channelNoProvider()

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 频道号（大号白色）
        Text(
            text = channelNo.toString(),
            style = MaterialTheme.typography.displayLarge,
            color = Color.White,
        )
        // 时间
        LiveDateTimeBar(showMode = LiveTimeShowMode.ALWAYS)
    }
}

@Composable
private fun LiveStatusBarPlayerInfo(
    modifier: Modifier = Modifier,
    videoWidthProvider: () -> Int = { 0 },
    videoHeightProvider: () -> Int = { 0 },
    networkSpeedProvider: () -> Long = { 0L },
) {
    CompositionLocalProvider(
        LocalTextStyle provides MaterialTheme.typography.bodyLarge,
        LocalContentColor provides Color.White,
    ) {
        Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            val videoWidth = videoWidthProvider()
            val videoHeight = videoHeightProvider()
            val networkSpeed = networkSpeedProvider()
            Text(
                text = if (videoWidth > 0 && videoHeight > 0) 
                    "分辨率：${videoWidth}×${videoHeight}" 
                else 
                    "分辨率：检测中",
            )
            Text(
                text = if (networkSpeed > 0) 
                    "网速：${networkSpeed}KB/s"
                else 
                    "网速：0KB/s",
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun LiveStatusBarActions(
    modifier: Modifier = Modifier,
    context: Context,
    channelProvider: () -> LiveChannel = { LiveChannel() },
    currentChannelUrlIdxProvider: () -> Int = { 0 },
    videoAspectRatioProvider: () -> LiveSettingsPreferences.Companion.VideoAspectRatio = { 
        LiveSettingsPreferences.Companion.VideoAspectRatio.ORIGINAL 
    },
    onChangeVideoAspectRatio: () -> Unit = {},
    onClearCache: () -> Unit = {},
    onSwitchSource: (String) -> Unit = {},
    showSourceDialogProvider: () -> Boolean = { false },
    onShowSourceDialogChange: (Boolean) -> Unit = {},
    showToastProvider: () -> Boolean = { false },
    toastMessageProvider: () -> String = { "" },
    onShowToastChange: (Boolean) -> Unit = {},
    onRefreshAllSources: () -> Unit = {},
    onUserAction: () -> Unit = {},
) {
    val channel = channelProvider()

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // 清除缓存按钮
        LiveStatusBarButton(
            title = "清除缓存",
            onSelect = {
                onUserAction()
                onClearCache()
                // 显示 Toast 提示
                Toast.makeText(context, "缓存已清除，请重启应用", Toast.LENGTH_LONG).show()
            },
        )

        // 切换源按钮
        LiveStatusBarButton(
            title = "切换源",
            onSelect = { 
                onUserAction()
                onShowSourceDialogChange(true) 
            },
        )

        // 画面比例按钮
        LiveStatusBarButton(
            title = "画面比例 " + when (videoAspectRatioProvider()) {
                LiveSettingsPreferences.Companion.VideoAspectRatio.ORIGINAL -> "原始"
                LiveSettingsPreferences.Companion.VideoAspectRatio.SIXTEEN_NINE -> "16:9"
                LiveSettingsPreferences.Companion.VideoAspectRatio.FOUR_THREE -> "4:3"
                LiveSettingsPreferences.Companion.VideoAspectRatio.AUTO -> "自动拉伸"
            },
            onSelect = {
                onUserAction()
                onChangeVideoAspectRatio()
            },
        )

        // 刷新全部源按钮
        LiveStatusBarButton(
            title = "刷新全部源",
            onSelect = {
                onUserAction()
                onRefreshAllSources()
                Toast.makeText(context, "正在刷新全部直播源...", Toast.LENGTH_SHORT).show()
            },
        )

        // 多线路按钮（如果有多个线路）
        if (channel.urlList.size > 1) {
            LiveStatusBarButton(
                title = "多线路",
                onSelect = { 
                    onUserAction()
                    /* TODO: 显示线路选择对话框 */ 
                },
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun LiveStatusBarButton(
    modifier: Modifier = Modifier,
    title: String,
    onSelect: () -> Unit = {},
) {
    val focusRequester = remember { FocusRequester() }
    var isFocused by remember { mutableStateOf(false) }

    androidx.tv.material3.Button(
        onClick = { },
        shape = ButtonDefaults.shape(
            shape = RoundedCornerShape(12.dp),
            focusedShape = RoundedCornerShape(12.dp),
        ),
        colors = ButtonDefaults.colors(
            containerColor = Color(0xFF1f1f1f),
            focusedContainerColor = PrimaryYellow,
            contentColor = Color.White,
            focusedContentColor = Color.Black,
        ),
        modifier = modifier
            .focusRequester(focusRequester)
            .onFocusChanged {
                isFocused = it.isFocused || it.hasFocus
            }
            .handleLiveKeyEvents(
                onSelect = {
                    if (isFocused) onSelect()
                    else focusRequester.requestFocus()
                },
            ),
    ) {
        androidx.tv.material3.Text(
            text = title,
            color = if (isFocused) Color.Black else Color.White,
        )
    }
}

/**
 * 直播源选择对话框 - 参考 mytv-android-main 样式
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun LiveSourceDialog(
    modifier: Modifier = Modifier,
    showDialogProvider: () -> Boolean = { false },
    sourceListProvider: () -> List<Pair<String, String>> = { emptyList() },
    currentSourceUrlProvider: () -> String = { "" },
    onDismissRequest: () -> Unit = {},
    onSourceSelected: (String) -> Unit = {},
    onShowQrcodeDialog: () -> Unit = {},
) {
    if (showDialogProvider()) {
        val sourceList = sourceListProvider()
        val currentSourceUrl = currentSourceUrlProvider()
        
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black.copy(0.5f)),
            contentAlignment = Alignment.TopEnd,
        ) {
            Surface(
                modifier = Modifier
                    .width(320.dp)
                    .height(320.dp)
                    .padding(top = 50.dp, end = 50.dp),
                shape = RoundedCornerShape(8.dp),
                tonalElevation = 0.dp,
            ) {
                Column(
                    modifier = Modifier
                        .background(Color(0xB3000000))
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    // 标题 - 白色文字
                    Text(
                        text = "选择直播源",
                        style = MaterialTheme.typography.titleSmall,
                        color = Color.White,
                        modifier = Modifier.padding(bottom = 4.dp),
                    )

                    TvLazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                        contentPadding = PaddingValues(vertical = 2.dp),
                    ) {
                        itemsIndexed(sourceList) { index, (name, url) ->
                            val focusRequester = remember { FocusRequester() }
                            var isFocused by remember { mutableStateOf(false) }
                            val isCurrentSource = url == currentSourceUrl

                            // 第一个项目自动获取焦点
                            LaunchedEffect(Unit) {
                                if (index == 0) {
                                    focusRequester.requestFocus()
                                }
                            }

                            ListItem(
                                selected = isFocused,
                                onClick = {
                                    onSourceSelected(url)
                                },
                                modifier = Modifier
                                    .focusRequester(focusRequester)
                                    .onFocusChanged { isFocused = it.isFocused },
                                colors = ListItemDefaults.colors(
                                    containerColor = Color.Transparent,
                                    contentColor = Color.White,
                                    focusedContainerColor = PrimaryYellow,
                                    focusedContentColor = Color.Black,
                                    selectedContainerColor = PrimaryYellow,
                                    selectedContentColor = Color.Black,
                                ),
                                scale = ListItemDefaults.scale(
                                    focusedScale = 1.02f,
                                ),
                                headlineContent = {
                                    Text(
                                        text = name,
                                        maxLines = 1,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = if (isFocused) Color.Black else Color.White,
                                    )
                                },
                                supportingContent = if (isFocused || isCurrentSource) {
                                    {
                                        Text(
                                            text = url,
                                            maxLines = if (isFocused) 2 else 1,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (isFocused) Color.Black else Color.White.copy(alpha = 0.7f),
                                        )
                                    }
                                } else null,
                                trailingContent = if (isCurrentSource) {
                                    {
                                        Icon(
                                            Icons.Default.CheckCircle,
                                            contentDescription = "当前源",
                                            tint = if (isFocused)
                                                Color.Black
                                            else
                                                PrimaryYellow
                                        )
                                    }
                                } else null,
                            )
                        }

                        // 添加自定义源按钮
                        item {
                            val focusRequester = remember { FocusRequester() }
                            var isFocused by remember { mutableStateOf(false) }

                            ListItem(
                                selected = isFocused,
                                onClick = { 
                                    onDismissRequest()
                                    onShowQrcodeDialog() 
                                },
                                modifier = Modifier
                                    .focusRequester(focusRequester)
                                    .onFocusChanged { isFocused = it.isFocused },
                                colors = ListItemDefaults.colors(
                                    containerColor = Color.Transparent,
                                    contentColor = Color.White,
                                    focusedContainerColor = PrimaryYellow,
                                    focusedContentColor = Color.Black,
                                    selectedContainerColor = PrimaryYellow,
                                    selectedContentColor = Color.Black,
                                ),
                                scale = ListItemDefaults.scale(
                                    focusedScale = 1.02f,
                                ),
                                headlineContent = {
                                    Text(
                                        text = "＋ 添加自定义源",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = if (isFocused) Color.Black else Color.White,
                                    )
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview(device = "id:Android TV (720p)")
@Composable
private fun LiveStatusBarPreview() {
    LomenTVTheme {
        LiveStatusBar(
            channelProvider = { LiveChannel.EXAMPLE },
            channelNoProvider = { 12 },
            currentProgrammesProvider = {
                CurrentProgramme(
                    now = com.lomen.tv.data.model.live.EpgProgramme(
                        title = "2025社会与法电视剧精选",
                        startAt = System.currentTimeMillis(),
                        endAt = System.currentTimeMillis() + 3600000
                    ),
                    next = com.lomen.tv.data.model.live.EpgProgramme(
                        title = "剧懂人心（第二季）",
                        startAt = System.currentTimeMillis() + 3600000,
                        endAt = System.currentTimeMillis() + 7200000
                    )
                )
            },
        )
    }
}

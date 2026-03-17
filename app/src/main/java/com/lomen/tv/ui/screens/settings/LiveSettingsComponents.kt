@file:OptIn(androidx.tv.material3.ExperimentalTvMaterial3Api::class)

package com.lomen.tv.ui.screens.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.delay
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
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
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
import androidx.compose.ui.window.DialogProperties
import androidx.tv.foundation.lazy.list.TvLazyColumn
import androidx.tv.foundation.lazy.list.TvLazyListState
import androidx.tv.foundation.lazy.list.items
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.ListItem
import androidx.tv.material3.ListItemDefaults
import androidx.tv.material3.Switch
import androidx.tv.material3.SwitchDefaults
import androidx.tv.material3.Text
import com.lomen.tv.ui.theme.BackgroundDark
import com.lomen.tv.ui.theme.PrimaryYellow
import com.lomen.tv.ui.theme.SurfaceDark
import com.lomen.tv.ui.theme.TextMuted
import com.lomen.tv.ui.theme.TextPrimary
import io.github.alexzhirkevich.qrose.options.QrBallShape
import io.github.alexzhirkevich.qrose.options.QrFrameShape
import io.github.alexzhirkevich.qrose.options.QrPixelShape
import io.github.alexzhirkevich.qrose.options.QrShapes
import io.github.alexzhirkevich.qrose.options.circle
import io.github.alexzhirkevich.qrose.options.roundCorners
import io.github.alexzhirkevich.qrose.rememberQrCodePainter
import kotlin.math.max

/**
 * 二维码组件
 */
@Composable
fun QrCodeImage(
    modifier: Modifier = Modifier,
    text: String,
) {
    Box(
        modifier = modifier
            .background(
                color = Color.White,
                shape = RoundedCornerShape(8.dp),
            )
    ) {
        Image(
            modifier = Modifier
                .fillMaxSize()
                .align(Alignment.Center)
                .padding(8.dp),
            painter = rememberQrCodePainter(
                data = text,
                shapes = QrShapes(
                    ball = QrBallShape.circle(),
                    darkPixel = QrPixelShape.roundCorners(),
                    frame = QrFrameShape.roundCorners(.25f),
                ),
            ),
            contentDescription = text,
        )
    }
}

/**
 * 二维码弹窗 - 参考 mytv-android-main 设计
 */
@Composable
fun QrCodeDialog(
    modifier: Modifier = Modifier,
    text: String,
    title: String = "网页配置",
    description: String? = null,
    showDialogProvider: () -> Boolean = { false },
    onDismissRequest: () -> Unit = {},
) {
    if (showDialogProvider()) {
        AlertDialog(
            modifier = modifier.width(420.dp),
            properties = DialogProperties(usePlatformDefaultWidth = false),
            onDismissRequest = onDismissRequest,
            containerColor = Color(0xFF2a2a2a),
            title = {
                Text(
                    text = title,
                    color = Color.White,
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    description?.let {
                        Text(
                            text = it,
                            color = Color.White.copy(alpha = 0.8f),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(bottom = 20.dp)
                        )
                    }
                    QrCodeImage(
                        text = text,
                        modifier = Modifier
                            .width(240.dp)
                            .height(240.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = text,
                        color = PrimaryYellow,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            },
            confirmButton = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "关闭",
                        color = Color.White,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                }
            }
        )
    }
}

/**
 * 历史列表弹窗
 */
@OptIn(ExperimentalTvMaterial3Api::class, androidx.tv.material3.ExperimentalTvMaterial3Api::class)
@Composable
fun <T> HistoryListDialog(
    modifier: Modifier = Modifier,
    title: String,
    showDialogProvider: () -> Boolean = { false },
    onDismissRequest: () -> Unit = {},
    items: List<T>,
    currentItem: T?,
    onSelected: (T) -> Unit,
    onDeleted: ((T) -> Unit)? = null,
    onAddNew: (() -> Unit)? = null,
    itemContent: @Composable (T, Boolean, Boolean) -> Pair<String, String?>,
    isBuiltInItem: (T) -> Boolean = { false }, // 判断是否为内置项
    addNewText: String = "添加其他",
    emptyText: String = "暂无历史记录"
) {
    if (showDialogProvider()) {
        AlertDialog(
            properties = DialogProperties(usePlatformDefaultWidth = false),
            modifier = modifier.width(600.dp),
            onDismissRequest = onDismissRequest,
            containerColor = Color(0xFF2a2a2a),
            confirmButton = {
                if (onDeleted != null) {
                    Text(
                        text = "短按切换；长按3秒删除历史记录",
                        color = PrimaryYellow,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            },
            title = {
                Text(
                    text = title,
                    color = Color.White,
                    style = MaterialTheme.typography.headlineSmall
                )
            },
            text = {
                var hasFocused by remember { mutableStateOf(false) }
                val currentIndex = items.indexOf(currentItem).coerceAtLeast(0)

                TvLazyColumn(
                    state = TvLazyListState(
                        max(0, currentIndex - 2),
                    ),
                    contentPadding = PaddingValues(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(items) { item ->
                        val focusRequester = remember { FocusRequester() }
                        var isFocused by remember { mutableStateOf(false) }
                        val isSelected = currentItem == item
                        val isBuiltIn = isBuiltInItem(item)
                        val (headline, supporting) = itemContent(item, isSelected, isFocused)
                        
                        // 长按检测状态
                        var isLongPressing by remember { mutableStateOf(false) }
                        var longPressProgress by remember { mutableStateOf(0f) }

                        LaunchedEffect(Unit) {
                            if (item == currentItem && !hasFocused) {
                                hasFocused = true
                                focusRequester.requestFocus()
                            }
                        }
                        
                        // 长按检测协程
                        LaunchedEffect(isLongPressing) {
                            if (isLongPressing && !isBuiltIn && onDeleted != null) {
                                val startTime = System.currentTimeMillis()
                                while (isLongPressing && longPressProgress < 1f) {
                                    val elapsed = System.currentTimeMillis() - startTime
                                    longPressProgress = (elapsed / 3000f).coerceIn(0f, 1f)
                                    if (longPressProgress >= 1f) {
                                        // 长按3秒，触发删除
                                        onDeleted(item)
                                        isLongPressing = false
                                    }
                                    delay(50)
                                }
                            } else {
                                longPressProgress = 0f
                            }
                        }

                        ListItem(
                            modifier = Modifier
                                .focusRequester(focusRequester)
                                .onFocusChanged { isFocused = it.isFocused || it.hasFocus },
                            selected = isSelected,
                            onClick = { 
                                // 短按选中
                                if (!isLongPressing) {
                                    onSelected(item)
                                }
                            },
                            colors = ListItemDefaults.colors(
                                containerColor = Color(0xFF1f1f1f),
                                focusedContainerColor = PrimaryYellow,
                                selectedContainerColor = Color(0xFF1f1f1f),
                                contentColor = Color.White,
                                focusedContentColor = Color.Black,
                                selectedContentColor = Color.White,
                            ),
                            shape = ListItemDefaults.shape(
                                shape = RoundedCornerShape(12.dp),
                                focusedShape = RoundedCornerShape(12.dp),
                                selectedShape = RoundedCornerShape(12.dp),
                            ),
                            headlineContent = {
                                Text(
                                    text = headline,
                                    modifier = Modifier.fillMaxWidth(),
                                    maxLines = if (isFocused) Int.MAX_VALUE else 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = if (isFocused) Color.Black else Color.White
                                )
                            },
                            supportingContent = supporting?.let {
                                {
                                    Text(
                                        text = it,
                                        maxLines = if (isFocused) Int.MAX_VALUE else 1,
                                        overflow = TextOverflow.Ellipsis,
                                        color = if (isFocused) Color.Black else TextMuted
                                    )
                                }
                            },
                            trailingContent = {
                                if (isSelected) {
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        contentDescription = "checked",
                                        tint = if (isFocused) Color.Black else PrimaryYellow
                                    )
                                }
                            },
                        )
                    }

                    // 添加新项
                    if (onAddNew != null) {
                        item {
                            val focusRequester = remember { FocusRequester() }
                            var isFocused by remember { mutableStateOf(false) }

                            ListItem(
                                modifier = Modifier
                                    .focusRequester(focusRequester)
                                    .onFocusChanged { isFocused = it.isFocused || it.hasFocus },
                                selected = false,
                                onClick = { onAddNew() },
                                colors = ListItemDefaults.colors(
                                    containerColor = Color(0xFF1f1f1f),
                                    focusedContainerColor = PrimaryYellow,
                                    selectedContainerColor = Color(0xFF1f1f1f),
                                    contentColor = Color.White,
                                    focusedContentColor = Color.Black,
                                    selectedContentColor = Color.White,
                                ),
                                shape = ListItemDefaults.shape(
                                    shape = RoundedCornerShape(12.dp),
                                    focusedShape = RoundedCornerShape(12.dp),
                                    selectedShape = RoundedCornerShape(12.dp),
                                ),
                                headlineContent = {
                                    Text(
                                        text = addNewText,
                                        color = if (isFocused) Color.Black else Color.White
                                    )
                                },
                            )
                        }
                    }
                }
            },
        )
    }
}

/**
 * 直播设置列表项卡片
 * 高亮时图标和文字都变为黑色
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun LiveSettingListCard(
    icon: ImageVector,
    iconBackgroundColor: Color,
    iconTint: Color,
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailingContent: @Composable (() -> Unit)? = null
) {
    var isFocused by remember { mutableStateOf(false) }
    
    Card(
        onClick = onClick,
        colors = CardDefaults.colors(
            containerColor = SurfaceDark,
            focusedContainerColor = PrimaryYellow,
        ),
        scale = CardDefaults.scale(
            scale = 1.0f,
            focusedScale = 1.02f,
            pressedScale = 1.0f
        ),
        modifier = modifier
            .fillMaxWidth()
            .onFocusChanged { focusState ->
                isFocused = focusState.isFocused
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 图标
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
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // 文字内容 - 只显示标题
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = if (isFocused) Color.Black else TextPrimary,
                modifier = Modifier.weight(1f)
            )

            // 尾部内容
            trailingContent?.invoke()
        }
    }
}


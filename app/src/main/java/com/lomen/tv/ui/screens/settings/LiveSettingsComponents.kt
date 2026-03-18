@file:OptIn(androidx.tv.material3.ExperimentalTvMaterial3Api::class, androidx.compose.ui.ExperimentalComposeUiApi::class)

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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
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
    // 删除确认弹窗状态
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var itemToDelete by remember { mutableStateOf<T?>(null) }
    
    // 内置源提示弹窗状态
    var showBuiltInTip by remember { mutableStateOf(false) }
    
    // 当子弹窗显示时，不响应 dismiss
    val isSubDialogShowing = showDeleteConfirm || showBuiltInTip

    if (showDialogProvider()) {
        AlertDialog(
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                dismissOnBackPress = !isSubDialogShowing,
                dismissOnClickOutside = false
            ),
            modifier = modifier.width(600.dp),
            onDismissRequest = { 
                if (!isSubDialogShowing) {
                    onDismissRequest()
                }
            },
            containerColor = Color(0xFF2a2a2a),
            confirmButton = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (onDeleted != null) "确定键切换；菜单键删除" else "确定键切换",
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

                        LaunchedEffect(Unit) {
                            if (item == currentItem && !hasFocused) {
                                hasFocused = true
                                focusRequester.requestFocus()
                            }
                        }

                        ListItem(
                            modifier = Modifier
                                .focusRequester(focusRequester)
                                .onFocusChanged { isFocused = it.isFocused || it.hasFocus }
                                .onPreviewKeyEvent { keyEvent ->
                                    // 按菜单键或 Q 键删除（非内置源）
                                    if ((keyEvent.key == Key.Menu || keyEvent.key == Key.Q) && keyEvent.type == KeyEventType.KeyUp) {
                                        if (isBuiltIn) {
                                            showBuiltInTip = true
                                        } else if (onDeleted != null) {
                                            itemToDelete = item
                                            showDeleteConfirm = true
                                        }
                                        true
                                    } else if ((keyEvent.key == Key.Enter || keyEvent.key == Key.DirectionCenter) && keyEvent.type == KeyEventType.KeyUp) {
                                        // 确定键选中
                                        onSelected(item)
                                        true
                                    } else {
                                        false
                                    }
                                },
                            selected = isSelected,
                            onClick = { onSelected(item) },
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
    
    // 删除确认弹窗
    if (showDeleteConfirm && itemToDelete != null) {
        AlertDialog(
            properties = DialogProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = false
            ),
            onDismissRequest = { 
                showDeleteConfirm = false
                itemToDelete = null
            },
            containerColor = Color(0xFF2a2a2a),
            title = {
                Text(
                    text = "确认删除",
                    color = Color.White,
                    style = MaterialTheme.typography.headlineSmall
                )
            },
            text = {
                Text(
                    text = "确定要删除这个历史记录吗？",
                    color = Color.White.copy(alpha = 0.8f),
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                val focusRequester = remember { FocusRequester() }
                var isFocused by remember { mutableStateOf(false) }
                LaunchedEffect(Unit) {
                    focusRequester.requestFocus()
                }
                Button(
                    onClick = {
                        itemToDelete?.let { onDeleted?.invoke(it) }
                        showDeleteConfirm = false
                        itemToDelete = null
                    },
                    modifier = Modifier
                        .focusRequester(focusRequester)
                        .onFocusChanged { isFocused = it.isFocused || it.hasFocus },
                    colors = ButtonDefaults.colors(
                        containerColor = Color(0xFF444444),
                        contentColor = Color.White,
                        focusedContainerColor = PrimaryYellow,
                        focusedContentColor = Color.Black
                    )
                ) {
                    Text("删除", color = if (isFocused) Color.Black else Color.White)
                }
            },
            dismissButton = {
                var isFocused by remember { mutableStateOf(false) }
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        itemToDelete = null
                    },
                    modifier = Modifier.onFocusChanged { isFocused = it.isFocused || it.hasFocus },
                    colors = ButtonDefaults.colors(
                        containerColor = Color(0xFF444444),
                        contentColor = Color.White,
                        focusedContainerColor = PrimaryYellow,
                        focusedContentColor = Color.Black
                    )
                ) {
                    Text("取消", color = if (isFocused) Color.Black else Color.White)
                }
            }
        )
    }
    
    // 内置源提示弹窗
    if (showBuiltInTip) {
        AlertDialog(
            properties = DialogProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = false
            ),
            onDismissRequest = { showBuiltInTip = false },
            containerColor = Color(0xFF2a2a2a),
            title = {
                Text(
                    text = "提示",
                    color = Color.White,
                    style = MaterialTheme.typography.headlineSmall
                )
            },
            text = {
                Text(
                    text = "内置源不可删除",
                    color = Color.White.copy(alpha = 0.8f),
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                val focusRequester = remember { FocusRequester() }
                var isFocused by remember { mutableStateOf(false) }
                LaunchedEffect(Unit) {
                    focusRequester.requestFocus()
                }
                Button(
                    onClick = { showBuiltInTip = false },
                    modifier = Modifier
                        .focusRequester(focusRequester)
                        .onFocusChanged { isFocused = it.isFocused || it.hasFocus },
                    colors = ButtonDefaults.colors(
                        containerColor = Color(0xFF444444),
                        contentColor = Color.White,
                        focusedContainerColor = PrimaryYellow,
                        focusedContentColor = Color.Black
                    )
                ) {
                    Text("知道了", color = if (isFocused) Color.Black else Color.White)
                }
            }
        )
    }
}

/**
 * 直播设置列表项卡片
 * 高亮时图标和文字都变为黑色
 */
@Composable
fun LiveSettingListCard(
    icon: ImageVector,
    iconBackgroundColor: Color,
    iconTint: Color,
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailingContent: @Composable (() -> Unit)? = null,
    isFirstItem: Boolean = false
) {
    var isFocused by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    
    // 第一个项目自动获取焦点
    LaunchedEffect(Unit) {
        if (isFirstItem) {
            focusRequester.requestFocus()
        }
    }
    
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
            .focusRequester(focusRequester)
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


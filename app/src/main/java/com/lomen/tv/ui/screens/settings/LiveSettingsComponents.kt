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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import android.app.Dialog as AndroidDialog
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
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
import androidx.compose.ui.focus.focusProperties
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
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lomen.tv.ui.LocalCompactUiScale
import com.lomen.tv.ui.DialogDimens
import com.lomen.tv.ui.scale
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.tv.foundation.lazy.list.TvLazyColumn
import androidx.tv.foundation.lazy.list.items
import androidx.tv.foundation.lazy.list.rememberTvLazyListState
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.IconButton
import androidx.tv.material3.IconButtonDefaults
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
import com.lomen.tv.ui.theme.TextSecondary
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
                // 缩小二维码白底上下留白，为下方地址文本腾出空间
                .padding(horizontal = 6.dp, vertical = 4.dp),
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
 * 二维码弹窗 - 统一风格设计，与 TMDB API 设置和 WebDAV 配置保持一致
 */
@OptIn(ExperimentalTvMaterial3Api::class)
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
        // 关闭按钮焦点请求器
        val closeFocusRequester = remember { FocusRequester() }

        // 自动聚焦到关闭按钮
        LaunchedEffect(Unit) {
            kotlinx.coroutines.delay(100)
            closeFocusRequester.requestFocus()
        }

        androidx.compose.ui.window.Dialog(
            onDismissRequest = onDismissRequest,
            properties = DialogProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = false,
                usePlatformDefaultWidth = false
            )
        ) {
            // 使用 Box 包裹，通过 fillMaxSize 和 contentAlignment 实现居中
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    onClick = {},
                    colors = CardDefaults.colors(
                        containerColor = SurfaceDark,
                        focusedContainerColor = SurfaceDark
                    ),
                    modifier = modifier
                        .width(DialogDimens.QrCardWidth)
                        .height(DialogDimens.QrCardHeight)
                        .onPreviewKeyEvent { keyEvent ->
                            // 拦截所有方向键，防止光标移出窗口
                            if (keyEvent.key == Key.DirectionUp ||
                                keyEvent.key == Key.DirectionDown ||
                                keyEvent.key == Key.DirectionLeft ||
                                keyEvent.key == Key.DirectionRight
                            ) {
                                true
                            } else if (keyEvent.key == Key.Back && keyEvent.type == KeyEventType.KeyUp) {
                                // 按返回键关闭窗口
                                onDismissRequest()
                                true
                            } else {
                                false
                            }
                        }
                ) {
                    Column(
                        modifier = Modifier.padding(DialogDimens.QrCardPadding),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // 标题栏 - 右上角关闭按钮
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.headlineSmall,
                                color = TextPrimary
                            )
                            // 右上角关闭按钮 - 黄色底黑色图标
                            IconButton(
                                onClick = onDismissRequest,
                                colors = IconButtonDefaults.colors(
                                    containerColor = Color.Transparent,
                                    contentColor = TextMuted,
                                    focusedContainerColor = PrimaryYellow,
                                    focusedContentColor = Color.Black
                                ),
                                modifier = Modifier
                                    .size(40.dp)
                                    .focusRequester(closeFocusRequester)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "关闭",
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // 说明文字
                        description?.let {
                            Text(
                                text = it,
                                color = TextSecondary,
                                style = MaterialTheme.typography.bodySmall,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(2.dp))

                        // 二维码 - 使用百分比尺寸以适应不同分辨率，缩小高度
                        QrCodeImage(
                            text = text,
                            modifier = Modifier
                                .fillMaxWidth(0.53f)
                                .heightIn(max = DialogDimens.QrImageMaxHeight)
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // 服务器地址
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "访问地址:",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = text,
                                style = MaterialTheme.typography.bodySmall,
                                color = PrimaryYellow,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // 提示信息
                        Text(
                            text = "提示: 使用手机扫描二维码进行网页配置",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

/**
 * 网页推送成功提示文案：按用户从哪一类列表进入「添加」区分。
 * [MIXED] 用于直播页状态栏等可同时配置多项的入口。
 */
enum class LiveWebPushSuccessKind {
    LIVE_SOURCE,
    PROGRAM_GUIDE,
    USER_AGENT,
    MIXED
}

private fun LiveWebPushSuccessKind.successMessage(): String = when (this) {
    LiveWebPushSuccessKind.LIVE_SOURCE ->
        "添加直播源已成功，请选择您需要的资源。"
    LiveWebPushSuccessKind.PROGRAM_GUIDE ->
        "添加节目表已成功，请选择您需要的资源。"
    LiveWebPushSuccessKind.USER_AGENT ->
        "添加 User-Agent（UA）已成功，请选择您需要的资源。"
    LiveWebPushSuccessKind.MIXED ->
        "直播源、节目表与 UA 已添加成功。请在对应列表中选择您需要的资源。"
}

/**
 * 网页推送直播源 / 节目表 / UA 成功后，关闭二维码后的说明弹窗。
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun LiveWebConfigSuccessHintDialog(
    onDismiss: () -> Unit,
    kind: LiveWebPushSuccessKind = LiveWebPushSuccessKind.MIXED
) {
    val closeFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        delay(100)
        closeFocusRequester.requestFocus()
    }

    // 必须使用 Dialog 而非 Popup：历史列表是 Material AlertDialog（独立窗口），
    // Popup 画在主窗口上会被挡在下面，导致「先关列表才看到提示」的顺序错误。
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        val view = LocalView.current
        SideEffect {
            var ctx: Context? = view.context
            while (ctx is ContextWrapper) {
                if (ctx is AndroidDialog) {
                    ctx.window?.setDimAmount(0f)
                    break
                }
                ctx = ctx.baseContext
            }
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .onPreviewKeyEvent { keyEvent ->
                    if (keyEvent.key == Key.DirectionUp ||
                        keyEvent.key == Key.DirectionDown ||
                        keyEvent.key == Key.DirectionLeft ||
                        keyEvent.key == Key.DirectionRight
                    ) {
                        true
                    } else if (keyEvent.key == Key.Back && keyEvent.type == KeyEventType.KeyUp) {
                        onDismiss()
                        true
                    } else {
                        false
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Card(
                onClick = {},
                colors = CardDefaults.colors(
                    containerColor = SurfaceDark,
                    focusedContainerColor = SurfaceDark
                ),
                modifier = Modifier
                    .widthIn(min = DialogDimens.SuccessHintWidthMin, max = DialogDimens.SuccessHintWidthMax)
                    .padding(12.dp)
                    .focusProperties {
                        canFocus = true
                    }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF10b981).copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = Color(0xFF10b981),
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "添加成功",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = kind.successMessage(),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.colors(
                            containerColor = SurfaceDark,
                            contentColor = Color.Black,
                            focusedContainerColor = PrimaryYellow,
                            focusedContentColor = Color.Black,
                            pressedContainerColor = PrimaryYellow,
                            pressedContentColor = Color.Black
                        ),
                        shape = ButtonDefaults.shape(shape = RoundedCornerShape(8.dp)),
                        modifier = Modifier.focusRequester(closeFocusRequester)
                    ) {
                        Text(
                            text = "我知道了",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Black,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
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
    /** 为 true 时（例如上层叠了二维码弹窗）禁止返回键关闭本列表，避免误关底层窗口 */
    blockDismissForOverlay: Boolean = false,
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
    val isSubDialogShowing = showDeleteConfirm || showBuiltInTip || blockDismissForOverlay

    if (showDialogProvider()) {
        AlertDialog(
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                dismissOnBackPress = !isSubDialogShowing,
                dismissOnClickOutside = false
            ),
            modifier = modifier.width(DialogDimens.CardWidthHistoryList),
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
                val currentIndex = remember(items, currentItem) {
                    items.indexOf(currentItem).coerceAtLeast(0)
                }
                val listState = rememberTvLazyListState(
                    initialFirstVisibleItemIndex = max(0, currentIndex - 2)
                )

                TvLazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(vertical = 2.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    items(items) { item ->
                        val focusRequester = remember { FocusRequester() }
                        var isFocused by remember { mutableStateOf(false) }
                        val isSelected = currentItem == item
                        val isBuiltIn = isBuiltInItem(item)
                        val (headline, supporting) = itemContent(item, isSelected, isFocused)

                        if (item == currentItem && !hasFocused) {
                            LaunchedEffect(item, currentItem) {
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
 * 直播设置列表项卡片（与设置页大卡片统一：左图标、右标题+副标题、固定高度与紧凑缩放）
 */
@Composable
fun LiveSettingListCard(
    icon: ImageVector,
    iconBackgroundColor: Color,
    iconTint: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailingContent: @Composable (() -> Unit)? = null,
    isFirstItem: Boolean = false
) {
    val s = LocalCompactUiScale.current
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
            .height(84.dp.scale(s))
            .focusRequester(focusRequester)
            .onFocusChanged { focusState ->
                isFocused = focusState.isFocused
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp.scale(s), vertical = 12.dp.scale(s)),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp.scale(s))
                    .clip(RoundedCornerShape(10.dp.scale(s)))
                    .background(iconBackgroundColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isFocused) Color.Black else iconTint,
                    modifier = Modifier.size(22.dp.scale(s))
                )
            }

            Spacer(modifier = Modifier.width(14.dp.scale(s)))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = (MaterialTheme.typography.titleMedium.fontSize.value * s).sp
                    ),
                    color = if (isFocused) Color.Black else TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (subtitle.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(2.dp.scale(s)))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = (MaterialTheme.typography.bodySmall.fontSize.value * s).sp
                        ),
                        color = if (isFocused) Color.Black.copy(alpha = 0.8f) else TextSecondary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            trailingContent?.invoke()
        }
    }
}


package com.lomen.tv.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.lomen.tv.domain.model.VersionInfo
import com.lomen.tv.ui.DialogDimens
import com.lomen.tv.ui.theme.PrimaryYellow
import androidx.tv.material3.*

/**
 * 版本更新对话框
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun VersionUpdateDialog(
    versionInfo: VersionInfo,
    onUpdate: () -> Unit,
    onCancel: () -> Unit
) {
    // 创建焦点请求器
    val cancelFocusRequester = remember { FocusRequester() }
    val updateFocusRequester = remember { FocusRequester() }
    
    Dialog(
        onDismissRequest = onCancel,
        properties = androidx.compose.ui.window.DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.7f)),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .width(DialogDimens.VersionUpdateWidth)
                    .heightIn(min = DialogDimens.VersionUpdateHeightMin, max = DialogDimens.VersionUpdateHeightMax)
                    .onPreviewKeyEvent { keyEvent ->
                        // 拦截方向键和返回键
                        if (keyEvent.type == KeyEventType.KeyUp) {
                            when (keyEvent.key) {
                                Key.Back -> {
                                    onCancel()
                                    true
                                }
                                else -> false
                            }
                        } else false
                    },
                colors = CardDefaults.colors(
                    containerColor = Color(0xFF333333)
                ),
                onClick = {}
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp)
                ) {
                    // 标题
                    Text(
                        text = "发现新版本 v${versionInfo.versionName}",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    
                    // 内容
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = "更新内容：",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Text(
                            text = versionInfo.releaseNotes,
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.8f),
                            lineHeight = 18.sp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "发布日期：${versionInfo.releaseDate}",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    }
                    
                    // 按钮
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        // 取消按钮 - 使用TV专用的Button
                        var cancelButtonFocused by remember { mutableStateOf(false) }
                        LaunchedEffect(Unit) {
                            // 对话框显示时立即聚焦到取消按钮
                            kotlinx.coroutines.delay(50)
                            cancelFocusRequester.requestFocus()
                        }
                        Button(
                            onClick = onCancel,
                            modifier = Modifier
                                .padding(horizontal = 8.dp)
                                .focusRequester(cancelFocusRequester)
                                .onFocusChanged { cancelButtonFocused = it.isFocused || it.hasFocus }
                                .onPreviewKeyEvent { keyEvent ->
                                    if (keyEvent.type == KeyEventType.KeyDown) {
                                        when (keyEvent.key) {
                                            Key.DirectionRight -> {
                                                updateFocusRequester.requestFocus()
                                                true
                                            }
                                            else -> false
                                        }
                                    } else false
                                },
                            colors = ButtonDefaults.colors(
                                containerColor = Color(0xFF666666),
                                contentColor = Color.White,
                                focusedContainerColor = PrimaryYellow,
                                focusedContentColor = Color.Black
                            ),
                            shape = ButtonDefaults.shape(shape = RoundedCornerShape(24.dp))
                        ) {
                            Text(
                                text = "取消",
                                color = if (cancelButtonFocused) Color.Black else Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        
                        // 立即更新按钮 - 使用TV专用的Button
                        var updateButtonFocused by remember { mutableStateOf(false) }
                        Button(
                            onClick = onUpdate,
                            modifier = Modifier
                                .padding(horizontal = 8.dp)
                                .focusRequester(updateFocusRequester)
                                .onFocusChanged { updateButtonFocused = it.isFocused || it.hasFocus }
                                .onPreviewKeyEvent { keyEvent ->
                                    if (keyEvent.type == KeyEventType.KeyDown) {
                                        when (keyEvent.key) {
                                            Key.DirectionLeft -> {
                                                cancelFocusRequester.requestFocus()
                                                true
                                            }
                                            else -> false
                                        }
                                    } else false
                                },
                            colors = ButtonDefaults.colors(
                                containerColor = Color(0xFF666666),
                                contentColor = Color.White,
                                focusedContainerColor = PrimaryYellow,
                                focusedContentColor = Color.Black
                            ),
                            shape = ButtonDefaults.shape(shape = RoundedCornerShape(24.dp))
                        ) {
                            Text(
                                text = "立即更新",
                                color = if (updateButtonFocused) Color.Black else Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}
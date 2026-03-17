package com.lomen.tv.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
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
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lomen.tv.domain.model.VersionInfo
import com.lomen.tv.ui.theme.PrimaryYellow
import androidx.tv.material3.*
import androidx.compose.ui.ExperimentalComposeUiApi

/**
 * 版本更新对话框
 */
@OptIn(ExperimentalTvMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun VersionUpdateDialog(
    versionInfo: VersionInfo,
    onUpdate: () -> Unit,
    onCancel: () -> Unit
) {
    // 创建焦点请求器
    val cancelFocusRequester = remember { FocusRequester() }
    val updateFocusRequester = remember { FocusRequester() }
    
    // 当对话框显示时请求焦点到取消按钮
    LaunchedEffect(Unit) {
        cancelFocusRequester.requestFocus()
    }
    
    // 使用tv-material3的Card作为背景
    Card(
        modifier = Modifier
            .fillMaxSize()
            .focusProperties { 
                // 阻止焦点逃出对话框
                exit = { FocusRequester.Cancel }
            },
        colors = CardDefaults.colors(
            containerColor = Color.Black.copy(alpha = 0.7f)
        ),
        onClick = {}
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .width(600.dp)
                    .heightIn(min = 400.dp, max = 600.dp)
                    .focusProperties { 
                        // 阻止焦点逃出对话框
                        exit = { FocusRequester.Cancel }
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
                        // 取消按钮
                        var cancelButtonFocused by remember { mutableStateOf(false) }
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 8.dp)
                                .focusRequester(cancelFocusRequester)
                                .focusable()
                                .onFocusChanged { cancelButtonFocused = it.isFocused }
                                .clickable(
                                    onClick = onCancel,
                                    indication = null,
                                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                                )
                                .background(
                                    if (cancelButtonFocused) PrimaryYellow else Color(0xFF666666),
                                    RoundedCornerShape(24.dp)
                                )
                                .padding(horizontal = 24.dp, vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "取消",
                                color = if (cancelButtonFocused) Color.Black else Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        
                        // 立即更新按钮
                        var updateButtonFocused by remember { mutableStateOf(false) }
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 8.dp)
                                .focusRequester(updateFocusRequester)
                                .focusable()
                                .onFocusChanged { updateButtonFocused = it.isFocused }
                                .clickable(
                                    onClick = onUpdate,
                                    indication = null,
                                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                                )
                                .background(
                                    if (updateButtonFocused) PrimaryYellow else Color(0xFF666666),
                                    RoundedCornerShape(24.dp)
                                )
                                .padding(horizontal = 24.dp, vertical = 12.dp),
                            contentAlignment = Alignment.Center
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
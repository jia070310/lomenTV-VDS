package com.lomen.tv.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lomen.tv.ui.theme.PrimaryYellow
import kotlinx.coroutines.delay

/**
 * 版本更新提示徽章
 * 屏幕居中显示，10秒后自动关闭，焦点时图标变红色
 */
@Composable
fun VersionUpdateBadge(
    versionName: String,
    onClick: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    // 10秒后自动关闭
    LaunchedEffect(Unit) {
        delay(10000)
        onDismiss()
    }
    
    // 焦点状态
    var isFocused by remember { mutableStateOf(false) }
    
    // 根据焦点状态决定颜色
    val iconBackgroundColor = if (isFocused) Color.Red else Color.White
    val iconTintColor = if (isFocused) Color.White else PrimaryYellow
    
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(PrimaryYellow)
            .onFocusChanged { isFocused = it.isFocused }
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // 信息图标
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(iconBackgroundColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = iconTintColor,
                modifier = Modifier.size(16.dp)
            )
        }
        
        // 版本文字
        Text(
            text = "新版本: v$versionName",
            color = Color.Black,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

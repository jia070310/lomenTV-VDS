package com.lomen.tv.ui.screens.settings

import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import com.lomen.tv.data.preferences.TmdbApiPreferences
import com.lomen.tv.ui.theme.BackgroundDark
import com.lomen.tv.ui.theme.PrimaryYellow
import com.lomen.tv.ui.theme.SurfaceDark
import com.lomen.tv.ui.theme.TextMuted
import com.lomen.tv.ui.theme.TextPrimary
import com.lomen.tv.ui.theme.TextSecondary
import com.lomen.tv.ui.viewmodel.TmdbApiViewModel

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TmdbApiSettingsDialog(
    onDismiss: () -> Unit,
    tmdbApiViewModel: TmdbApiViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    
    // 获取当前状态
    val currentApiKey by tmdbApiViewModel.apiKey.collectAsState(initial = TmdbApiPreferences.DEFAULT_API_KEY)
    val isServerRunning by tmdbApiViewModel.isServerRunning.collectAsState()
    val hasCustomKey by tmdbApiViewModel.hasCustomApiKey.collectAsState(initial = false)
    
    // 服务器地址
    val serverUrl = tmdbApiViewModel.serverUrl
    
    // 生成二维码 - 增大尺寸以获得更清晰的二维码
    val qrCodeBitmap = remember(serverUrl) {
        generateQRCode(serverUrl, 400)
    }
    
    // 焦点管理
    val closeButtonFocus = remember { FocusRequester() }
    
    // 启动服务器
    LaunchedEffect(Unit) {
        android.util.Log.d("TmdbApiSettingsDialog", "Starting config server...")
        tmdbApiViewModel.startConfigServer {
            android.util.Log.d("TmdbApiSettingsDialog", "Config received, showing toast")
            Toast.makeText(context, "TMDB API 配置已更新", Toast.LENGTH_SHORT).show()
        }
        android.util.Log.d("TmdbApiSettingsDialog", "Config server started")
    }
    
    // 关闭时停止服务器 - 只在服务器从运行变为停止时关闭对话框
    var wasRunning by remember { mutableStateOf(false) }
    LaunchedEffect(isServerRunning) {
        android.util.Log.d("TmdbApiSettingsDialog", "Server running state: $isServerRunning, wasRunning: $wasRunning")
        if (wasRunning && !isServerRunning) {
            android.util.Log.d("TmdbApiSettingsDialog", "Server stopped, dismissing dialog")
            onDismiss()
        }
        if (isServerRunning) {
            wasRunning = true
        }
    }
    
    Dialog(onDismissRequest = {
        tmdbApiViewModel.stopConfigServer()
        onDismiss()
    }) {
        Box(
            modifier = Modifier
                .width(520.dp)  // 固定宽度，避免内容变化时尺寸改变
                .clip(RoundedCornerShape(16.dp))
                .background(BackgroundDark)
                .padding(32.dp)
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 标题
                Text(
                    text = "TMDB API 设置",
                    style = MaterialTheme.typography.headlineMedium,
                    color = TextPrimary
                )
                
                // 说明文字
                Text(
                    text = "扫描二维码打开手机网页，输入您的 TMDB API Key",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
                
                // 二维码 - 增大尺寸，减少边距让二维码更大
                if (qrCodeBitmap != null) {
                    Box(
                        modifier = Modifier
                            .size(320.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White)
                            .padding(8.dp),  // 减少边距，让二维码更大
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            bitmap = qrCodeBitmap.asImageBitmap(),
                            contentDescription = "配置二维码",
                            modifier = Modifier
                                .fillMaxSize()  // 填满整个可用空间
                                .padding(4.dp)  // 少量内边距避免贴边
                        )
                    }
                }
                
                // 服务器地址
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "或手动访问地址:",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = serverUrl,
                        style = MaterialTheme.typography.bodyMedium,
                        color = PrimaryYellow
                    )
                }
                
                // 按钮区域
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    // 关闭按钮 - 未选中时灰色底白色字，选中时黄色底黑字
                    var isCloseButtonFocused by remember { mutableStateOf(false) }
                    Button(
                        onClick = {
                            tmdbApiViewModel.stopConfigServer()
                            onDismiss()
                        },
                        colors = ButtonDefaults.colors(
                            containerColor = SurfaceDark,
                            focusedContainerColor = PrimaryYellow,
                            contentColor = TextPrimary,
                            focusedContentColor = Color.Black
                        ),
                        modifier = Modifier
                            .focusRequester(closeButtonFocus)
                            .onFocusChanged { isCloseButtonFocused = it.isFocused }
                    ) {
                        Text(
                            text = "关闭",
                            color = if (isCloseButtonFocused) Color.Black else TextPrimary
                        )
                    }
                    
                    if (hasCustomKey) {
                        Spacer(modifier = Modifier.width(16.dp))
                        
                        // 删除 API 按钮 - 未选中时灰色底白色字，选中时黄色底黑字
                        var isDeleteButtonFocused by remember { mutableStateOf(false) }
                        Button(
                            onClick = {
                                tmdbApiViewModel.clearApiKey()
                                Toast.makeText(context, "API 已删除", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.colors(
                                containerColor = SurfaceDark,
                                focusedContainerColor = PrimaryYellow,
                                contentColor = TextPrimary,
                                focusedContentColor = Color.Black
                            ),
                            modifier = Modifier.onFocusChanged { isDeleteButtonFocused = it.isFocused }
                        ) {
                            Text(
                                text = "删除 API",
                                color = if (isDeleteButtonFocused) Color.Black else TextPrimary
                            )
                        }
                    }
                }
                
                // 提示信息
                Text(
                    text = "提示: 在 https://www.themoviedb.org/settings/api 申请免费 API Key",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )
            }
        }
    }
    
    // 自动聚焦到关闭按钮
    LaunchedEffect(Unit) {
        closeButtonFocus.requestFocus()
    }
}

/**
 * 生成二维码
 */
private fun generateQRCode(content: String, size: Int): Bitmap? {
    return try {
        val writer = QRCodeWriter()
        val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, size, size)
        val width = bitMatrix.width
        val height = bitMatrix.height
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
        
        for (x in 0 until width) {
            for (y in 0 until height) {
                bitmap.setPixel(x, y, if (bitMatrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
            }
        }
        bitmap
    } catch (e: Exception) {
        null
    }
}

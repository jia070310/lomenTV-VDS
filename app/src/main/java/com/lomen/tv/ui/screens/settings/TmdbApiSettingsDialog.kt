package com.lomen.tv.ui.screens.settings

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.foundation.focusable
import androidx.compose.ui.Modifier
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.IconButton
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.lomen.tv.ui.theme.PrimaryYellow
import com.lomen.tv.ui.theme.DialogUiTokens
import com.lomen.tv.ui.theme.SurfaceDark
import com.lomen.tv.ui.theme.TextMuted
import com.lomen.tv.ui.theme.TextPrimary
import com.lomen.tv.ui.theme.TextSecondary
import com.lomen.tv.ui.DialogDimens
import com.lomen.tv.ui.viewmodel.TmdbApiViewModel

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TmdbApiSettingsDialog(
    onDismiss: () -> Unit,
    tmdbApiViewModel: TmdbApiViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val isServerRunning by tmdbApiViewModel.isServerRunning.collectAsState()

    // 服务器地址
    val serverUrl = tmdbApiViewModel.serverUrl

    // 焦点管理 - 创建外部容器的焦点请求器
    val closeButtonFocus = remember { FocusRequester() }
    val scope = rememberCoroutineScope()

    // 启动服务器
    LaunchedEffect(Unit) {
        android.util.Log.d("TmdbApiSettingsDialog", "Starting config server...")
        tmdbApiViewModel.startConfigServer {
            android.util.Log.d("TmdbApiSettingsDialog", "Config received, showing toast")
            Toast.makeText(context, "TMDB API 配置已更新", Toast.LENGTH_SHORT).show()
            // 提示显示片刻后停止服务，由下方 LaunchedEffect(isServerRunning) 触发关闭二维码窗口
            scope.launch {
                delay(2_000) // 与 Toast.LENGTH_SHORT 展示时间相当后再关窗
                tmdbApiViewModel.stopConfigServer()
            }
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
    
    Dialog(
        onDismissRequest = {
            tmdbApiViewModel.stopConfigServer()
            onDismiss()
        },
        properties = androidx.compose.ui.window.DialogProperties(
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
                    containerColor = DialogUiTokens.ContainerColor,
                    focusedContainerColor = DialogUiTokens.ContainerColor
                ),
                modifier = Modifier
                    .width(DialogDimens.QrCardWidth)
                    .height(DialogDimens.QrCardHeight)
                    .border(DialogUiTokens.BorderWidth, DialogUiTokens.BorderColor, RoundedCornerShape(DialogUiTokens.CornerRadius))
                    .onPreviewKeyEvent { keyEvent ->
                        // 拦截所有按键事件，防止光标移出窗口
                        when (keyEvent.key) {
                            Key.DirectionUp,
                            Key.DirectionDown,
                            Key.DirectionLeft,
                            Key.DirectionRight -> {
                                true
                            }
                            Key.Back -> {
                                if (keyEvent.type == KeyEventType.KeyUp) {
                                    // 按返回键关闭窗口
                                    tmdbApiViewModel.stopConfigServer()
                                    onDismiss()
                                }
                                true
                            }
                            else -> false
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
                            text = "TMDB API 设置",
                            style = MaterialTheme.typography.headlineSmall,
                            color = TextPrimary
                        )
                        // 右上角关闭按钮 - 黄色底黑色图标
                        IconButton(
                            onClick = {
                                tmdbApiViewModel.stopConfigServer()
                                onDismiss()
                            },
                            colors = androidx.tv.material3.IconButtonDefaults.colors(
                                containerColor = Color.Transparent,
                                contentColor = TextMuted,
                                focusedContainerColor = PrimaryYellow,
                                focusedContentColor = Color.Black
                            ),
                            modifier = Modifier
                                .size(40.dp)
                                .focusRequester(closeButtonFocus)
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
                    Text(
                        text = "扫描二维码打开手机网页，输入您的 TMDB API Key",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    // 二维码样式与直播设置保持一致
                    QrCodeImage(
                        text = serverUrl,
                        modifier = Modifier
                            .fillMaxWidth(0.53f)
                            .height(DialogDimens.QrImageMaxHeight)
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // 服务器地址
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "或手动访问地址:",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = serverUrl,
                            style = MaterialTheme.typography.bodySmall,
                            color = PrimaryYellow,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // 提示信息
                    Text(
                        text = "提示: 在 https://www.themoviedb.org/settings/api 申请免费 API Key",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }

    // 自动聚焦到关闭按钮 - 延迟执行确保对话框已完全显示
    LaunchedEffect(Unit) {
        delay(100) // 短暂延迟确保布局完成
        closeButtonFocus.requestFocus()
    }
}


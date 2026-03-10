@file:OptIn(androidx.tv.material3.ExperimentalTvMaterial3Api::class)

package com.lomen.tv.ui.screens.settings

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.material3.LocalContentColor
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.layout.layout
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.IconButton
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import com.lomen.tv.service.WebDavConfigServer
import com.lomen.tv.ui.theme.BackgroundDark
import com.lomen.tv.ui.theme.GlassBackground
import com.lomen.tv.ui.theme.PrimaryYellow
import com.lomen.tv.ui.theme.SurfaceDark
import com.lomen.tv.ui.theme.TextMuted
import com.lomen.tv.ui.theme.TextPrimary
import com.lomen.tv.ui.theme.TextSecondary
import com.lomen.tv.domain.model.ResourceLibrary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.NetworkInterface
import java.util.Collections

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun WebDavConfigDialog(
    onDismiss: () -> Unit,
    onConfigReceived: (WebDavConfigServer.WebDavConfig) -> Unit,
    existingLibrary: ResourceLibrary? = null
) {
    // 如果有现有库，显示编辑表单；否则显示二维码配置
    if (existingLibrary != null) {
        WebDavEditForm(
            existingLibrary = existingLibrary,
            onDismiss = onDismiss,
            onConfigReceived = onConfigReceived
        )
    } else {
        WebDavQrCodeConfig(
            onDismiss = onDismiss,
            onConfigReceived = onConfigReceived
        )
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun WebDavEditForm(
    existingLibrary: ResourceLibrary,
    onDismiss: () -> Unit,
    onConfigReceived: (WebDavConfigServer.WebDavConfig) -> Unit
) {
    var protocol by remember { mutableStateOf(existingLibrary.protocol) }
    var host by remember { mutableStateOf(existingLibrary.host) }
    var port by remember { mutableStateOf(existingLibrary.port.toString()) }
    var path by remember { mutableStateOf(existingLibrary.path) }
    var username by remember { mutableStateOf(existingLibrary.username) }
    var password by remember { mutableStateOf(existingLibrary.password) }
    var passwordVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isTesting by remember { mutableStateOf(false) }
    var showErrorDialog by remember { mutableStateOf(false) }
    var connectionError by remember { mutableStateOf<String?>(null) }

    val scope = rememberCoroutineScope()

    // 焦点请求器 - 用于HTTP按钮接收初始焦点
    val httpButtonFocusRequester = remember { FocusRequester() }
    val hostFocusRequester = remember { FocusRequester() }
    val portFocusRequester = remember { FocusRequester() }
    val pathFocusRequester = remember { FocusRequester() }
    val usernameFocusRequester = remember { FocusRequester() }
    val passwordFocusRequester = remember { FocusRequester() }
    val saveButtonFocusRequester = remember { FocusRequester() }

    // 使用Dialog包装，自动处理焦点管理和返回键
    Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        // 居中容器，带半透明背景遮罩
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.6f))
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                onClick = {},
                colors = CardDefaults.colors(
                    containerColor = SurfaceDark
                ),
                modifier = Modifier
                    .widthIn(min = 480.dp, max = 560.dp)
                    .heightIn(max = 800.dp)
                    .onPreviewKeyEvent { keyEvent ->
                        if (keyEvent.key == Key.Back && keyEvent.type == KeyEventType.KeyUp) {
                            onDismiss()
                            true
                        } else {
                            false
                        }
                    }
            ) {
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
            // 标题栏
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "编辑WebDAV网盘",
                    style = MaterialTheme.typography.headlineSmall,
                    color = TextPrimary
                )
                IconButton(
                    onClick = onDismiss,
                    colors = androidx.tv.material3.IconButtonDefaults.colors(
                        containerColor = Color.Transparent,
                        contentColor = TextMuted,
                        focusedContainerColor = PrimaryYellow,
                        focusedContentColor = Color.Black
                    ),
                    scale = androidx.tv.material3.IconButtonDefaults.scale(
                        scale = 1.0f,
                        focusedScale = 1.05f
                    ),
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "关闭",
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

                // 表单字段 - 使用weight确保表单区域可滚动但按钮始终可见
            androidx.compose.foundation.lazy.LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                // 协议选择
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // HTTP按钮 - 初始焦点在这里
                    Button(
                        onClick = { protocol = "http" },
                        colors = ButtonDefaults.colors(
                            containerColor = Color.Transparent,
                            contentColor = TextMuted,
                            focusedContainerColor = PrimaryYellow,
                            focusedContentColor = Color.Black,
                            pressedContainerColor = PrimaryYellow,
                            pressedContentColor = Color.Black
                        ),
                        shape = ButtonDefaults.shape(shape = RoundedCornerShape(8.dp)),
                        scale = ButtonDefaults.scale(
                            scale = 1.0f,
                            focusedScale = 1.02f
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(httpButtonFocusRequester)
                    ) {
                        Text(
                            text = "HTTP",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                color = Color.Unspecified
                            ),
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                    
                    // HTTPS按钮
                    Button(
                        onClick = { protocol = "https" },
                        colors = ButtonDefaults.colors(
                            containerColor = Color.Transparent,
                            contentColor = TextMuted,
                            focusedContainerColor = PrimaryYellow,
                            focusedContentColor = Color.Black,
                            pressedContainerColor = PrimaryYellow,
                            pressedContentColor = Color.Black
                        ),
                        shape = ButtonDefaults.shape(shape = RoundedCornerShape(8.dp)),
                        scale = ButtonDefaults.scale(
                            scale = 1.0f,
                            focusedScale = 1.02f
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "HTTPS",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                color = Color.Unspecified
                            ),
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                }
                
                // 对话框打开时请求焦点到HTTP按钮
                LaunchedEffect(Unit) {
                    httpButtonFocusRequester.requestFocus()
                }

                // 服务器地址
                TvTextField(
                    label = "服务器地址",
                    value = host,
                    onValueChange = { host = it },
                    placeholder = "例如: 192.168.1.100",
                    focusRequester = hostFocusRequester,
                    onNext = { portFocusRequester.requestFocus() },
                    onPrev = { httpButtonFocusRequester.requestFocus() }
                )

                // 端口
                TvTextField(
                    label = "端口",
                    value = port,
                    onValueChange = { port = it.filter { c -> c.isDigit() } },
                    placeholder = "例如: 5244",
                    focusRequester = portFocusRequester,
                    onNext = { pathFocusRequester.requestFocus() },
                    onPrev = { hostFocusRequester.requestFocus() }
                )

                // 路径
                TvTextField(
                    label = "路径",
                    value = path,
                    onValueChange = { path = it },
                    placeholder = "例如: /dav",
                    focusRequester = pathFocusRequester,
                    onNext = { usernameFocusRequester.requestFocus() },
                    onPrev = { portFocusRequester.requestFocus() }
                )

                // 用户名
                TvTextField(
                    label = "用户名（可选）",
                    value = username,
                    onValueChange = { username = it },
                    placeholder = "用户名",
                    focusRequester = usernameFocusRequester,
                    onNext = { passwordFocusRequester.requestFocus() },
                    onPrev = { pathFocusRequester.requestFocus() }
                )

                // 密码
                TvPasswordField(
                    label = "密码（可选）",
                    value = password,
                    onValueChange = { password = it },
                    placeholder = "密码",
                    passwordVisible = passwordVisible,
                    onToggleVisibility = { passwordVisible = !passwordVisible },
                    focusRequester = passwordFocusRequester,
                    onNext = { saveButtonFocusRequester.requestFocus() },
                    onPrev = { usernameFocusRequester.requestFocus() }
                )
                }
            }

            // 错误信息
            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = errorMessage!!,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFef4444)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 按钮行
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.colors(
                        containerColor = Color.Transparent,
                        contentColor = TextMuted,
                        focusedContainerColor = PrimaryYellow,
                        focusedContentColor = Color.Black,
                        pressedContainerColor = PrimaryYellow,
                        pressedContentColor = Color.Black
                    ),
                    shape = ButtonDefaults.shape(shape = RoundedCornerShape(8.dp)),
                    scale = ButtonDefaults.scale(
                        scale = 1.0f,
                        focusedScale = 1.02f
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "取消",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = Color.Unspecified
                        ),
                        modifier = Modifier.padding(vertical = 6.dp)
                    )
                }

                Button(
                    onClick = {
                        // 验证输入
                        if (host.isBlank()) {
                            errorMessage = "请输入服务器地址"
                            return@Button
                        }
                        val portNumber = port.toIntOrNull()
                        if (portNumber == null || portNumber <= 0 || portNumber > 65535) {
                            errorMessage = "请输入有效的端口号（1-65535）"
                            return@Button
                        }
                        if (path.isBlank()) {
                            errorMessage = "请输入路径"
                            return@Button
                        }

                        // 确保路径以/开头
                        val normalizedPath = if (path.startsWith("/")) path else "/$path"

                        val config = WebDavConfigServer.WebDavConfig(
                            protocol = protocol,
                            host = host.trim(),
                            port = portNumber,
                            path = normalizedPath,
                            username = username.trim(),
                            password = password
                        )
                        
                        // 先测试连接
                        isTesting = true
                        errorMessage = null
                        scope.launch {
                            val result = testWebDavConnection(config)
                            isTesting = false
                            if (result.isSuccess) {
                                onConfigReceived(config)
                            } else {
                                connectionError = result.errorMessage
                                showErrorDialog = true
                            }
                        }
                    },
                    enabled = !isTesting,
                    colors = ButtonDefaults.colors(
                        containerColor = Color.Transparent,
                        contentColor = TextMuted,
                        focusedContainerColor = PrimaryYellow,
                        focusedContentColor = Color.Black,
                        pressedContainerColor = PrimaryYellow,
                        pressedContentColor = Color.Black
                    ),
                    shape = ButtonDefaults.shape(shape = RoundedCornerShape(8.dp)),
                    scale = ButtonDefaults.scale(
                        scale = 1.0f,
                        focusedScale = 1.02f
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(saveButtonFocusRequester)
                ) {
                    if (isTesting) {
                        Text(
                            text = "测试中...",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = Color.Unspecified
                            ),
                            modifier = Modifier.padding(vertical = 6.dp)
                        )
                    } else {
                        Text(
                            text = "保存",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = Color.Unspecified
                            ),
                            modifier = Modifier.padding(vertical = 6.dp)
                        )
                    }
                }
            }
        }
    }
    
    // 错误对话框
    if (showErrorDialog) {
        connectionError?.let { error ->
            ErrorDialog(
                errorMessage = error,
                onDismiss = { showErrorDialog = false }
            )
        }
    }
}
}
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun ProtocolButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.colors(
            containerColor = if (selected) PrimaryYellow else SurfaceDark,
            contentColor = if (selected) BackgroundDark else TextPrimary
        ),
        shape = ButtonDefaults.shape(shape = RoundedCornerShape(8.dp)),
        modifier = modifier
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(vertical = 8.dp)
        )
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun TvTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    focusRequester: FocusRequester,
    onNext: () -> Unit,
    onPrev: (() -> Unit)? = null
) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = TextMuted
        )
        Spacer(modifier = Modifier.height(4.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(text = placeholder, color = TextMuted) },
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester)
                .onPreviewKeyEvent { keyEvent ->
                    // 只在焦点状态下处理方向键
                    when {
                        keyEvent.key == Key.DirectionDown && keyEvent.type == KeyEventType.KeyUp -> {
                            onNext()
                            true
                        }
                        keyEvent.key == Key.DirectionUp && keyEvent.type == KeyEventType.KeyUp && onPrev != null -> {
                            onPrev()
                            true
                        }
                        else -> false
                    }
                },
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = SurfaceDark,
                unfocusedContainerColor = Color.Transparent,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextMuted,
                focusedBorderColor = PrimaryYellow,
                unfocusedBorderColor = TextMuted.copy(alpha = 0.5f)
            ),
            shape = RoundedCornerShape(8.dp)
        )
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun TvPasswordField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    passwordVisible: Boolean,
    onToggleVisibility: () -> Unit,
    focusRequester: FocusRequester,
    onNext: () -> Unit,
    onPrev: (() -> Unit)? = null
) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = TextMuted
        )
        Spacer(modifier = Modifier.height(4.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(text = placeholder, color = TextMuted) },
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(
                    onClick = onToggleVisibility,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = if (passwordVisible) "隐藏密码" else "显示密码",
                        tint = TextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester)
                .onPreviewKeyEvent { keyEvent ->
                    // 只在焦点状态下处理方向键
                    when {
                        keyEvent.key == Key.DirectionDown && keyEvent.type == KeyEventType.KeyUp -> {
                            onNext()
                            true
                        }
                        keyEvent.key == Key.DirectionUp && keyEvent.type == KeyEventType.KeyUp && onPrev != null -> {
                            onPrev()
                            true
                        }
                        else -> false
                    }
                },
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = SurfaceDark,
                unfocusedContainerColor = Color.Transparent,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextMuted,
                focusedBorderColor = PrimaryYellow,
                unfocusedBorderColor = TextMuted.copy(alpha = 0.5f)
            ),
            shape = RoundedCornerShape(8.dp)
        )
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun WebDavQrCodeConfig(
    onDismiss: () -> Unit,
    onConfigReceived: (WebDavConfigServer.WebDavConfig) -> Unit
) {
    val context = LocalContext.current
    val server = remember { WebDavConfigServer(context, 8893) }
    var serverUrl by remember { mutableStateOf("") }
    var isServerRunning by remember { mutableStateOf(false) }
    var configReceived by remember { mutableStateOf<WebDavConfigServer.WebDavConfig?>(null) }
    var isTesting by remember { mutableStateOf(false) }
    var showErrorDialog by remember { mutableStateOf(false) }
    var connectionError by remember { mutableStateOf<String?>(null) }
    
    // 关闭按钮焦点请求器
    val closeFocusRequester = remember { FocusRequester() }
    val scope = rememberCoroutineScope()

    // 启动服务器
    LaunchedEffect(Unit) {
        val ip = getLocalIpAddress() ?: "192.168.1.100"
        serverUrl = "http://$ip:8893"
        
        server.startServer { config ->
            configReceived = config
            // 先测试连接
            isTesting = true
            scope.launch {
                val result = testWebDavConnection(config)
                isTesting = false
                if (result.isSuccess) {
                    onConfigReceived(config)
                } else {
                    connectionError = result.errorMessage
                    showErrorDialog = true
                }
            }
        }
        isServerRunning = true
        
        // 延迟请求焦点到关闭按钮
        kotlinx.coroutines.delay(100)
        closeFocusRequester.requestFocus()
    }

    // 清理
    DisposableEffect(Unit) {
        onDispose {
            server.stopServer()
        }
    }

    // 删除遮罩层，只保留Card，居中显示，宽高比4:5
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
            modifier = Modifier
                .fillMaxWidth(0.25f)
                .aspectRatio(4f / 5f)
                .padding(12.dp)
        ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 标题栏
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "添加WebDAV网盘",
                    style = MaterialTheme.typography.headlineMedium,
                    color = TextPrimary
                )
                IconButton(
                    onClick = onDismiss,
                    colors = androidx.tv.material3.IconButtonDefaults.colors(
                        containerColor = Color.Transparent,
                        contentColor = TextMuted,
                        focusedContainerColor = PrimaryYellow,
                        focusedContentColor = BackgroundDark
                    ),
                    modifier = Modifier
                        .size(40.dp)
                        .focusRequester(closeFocusRequester)
                        .onPreviewKeyEvent { keyEvent ->
                            // 拦截所有方向键，防止光标移出窗口
                            if (keyEvent.key == Key.DirectionUp ||
                                keyEvent.key == Key.DirectionDown ||
                                keyEvent.key == Key.DirectionLeft ||
                                keyEvent.key == Key.DirectionRight) {
                                true
                            } else if (keyEvent.key == Key.Back || keyEvent.key == Key.Escape) {
                                // 按返回键关闭窗口
                                onDismiss()
                                true
                            } else {
                                false
                            }
                        }
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "关闭"
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (isTesting) {
                // 显示测试中
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(32.dp)
                ) {
                    Text(
                        text = "正在测试连接...",
                        style = MaterialTheme.typography.bodyLarge,
                        color = PrimaryYellow
                    )
                }
            } else if (configReceived != null) {
                // 配置已接收成功
                ConfigSuccessView(
                    config = configReceived!!,
                    onConfirm = onDismiss
                )
            } else {
                // 显示二维码
                QrCodeView(
                    serverUrl = serverUrl,
                    isRunning = isServerRunning
                )
            }
        }
    }
}
    
    // 错误对话框
    if (showErrorDialog) {
        connectionError?.let { error ->
            ErrorDialog(
                errorMessage = error,
                onDismiss = { 
                    showErrorDialog = false
                    configReceived = null
                    isTesting = false
                    // 延迟请求焦点回到关闭按钮
                    scope.launch {
                        kotlinx.coroutines.delay(100)
                        closeFocusRequester.requestFocus()
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun QrCodeView(
    serverUrl: String,
    isRunning: Boolean
) {
    val qrCodeBitmap = rememberQrCodeBitmap(serverUrl)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 二维码
        Box(
            modifier = Modifier
                .size(200.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(androidx.compose.ui.graphics.Color.White)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            qrCodeBitmap?.let { bitmap ->
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "配置二维码",
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 扫码提示
        Text(
            text = "扫码前往设置页面",
            style = MaterialTheme.typography.bodyLarge,
            color = PrimaryYellow,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        // 浏览器访问地址
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = "浏览器访问地址：",
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted
            )
            Text(
                text = serverUrl,
                style = MaterialTheme.typography.bodySmall,
                color = PrimaryYellow
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun ConfigSuccessView(
    config: WebDavConfigServer.WebDavConfig,
    onConfirm: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 成功图标
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(Color(0xFF10b981).copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = Color(0xFF10b981),
                modifier = Modifier.size(40.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "配置已接收",
            style = MaterialTheme.typography.headlineSmall,
            color = TextPrimary
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "WebDAV服务器信息已保存",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )

        Spacer(modifier = Modifier.height(24.dp))

        // 配置信息卡片
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(GlassBackground)
                .padding(20.dp)
        ) {
            Column {
                ConfigInfoRow("协议", config.protocol.uppercase())
                ConfigInfoRow("服务器", config.host)
                ConfigInfoRow("端口", config.port.toString())
                ConfigInfoRow("路径", config.path)
                if (config.username.isNotEmpty()) {
                    ConfigInfoRow("用户名", config.username)
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onConfirm,
            colors = ButtonDefaults.colors(
                containerColor = PrimaryYellow,
                contentColor = BackgroundDark
            ),
            shape = ButtonDefaults.shape(shape = RoundedCornerShape(12.dp))
        ) {
            Text(
                text = "完成",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(horizontal = 32.dp, vertical = 8.dp)
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun ConfigInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = TextMuted
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = TextPrimary
        )
    }
}

@Composable
private fun rememberQrCodeBitmap(content: String): Bitmap? {
    return remember(content) {
        try {
            val writer = QRCodeWriter()
            val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, 512, 512)
            val width = bitMatrix.width
            val height = bitMatrix.height
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
            
            for (x in 0 until width) {
                for (y in 0 until height) {
                    val pixelColor = if (bitMatrix.get(x, y)) android.graphics.Color.BLACK else android.graphics.Color.WHITE
                    bitmap.setPixel(x, y, pixelColor)
                }
            }
            bitmap
        } catch (e: Exception) {
            null
        }
    }
}

private fun getLocalIpAddress(): String? {
    return try {
        val interfaces = NetworkInterface.getNetworkInterfaces()
        for (networkInterface in Collections.list(interfaces)) {
            val addresses = networkInterface.inetAddresses
            for (address in Collections.list(addresses)) {
                if (!address.isLoopbackAddress && address.hostAddress?.contains(":") == false) {
                    return address.hostAddress
                }
            }
        }
        null
    } catch (e: Exception) {
        null
    }
}

data class ConnectionTestResult(
    val isSuccess: Boolean,
    val errorMessage: String?
)

private suspend fun testWebDavConnection(config: WebDavConfigServer.WebDavConfig): ConnectionTestResult = withContext(Dispatchers.IO) {
    // 先用 GET 测试基本连接，因为有些服务器不支持 PROPFIND
    val getResult = testWithGetMethod(config)
    
    // 如果 GET 测试成功，尝试用 PROPFIND 再测一次（更准确的 WebDAV 认证检测）
    if (getResult.isSuccess) {
        return@withContext tryPropfindMethod(config) ?: getResult
    }
    
    return@withContext getResult
}

// 尝试使用 PROPFIND 方法，如果失败则返回 null（使用 GET 结果）
private suspend fun tryPropfindMethod(config: WebDavConfigServer.WebDavConfig): ConnectionTestResult? = withContext(Dispatchers.IO) {
    try {
        // 连接测试只需测根路径，不用具体存储路径（避免子目录 404）
        val testPath = "/"

        val url = java.net.URL("${config.protocol}://${config.host}:${config.port}$testPath")
        val connection = url.openConnection() as java.net.HttpURLConnection
        connection.requestMethod = "PROPFIND"
        connection.setRequestProperty("Depth", "0")
        connection.connectTimeout = 10000
        connection.readTimeout = 10000
        connection.instanceFollowRedirects = false

        if (config.username.isNotEmpty() && config.password.isNotEmpty()) {
            val auth = "${config.username}:${config.password}"
            val encodedAuth = android.util.Base64.encodeToString(auth.toByteArray(), android.util.Base64.NO_WRAP)
            connection.setRequestProperty("Authorization", "Basic $encodedAuth")
        }

        val responseCode = connection.responseCode
        android.util.Log.d("WebDAVTest", "PROPFIND Response: $responseCode")
        connection.disconnect()

        when {
            responseCode in 200..299 || responseCode == 207 -> {
                ConnectionTestResult(isSuccess = true, errorMessage = null)
            }
            responseCode == 401 -> {
                if (config.username.isEmpty() || config.password.isEmpty()) {
                    ConnectionTestResult(
                        isSuccess = false,
                        errorMessage = "服务器需要身份验证，请输入用户名和密码"
                    )
                } else {
                    ConnectionTestResult(
                        isSuccess = false,
                        errorMessage = "身份验证失败：用户名或密码错误"
                    )
                }
            }
            responseCode == 403 -> {
                ConnectionTestResult(
                    isSuccess = false,
                    errorMessage = "访问被禁止：该用户没有访问此路径的权限"
                )
            }
            responseCode == 405 -> {
                // PROPFIND 也不支持，返回 null 继续尝试 OPTIONS
                null
            }
            else -> null // 其他情况使用 GET 结果
        }
    } catch (e: Exception) {
        android.util.Log.d("WebDAVTest", "PROPFIND not supported: ${e.message}")
        null // 不支持 PROPFIND，使用 GET 结果
    }
}

// 尝试使用 OPTIONS 方法，这是 WebDAV 标准方法
private suspend fun tryOptionsMethod(config: WebDavConfigServer.WebDavConfig): ConnectionTestResult = withContext(Dispatchers.IO) {
    try {
        // 连接测试只需测根路径，不用具体存储路径（避免子目录 404）
        val testPath = "/"

        val url = java.net.URL("${config.protocol}://${config.host}:${config.port}$testPath")
        val connection = url.openConnection() as java.net.HttpURLConnection
        connection.requestMethod = "OPTIONS"
        connection.connectTimeout = 10000
        connection.readTimeout = 10000
        connection.instanceFollowRedirects = false

        if (config.username.isNotEmpty() && config.password.isNotEmpty()) {
            val auth = "${config.username}:${config.password}"
            val encodedAuth = android.util.Base64.encodeToString(auth.toByteArray(), android.util.Base64.NO_WRAP)
            connection.setRequestProperty("Authorization", "Basic $encodedAuth")
        }

        val responseCode = connection.responseCode
        val allowHeader = connection.getHeaderField("Allow") ?: ""
        android.util.Log.d("WebDAVTest", "OPTIONS Response: $responseCode, Allow: $allowHeader")
        connection.disconnect()

        when {
            responseCode in 200..299 -> {
                // OPTIONS 成功，WebDAV 服务器通常会返回 Allow 头
                ConnectionTestResult(isSuccess = true, errorMessage = null)
            }
            responseCode == 401 -> {
                if (config.username.isEmpty() || config.password.isEmpty()) {
                    ConnectionTestResult(
                        isSuccess = false,
                        errorMessage = "服务器需要身份验证，请输入用户名和密码"
                    )
                } else {
                    ConnectionTestResult(
                        isSuccess = false,
                        errorMessage = "身份验证失败：用户名或密码错误"
                    )
                }
            }
            responseCode == 403 -> {
                ConnectionTestResult(
                    isSuccess = false,
                    errorMessage = "访问被禁止：该用户没有访问此路径的权限"
                )
            }
            else -> {
                ConnectionTestResult(
                    isSuccess = false,
                    errorMessage = "连接失败：HTTP 状态码 $responseCode"
                )
            }
        }
    } catch (e: Exception) {
        ConnectionTestResult(
            isSuccess = false,
            errorMessage = "连接失败：${e.message ?: e.javaClass.simpleName}"
        )
    }
}

// 备用方法：如果服务器不支持 PROPFIND，使用 GET 测试
private suspend fun testWithGetMethod(config: WebDavConfigServer.WebDavConfig): ConnectionTestResult = withContext(Dispatchers.IO) {
    try {
        // 连接测试只需测根路径，不用具体存储路径（避免子目录 404）
        val testPath = "/"

        val url = java.net.URL("${config.protocol}://${config.host}:${config.port}$testPath")
        val connection = url.openConnection() as java.net.HttpURLConnection
        connection.requestMethod = "GET"
        connection.connectTimeout = 10000
        connection.readTimeout = 10000
        connection.instanceFollowRedirects = false

        if (config.username.isNotEmpty() && config.password.isNotEmpty()) {
            val auth = "${config.username}:${config.password}"
            val encodedAuth = android.util.Base64.encodeToString(auth.toByteArray(), android.util.Base64.NO_WRAP)
            connection.setRequestProperty("Authorization", "Basic $encodedAuth")
        }

        val responseCode = connection.responseCode
        val responseBody = try {
            connection.inputStream.bufferedReader().use { it.readText().take(500) }
        } catch (e: Exception) {
            ""
        }
        
        android.util.Log.d("WebDAVTest", "GET Response: $responseCode")
        android.util.Log.d("WebDAVTest", "Response body: ${responseBody.take(200)}")
        connection.disconnect()
        
        // 检测是否是 AList/OpenList 服务器（通过特定的 HTML 内容或 API 端点）
        val isAList = responseBody.contains("AList", ignoreCase = true) || 
                      responseBody.contains("OpenList", ignoreCase = true) ||
                      responseBody.contains("/api/auth/login", ignoreCase = true)
        
        if (isAList) {
            android.util.Log.d("WebDAVTest", "Detected AList server, using API test")
            return@withContext testAListConnection(config)
        }

        when {
            responseCode in 200..299 -> {
                ConnectionTestResult(isSuccess = true, errorMessage = null)
            }
            responseCode == 405 -> {
                // 405 Method Not Allowed - GET 不被允许，可能是 WebDAV 服务器只允许特定方法
                // 尝试 PROPFIND 或 OPTIONS
                android.util.Log.d("WebDAVTest", "GET returned 405, trying PROPFIND")
                val propfindResult = tryPropfindMethod(config)
                if (propfindResult != null) {
                    return@withContext propfindResult
                }
                // 如果 PROPFIND 也失败，尝试 OPTIONS
                return@withContext tryOptionsMethod(config)
            }
            responseCode == 401 -> {
                if (config.username.isEmpty() || config.password.isEmpty()) {
                    ConnectionTestResult(
                        isSuccess = false,
                        errorMessage = "服务器需要身份验证，请输入用户名和密码"
                    )
                } else {
                    ConnectionTestResult(
                        isSuccess = false,
                        errorMessage = "身份验证失败：用户名或密码错误"
                    )
                }
            }
            responseCode == 403 -> {
                ConnectionTestResult(
                    isSuccess = false,
                    errorMessage = "访问被禁止：该用户没有访问此路径的权限"
                )
            }
            else -> {
                ConnectionTestResult(
                    isSuccess = false,
                    errorMessage = "连接失败：HTTP 状态码 $responseCode"
                )
            }
        }
    } catch (e: java.net.SocketTimeoutException) {
        ConnectionTestResult(
            isSuccess = false,
            errorMessage = "连接超时：无法连接到服务器，请检查网络或服务器地址"
        )
    } catch (e: java.net.UnknownHostException) {
        ConnectionTestResult(
            isSuccess = false,
            errorMessage = "地址错误：无法解析主机名 '${config.host}'"
        )
    } catch (e: java.net.ConnectException) {
        ConnectionTestResult(
            isSuccess = false,
            errorMessage = "连接拒绝：端口 ${config.port} 不可达，请检查端口号是否正确"
        )
    } catch (e: Exception) {
        ConnectionTestResult(
            isSuccess = false,
            errorMessage = "连接失败：${e.message ?: e.javaClass.simpleName}"
        )
    }
}

// AList 服务器连接测试
private suspend fun testAListConnection(config: WebDavConfigServer.WebDavConfig): ConnectionTestResult = withContext(Dispatchers.IO) {
    try {
        // 尝试登录 AList
        val loginUrl = "${config.protocol}://${config.host}:${config.port}/api/auth/login"
        android.util.Log.d("WebDAVTest", "Testing AList login: $loginUrl")
        
        val loginJson = org.json.JSONObject().apply {
            put("username", config.username)
            put("password", config.password)
        }.toString()
        
        val loginConnection = java.net.URL(loginUrl).openConnection() as java.net.HttpURLConnection
        loginConnection.requestMethod = "POST"
        loginConnection.setRequestProperty("Content-Type", "application/json")
        loginConnection.connectTimeout = 10000
        loginConnection.readTimeout = 10000
        loginConnection.doOutput = true
        
        loginConnection.outputStream.use { os ->
            os.write(loginJson.toByteArray())
        }
        
        val loginCode = loginConnection.responseCode
        val loginResponse = try {
            loginConnection.inputStream.bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            loginConnection.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
        }
        
        android.util.Log.d("WebDAVTest", "AList login response: $loginCode")
        android.util.Log.d("WebDAVTest", "AList response body: ${loginResponse.take(200)}")
        loginConnection.disconnect()
        
        // 解析响应
        val jsonResponse = try {
            org.json.JSONObject(loginResponse)
        } catch (e: Exception) {
            return@withContext ConnectionTestResult(
                isSuccess = false,
                errorMessage = "AList 服务器响应格式错误"
            )
        }
        
        val code = jsonResponse.optInt("code", -1)
        val message = jsonResponse.optString("message", "")
        
        android.util.Log.d("WebDAVTest", "AList response code: $code, message: $message")
        
        when (code) {
            200 -> {
                // 登录成功
                ConnectionTestResult(isSuccess = true, errorMessage = null)
            }
            400 -> {
                // AList 典型错误码：密码错误、用户名错误等
                val errorMsg = when {
                    message.contains("password", ignoreCase = true) || 
                    message.contains("密码") -> "身份验证失败：密码错误"
                    message.contains("username", ignoreCase = true) || 
                    message.contains("用户") -> "身份验证失败：用户名不存在"
                    message.contains("not found", ignoreCase = true) -> "身份验证失败：用户不存在"
                    else -> "身份验证失败：用户名或密码错误"
                }
                ConnectionTestResult(
                    isSuccess = false,
                    errorMessage = errorMsg
                )
            }
            401, 403 -> {
                ConnectionTestResult(
                    isSuccess = false,
                    errorMessage = "身份验证失败：用户名或密码错误"
                )
            }
            404 -> {
                ConnectionTestResult(
                    isSuccess = false,
                    errorMessage = "AList 服务器错误：登录接口不存在，请检查 AList 版本"
                )
            }
            500 -> {
                ConnectionTestResult(
                    isSuccess = false,
                    errorMessage = "AList 服务器内部错误，请检查服务器配置"
                )
            }
            else -> {
                // 其他错误，尝试翻译常见错误
                val errorMsg = when {
                    message.contains("password", ignoreCase = true) -> "密码错误"
                    message.contains("username", ignoreCase = true) -> "用户名错误"
                    message.contains("not found", ignoreCase = true) -> "用户不存在"
                    message.contains("incorrect", ignoreCase = true) -> "信息不正确"
                    message.contains("invalid", ignoreCase = true) -> "无效的凭证"
                    message.isNotEmpty() -> message
                    else -> "认证失败"
                }
                ConnectionTestResult(
                    isSuccess = false,
                    errorMessage = "AList 认证失败：$errorMsg"
                )
            }
        }
    } catch (e: java.net.SocketTimeoutException) {
        ConnectionTestResult(
            isSuccess = false,
            errorMessage = "连接超时：无法连接到 AList 服务器"
        )
    } catch (e: java.net.UnknownHostException) {
        ConnectionTestResult(
            isSuccess = false,
            errorMessage = "地址错误：无法解析主机名 '${config.host}'"
        )
    } catch (e: java.net.ConnectException) {
        ConnectionTestResult(
            isSuccess = false,
            errorMessage = "连接拒绝：端口 ${config.port} 不可达"
        )
    } catch (e: Exception) {
        ConnectionTestResult(
            isSuccess = false,
            errorMessage = "AList 连接失败：${e.message ?: e.javaClass.simpleName}"
        )
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun ErrorDialog(
    errorMessage: String,
    onDismiss: () -> Unit
) {
    val closeFocusRequester = remember { FocusRequester() }
    
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(100)
        closeFocusRequester.requestFocus()
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark.copy(alpha = 0.9f))
            .onPreviewKeyEvent { keyEvent ->
                // 拦截所有方向键，锁定焦点在当前窗口
                if (keyEvent.key in listOf(
                    Key.DirectionUp, Key.DirectionDown, 
                    Key.DirectionLeft, Key.DirectionRight
                )) {
                    // 方向键直接拦截，不传递
                    true
                } else if (keyEvent.key == Key.Back && keyEvent.type == KeyEventType.KeyUp) {
                    // 返回键关闭对话框
                    onDismiss()
                    true
                } else {
                    false
                }
            }
            .clickable(
                onClick = { },
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ),
        contentAlignment = Alignment.Center
    ) {
        Card(
            onClick = {},
            colors = CardDefaults.colors(
                containerColor = SurfaceDark,
                focusedContainerColor = SurfaceDark
            ),
            modifier = Modifier
                .widthIn(min = 280.dp, max = 360.dp)
                .padding(24.dp)
                .focusProperties {
                    canFocus = true
                }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 错误图标
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFef4444).copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = null,
                        tint = Color(0xFFef4444),
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "连接失败",
                    style = MaterialTheme.typography.headlineSmall,
                    color = TextPrimary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = errorMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(24.dp))

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
                    modifier = Modifier
                        .focusRequester(closeFocusRequester)
                ) {
                    Text(
                        text = "我知道了",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.Black,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

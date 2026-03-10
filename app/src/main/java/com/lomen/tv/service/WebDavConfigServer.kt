package com.lomen.tv.service

import android.content.Context
import android.util.Log
import fi.iki.elonen.NanoHTTPD
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.IOException

class WebDavConfigServer(
    private val context: Context,
    port: Int = 8893
) : NanoHTTPD(port) {

    companion object {
        private const val TAG = "WebDavConfigServer"
    }

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning

    private var onConfigReceived: ((WebDavConfig) -> Unit)? = null

    data class WebDavConfig(
        val protocol: String = "http",
        val host: String = "",
        val port: Int = 8080,
        val path: String = "/",
        val username: String = "",
        val password: String = ""
    )

    fun startServer(onConfig: (WebDavConfig) -> Unit) {
        onConfigReceived = onConfig
        try {
            start()
            _isRunning.value = true
            Log.d(TAG, "Server started on port $listeningPort")
        } catch (e: IOException) {
            Log.e(TAG, "Failed to start server", e)
        }
    }

    fun stopServer() {
        stop()
        _isRunning.value = false
        onConfigReceived = null
        Log.d(TAG, "Server stopped")
    }

    override fun serve(session: IHTTPSession): Response {
        val uri = session.uri
        val method = session.method

        return when {
            uri == "/" && method == Method.GET -> serveMainPage()
            uri == "/api/config" && method == Method.POST -> handleConfigPost(session)
            uri == "/api/status" && method == Method.GET -> serveStatus()
            else -> newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "Not Found")
        }
    }

    private fun serveMainPage(): Response {
        val html = """
            <!DOCTYPE html>
            <html lang="zh-CN">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>柠檬TV - WebDAV配置</title>
                <style>
                    * {
                        margin: 0;
                        padding: 0;
                        box-sizing: border-box;
                    }
                    body {
                        font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
                        background: linear-gradient(135deg, #0a0a0c 0%, #1a1d24 100%);
                        min-height: 100vh;
                        display: flex;
                        justify-content: center;
                        align-items: center;
                        padding: 20px;
                        color: #e2e8f0;
                    }
                    .container {
                        background: rgba(26, 29, 36, 0.95);
                        border-radius: 24px;
                        padding: 40px;
                        max-width: 480px;
                        width: 100%;
                        box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.5);
                        border: 1px solid rgba(255, 255, 255, 0.1);
                    }
                    .header {
                        text-align: center;
                        margin-bottom: 32px;
                    }
                    .logo {
                        width: 64px;
                        height: 64px;
                        background: #fddd0e;
                        border-radius: 16px;
                        display: flex;
                        align-items: center;
                        justify-content: center;
                        margin: 0 auto 16px;
                        font-size: 32px;
                    }
                    h1 {
                        font-size: 24px;
                        font-weight: 700;
                        color: #ffffff;
                        margin-bottom: 8px;
                    }
                    .subtitle {
                        color: #94a3b8;
                        font-size: 14px;
                    }
                    .form-group {
                        margin-bottom: 20px;
                    }
                    label {
                        display: block;
                        margin-bottom: 8px;
                        font-size: 14px;
                        font-weight: 500;
                        color: #cbd5e1;
                    }
                    .protocol-group {
                        display: flex;
                        gap: 16px;
                        margin-bottom: 20px;
                    }
                    .radio-label {
                        display: flex;
                        align-items: center;
                        cursor: pointer;
                        font-size: 14px;
                        color: #e2e8f0;
                    }
                    .radio-label input {
                        margin-right: 8px;
                        width: 18px;
                        height: 18px;
                        accent-color: #fddd0e;
                    }
                    input[type="text"],
                    input[type="number"],
                    input[type="password"] {
                        width: 100%;
                        padding: 14px 16px;
                        background: rgba(15, 17, 21, 0.8);
                        border: 1px solid rgba(255, 255, 255, 0.1);
                        border-radius: 12px;
                        color: #ffffff;
                        font-size: 15px;
                        transition: all 0.2s;
                    }
                    input[type="text"]:focus,
                    input[type="number"]:focus,
                    input[type="password"]:focus {
                        outline: none;
                        border-color: #fddd0e;
                        box-shadow: 0 0 0 3px rgba(253, 221, 14, 0.1);
                    }
                    input::placeholder {
                        color: #64748b;
                    }
                    .row {
                        display: grid;
                        grid-template-columns: 2fr 1fr;
                        gap: 12px;
                    }
                    .btn-submit {
                        width: 100%;
                        padding: 16px;
                        background: #fddd0e;
                        color: #0a0a0c;
                        border: none;
                        border-radius: 12px;
                        font-size: 16px;
                        font-weight: 600;
                        cursor: pointer;
                        transition: all 0.2s;
                        margin-top: 8px;
                    }
                    .btn-submit:hover {
                        background: #fde047;
                        transform: translateY(-1px);
                    }
                    .btn-submit:active {
                        transform: translateY(0);
                    }
                    .status {
                        text-align: center;
                        padding: 12px;
                        border-radius: 8px;
                        margin-top: 16px;
                        font-size: 14px;
                        display: none;
                    }
                    .status.success {
                        background: rgba(16, 185, 129, 0.2);
                        color: #34d399;
                        display: block;
                    }
                    .status.error {
                        background: rgba(239, 68, 68, 0.2);
                        color: #f87171;
                        display: block;
                    }
                    .info-box {
                        background: rgba(253, 221, 14, 0.1);
                        border: 1px solid rgba(253, 221, 14, 0.3);
                        border-radius: 12px;
                        padding: 16px;
                        margin-bottom: 24px;
                    }
                    .info-box p {
                        font-size: 13px;
                        color: #fddd0e;
                        line-height: 1.5;
                    }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <div class="logo">▶</div>
                        <h1>WebDAV 网盘配置</h1>
                        <p class="subtitle">配置您的本地WebDAV服务器连接信息</p>
                    </div>
                    
                    <div class="info-box">
                        <p>💡 提示：配置完成后，柠檬TV将连接到您的WebDAV服务器获取媒体资源。</p>
                    </div>
                    
                    <form id="configForm">
                        <div class="form-group">
                            <label>协议</label>
                            <div class="protocol-group">
                                <label class="radio-label">
                                    <input type="radio" name="protocol" value="http" checked>
                                    HTTP
                                </label>
                                <label class="radio-label">
                                    <input type="radio" name="protocol" value="https">
                                    HTTPS
                                </label>
                            </div>
                        </div>
                        
                        <div class="row">
                            <div class="form-group">
                                <label>IP / 域名</label>
                                <input type="text" name="host" placeholder="192.168.1.100" required>
                            </div>
                            <div class="form-group">
                                <label>端口</label>
                                <input type="number" name="port" placeholder="8080" value="8080" required>
                            </div>
                        </div>
                        
                        <div class="form-group">
                            <label>存储路径</label>
                            <input type="text" name="path" placeholder="/" value="/">
                        </div>
                        
                        <div class="form-group">
                            <label>用户名（可选）</label>
                            <input type="text" name="username" placeholder="请输入用户名">
                        </div>
                        
                        <div class="form-group">
                            <label>密码（可选）</label>
                            <input type="password" name="password" placeholder="请输入密码">
                        </div>
                        
                        <button type="submit" class="btn-submit">保存配置</button>
                    </form>
                    
                    <div id="status" class="status"></div>
                </div>
                
                <script>
                    document.getElementById('configForm').addEventListener('submit', async function(e) {
                        e.preventDefault();
                        
                        const formData = new FormData(this);
                        const data = {
                            protocol: formData.get('protocol'),
                            host: formData.get('host'),
                            port: parseInt(formData.get('port')),
                            path: formData.get('path'),
                            username: formData.get('username'),
                            password: formData.get('password')
                        };
                        
                        try {
                            const response = await fetch('/api/config', {
                                method: 'POST',
                                headers: {
                                    'Content-Type': 'application/json'
                                },
                                body: JSON.stringify(data)
                            });
                            
                            const result = await response.json();
                            const statusDiv = document.getElementById('status');
                            
                            if (response.ok) {
                                statusDiv.className = 'status success';
                                statusDiv.textContent = '✓ 配置保存成功！请返回TV查看。';
                            } else {
                                statusDiv.className = 'status error';
                                statusDiv.textContent = '✗ 保存失败：' + (result.error || '未知错误');
                            }
                        } catch (error) {
                            const statusDiv = document.getElementById('status');
                            statusDiv.className = 'status error';
                            statusDiv.textContent = '✗ 网络错误：' + error.message;
                        }
                    });
                </script>
            </body>
            </html>
        """.trimIndent()

        return newFixedLengthResponse(Response.Status.OK, "text/html", html)
    }

    private fun handleConfigPost(session: IHTTPSession): Response {
        return try {
            val contentLength = session.headers["content-length"]?.toInt() ?: 0
            val buffer = ByteArray(contentLength)
            session.inputStream.read(buffer, 0, contentLength)
            val body = String(buffer)

            // 解析JSON
            val config = parseConfigJson(body)
            onConfigReceived?.invoke(config)

            val response = """{"success": true, "message": "配置已保存"}"""
            newFixedLengthResponse(Response.Status.OK, "application/json", response)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse config", e)
            val response = """{"success": false, "error": "${e.message}"}"""
            newFixedLengthResponse(Response.Status.BAD_REQUEST, "application/json", response)
        }
    }

    private fun serveStatus(): Response {
        val status = """{"running": true, "port": $listeningPort}"""
        return newFixedLengthResponse(Response.Status.OK, "application/json", status)
    }

    private fun parseConfigJson(json: String): WebDavConfig {
        // 简单JSON解析
        val map = mutableMapOf<String, String>()
        val regex = """"(\w+)":\s*"?([^",\}]*)"?""".toRegex()
        regex.findAll(json).forEach { match ->
            map[match.groupValues[1]] = match.groupValues[2]
        }

        return WebDavConfig(
            protocol = map["protocol"] ?: "http",
            host = map["host"] ?: "",
            port = map["port"]?.toIntOrNull() ?: 8080,
            path = map["path"] ?: "/",
            username = map["username"] ?: "",
            password = map["password"] ?: ""
        )
    }
}

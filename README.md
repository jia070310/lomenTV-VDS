# LomenTV VDS

一款专为 Android TV 设计的视频播放器，支持 WebDAV 网盘资源导入、智能刮削、记忆续播、IPTV 直播等功能。
![image](https://github.com/jia070310/lomenTV-VDS/blob/main/23.png)

## 最新版本 v1.0.7

### 新增功能
- **IPTV 直播**：支持 M3U 直播源，内置多个直播源
- **跳过片头片尾设置**：播放时可设置片头片尾时间，自动跳过
- **自动跳过开关**：设置中心可开启/关闭自动跳过片头片尾功能
- **记忆续播开关**：设置中心可开启/关闭记忆播放进度功能
- **自动下一集**：电视剧播放完自动播放下一集

### 优化改进
- **EPG 节目表**：更新默认 EPG 地址，支持节目预告
- **记忆播放修复**：修复从记忆位置播放时总时长显示错误的问题
- **跳过功能优化**：未设置跳过时间的剧集默认从 0 开始
- **UI 优化**：设置界面开关样式统一，操作更便捷

## 功能特性
##新增直播功能

### 媒体库管理
- **WebDAV 网盘支持**：连接 OpenList/AList 等 WebDAV 服务
- **智能刮削**：优先使用本地 .nfo 文件，其次 TMDB 网络刮削
- **多类型媒体**：支持电影、电视剧、纪录片、演唱会
- **资源库管理**：支持多个 WebDAV 资源库切换

### 播放功能
- **记忆续播**：自动记录播放进度，下次从断点继续（可开关控制）
- **智能跳过**：支持跳过片头片尾，按剧集系列统一配置
- **自动下一集**：电视剧自动连播，最后一集提示结束
- **音轨/字幕选择**：播放时切换音轨和字幕
- **播放控制**：支持快进/快退、暂停、音量调节

### IPTV 直播
- **直播源管理**：支持自定义 M3U 直播源
- **内置直播源**：提供多个内置直播源选择
- **EPG 节目表**：支持节目预告显示
- **频道切换**：数字键快速换台，上下键切换频道

### TV 端优化
- **TV 专属 UI**：针对遥控器操作优化
- **焦点导航**：方向键控制焦点移动
- **沉浸式体验**：全屏播放，自动隐藏状态栏

### 其他功能
- **TMDB API 配置**：支持自定义 TMDB API Key
- **最近观看**：显示最近观看记录
- **搜索功能**：支持媒体搜索
- **通知栏**：首页显示滚动通知，支持远程配置

## 系统要求

- Android 8.0 (API 26) 及以上
- Android TV 或电视盒子

## 技术栈

- **语言**：Kotlin
- **UI 框架**：Jetpack Compose TV (TvMaterial3)
- **架构**：MVVM + Hilt 依赖注入
- **数据库**：Room
- **网络**：Retrofit + OkHttp
- **图片加载**：Coil

## 构建说明

```bash
# 克隆项目
git clone https://github.com/jia070310/lomenTV-VDS.git

# 进入项目目录
cd lomenTV-VDS

# 构建 Debug 版本
./gradlew assembleDebug

# 构建 Release 版本
./gradlew assembleRelease
```

## 使用说明

### 首次使用
1. 启动应用后，配置 TMDB API Key（可选，用于媒体刮削）
2. 在设置页添加 WebDAV 网盘
3. 等待媒体刮削完成
4. 开始观看

### 添加 WebDAV 网盘
1. 进入设置页
2. 选择"添加 WebDAV 网盘"
3. 填写服务器地址、端口、路径、用户名、密码
4. 保存并测试连接

### TMDB API 配置
1. 访问 [TMDB 官网](https://www.themoviedb.org/settings/api) 申请 API Key
2. 在设置页选择"TMDB API 设置"
3. 扫描二维码或手动输入 API Key

### 通知配置
应用支持从远程服务器获取通知并在首页显示。

**通知文件位置**：`notifications.json`

**访问方式**：应用通过 `https://gh-proxy.org/` 加速器访问 GitHub 上的通知文件

**配置格式**：
```json
{
  "notifications": [
    {
      "id": "welcome",
      "title": "欢迎使用",
      "content": "通知内容",
      "type": "info",
      "enabled": true
    }
  ]
}
```

**更新方式**：
1. 修改 `notifications.json` 文件
2. 提交到 GitHub 仓库
3. 应用自动获取最新通知（每次打开首页时刷新）

## 项目结构

```
app/src/main/java/com/lomen/tv/
├── data/               # 数据层
│   ├── local/          # 本地数据库
│   ├── remote/         # 远程 API
│   ├── repository/     # 仓库
│   ├── scraper/        # 媒体刮削
│   └── webdav/         # WebDAV 客户端
├── domain/             # 领域层
│   ├── model/          # 数据模型
│   └── service/        # 业务服务
├── ui/                 # UI 层
│   ├── screens/        # 页面
│   ├── components/     # 组件
│   ├── theme/          # 主题
│   └── viewmodel/      # ViewModel
└── utils/              # 工具类
```

## 开源协议

[MIT License](LICENSE)

## 致谢

- [TMDB](https://www.themoviedb.org/) - 提供媒体元数据 API
- [Jetpack Compose TV](https://developer.android.com/jetpack/androidx/releases/tv) - TV 端 UI 框架

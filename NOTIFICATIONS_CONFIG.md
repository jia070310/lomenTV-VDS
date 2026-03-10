# LomenTV VDS 通知配置

## 配置说明

此文件用于配置应用首页显示的通知栏信息。

## 通知格式

通知以 JSON 格式存储在 `notifications.json` 文件中，结构如下：

```json
{
  "notifications": [
    {
      "id": "1",
      "title": "通知标题",
      "content": "通知内容",
      "type": "info",
      "startTime": "2024-01-01T00:00:00",
      "endTime": "2024-12-31T23:59:59",
      "enabled": true
    }
  ]
}
```

## 字段说明

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| id | string | 是 | 通知唯一标识 |
| title | string | 否 | 通知标题 |
| content | string | 是 | 通知内容 |
| type | string | 否 | 通知类型：info/warning/error |
| startTime | string | 否 | 开始时间（ISO 8601格式） |
| endTime | string | 否 | 结束时间（ISO 8601格式） |
| enabled | boolean | 是 | 是否启用 |

## 更新方式

1. 修改 `notifications.json` 文件
2. 提交到 GitHub 仓库
3. 应用会自动获取最新通知（每次打开首页时刷新）

## 加速器配置

应用使用 `https://gh-proxy.org/` 作为 GitHub 加速器访问通知文件。

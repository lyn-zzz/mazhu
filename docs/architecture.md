# Architecture

码住采用本地优先架构。Android App 负责低摩擦收藏；Supabase 提供托管同步层；CLI 和 Codex Skill 负责知识库能力。

## Components

```text
WeChat share menu
        |
        v
Android App
  - Room local database
  - WorkManager parsing and sync
  - Supabase Auth
        |
        v
Supabase sync
  - collections
  - bookmarks
  - bookmark_collections
  - RLS by auth.uid()
        |
        v
CLI
  - search
  - read
  - summarize
        |
        v
Codex Skill
  - query saved articles
  - answer with citations
```

## Android

Android App 接收微信分享内容后，会立即写入 Room 数据库。这样即使网络不可用、Supabase 无法访问或解析失败，用户的收藏动作也不会丢失。

后台任务分两类：

- 解析任务：拉取微信公众号文章，提取标题、公众号、封面图和正文。
- 同步任务：把本地收藏夹和文章同步到 Supabase。

如果同步失败，数据保留在本地，后续可自动或手动重试。

## Supabase

Supabase 提供认证、Postgres API 和 RLS。普通用户使用官方 APK 时不需要自己创建 Supabase 项目；开发者自建版本可以按迁移文件部署自己的 Supabase 后端。核心表：

- `collections`：收藏夹。
- `bookmarks`：文章收藏。
- `bookmark_collections`：文章和收藏夹的多对多归属关系。

每条记录都有 `user_id`，RLS 策略限制用户只能访问自己的数据。

`bookmarks.collection_id` 保留为兼容字段；当前收藏夹归属以 `bookmark_collections` 为准。收藏夹拖动排序属于 Android 本地视觉状态，不影响桌面端 Skill 的检索和问答。

## CLI

CLI 是桌面端知识库入口。它不需要自建服务器，直接访问 Supabase API。未启用云同步时，CLI 无法读取手机本地数据库；因此 AI 问答能力依赖同步层。

主要命令：

- `login`：保存 Supabase 登录会话。
- `collections --json`：列出收藏夹和文章数量。
- `search --json`：按标题、公众号、收藏夹、摘要和 topics 检索，并返回原文链接、封面、摘要状态和多收藏夹归属。
- `read --json`：读取单篇文章全文，并返回摘要、topics、要点和引用信息。
- `summarize`：调用 OpenAI-compatible 模型生成摘要。

摘要字段保存在 Supabase，避免每次问答都重复读取全文和调用模型。

## Codex Skill

Codex Skill 是面向 AI agent 的操作说明。它指导 Codex 使用本地 CLI：

1. 先用 JSON 搜索相关文章和摘要。
2. 摘要足够时直接回答。
3. 摘要不够时读取少量全文。
4. 回答时引用文章标题和原文链接。

这种方式比 embedding 检索更轻量，适合个人收藏夹规模和早期版本。

## Why No Server

当前没有独立后端服务。原因是：

- Android App 可以直接通过 Supabase Auth 和 RLS 安全访问数据。
- CLI 也可以直接访问 Supabase。
- AI 摘要生成适合放在本地 CLI 中，用户自行配置模型服务和 API Key。

如果未来需要团队共享、统一模型调用、任务队列或网页端，可以再引入后端服务。

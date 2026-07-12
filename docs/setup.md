# Setup

本文档说明如何从零运行码住。项目由 Android App、可选 Supabase 同步、CLI 和 Codex Skill 四部分组成。

## Prerequisites

- Android Studio
- Android 手机，已开启 USB 调试
- Node.js 26 或更高版本
- 可选：Supabase 项目，用于云同步和桌面端 AI 问答
- 可选：Codex，用于安装 `mazhu-knowledge` Skill

## 1. Android App

如果只想把码住作为手机本地收藏夹使用，不需要先配置 Supabase。安装 App 后即可从微信分享菜单保存文章。

开发运行时，复制配置模板：

```bash
cp android/local.properties.example android/local.properties
```

只填写 Android SDK 路径也可以构建本地-only 版本：

```properties
sdk.dir=/path/to/Android/sdk
```

如需在 APK 中内置默认 Supabase 配置，可继续填写：

```properties
SUPABASE_URL=https://your-project-ref.supabase.co
SUPABASE_PUBLISHABLE_KEY=sb_publishable_xxx
```

也可以不在构建时填写 Supabase 配置，后续在 App 的云同步设置中输入。

`android/local.properties` 包含本机路径和项目配置，不应提交到 Git。

用 Android Studio 打开 `android/` 目录，连接 Android 手机后运行应用。

保存公众号文章的路径：

1. 在微信中打开公众号文章。
2. 点击右上角分享。
3. 选择“码住”。
4. 选择收藏夹，或直接保存到“默认收藏夹”。

未启用云同步时，文章会显示为“仅本地”。启用云同步并登录后，本地收藏会进入同步队列。

## 2. Supabase Sync

创建 Supabase 项目后，在 SQL Editor 中按顺序执行：

```text
supabase/migrations/0001_create_core_schema.sql
supabase/migrations/0002_add_summary_fields.sql
supabase/migrations/0003_add_bookmark_collections.sql
```

数据库使用 Supabase Auth 的用户 ID 做隔离，并启用 RLS。每个登录用户只能读写自己的收藏夹和文章。

在 App 中打开云同步设置，填写 Supabase URL 和 publishable key，然后用邮箱密码注册或登录。网络不可用时，文章会先保存在本地；网络恢复后可以手动同步。

## 3. CLI

在项目根目录运行：

```bash
npm run cli -- login
npm run cli -- collections
npm run cli -- search GitHub
npm run cli -- read <article-id-prefix>
```

CLI 登录会话保存在 `~/.mazhu/session.json`。

配置 OpenAI-compatible 模型：

```bash
npm run cli -- config set
```

模型配置保存在 `~/.mazhu/config.json`。也可以使用环境变量临时覆盖：

```bash
MAZHU_MODEL_BASE_URL=https://example.com/compatible-mode/v1 \
MAZHU_MODEL_API_KEY=sk-xxx \
MAZHU_MODEL_NAME=deepseek-v4-flash \
npm run cli -- summarize --all
```

生成摘要：

```bash
npm run cli -- summarize --all
```

摘要生成前会检查文章解析质量。疑似错误页、验证页、正文过短或高度重复的内容会被跳过。

## 4. Codex Skill

安装本仓库内置的 Skill：

```bash
./scripts/install-codex-skill.sh
```

安装完成后，可以在 Codex 中使用：

```text
使用 $mazhu-knowledge，帮我总结收藏夹里和 AI 工具相关的文章
```

Skill 会调用本地 `mazhu` CLI 查询 Supabase 中的收藏数据，并优先使用摘要回答；必要时才读取全文。

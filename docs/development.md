# Development Guide

这份文档面向开发者和 AI 编程工具。它把本地开发、发布、Supabase、自定义部署和项目结构放在同一个入口里，避免文档拆得过细。

## Requirements

- Android Studio 或 Android SDK/Gradle 环境
- JDK 17
- Node.js 26+
- Android 手机或 Android Emulator
- 可选：Supabase 项目，用于自建同步后端
- 可选：Codex，用于安装 `mazhu-knowledge` Skill

## Local Android Build

复制本地配置模板：

```bash
cp android/local.properties.example android/local.properties
```

只构建本地-only App 时，填写 Android SDK 路径即可：

```properties
sdk.dir=/path/to/Android/sdk
```

如果你想让自己构建的 APK 连接某个 Supabase 项目，继续填写：

```properties
SUPABASE_URL=https://your-project-ref.supabase.co
SUPABASE_PUBLISHABLE_KEY=sb_publishable_xxx
```

运行检查：

```bash
npm run check
cd android
./gradlew testDebugUnitTest assembleDebug
```

`android/local.properties` 包含本机路径和项目配置，不应提交。

## Supabase Schema

自建 Supabase 后端时，在 SQL Editor 中按顺序执行：

```text
supabase/migrations/0001_create_core_schema.sql
supabase/migrations/0002_add_summary_fields.sql
supabase/migrations/0003_add_bookmark_collections.sql
```

数据库使用 Supabase Auth 的用户 ID 做隔离，并启用 RLS。每个登录用户只能读写自己的收藏夹和文章。

核心表：

- `collections`：收藏夹。
- `bookmarks`：文章。
- `bookmark_collections`：文章和收藏夹的多对多关系。

`bookmarks.collection_id` 是兼容字段，当前收藏夹归属以 `bookmark_collections` 为准。

## CLI And Skill

CLI 默认从环境变量读取 Supabase 配置；如果没有，再读取 `android/local.properties`。

```bash
MAZHU_SUPABASE_URL=https://your-project-ref.supabase.co \
MAZHU_SUPABASE_PUBLISHABLE_KEY=sb_publishable_xxx \
npm run cli -- login
```

常用命令：

```bash
npm run cli -- collections --json
npm run cli -- search GitHub --json
npm run cli -- read <article-id-prefix> --json
npm run cli -- config set
npm run cli -- summarize --all
```

安装本仓库内置 Skill：

```bash
./scripts/install-codex-skill.sh
```

模型摘要使用 OpenAI-compatible Chat Completions API。模型配置保存在 `~/.mazhu/config.json`，登录会话保存在 `~/.mazhu/session.json`。

## Release APK

项目提供 `.github/workflows/release-apk.yml`：

- 推送 `v*` tag 时构建签名 APK 并发布到 GitHub Release。
- 手动触发 workflow 时只上传 workflow artifact。

需要在 GitHub repository secrets 中配置：

```text
MAZHU_SUPABASE_URL
MAZHU_SUPABASE_PUBLISHABLE_KEY
MAZHU_RELEASE_KEYSTORE_BASE64
MAZHU_RELEASE_KEYSTORE_PASSWORD
MAZHU_RELEASE_KEY_ALIAS
MAZHU_RELEASE_KEY_PASSWORD
```

生成 keystore 示例：

```bash
keytool -genkeypair \
  -v \
  -keystore mazhu-release.jks \
  -alias mazhu \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000

base64 -i mazhu-release.jks | pbcopy
```

务必备份 release keystore。丢失 keystore 后，用户无法用同一个包名正常覆盖升级已安装的 release APK。

## Project Structure

```text
android/                    Android 原生应用
src/cli.ts                  mazhu CLI 入口和桌面知识库能力
skills/mazhu-knowledge/     Codex Skill 源文件
supabase/migrations/        Supabase 数据库迁移
docs/                       用户、开发和架构文档
scripts/                    本地安装和维护脚本
```

Android 代码位于 `android/app/src/main/java/com/lyn/mazhu/`：

```text
MainActivity.kt             Activity 启动、顶层状态编排、页面切换
MainViewModel.kt            UI 调用入口，连接 Repository/Auth/Worker
MazhuApplication.kt         应用级依赖组装

SearchPage.kt               全局/收藏夹内搜索页
SaveToCollectionsScreen.kt  保存到收藏夹流程
ArticleActionSheet.kt       单篇文章操作底部弹窗
BookmarkList.kt             文章卡片、批量操作栏
HomeComponents.kt           首页 tab、统计行、收藏夹卡片、拖动排序
CommonUi.kt                 小型可复用 UI 原子组件
Dialogs.kt                  登录、同步设置、收藏夹选择/命名等弹窗

PlatformActions.kt          打开链接、复制链接、剪切板检测
SearchHistoryStore.kt       搜索历史本地偏好存储
BookmarkUiFormatters.kt     UI 展示格式化

data/                       Room entities、DAO、Repository、本地业务规则
parser/                     微信文章解析
supabase/                   Supabase Auth/HTTP/data client
worker/                     WorkManager 解析和同步任务
ui/theme/                   Material theme、颜色、形状等视觉 token
```

## Change Rules

- 新页面：新增 `FeatureScreen.kt`，只把入口状态和导航回调接到 `MainActivity.kt`。
- 新复杂底部弹窗：单独建 `FeatureSheet.kt`，不要继续堆进 `MainActivity.kt`。
- 新数据库字段：同步修改 Room entity、migration、Supabase migration、`SupabaseDataClient`、CLI 类型和 Skill 文档。
- 新 CLI JSON 字段：保持向后兼容；Skill 依赖 `collections --json`、`search --json`、`read --json`。
- Composable 不直接做数据库、网络或长任务；这些逻辑放到 ViewModel、Repository、Worker 或 platform helper。
- 临时调查文档、截图路径、真实 token、API key、`android/local.properties` 不提交。

## Verification Checklist

常规改动：

```bash
npm run check
cd android
./gradlew testDebugUnitTest assembleDebug
cd ..
git diff --check
```

Schema 或同步相关改动还要检查：

- Room migration 是否覆盖旧版本升级。
- Supabase migration 是否和 Room 字段语义一致。
- Sync worker 是否同步新增字段。
- CLI 类型和 Skill 文档是否同步更新。

# 码住

码住是一个面向微信公众号文章的本地优先收藏和知识库工具。

它让 Android 用户可以从微信分享菜单快速保存公众号文章。默认情况下，收藏数据保存在手机本地；启用云同步后，数据可以同步到 Supabase，并进一步通过 CLI 和 Codex Skill 变成轻量知识库。

## Features

- 从 Android 分享菜单接收微信公众号文章链接。
- 本地优先保存，网络不可用时不会丢失收藏。
- 默认保存到“默认收藏夹”，也支持自定义收藏夹。
- 后台解析文章标题、公众号、正文等信息。
- 可选启用 Supabase Auth 和 PostgreSQL 同步收藏夹与文章。
- 通过 CLI 管理收藏、搜索文章、读取全文、生成 AI 摘要。
- 通过 Codex Skill 将收藏夹作为轻量知识库使用，并在回答中引用原文链接。

## Repository

```text
android/                    Android 原生应用
src/cli.ts                  mazhu CLI
skills/mazhu-knowledge/     Codex Skill 源文件
supabase/migrations/        Supabase 数据库迁移
scripts/install-codex-skill.sh
docs/                       安装和架构文档
```

## Quick Start

完整安装步骤见 [docs/setup.md](docs/setup.md)。

普通用户路径：

1. 安装码住 Android App。
2. 在微信文章分享菜单中选择“码住”。
3. 选择收藏夹，或直接保存到“默认收藏夹”。
4. 不登录也可以作为本地收藏夹使用。

增强路径：

1. 在 App 中启用云同步，登录 Supabase 账号。
2. 在电脑上通过 CLI 登录同一个账号。
3. 安装 `mazhu-knowledge` Codex Skill，让 Codex 基于收藏夹回答问题。

## CLI

```bash
npm run cli -- login
npm run cli -- collections
npm run cli -- search GitHub
npm run cli -- read <article-id-prefix>
npm run cli -- config set
npm run cli -- summarize --all
```

CLI 会读取 `android/local.properties` 中的 Supabase 项目配置，也可以在后续版本中切换到官方服务配置。用户登录会话保存在 `~/.mazhu/session.json`，模型配置保存在 `~/.mazhu/config.json`。

摘要功能要求模型服务兼容 OpenAI Chat Completions API，配置项为：

- `baseUrl`
- `apiKey`
- `modelName`

也可以用环境变量临时覆盖：

```bash
MAZHU_MODEL_BASE_URL=https://example.com/compatible-mode/v1 \
MAZHU_MODEL_API_KEY=sk-xxx \
MAZHU_MODEL_NAME=deepseek-v4-flash \
npm run cli -- summarize --all
```

## Codex Skill

Skill 源文件位于 `skills/mazhu-knowledge/`。

安装到本机 Codex：

```bash
./scripts/install-codex-skill.sh
```

安装后可以在 Codex 中使用：

```text
使用 $mazhu-knowledge，帮我看看收藏夹里有没有 UI 设计相关的 GitHub 开源项目
```

Skill 的默认策略是先搜索标题、收藏夹、AI 摘要和 topics；只有摘要不足时才读取全文；回答时引用文章标题和原文链接。

## Development

Android:

```bash
cd android
./gradlew testDebugUnitTest assembleDebug
```

CLI:

```bash
npm run check
npm run cli -- --help
```

CI 会在 push 和 pull request 时运行 CLI 检查、Android 单测和 debug APK 构建。

## Design Notes

- Android App 只负责收藏、查看和同步，不展示 AI 摘要。
- AI 摘要、全文读取和问答由 CLI 与 Codex Skill 负责。
- 当前不使用 embedding 检索，优先通过收藏夹、标题、摘要和 topics 做轻量检索。
- 项目不需要自建服务器；云同步依赖 Supabase。
- 解析失败或疑似异常的文章不会直接送入模型生成摘要。

更多设计说明见 [docs/architecture.md](docs/architecture.md)。

## Status

码住目前处于早期版本，已覆盖 Android 本地收藏、可选 Supabase 同步、CLI 查询、AI 摘要和 Codex Skill 问答主路径。

当前边界：

- 暂不支持 iOS 和 Windows。
- 暂不提供桌面 GUI。
- 暂不自动发布签名 APK。

## License

MIT

# 码住

码住是一个 Android 上的微信公众号文章收藏夹，也是一套给 AI agent 使用的轻量知识库工具。

你可以把微信里的公众号文章保存到码住，按收藏夹整理；如果启用云同步，同一份收藏数据还能被桌面端 CLI 和 Codex/Claude Code 等 agent 读取，用来搜索、总结和问答。

## Features

- 从 Android 分享菜单接收微信公众号文章链接。
- 复制 `mp.weixin.qq.com` 链接后打开 App，可自动识别并添加收藏。
- 本地优先保存，网络不可用时不会丢失收藏。
- 支持“默认收藏夹”、自定义收藏夹、一篇文章加入多个收藏夹。
- 后台解析标题、公众号、封面图和正文。
- 支持邮箱密码注册/登录，把收藏数据同步到官方 Supabase 后端。
- 提供桌面端 CLI，用于搜索收藏、读取全文和生成 AI 摘要。
- 提供 Codex Skill，让 agent 基于你的收藏夹回答问题并引用原文链接。

## Repository

```text
site/                       GitHub Pages 静态落地页和更新清单
android/                    Android 原生应用
src/cli.ts                  mazhu CLI
skills/mazhu-knowledge/     Codex Skill 源文件
supabase/migrations/        Supabase 数据库迁移
docs/user-guide.md          普通用户使用指南
docs/development.md         开发、发布和项目结构说明
docs/architecture.md        架构说明
scripts/install-codex-skill.sh
```

## Quick Start

普通用户只需要：

1. 打开项目主页：<https://lyn-zzz.github.io/mazhu/>。
2. 在 Android 手机上安装码住。
3. 在微信文章中选择分享给“码住”，或复制文章链接后打开码住。
4. 选择收藏夹保存。
5. 如需跨设备和 AI 问答能力，在 App 内注册/登录账号并同步。

详细步骤见 [docs/user-guide.md](docs/user-guide.md)。

## Agent Knowledge Base

启用云同步后，桌面端可以通过 CLI 和 Skill 查询同一个收藏夹。

```bash
npm run cli -- login
npm run cli -- collections --json
npm run cli -- search GitHub --json
npm run cli -- read <article-id-prefix> --json
npm run cli -- config set
npm run cli -- summarize --all
```

安装 Codex Skill：

```bash
./scripts/install-codex-skill.sh
```

之后可以在 Codex 中问：

```text
使用 $mazhu-knowledge，帮我看看收藏夹里有没有 UI 设计相关的 GitHub 开源项目
```

Skill 的默认策略是先搜索标题、收藏夹、AI 摘要和 topics；只有摘要不足时才读取少量全文；回答时引用文章标题和原文链接。

## Development

```bash
npm run check
cd android
./gradlew testDebugUnitTest assembleDebug
```

开发者指南见 [docs/development.md](docs/development.md)。它包含本地构建、自建 Supabase、Release APK、项目结构和 agent 维护约定。

## Design Notes

- Android App 负责收藏、查看和同步，不在移动端展示 AI 摘要。
- AI 摘要、全文读取和问答由 CLI 与 Skill 负责。
- 当前不使用 embedding，优先通过收藏夹、标题、公众号、摘要和 topics 做轻量检索。
- 项目不需要自建服务器；官方 APK 使用 Supabase 托管的 Auth、Postgres API 和 RLS。
- 解析失败或疑似异常的文章不会直接送入模型生成摘要。

更多系统设计见 [docs/architecture.md](docs/architecture.md)。

## Status

码住目前处于早期版本，已覆盖 Android 本地收藏、云同步、CLI 查询、AI 摘要和 Codex Skill 问答主路径。

当前边界：

- 暂不支持 iOS 和 Windows。
- 暂不提供桌面 GUI。
- APK 通过 GitHub Releases 分发，暂不上架应用商店。
- 项目主页通过 GitHub Pages 托管，并提供二维码、APK 下载入口和更新清单。

## License

MIT

# Contributing

码住目前处于早期阶段，欢迎围绕 Android 收藏体验、微信公众号解析、Supabase 同步、CLI 和 Codex Skill 做改进。

## Development Checks

提交前建议运行：

```bash
npm run check
cd android
./gradlew testDebugUnitTest assembleDebug
```

## Pull Requests

- 保持改动聚焦，避免把无关重构和功能混在同一个 PR。
- 不要提交 `android/local.properties`、API Key、Supabase token 或模型服务密钥。
- 修改数据库结构时，同时更新 `supabase/migrations/`。
- 修改 CLI 命令时，同步更新 README、`docs/development.md` 或 Skill。
- 修改 Skill 使用方式时，同步更新 `skills/mazhu-knowledge/SKILL.md`。

## Issue Reports

报告问题时请尽量包含：

- Android 设备和系统版本。
- 文章链接或可复现的分享文本。
- 预期行为和实际行为。
- CLI 或 Android 构建错误日志。

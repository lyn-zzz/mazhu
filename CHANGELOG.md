# Changelog

## Unreleased

- Android App 支持未配置 Supabase 时以本地-only 模式运行。
- 增加 App 内云同步设置，可在运行期配置 Supabase URL 和 publishable key。
- 未启用云同步时，文章状态显示为“仅本地”。

## 0.1.0

- Android App 支持从微信分享菜单收藏公众号文章。
- 支持默认收藏夹和自定义收藏夹。
- 支持本地 Room 存储、后台解析、自动同步和手动同步。
- 支持 Supabase Auth、collections/bookmarks 数据表和 RLS。
- 提供 `mazhu` CLI，用于登录、查询收藏夹、搜索文章、读取全文和生成摘要。
- 提供 `mazhu-knowledge` Codex Skill，用于基于收藏夹问答并引用原文链接。
- 提供 GitHub Actions CI，覆盖 CLI 检查、Android 单测和 debug APK 构建。

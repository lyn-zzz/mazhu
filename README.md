# 码住

码住是一个面向微信公众号文章的本地优先收藏应用。它解决三个问题：

- 在 Android 手机上从微信分享菜单快速收藏公众号文章。
- 将收藏夹和文章同步到 Supabase。
- 在 Mac 上通过 CLI 和 Codex Skill 把收藏夹变成轻量知识库。

当前仓库包含：

```text
android/                    Android 原生应用
src/cli.ts                  Mac 端 mazhu CLI
skills/mazhu-knowledge/     Codex Skill 源文件
supabase/migrations/        Supabase 数据库迁移
scripts/install-codex-skill.sh
```

## 功能

### Android App

- 接收微信文章分享。
- 立即保存到本地 Room 数据库。
- 默认保存到“默认收藏夹”。
- 支持新建、重命名、删除收藏夹。
- 支持分享时选择收藏夹。
- 后台解析微信公众号文章标题、公众号、正文等信息。
- 邮箱密码登录 Supabase。
- 自动同步和手动重试同步。
- 网络失败时保留“未同步”，不丢失本地收藏。

### Mac CLI

CLI 用于读取和整理已经同步到 Supabase 的收藏数据：

```bash
npm run cli -- login
npm run cli -- collections
npm run cli -- search GitHub
npm run cli -- search UI
npm run cli -- read <文章ID前缀>
npm run cli -- config set
npm run cli -- summarize --all
```

CLI 默认读取 `android/local.properties` 中的 Supabase 项目配置。登录会话保存在 `~/.mazhu/session.json`，模型配置保存在 `~/.mazhu/config.json`。

### Codex Skill

Skill 源文件在：

```text
skills/mazhu-knowledge/
```

安装到本机 Codex：

```bash
./scripts/install-codex-skill.sh
```

安装脚本会把当前项目目录写入本机 skill。也就是说，别人 clone 到自己的路径后运行安装脚本，skill 会自动指向他自己的项目目录。

安装后可以在 Codex 中使用：

```text
使用 $mazhu-knowledge，帮我看看我收藏夹里有没有 UI 设计相关的 GitHub 开源项目
```

Skill 的策略是：先用 `mazhu search` 读取标题、收藏夹、AI 摘要和 topics；只有摘要不够时才用 `mazhu read` 读取全文；回答时带文章标题和原文链接。

## 本地配置

### Android Supabase 配置

复制模板：

```bash
cp android/local.properties.example android/local.properties
```

填写：

```properties
sdk.dir=/path/to/Android/sdk
SUPABASE_URL=https://your-project-ref.supabase.co
SUPABASE_PUBLISHABLE_KEY=sb_publishable_xxx
```

`android/local.properties` 不要提交到 Git。

### CLI 模型配置

摘要功能要求模型服务兼容 OpenAI Chat Completions API，只需要三项配置：

- `baseUrl`
- `apiKey`
- `modelName`

交互式配置：

```bash
npm run cli -- config set
```

也可以使用环境变量临时运行：

```bash
MAZHU_MODEL_BASE_URL=https://example.com/compatible-mode/v1 \
MAZHU_MODEL_API_KEY=sk-xxx \
MAZHU_MODEL_NAME=deepseek-v4-flash \
npm run cli -- summarize --all
```

不要把 API Key 写进仓库。

## Supabase 初始化

创建 Supabase 项目后，按顺序执行：

```sql
supabase/migrations/0001_create_core_schema.sql
supabase/migrations/0002_add_summary_fields.sql
```

表结构使用 Supabase Auth 的 `auth.users`，并启用 RLS。客户端只能访问当前登录用户自己的收藏夹和文章。

## 开发命令

Android：

```bash
cd android
JAVA_HOME='/Applications/Android Studio.app/Contents/jbr/Contents/Home' ./gradlew testDebugUnitTest assembleDebug
```

CLI：

```bash
npm run check
npm run cli -- --help
```

## 解析质量策略

`mazhu summarize` 不会盲目把所有正文丢给模型。它会先检查：

- 文章解析状态必须是 `success`
- 正文不能过短
- 正文开头不能明显是错误页、验证页或反爬提示
- 正文需要包含足够中文内容
- 正文不能明显高度重复

不通过检查的文章会标记为 `skipped` 和 `content_quality_status = suspect`，后续可以改进解析器后再用 `--force` 重新生成摘要。

## 当前边界

- 暂不支持 iOS 和 Windows。
- 暂不做 embedding 检索。
- 手机端不展示 AI 摘要，只负责收藏、查看和同步。
- Mac 端 CLI 和 Codex Skill 负责知识库问答。

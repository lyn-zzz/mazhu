# CLAUDE.md

Claude Code should use this as the always-loaded orientation for Mazhu. Keep responses and edits aligned with the project scope below.

## North Star

Mazhu should feel like a lightweight, native Android bookmark app for WeChat public-account articles, with an optional synced knowledge layer for AI agents. The core UX is: save fast, organize simply, sync safely, query later.

## Architecture Invariants

- Android is local-first. A bookmark is successfully captured once it is written to Room.
- Supabase is a managed sync layer, not a custom backend. Use Auth, Postgres API, RLS, and migrations.
- CLI and Skill operate on synced Supabase data. They cannot read the phone's local Room database directly.
- AI summaries are generated on desktop through the CLI, not displayed or generated inside the Android app.
- `bookmark_collections` is the source of truth for multi-collection membership. `bookmarks.collection_id` is legacy compatibility.
- Collection drag ordering is Android visual state and should not affect desktop Skill retrieval.

## Key Files

- Android shell/state: `android/app/src/main/java/com/lyn/mazhu/MainActivity.kt`, `MainViewModel.kt`, `MazhuApplication.kt`
- Home UI: `HomeComponents.kt`
- Article list and bulk actions: `BookmarkList.kt`
- Article action sheet: `ArticleActionSheet.kt`
- Save flow: `SaveToCollectionsScreen.kt`
- Search: `SearchPage.kt`, `SearchHistoryStore.kt`
- Local data: `data/BookmarkDatabase.kt`, `data/BookmarkDao.kt`, `data/BookmarkRepository.kt`
- Parsing: `parser/WechatArticleParser.kt`
- Sync/Auth: `supabase/`, `worker/`
- CLI: `src/cli.ts`
- Skill: `skills/mazhu-knowledge/SKILL.md`
- Docs: `docs/user-guide.md`, `docs/development.md`, `docs/architecture.md`

## Development Commands

```bash
npm run check
cd android
./gradlew testDebugUnitTest assembleDebug
```

For networked commands on this Mac, wrap the command with:

```bash
zsh -ic 'proxy_on; <command>'
```

## Android UI Guidance

- Prefer Jetpack Compose + Material 3 patterns already used in the repo.
- Keep UI logic out of repositories/workers and data/network logic out of Composables.
- Use stable keys for lazy lists.
- Use Coil for remote article covers; avoid hand-written image loading.
- Respect edge-to-edge, navigation bar padding, keyboard insets, and bottom sheet padding.
- Reuse theme tokens before adding raw colors/radii/shadows.

## Release Notes

Release APK publishing is handled by `.github/workflows/release-apk.yml`. The workflow expects GitHub Secrets for official Supabase config and APK signing. Do not replace release signing with debug signing for public APKs.

## Privacy And Secrets

Do not expose real API keys, Supabase tokens, keystores, or local session files. `~/.mazhu/session.json` stores the user's CLI Supabase session; `~/.mazhu/config.json` stores model provider config.

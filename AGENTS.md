# AGENTS.md

This file is the entry point for AI coding agents working on Mazhu. Claude Code also reads `CLAUDE.md`; other agents should start here.

## Product Shape

Mazhu is an Android-first WeChat public-account article bookmark app plus a desktop CLI/Skill knowledge layer.

- Android app: capture WeChat article links, store locally first, parse article metadata/content, sync when possible.
- Supabase: hosted Auth + Postgres API + RLS. There is no custom backend server.
- CLI: search synced bookmarks, read full text, generate OpenAI-compatible summaries.
- Skill: tells Codex/agents how to answer from the user's synced bookmarks.

Do not turn this into a generic read-later service, desktop GUI, embedding/RAG platform, or custom server unless the user explicitly reopens that scope.

## Read First

1. `README.md` for product overview.
2. `docs/development.md` for setup, release, structure, and change rules.
3. `docs/architecture.md` for system design.
4. `skills/mazhu-knowledge/SKILL.md` before changing CLI JSON output or agent behavior.

## Common Commands

```bash
npm run check
cd android
./gradlew testDebugUnitTest assembleDebug
```

When running commands that need network access on the user's Mac, use:

```bash
zsh -ic 'proxy_on; <command>'
```

Do not run `proxy_on` separately; its environment only applies within that shell process.

## Safety Rules

- Never commit `android/local.properties`, keystores, API keys, Supabase tokens, or model API keys.
- Keep official Supabase service access limited to publishable client config plus RLS. Never put a service-role key in Android or CLI code.
- Release APK signing requires a stable keystore. Losing it breaks normal app updates.
- Preserve local-first behavior: saving an article must not depend on network, Supabase, parsing, or AI summary generation.

## Change Rules

- New Android screens should get their own `FeatureScreen.kt` file and only connect navigation/state at the app shell.
- Composables should render state and emit events; database/network/work logic belongs in ViewModel, Repository, Worker, or platform helpers.
- Room schema changes require entity updates, migration updates, sync worker checks, and usually Supabase/CLI/Skill updates.
- Supabase schema changes require a migration under `supabase/migrations/` and RLS review.
- CLI JSON output is an agent contract. Keep it backward-compatible or update `skills/mazhu-knowledge/SKILL.md` in the same change.
- User-facing docs should stay compact: ordinary use in `docs/user-guide.md`, development/release/project structure in `docs/development.md`, system design in `docs/architecture.md`.

## Before Finishing

Run the narrowest relevant verification. For most edits:

```bash
npm run check
cd android
./gradlew testDebugUnitTest assembleDebug
cd ..
git diff --check
```

If Android UI behavior changed, test on emulator or a real Android device when available.

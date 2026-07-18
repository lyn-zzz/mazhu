---
name: mazhu-knowledge
description: Query and answer from the user's Mazhu WeChat article knowledge base. Use when the user asks about saved微信公众号/WeChat public account articles, saved GitHub/open-source/AI/tool/UI articles, asks what has been collected on a topic, wants recommendations based on their Mazhu收藏夹, or asks to inspect, summarize, cite, or answer using the local `mazhu` CLI and Supabase-backed article summaries/full text.
---

# Mazhu Knowledge

Use the local `mazhu` CLI to answer from the user's saved WeChat public account articles.

## CLI Location

Run commands from the installed Mazhu project root:

```bash
cd __MAZHU_PROJECT_ROOT__
```

Use:

```bash
npm run cli -- <command>
```

If that fails, try direct execution:

```bash
node src/cli.ts <command>
```

## Core Workflow

1. Check login first:

```bash
npm run cli -- whoami
```

If not logged in, tell the user to run `npm run cli -- login`.

2. Search with JSON output so article IDs, collections, summaries, topics, and source URLs are not misread:

```bash
npm run cli -- search <keywords> --limit 10 --json
```

Search matches title, 公众号名称, original URL, all collection names, AI summary, and topics. A saved article may belong to multiple collections; use the `collections` array in JSON, not the legacy `collection_id` field.

3. Prefer answering from `ai_summary`, `ai_key_points`, `ai_topics`, title, account name, and collection names when enough. Do not read full text just to answer broad discovery questions.

4. Read selected full text only when summaries are missing, ambiguous, or the user asks for implementation details, exact project names, comparisons, or quotes:

```bash
npm run cli -- read <article-id-prefix> --json
```

5. If summaries are missing and the user asks for knowledge-base QA, generate a small batch first:

```bash
npm run cli -- summarize --all --limit 10
```

If model configuration is missing, ask the user to configure it with:

```bash
npm run cli -- config set
```

## Answering Rules

- Answer only from Mazhu results unless the user explicitly asks for outside research.
- Cite each supporting article with title and original URL.
- Say when the knowledge base has no clear evidence.
- Distinguish summary-derived claims from full-text-derived claims if precision matters.
- For time-sensitive claims such as star counts or current project status, say they come from the saved article and may need live verification.
- Do not bulk-read full texts. Search/list first, read only the small candidate set needed.
- If `summary_status` is `pending`, `failed`, or `skipped`, treat the summary as unavailable and decide whether to run `summarize` or read the article.
- If `content_quality_status` is `suspect`, warn that the parsed content may be unreliable.

## Useful Commands

List collections:

```bash
npm run cli -- collections --json
```

Search all saved articles:

```bash
npm run cli -- search AI --limit 10 --json
```

Search one collection:

```bash
npm run cli -- search GitHub --collection UI/UX --limit 10 --json
```

Read full article:

```bash
npm run cli -- read 4430582a --json
```

Generate missing summaries:

```bash
npm run cli -- summarize --all --limit 10
```

Show model configuration without exposing the full API key:

```bash
npm run cli -- config show
```

## Response Pattern

For topic discovery questions, structure the response as:

- Direct answer: what was found.
- Relevant saved articles: title, collections, short reason, original URL.
- Evidence limits: whether only summary was used or full text was read.

For recommendation questions, group by use case rather than by article order.

For "有没有..." questions, be explicit:

- "收藏夹里找到了..."
- "收藏夹里暂时没找到..."

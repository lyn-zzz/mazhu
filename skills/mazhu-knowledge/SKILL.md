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

1. Check the user is logged in:

```bash
npm run cli -- whoami
```

If not logged in, tell the user to run `npm run cli -- login`.

2. Search first, using the user's question keywords:

```bash
npm run cli -- search <keywords> --limit 10
```

Search matches article title,公众号名称, original URL,收藏夹名称, AI summary, and topics.

3. Prefer answering from search results when summaries are enough.

4. Read full text only when needed:

```bash
npm run cli -- read <article-id-prefix>
```

Use full text for detailed comparisons, implementation steps, exact project names, or when the summary is ambiguous.

5. If summaries are missing and the user asks for knowledge-base QA, generate them before answering:

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
- Do not read all full texts by default. Start with summaries, then selectively read.

## Useful Commands

List folders:

```bash
npm run cli -- collections
```

Search all saved articles:

```bash
npm run cli -- search AI --limit 10
```

Search one folder:

```bash
npm run cli -- search --collection UI/UX --limit 10
```

Read full article:

```bash
npm run cli -- read 4430582a
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
- Relevant saved articles: title, collection, short reason, original URL.
- Evidence limits: whether only summary was used or full text was read.

For recommendation questions, group by use case rather than by article order.

For "有没有..." questions, be explicit:

- "收藏夹里找到了..."
- "收藏夹里暂时没找到..."

#!/usr/bin/env node

import { createInterface } from "node:readline/promises";
import { stdin as input, stdout as output } from "node:process";
import { chmod, mkdir, readFile, stat, unlink, writeFile } from "node:fs/promises";
import { existsSync } from "node:fs";
import { dirname, join, resolve } from "node:path";
import { homedir } from "node:os";

const MAZHU_DIR = join(homedir(), ".mazhu");
const SESSION_FILE = join(MAZHU_DIR, "session.json");
const SESSION_LOCK_FILE = join(MAZHU_DIR, "session.lock");
const MODEL_CONFIG_FILE = join(MAZHU_DIR, "config.json");
const DEFAULT_LIMIT = 20;
const MAX_SUMMARY_INPUT_CHARS = 24_000;
const SESSION_LOCK_STALE_MS = 15_000;

type AppConfig = {
  supabaseUrl: string;
  publishableKey: string;
};

type ModelConfig = {
  baseUrl: string;
  apiKey: string;
  modelName: string;
};

type Session = {
  accessToken: string;
  refreshToken: string;
  userId: string;
  email?: string;
  expiresAtEpochSeconds: number;
};

type CollectionRow = {
  id: string;
  name: string;
  created_at: string;
};

type BookmarkRow = {
  id: string;
  original_url: string;
  normalized_url: string;
  title: string;
  account_name: string | null;
  cover_url: string | null;
  published_at: string | null;
  content_text: string | null;
  collection_id: string;
  parse_status: string;
  parse_error: string | null;
  created_at: string;
  ai_summary?: string | null;
  ai_key_points?: string[] | null;
  ai_topics?: string[] | null;
  ai_links?: string[] | null;
  summary_model?: string | null;
  summarized_at?: string | null;
  summary_status?: string | null;
  summary_error?: string | null;
  content_quality_status?: string | null;
  content_quality_error?: string | null;
};

type BookmarkCollectionRow = {
  bookmark_id: string;
  collection_id: string;
  created_at?: string;
};

type CollectionWithCount = CollectionRow & {
  article_count: number;
};

type CollectionsContext = {
  collections: CollectionRow[];
  collectionById: Map<string, CollectionRow>;
  collectionIdsByBookmarkId: Map<string, string[]>;
};

type SummaryResult = {
  summary: string;
  key_points: string[];
  topics: string[];
  links: string[];
};

async function main() {
  const [command, ...args] = process.argv.slice(2);
  const config = await loadConfig();

  try {
    switch (command) {
      case "login":
        await login(config);
        break;
      case "logout":
        await logout();
        break;
      case "whoami":
        await whoami();
        break;
      case "config":
        await configureModel(args);
        break;
      case "collections":
        await listCollections(config, args);
        break;
      case "search":
        await searchBookmarks(config, args);
        break;
      case "read":
        await readBookmark(config, args);
        break;
      case "summarize":
        await summarizeBookmarks(config, args);
        break;
      case undefined:
      case "-h":
      case "--help":
      case "help":
        printHelp();
        break;
      default:
        throw new Error(`未知命令：${command}`);
    }
  } catch (error) {
    const message = error instanceof Error ? error.message : String(error);
    console.error(`错误：${message}`);
    process.exitCode = 1;
  }
}

async function loadConfig(): Promise<AppConfig> {
  const envUrl = process.env.MAZHU_SUPABASE_URL;
  const envKey = process.env.MAZHU_SUPABASE_PUBLISHABLE_KEY;
  if (envUrl && envKey) {
    return {
      supabaseUrl: envUrl.replace(/\/$/, ""),
      publishableKey: envKey,
    };
  }

  const localPropertiesPath = resolve("android", "local.properties");
  if (!existsSync(localPropertiesPath)) {
    throw new Error(
      "找不到 Supabase 配置。请设置 MAZHU_SUPABASE_URL 和 MAZHU_SUPABASE_PUBLISHABLE_KEY，或在项目根目录提供 android/local.properties。",
    );
  }

  const localProperties = await readFile(localPropertiesPath, "utf8");
  const values = Object.fromEntries(
    localProperties
      .split(/\r?\n/)
      .map((line) => line.trim())
      .filter((line) => line && !line.startsWith("#"))
      .map((line) => {
        const index = line.indexOf("=");
        return [line.slice(0, index), line.slice(index + 1)];
      }),
  );

  const supabaseUrl = values.SUPABASE_URL;
  const publishableKey = values.SUPABASE_PUBLISHABLE_KEY;
  if (!supabaseUrl || !publishableKey) {
    throw new Error("android/local.properties 缺少 SUPABASE_URL 或 SUPABASE_PUBLISHABLE_KEY。");
  }
  return {
    supabaseUrl: supabaseUrl.replace(/\/$/, ""),
    publishableKey,
  };
}

async function login(config: AppConfig) {
  const rl = createInterface({ input, output });
  const email = (await rl.question("邮箱：")).trim();
  const password = await questionHidden(rl, "密码：");
  rl.close();

  if (!email || password.length < 6) {
    throw new Error("邮箱不能为空，密码至少 6 位。");
  }

  const response = await requestJson(config, "/auth/v1/token?grant_type=password", {
    method: "POST",
    body: {
      email,
      password,
    },
  });
  const session = parseSession(response);
  await saveSession(session);
  console.log(`已登录：${session.email ?? email}`);
}

async function logout() {
  if (existsSync(SESSION_FILE)) {
    await unlink(SESSION_FILE);
  }
  console.log("已退出登录。");
}

async function whoami() {
  const session = await loadSession();
  if (!session) {
    console.log("未登录。");
    return;
  }
  console.log(session.email ?? session.userId);
}

async function configureModel(args: string[]) {
  const action = args[0] ?? "show";
  if (action === "path") {
    console.log(MODEL_CONFIG_FILE);
    return;
  }
  if (action === "show") {
    const config = await loadModelConfig(false);
    if (!config) {
      console.log("尚未配置模型。运行：mazhu config set");
      return;
    }
    console.log(`base_url: ${config.baseUrl}`);
    console.log(`model_name: ${config.modelName}`);
    console.log(`api_key: ${maskSecret(config.apiKey)}`);
    return;
  }
  if (action !== "set") {
    throw new Error("用法：mazhu config set|show|path");
  }

  const existing = await loadModelConfig(false);
  const flags = parseFlags(args.slice(1));
  const rl = createInterface({ input, output });
  const baseUrlInput = flags["base-url"] ?? await questionWithExistingDefault(
    rl,
    "base_url",
    existing?.baseUrl,
  );
  const modelNameInput = flags.model ?? await questionWithExistingDefault(
    rl,
    "model_name",
    existing?.modelName,
  );
  const apiKey = flags["api-key"] ?? await questionRequiredHidden(rl, "api_key");
  rl.close();
  const baseUrl = baseUrlInput || existing?.baseUrl;
  const modelName = modelNameInput || existing?.modelName;

  if (!baseUrl || !modelName || !apiKey) {
    throw new Error("base_url、api_key、model_name 都必须配置。");
  }
  await saveModelConfig({
    baseUrl: baseUrl.replace(/\/$/, ""),
    apiKey,
    modelName,
  });
  console.log(`模型配置已保存到 ${MODEL_CONFIG_FILE}`);
}

async function listCollections(config: AppConfig, args: string[]) {
  const session = await requireSession(config);
  const { json } = parseListArgs(args);
  const collections = await requestJson<CollectionRow[]>(
    config,
    "/rest/v1/collections?select=id,name,created_at&order=created_at.asc",
    {
      accessToken: session.accessToken,
    },
  );
  const relations = await getBookmarkCollectionRows(config, session);
  const counts = new Map<string, number>();
  for (const relation of relations) {
    counts.set(relation.collection_id, (counts.get(relation.collection_id) ?? 0) + 1);
  }
  const rows: CollectionWithCount[] = collections.map((collection) => ({
    ...collection,
    article_count: counts.get(collection.id) ?? 0,
  }));

  if (json) {
    printJson({ collections: rows });
    return;
  }

  if (rows.length === 0) {
    console.log("暂无收藏夹。");
    return;
  }
  for (const row of rows) {
    console.log(`${row.name}\t${row.article_count} 篇\t${row.id}`);
  }
}

async function searchBookmarks(config: AppConfig, args: string[]) {
  const session = await requireSession(config);
  const { query, limit, collection, json } = parseSearchArgs(args);
  const { collections, collectionById, collectionIdsByBookmarkId } =
    await getCollectionsContext(config, session);
  const collectionFilter = collection
    ? collections.find((item) => item.name === collection || item.id === collection)
    : undefined;

  if (collection && !collectionFilter) {
    throw new Error(`找不到收藏夹：${collection}`);
  }

  const params = new URLSearchParams();
  params.set(
    "select",
    [
      "id",
      "title",
      "account_name",
      "original_url",
      "normalized_url",
      "cover_url",
      "collection_id",
      "parse_status",
      "published_at",
      "created_at",
      "ai_summary",
      "ai_key_points",
      "ai_topics",
      "ai_links",
      "summary_status",
      "content_quality_status",
    ].join(","),
  );
  params.set("order", "created_at.desc");
  params.set("limit", "1000");

  const allBookmarks = await requestJson<BookmarkRow[]>(
    config,
    `/rest/v1/bookmarks?${params}`,
    {
      accessToken: session.accessToken,
    },
  );
  const normalizedQuery = query.toLocaleLowerCase();
  const bookmarks = allBookmarks
    .filter((bookmark) => {
      if (!collectionFilter) {
        return true;
      }
      return collectionIdsByBookmarkId.get(bookmark.id)?.includes(collectionFilter.id) ?? false;
    })
    .filter((bookmark) => {
      if (!normalizedQuery) {
        return true;
      }
      const collectionNames = getCollectionNames(
        bookmark,
        collectionIdsByBookmarkId,
        collectionById,
      );
      return [
        bookmark.title,
        bookmark.account_name ?? "",
        bookmark.original_url,
        ...collectionNames,
        bookmark.ai_summary ?? "",
        ...(bookmark.ai_topics ?? []),
      ].some((value) => value.toLocaleLowerCase().includes(normalizedQuery));
    })
    .slice(0, limit);

  if (json) {
    printJson({
      query,
      collection: collectionFilter
        ? {
            id: collectionFilter.id,
            name: collectionFilter.name,
          }
        : null,
      count: bookmarks.length,
      bookmarks: bookmarks.map((bookmark) =>
        formatBookmarkForAgent(bookmark, collectionIdsByBookmarkId, collectionById, false),
      ),
    });
    return;
  }

  if (bookmarks.length === 0) {
    console.log("没有找到匹配文章。");
    return;
  }
  for (const bookmark of bookmarks) {
    const collectionNames = getCollectionNames(
      bookmark,
      collectionIdsByBookmarkId,
      collectionById,
    );
    console.log(`${bookmark.id.slice(0, 8)}\t[${collectionNames.join(" / ")}]\t${bookmark.title}`);
    console.log(`  ${bookmark.account_name ?? "未知公众号"} · ${bookmark.original_url}`);
    if (bookmark.ai_summary) {
      console.log(`  摘要：${bookmark.ai_summary}`);
    } else {
      console.log(`  摘要：${bookmark.summary_status === "failed" ? "生成失败" : "未生成"}`);
    }
    if (bookmark.ai_topics?.length) {
      console.log(`  Topics：${bookmark.ai_topics.join("、")}`);
    }
  }
}

async function getCollectionsContext(
  config: AppConfig,
  session: Session,
): Promise<CollectionsContext> {
  const collections = await requestJson<CollectionRow[]>(
    config,
    "/rest/v1/collections?select=id,name,created_at",
    {
      accessToken: session.accessToken,
    },
  );
  const relations = await getBookmarkCollectionRows(config, session);
  return {
    collections,
    collectionById: new Map(collections.map((item) => [item.id, item])),
    collectionIdsByBookmarkId: groupCollectionIdsByBookmarkId(relations),
  };
}

async function getBookmarkCollectionRows(
  config: AppConfig,
  session: Session,
): Promise<BookmarkCollectionRow[]> {
  return requestJson<BookmarkCollectionRow[]>(
    config,
    "/rest/v1/bookmark_collections?select=bookmark_id,collection_id",
    {
      accessToken: session.accessToken,
    },
  );
}

function groupCollectionIdsByBookmarkId(
  relations: BookmarkCollectionRow[],
): Map<string, string[]> {
  const grouped = new Map<string, string[]>();
  for (const relation of relations) {
    grouped.set(
      relation.bookmark_id,
      [...(grouped.get(relation.bookmark_id) ?? []), relation.collection_id],
    );
  }
  return grouped;
}

function getCollectionNames(
  bookmark: BookmarkRow,
  collectionIdsByBookmarkId: Map<string, string[]>,
  collectionById: Map<string, CollectionRow>,
): string[] {
  const collectionIds = collectionIdsByBookmarkId.get(bookmark.id) ?? [bookmark.collection_id];
  return collectionIds.map((id) => collectionById.get(id)?.name ?? id);
}

async function readBookmark(config: AppConfig, args: string[]) {
  const { idOrPrefix, json } = parseReadArgs(args);
  if (!idOrPrefix) {
    throw new Error("用法：mazhu read <文章ID或前缀> [--json]");
  }

  const session = await requireSession(config);
  const bookmark = await findOneBookmark(config, session, idOrPrefix);
  const { collectionById, collectionIdsByBookmarkId } = await getCollectionsContext(config, session);

  if (json) {
    printJson({
      bookmark: formatBookmarkForAgent(
        bookmark,
        collectionIdsByBookmarkId,
        collectionById,
        true,
      ),
    });
    return;
  }

  console.log(`# ${bookmark.title}`);
  console.log("");
  console.log(`公众号：${bookmark.account_name ?? "未知"}`);
  console.log(`链接：${bookmark.original_url}`);
  console.log(`收藏夹：${getCollectionNames(bookmark, collectionIdsByBookmarkId, collectionById).join(" / ")}`);
  console.log(`解析状态：${bookmark.parse_status}`);
  console.log(`摘要状态：${bookmark.summary_status ?? "pending"}`);
  if (bookmark.cover_url) {
    console.log(`封面：${bookmark.cover_url}`);
  }
  if (bookmark.ai_summary) {
    console.log(`摘要：${bookmark.ai_summary}`);
  }
  if (bookmark.ai_key_points?.length) {
    console.log("");
    console.log("要点：");
    for (const point of bookmark.ai_key_points) {
      console.log(`- ${point}`);
    }
  }
  console.log("");
  console.log(bookmark.content_text?.trim() || "暂无正文。");
}

async function summarizeBookmarks(config: AppConfig, args: string[]) {
  const session = await requireSession(config);
  const modelConfig = await loadModelConfig(true);
  const options = parseSummarizeArgs(args);
  const bookmarks = await getSummarizeTargets(config, session, options);

  if (bookmarks.length === 0) {
    console.log("没有需要生成摘要的文章。");
    return;
  }

  for (const bookmark of bookmarks) {
    const label = `${bookmark.id.slice(0, 8)} ${bookmark.title}`;
    const quality = validateContentQuality(bookmark);
    if (!quality.ok) {
      await patchBookmark(config, session, bookmark.id, {
        summary_status: "skipped",
        summary_error: quality.reason,
        content_quality_status: "suspect",
        content_quality_error: quality.reason,
      });
      console.log(`跳过：${label}`);
      console.log(`  原因：${quality.reason}`);
      continue;
    }

    console.log(`生成摘要：${label}`);
    try {
      await patchBookmark(config, session, bookmark.id, {
        summary_status: "pending",
        summary_error: null,
        content_quality_status: "ok",
        content_quality_error: null,
      });
      const result = await generateSummary(modelConfig, bookmark);
      await patchBookmark(config, session, bookmark.id, {
        ai_summary: result.summary,
        ai_key_points: result.key_points,
        ai_topics: result.topics,
        ai_links: result.links,
        summary_model: modelConfig.modelName,
        summarized_at: new Date().toISOString(),
        summary_status: "success",
        summary_error: null,
        content_quality_status: "ok",
        content_quality_error: null,
      });
      console.log(`  完成：${result.summary}`);
    } catch (error) {
      const message = error instanceof Error ? error.message : String(error);
      await patchBookmark(config, session, bookmark.id, {
        summary_status: "failed",
        summary_error: message.slice(0, 500),
      });
      console.log(`  失败：${message}`);
    }
  }
}

function parseSearchArgs(args: string[]) {
  let limit = DEFAULT_LIMIT;
  let collection: string | undefined;
  let json = false;
  const queryParts: string[] = [];

  for (let index = 0; index < args.length; index += 1) {
    const arg = args[index];
    if (arg === "--json") {
      json = true;
      continue;
    }
    if (arg === "--limit" || arg === "-n") {
      limit = Number(args[++index]);
      continue;
    }
    if (arg === "--collection" || arg === "-c") {
      collection = args[++index];
      continue;
    }
    queryParts.push(arg);
  }

  if (!Number.isFinite(limit) || limit <= 0 || limit > 100) {
    throw new Error("--limit 必须是 1 到 100 之间的数字。");
  }

  return {
    query: queryParts.join(" ").trim(),
    limit,
    collection,
    json,
  };
}

function parseListArgs(args: string[]) {
  return {
    json: args.includes("--json"),
  };
}

function parseReadArgs(args: string[]) {
  let json = false;
  let idOrPrefix: string | undefined;

  for (const arg of args) {
    if (arg === "--json") {
      json = true;
      continue;
    }
    idOrPrefix = arg;
  }

  return { idOrPrefix, json };
}

function parseSummarizeArgs(args: string[]) {
  let limit = 10;
  let collection: string | undefined;
  let force = false;
  let all = false;
  let idOrPrefix: string | undefined;

  for (let index = 0; index < args.length; index += 1) {
    const arg = args[index];
    if (arg === "--limit" || arg === "-n") {
      limit = Number(args[++index]);
      continue;
    }
    if (arg === "--collection" || arg === "-c") {
      collection = args[++index];
      continue;
    }
    if (arg === "--force") {
      force = true;
      continue;
    }
    if (arg === "--all") {
      all = true;
      continue;
    }
    idOrPrefix = arg;
  }

  if (!Number.isFinite(limit) || limit <= 0 || limit > 100) {
    throw new Error("--limit 必须是 1 到 100 之间的数字。");
  }

  return {
    all,
    collection,
    force,
    idOrPrefix,
    limit,
  };
}

async function getSummarizeTargets(
  config: AppConfig,
  session: Session,
  options: ReturnType<typeof parseSummarizeArgs>,
): Promise<BookmarkRow[]> {
  if (options.idOrPrefix) {
    return [await findOneBookmark(config, session, options.idOrPrefix)];
  }

  const params = new URLSearchParams();
  params.set("select", "*");
  params.set("order", "created_at.desc");
  params.set("limit", String(options.limit));
  params.set("parse_status", "eq.success");
  if (!options.force) {
    params.set("summary_status", "neq.success");
  }
  if (options.collection) {
    const collection = await findCollection(config, session, options.collection);
    const relations = await getBookmarkCollectionRows(config, session);
    const bookmarkIds = relations
      .filter((relation) => relation.collection_id === collection.id)
      .map((relation) => relation.bookmark_id);
    if (bookmarkIds.length === 0) {
      return [];
    }
    params.set("id", `in.(${bookmarkIds.map(encodeURIComponent).join(",")})`);
  }
  if (!options.all && !options.collection) {
    params.set("summary_status", "neq.success");
  }
  return requestJson<BookmarkRow[]>(config, `/rest/v1/bookmarks?${params}`, {
    accessToken: session.accessToken,
  });
}

async function findCollection(
  config: AppConfig,
  session: Session,
  nameOrId: string,
): Promise<CollectionRow> {
  const collections = await requestJson<CollectionRow[]>(
    config,
    "/rest/v1/collections?select=id,name,created_at",
    {
      accessToken: session.accessToken,
    },
  );
  const collection = collections.find((item) => item.id === nameOrId || item.name === nameOrId);
  if (!collection) {
    throw new Error(`找不到收藏夹：${nameOrId}`);
  }
  return collection;
}

async function findOneBookmark(
  config: AppConfig,
  session: Session,
  idOrPrefix: string,
): Promise<BookmarkRow> {
  const idFilter = encodeURIComponent(`${idOrPrefix}*`);
  const bookmarks = await requestJson<BookmarkRow[]>(
    config,
    `/rest/v1/bookmarks?select=*&id=like.${idFilter}&order=created_at.desc&limit=5`,
    {
      accessToken: session.accessToken,
    },
  );
  if (bookmarks.length === 0) {
    throw new Error(`找不到文章：${idOrPrefix}`);
  }
  if (bookmarks.length > 1) {
    throw new Error(`匹配到多篇文章，请使用更长 ID：${bookmarks.map((item) => item.id).join(", ")}`);
  }
  return bookmarks[0];
}

function formatBookmarkForAgent(
  bookmark: BookmarkRow,
  collectionIdsByBookmarkId: Map<string, string[]>,
  collectionById: Map<string, CollectionRow>,
  includeContent: boolean,
) {
  const collectionIds = collectionIdsByBookmarkId.get(bookmark.id) ?? [bookmark.collection_id];
  return {
    id: bookmark.id,
    short_id: bookmark.id.slice(0, 8),
    title: bookmark.title,
    account_name: bookmark.account_name,
    original_url: bookmark.original_url,
    normalized_url: bookmark.normalized_url,
    cover_url: bookmark.cover_url,
    collections: collectionIds.map((id) => ({
      id,
      name: collectionById.get(id)?.name ?? id,
    })),
    published_at: bookmark.published_at,
    created_at: bookmark.created_at,
    parse_status: bookmark.parse_status,
    summary_status: bookmark.summary_status ?? "pending",
    content_quality_status: bookmark.content_quality_status ?? "unchecked",
    ai_summary: bookmark.ai_summary ?? null,
    ai_key_points: bookmark.ai_key_points ?? [],
    ai_topics: bookmark.ai_topics ?? [],
    ai_links: bookmark.ai_links ?? [],
    content_text: includeContent ? bookmark.content_text ?? "" : undefined,
  };
}

function validateContentQuality(bookmark: BookmarkRow): { ok: true } | { ok: false; reason: string } {
  if (bookmark.parse_status !== "success") {
    return { ok: false, reason: `文章解析状态不是 success：${bookmark.parse_status}` };
  }
  const text = bookmark.content_text?.trim() ?? "";
  if (text.length < 300) {
    return { ok: false, reason: "正文过短，可能解析失败" };
  }
  if (/验证|环境异常|请在微信客户端打开|访问过于频繁|service unavailable/i.test(text.slice(0, 800))) {
    return { ok: false, reason: "正文疑似错误页或反爬提示" };
  }
  const chineseChars = text.match(/[\u4e00-\u9fff]/g)?.length ?? 0;
  if (chineseChars < 80) {
    return { ok: false, reason: "正文中文内容过少，可能不是文章正文" };
  }
  const uniqueChars = new Set(text.slice(0, 1_000)).size;
  if (uniqueChars < 30) {
    return { ok: false, reason: "正文重复度异常，可能解析失败" };
  }
  return { ok: true };
}

async function generateSummary(
  config: ModelConfig,
  bookmark: BookmarkRow,
): Promise<SummaryResult> {
  const content = (bookmark.content_text ?? "").slice(0, MAX_SUMMARY_INPUT_CHARS);
  const response = await fetch(`${config.baseUrl}/chat/completions`, {
    method: "POST",
    headers: {
      Authorization: `Bearer ${config.apiKey}`,
      "Content-Type": "application/json",
    },
    body: JSON.stringify({
      model: config.modelName,
      messages: [
        {
          role: "system",
          content:
            "你是个人知识库的文章整理助手。你必须只基于用户提供的文章内容总结，不要编造。输出必须是严格 JSON，不要 Markdown。",
        },
        {
          role: "user",
          content: `请为下面这篇微信公众号文章生成面向 AI Agent 检索的结构化摘要。\n\n要求：\n- summary: 120 字以内，一句话说明文章讲什么\n- key_points: 3 到 6 条关键要点\n- topics: 3 到 8 个检索关键词，优先包含项目名、技术名、适用场景\n- links: 提取正文中明确出现的重要链接，最多 8 个\n- 如果正文看起来不是正常文章正文，请在 summary 中明确说明“正文疑似解析异常”\n\n标题：${bookmark.title}\n公众号：${bookmark.account_name ?? "未知"}\n原文链接：${bookmark.original_url}\n\n正文：\n${content}`,
        },
      ],
      temperature: 0.2,
    }),
  });
  const text = await response.text();
  if (!response.ok) {
    throw new Error(parseErrorMessage(text) ?? `模型请求失败：HTTP ${response.status}`);
  }
  const json = JSON.parse(text) as Record<string, unknown>;
  const choices = json.choices as Array<{ message?: { content?: string } }> | undefined;
  const contentText = choices?.[0]?.message?.content;
  if (!contentText) {
    throw new Error("模型响应缺少 message.content");
  }
  const parsed = parseModelJson(contentText) as Partial<SummaryResult>;
  if (!parsed.summary || typeof parsed.summary !== "string") {
    throw new Error("模型输出缺少 summary");
  }
  return {
    summary: parsed.summary.trim(),
    key_points: normalizeStringArray(parsed.key_points).slice(0, 8),
    topics: normalizeStringArray(parsed.topics).slice(0, 12),
    links: normalizeStringArray(parsed.links).slice(0, 12),
  };
}

function parseModelJson(content: string): unknown {
  const trimmed = content.trim();
  if (trimmed.startsWith("```")) {
    const fenced = trimmed
      .replace(/^```(?:json)?\s*/i, "")
      .replace(/\s*```$/, "")
      .trim();
    return JSON.parse(fenced);
  }
  const start = trimmed.indexOf("{");
  const end = trimmed.lastIndexOf("}");
  if (start >= 0 && end > start) {
    return JSON.parse(trimmed.slice(start, end + 1));
  }
  return JSON.parse(trimmed);
}

function normalizeStringArray(value: unknown): string[] {
  if (!Array.isArray(value)) {
    return [];
  }
  return value
    .map((item) => String(item).trim())
    .filter(Boolean);
}

async function patchBookmark(
  config: AppConfig,
  session: Session,
  bookmarkId: string,
  body: Record<string, unknown>,
) {
  await requestJson(config, `/rest/v1/bookmarks?id=eq.${encodeURIComponent(bookmarkId)}`, {
    method: "PATCH",
    accessToken: session.accessToken,
    body,
    prefer: "return=minimal",
  });
}

async function requireSession(config: AppConfig): Promise<Session> {
  const session = await loadSession();
  if (!session) {
    throw new Error("请先运行 mazhu login。");
  }
  if (!needsRefresh(session)) {
    return session;
  }

  return refreshSessionWithLock(config);
}

async function refreshSessionWithLock(config: AppConfig): Promise<Session> {
  for (let attempt = 0; attempt < 80; attempt += 1) {
    if (await tryAcquireSessionLock()) {
      try {
        const latest = await loadSession();
        if (!latest) {
          throw new Error("请先运行 mazhu login。");
        }
        if (!needsRefresh(latest)) {
          return latest;
        }

        const response = await requestJson(config, "/auth/v1/token?grant_type=refresh_token", {
          method: "POST",
          body: {
            refresh_token: latest.refreshToken,
          },
        }).catch((error) => {
          const message = error instanceof Error ? error.message : String(error);
          if (/invalid refresh token|already used|refresh token/i.test(message)) {
            throw new Error("登录会话已失效，请重新运行 mazhu login。");
          }
          throw error;
        });
        const refreshed = parseSession(response);
        await saveSession(refreshed);
        return refreshed;
      } finally {
        await releaseSessionLock();
      }
    }

    await delay(250);
    const latest = await loadSession();
    if (latest && !needsRefresh(latest)) {
      return latest;
    }
  }

  throw new Error("等待登录会话刷新超时，请稍后重试。");
}

async function tryAcquireSessionLock(): Promise<boolean> {
  await mkdir(MAZHU_DIR, { recursive: true });
  try {
    await writeFile(SESSION_LOCK_FILE, `${process.pid}\n${Date.now()}\n`, {
      flag: "wx",
      mode: 0o600,
    });
    return true;
  } catch (error) {
    if (!isNodeError(error) || error.code !== "EEXIST") {
      throw error;
    }
  }

  const lockStat = await stat(SESSION_LOCK_FILE).catch(() => null);
  if (lockStat && Date.now() - lockStat.mtimeMs > SESSION_LOCK_STALE_MS) {
    await unlink(SESSION_LOCK_FILE).catch(() => undefined);
  }
  return false;
}

async function releaseSessionLock() {
  await unlink(SESSION_LOCK_FILE).catch(() => undefined);
}

function delay(ms: number): Promise<void> {
  return new Promise((resolveDelay) => {
    setTimeout(resolveDelay, ms);
  });
}

function isNodeError(error: unknown): error is NodeJS.ErrnoException {
  return error instanceof Error && "code" in error;
}


function needsRefresh(session: Session) {
  return session.expiresAtEpochSeconds <= Math.floor(Date.now() / 1000) + 60;
}

function parseSession(value: unknown): Session {
  const json = value as Record<string, unknown>;
  const user = json.user as Record<string, unknown> | undefined;
  if (!user || typeof json.access_token !== "string" || typeof json.refresh_token !== "string") {
    throw new Error("Supabase 登录响应格式异常。");
  }
  return {
    accessToken: json.access_token,
    refreshToken: json.refresh_token,
    userId: String(user.id),
    email: typeof user.email === "string" ? user.email : undefined,
    expiresAtEpochSeconds: Math.floor(Date.now() / 1000) + Number(json.expires_in ?? 3600),
  };
}

async function loadSession(): Promise<Session | null> {
  if (!existsSync(SESSION_FILE)) {
    return null;
  }
  return JSON.parse(await readFile(SESSION_FILE, "utf8")) as Session;
}

async function saveSession(session: Session) {
  await mkdir(dirname(SESSION_FILE), { recursive: true });
  await writeFile(SESSION_FILE, JSON.stringify(session, null, 2));
  await chmod(SESSION_FILE, 0o600);
}

async function loadModelConfig(required: true): Promise<ModelConfig>;
async function loadModelConfig(required: false): Promise<ModelConfig | null>;
async function loadModelConfig(required: boolean): Promise<ModelConfig | null> {
  const envBaseUrl = process.env.MAZHU_MODEL_BASE_URL;
  const envApiKey = process.env.MAZHU_MODEL_API_KEY;
  const envModelName = process.env.MAZHU_MODEL_NAME;
  if (envBaseUrl && envApiKey && envModelName) {
    return {
      baseUrl: envBaseUrl.replace(/\/$/, ""),
      apiKey: envApiKey,
      modelName: envModelName,
    };
  }
  if (!existsSync(MODEL_CONFIG_FILE)) {
    if (required) {
      throw new Error("尚未配置模型。请先运行 mazhu config set。");
    }
    return null;
  }
  return JSON.parse(await readFile(MODEL_CONFIG_FILE, "utf8")) as ModelConfig;
}

async function saveModelConfig(config: ModelConfig) {
  await mkdir(dirname(MODEL_CONFIG_FILE), { recursive: true });
  await writeFile(MODEL_CONFIG_FILE, JSON.stringify(config, null, 2));
  await chmod(MODEL_CONFIG_FILE, 0o600);
}

async function requestJson<T = unknown>(
  config: AppConfig,
  path: string,
  options: {
    method?: string;
    accessToken?: string;
    body?: unknown;
    prefer?: string;
  } = {},
): Promise<T> {
  const headers: Record<string, string> = {
    apikey: config.publishableKey,
    Authorization: `Bearer ${options.accessToken ?? config.publishableKey}`,
    Accept: "application/json",
    "Content-Type": "application/json",
  };
  if (options.prefer) {
    headers.Prefer = options.prefer;
  }
  const response = await fetch(`${config.supabaseUrl}${path}`, {
    method: options.method ?? "GET",
    headers,
    body: options.body === undefined ? undefined : JSON.stringify(options.body),
  });
  const text = await response.text();
  if (!response.ok) {
    throw new Error(parseErrorMessage(text) ?? `HTTP ${response.status}`);
  }
  return text ? (JSON.parse(text) as T) : ([] as T);
}

function parseErrorMessage(text: string) {
  if (!text) {
    return undefined;
  }
  try {
    const json = JSON.parse(text) as Record<string, unknown>;
    return String(json.message ?? json.msg ?? json.error_description ?? json.error ?? text);
  } catch {
    return text.slice(0, 300);
  }
}

function printJson(value: unknown) {
  console.log(JSON.stringify(value, null, 2));
}

function parseFlags(args: string[]) {
  const flags: Record<string, string> = {};
  for (let index = 0; index < args.length; index += 1) {
    const arg = args[index];
    if (arg === "--") {
      continue;
    }
    if (!arg.startsWith("--")) {
      continue;
    }
    const value = args[index + 1];
    if (!value || value.startsWith("--")) {
      throw new Error(`参数 ${arg} 缺少取值。`);
    }
    flags[arg.slice(2)] = value;
    index += 1;
  }
  return flags;
}

async function questionWithExistingDefault(
  rl: ReturnType<typeof createInterface>,
  label: string,
  existingValue?: string,
): Promise<string> {
  const answer = (await rl.question(`${label}${existingValue ? ` [${existingValue}]` : ""}：`)).trim();
  return answer || existingValue || "";
}

async function questionRequiredHidden(
  rl: ReturnType<typeof createInterface>,
  label: string,
): Promise<string> {
  const answer = await questionHidden(rl, `${label}：`);
  if (!answer) {
    throw new Error(`${label} 不能为空。`);
  }
  return answer;
}

function maskSecret(secret: string) {
  if (secret.length <= 8) {
    return "********";
  }
  return `${secret.slice(0, 4)}...${secret.slice(-4)}`;
}

async function questionHidden(
  rl: ReturnType<typeof createInterface>,
  prompt: string,
): Promise<string> {
  const mutableOutput = output as typeof output & {
    muted?: boolean;
  };
  const originalWrite = mutableOutput.write.bind(mutableOutput);
  mutableOutput.write = ((chunk: string | Uint8Array, encoding?: BufferEncoding, callback?: () => void) => {
    if (mutableOutput.muted) {
      return true;
    }
    return originalWrite(chunk, encoding, callback);
  }) as typeof output.write;

  mutableOutput.muted = false;
  const question = rl.question(prompt);
  mutableOutput.muted = true;
  const answer = await question;
  mutableOutput.muted = false;
  originalWrite("\n");
  mutableOutput.write = originalWrite as typeof output.write;
  return answer;
}

function printHelp() {
  console.log(`mazhu CLI

用法：
  mazhu login                              登录 Supabase
  mazhu logout                             清除本机会话
  mazhu whoami                             查看当前登录账号
  mazhu config set                         配置 OpenAI-compatible 摘要模型
  mazhu config show                        查看模型配置
  mazhu collections                        列出收藏夹
  mazhu collections --json                 以 JSON 列出收藏夹，适合 agent 使用
  mazhu search [关键词]                    搜索文章标题、公众号、链接、收藏夹和摘要
  mazhu search GitHub --json               以 JSON 返回搜索结果和摘要字段
  mazhu search UI --collection UI/UX       在指定收藏夹内搜索
  mazhu read <文章ID前缀>                  读取文章全文
  mazhu read <文章ID前缀> --json           以 JSON 返回全文、摘要和引用信息
  mazhu summarize --all                    为未摘要文章生成摘要
  mazhu summarize <文章ID前缀>             为单篇文章生成摘要
  mazhu summarize --collection UI/UX       为指定收藏夹生成摘要
  mazhu summarize --all --force            重新生成摘要

配置：
  Supabase 优先读取 MAZHU_SUPABASE_URL 和 MAZHU_SUPABASE_PUBLISHABLE_KEY，未设置时读取 android/local.properties。
  模型配置保存在 ~/.mazhu/config.json。
  也可以设置 MAZHU_MODEL_BASE_URL、MAZHU_MODEL_API_KEY、MAZHU_MODEL_NAME。
`);
}

await main();

alter table public.bookmarks
  add column if not exists ai_summary text,
  add column if not exists ai_key_points jsonb,
  add column if not exists ai_topics text[],
  add column if not exists ai_links jsonb,
  add column if not exists summary_model text,
  add column if not exists summarized_at timestamptz,
  add column if not exists summary_status text not null default 'pending' check (summary_status in ('pending', 'success', 'failed', 'skipped')),
  add column if not exists summary_error text,
  add column if not exists content_quality_status text not null default 'unchecked' check (content_quality_status in ('unchecked', 'ok', 'suspect')),
  add column if not exists content_quality_error text;

create index if not exists bookmarks_summary_status_idx
  on public.bookmarks(user_id, summary_status, created_at desc);

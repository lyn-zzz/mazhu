create table public.collections (
  user_id uuid not null references auth.users(id) on delete cascade,
  id text not null,
  name text not null check (length(trim(name)) between 1 and 80),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  primary key (user_id, id),
  unique (user_id, name)
);

create table public.bookmarks (
  user_id uuid not null references auth.users(id) on delete cascade,
  id text not null,
  original_url text not null,
  normalized_url text not null,
  title text not null,
  account_name text,
  cover_url text,
  published_at timestamptz,
  content_text text,
  collection_id text not null,
  parse_status text not null check (parse_status in ('pending', 'processing', 'success', 'failed')),
  parse_error text,
  created_at timestamptz not null,
  updated_at timestamptz not null default now(),
  primary key (user_id, id),
  unique (user_id, normalized_url),
  foreign key (user_id, collection_id)
    references public.collections(user_id, id)
    on update cascade
    on delete restrict
);

create index bookmarks_user_collection_created_idx
  on public.bookmarks(user_id, collection_id, created_at desc);

create or replace function public.set_updated_at()
returns trigger
language plpgsql
set search_path = ''
as $$
begin
  new.updated_at = now();
  return new;
end;
$$;

create trigger collections_set_updated_at
before update on public.collections
for each row execute function public.set_updated_at();

create trigger bookmarks_set_updated_at
before update on public.bookmarks
for each row execute function public.set_updated_at();

create or replace function public.create_default_collection_for_user()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
begin
  insert into public.collections(user_id, id, name)
  values (new.id, 'default', '默认收藏夹')
  on conflict (user_id, id) do nothing;
  return new;
end;
$$;

revoke execute on function public.create_default_collection_for_user() from public;
revoke execute on function public.create_default_collection_for_user() from anon;
revoke execute on function public.create_default_collection_for_user() from authenticated;

create trigger create_default_collection_after_signup
after insert on auth.users
for each row execute function public.create_default_collection_for_user();

alter table public.collections enable row level security;
alter table public.bookmarks enable row level security;

create policy collections_select_own
on public.collections for select
to authenticated
using ((select auth.uid()) = user_id);

create policy collections_insert_own
on public.collections for insert
to authenticated
with check ((select auth.uid()) = user_id);

create policy collections_update_own
on public.collections for update
to authenticated
using ((select auth.uid()) = user_id)
with check ((select auth.uid()) = user_id);

create policy collections_delete_own
on public.collections for delete
to authenticated
using ((select auth.uid()) = user_id and id <> 'default');

create policy bookmarks_select_own
on public.bookmarks for select
to authenticated
using ((select auth.uid()) = user_id);

create policy bookmarks_insert_own
on public.bookmarks for insert
to authenticated
with check ((select auth.uid()) = user_id);

create policy bookmarks_update_own
on public.bookmarks for update
to authenticated
using ((select auth.uid()) = user_id)
with check ((select auth.uid()) = user_id);

create policy bookmarks_delete_own
on public.bookmarks for delete
to authenticated
using ((select auth.uid()) = user_id);

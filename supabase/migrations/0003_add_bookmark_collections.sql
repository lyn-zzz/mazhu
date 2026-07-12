create table if not exists public.bookmark_collections (
  user_id uuid not null references auth.users(id) on delete cascade,
  bookmark_id text not null,
  collection_id text not null,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  primary key (user_id, bookmark_id, collection_id),
  foreign key (user_id, bookmark_id)
    references public.bookmarks(user_id, id)
    on update cascade
    on delete cascade,
  foreign key (user_id, collection_id)
    references public.collections(user_id, id)
    on update cascade
    on delete cascade
);

insert into public.bookmark_collections(
  user_id,
  bookmark_id,
  collection_id,
  created_at
)
select
  user_id,
  id,
  collection_id,
  created_at
from public.bookmarks
on conflict (user_id, bookmark_id, collection_id) do nothing;

create index if not exists bookmark_collections_user_collection_idx
  on public.bookmark_collections(user_id, collection_id, created_at desc);

create trigger bookmark_collections_set_updated_at
before update on public.bookmark_collections
for each row execute function public.set_updated_at();

alter table public.bookmark_collections enable row level security;

create policy bookmark_collections_select_own
on public.bookmark_collections for select
to authenticated
using ((select auth.uid()) = user_id);

create policy bookmark_collections_insert_own
on public.bookmark_collections for insert
to authenticated
with check ((select auth.uid()) = user_id);

create policy bookmark_collections_update_own
on public.bookmark_collections for update
to authenticated
using ((select auth.uid()) = user_id)
with check ((select auth.uid()) = user_id);

create policy bookmark_collections_delete_own
on public.bookmark_collections for delete
to authenticated
using ((select auth.uid()) = user_id);

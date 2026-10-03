-- User-owned Kallior statistics. Run this in the Supabase SQL Editor before
-- shipping a client that writes user_id. This migration deliberately stops if
-- legacy rows exist: their owner cannot be inferred safely.

begin;

alter table public.daily_stats add column if not exists user_id uuid;
alter table public.shadow_stats add column if not exists user_id uuid;

do $$
begin
  if exists (select 1 from public.daily_stats where user_id is null)
     or exists (select 1 from public.shadow_stats where user_id is null) then
    raise exception
      'Existing statistics have no owner. Backfill user_id manually before applying this migration.';
  end if;
end $$;

alter table public.daily_stats
  alter column user_id set not null,
  add constraint daily_stats_user_id_fkey
    foreign key (user_id) references auth.users(id) on delete cascade;
alter table public.shadow_stats
  alter column user_id set not null,
  add constraint shadow_stats_user_id_fkey
    foreign key (user_id) references auth.users(id) on delete cascade;

-- The application model establishes date as the natural identity for each
-- user's daily and shadow aggregates. Existing surrogate keys are preserved.
alter table public.daily_stats
  add constraint daily_stats_user_date_key unique (user_id, date);
alter table public.shadow_stats
  add constraint shadow_stats_user_date_key unique (user_id, date);

alter table public.daily_stats enable row level security;
alter table public.shadow_stats enable row level security;

drop policy if exists "Users read own daily stats" on public.daily_stats;
drop policy if exists "Users insert own daily stats" on public.daily_stats;
drop policy if exists "Users update own daily stats" on public.daily_stats;
drop policy if exists "Users delete own daily stats" on public.daily_stats;
drop policy if exists "Users read own shadow stats" on public.shadow_stats;
drop policy if exists "Users insert own shadow stats" on public.shadow_stats;
drop policy if exists "Users update own shadow stats" on public.shadow_stats;
drop policy if exists "Users delete own shadow stats" on public.shadow_stats;

create policy "Users read own daily stats" on public.daily_stats
  for select to authenticated using ((select auth.uid()) = user_id);
create policy "Users insert own daily stats" on public.daily_stats
  for insert to authenticated with check ((select auth.uid()) = user_id);
create policy "Users update own daily stats" on public.daily_stats
  for update to authenticated using ((select auth.uid()) = user_id)
  with check ((select auth.uid()) = user_id);
create policy "Users delete own daily stats" on public.daily_stats
  for delete to authenticated using ((select auth.uid()) = user_id);

create policy "Users read own shadow stats" on public.shadow_stats
  for select to authenticated using ((select auth.uid()) = user_id);
create policy "Users insert own shadow stats" on public.shadow_stats
  for insert to authenticated with check ((select auth.uid()) = user_id);
create policy "Users update own shadow stats" on public.shadow_stats
  for update to authenticated using ((select auth.uid()) = user_id)
  with check ((select auth.uid()) = user_id);
create policy "Users delete own shadow stats" on public.shadow_stats
  for delete to authenticated using ((select auth.uid()) = user_id);

grant select, insert, update, delete on public.daily_stats to authenticated;
grant select, insert, update, delete on public.shadow_stats to authenticated;

commit;

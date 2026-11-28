-- 근무 1회. id는 앱이 근무 시작 때 만든 UUID(재시도해도 중복 없음)
create table public.shifts (
  id uuid primary key,
  worker_id uuid not null default auth.uid() references auth.users (id) on delete cascade,
  zone_id text not null references public.zones (grid_id),
  started_at timestamptz not null,
  ended_at timestamptz not null check (ended_at >= started_at),
  created_at timestamptz not null default now()
);

-- 근무 종료 때 입력한 수거량. 근무당 1행. 봉투 수는 19~20/20~21/21~22/22~23시 순서
create table public.collections (
  shift_id uuid primary key references public.shifts (id) on delete cascade,
  bags_by_hour smallint[] not null check (
    cardinality(bags_by_hour) = 4
    and array_position(bags_by_hour, null) is null
    and 0 <= all (bags_by_hour) and 50 >= all (bags_by_hour)
  ),
  general_kg numeric(6, 2) not null check (general_kg between 0 and 500),
  recycle_kg numeric(6, 2) not null check (recycle_kg between 0 and 500),
  butts_g numeric(8, 1) not null check (butts_g between 0 and 100000),
  note text check (char_length(note) <= 1000),
  created_at timestamptz not null default now()
);

alter table public.shifts enable row level security;
alter table public.collections enable row level security;

-- 워커 역할인 본인만 자기 근무를 쓰고 읽음
create policy "shifts insert own" on public.shifts for insert to authenticated
  with check (
    worker_id = (select auth.uid())
    and exists (select 1 from public.profiles p where p.user_id = (select auth.uid()) and p.role = 'worker')
  );
create policy "shifts read own" on public.shifts for select to authenticated
  using (worker_id = (select auth.uid()));

create policy "collections insert own" on public.collections for insert to authenticated
  with check (exists (select 1 from public.shifts s where s.id = shift_id and s.worker_id = (select auth.uid())));
create policy "collections read own" on public.collections for select to authenticated
  using (exists (select 1 from public.shifts s where s.id = shift_id and s.worker_id = (select auth.uid())));

grant select, insert on public.shifts, public.collections to authenticated;

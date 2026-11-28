-- 근무 중 위치(동의 후 근무 중에만). 부정 방지·구역 확인용
create table public.track_points (
  id bigint generated always as identity primary key,
  shift_id uuid not null references public.shifts (id) on delete cascade,
  recorded_at timestamptz not null,
  lat double precision not null check (lat between -90 and 90),
  lng double precision not null check (lng between -180 and 180),
  accuracy_m real check (accuracy_m >= 0)
);
create index track_points_shift_idx on public.track_points (shift_id, recorded_at);
alter table public.track_points enable row level security;

-- 워커 본인 근무에만 쓰고 읽음
create policy "track_points insert own" on public.track_points for insert to authenticated
  with check (exists (select 1 from public.shifts s where s.id = shift_id and s.worker_id = (select auth.uid())));
create policy "track_points read own" on public.track_points for select to authenticated
  using (exists (select 1 from public.shifts s where s.id = shift_id and s.worker_id = (select auth.uid())));
grant select, insert on public.track_points to authenticated;

-- 체크인 대상 가게. qr_code는 가게 QR에 담긴 값(URL이면 마지막 경로)
create table public.shops (
  id bigint generated always as identity primary key,
  name text not null,
  lat double precision not null check (lat between -90 and 90),
  lng double precision not null check (lng between -180 and 180),
  qr_code text not null unique
);
alter table public.shops enable row level security;
create policy "shops read" on public.shops for select to anon, authenticated using (true);
grant select on public.shops to anon, authenticated;

-- 예시 가게 5곳(이태원역 주변 좌표). 시범 가게가 정해지면 갈아 끼운다
insert into public.shops (name, lat, lng, qr_code) values
  ('[가게명 1]', 37.53480, 126.99410, 'NC-SHOP-001'),
  ('[가게명 2]', 37.53430, 126.99300, 'NC-SHOP-002'),
  ('[가게명 3]', 37.53510, 126.99560, 'NC-SHOP-003'),
  ('[가게명 4]', 37.53390, 126.99480, 'NC-SHOP-004'),
  ('[가게명 5]', 37.53550, 126.99350, 'NC-SHOP-005');

-- 근무 행은 첫 체크인 때 먼저 생기므로 종료 시각은 비어 있을 수 있음. 근무 종료 때 upsert로 채움
alter table public.shifts alter column ended_at drop not null;
create policy "shifts update own" on public.shifts for update to authenticated
  using (worker_id = (select auth.uid()))
  with check (worker_id = (select auth.uid()));
grant update on public.shifts to authenticated;

create table public.checkins (
  id bigint generated always as identity primary key,
  shift_id uuid not null references public.shifts (id) on delete cascade,
  shop_id bigint not null references public.shops (id),
  checked_at timestamptz not null default now(),
  lat double precision not null check (lat between -90 and 90),
  lng double precision not null check (lng between -180 and 180),
  distance_m integer not null check (distance_m between 0 and 50),
  unique (shift_id, shop_id)
);
create index checkins_shop_id_idx on public.checkins (shop_id);
alter table public.checkins enable row level security;
-- 쓰기는 check_in 함수로만(거리 판정을 서버에서 강제)
create policy "checkins read own" on public.checkins for select to authenticated
  using (exists (select 1 from public.shifts s where s.id = shift_id and s.worker_id = (select auth.uid())));
grant select on public.checkins to authenticated;

-- 체크인: 워커 확인 → 근무 행 보장 → QR로 가게 조회 → 50m 안이면 기록. 모르는 QR이면 빈 결과
create function public.check_in(
  p_shift_id uuid,
  p_zone_id text,
  p_started_at timestamptz,
  p_qr_code text,
  p_lat double precision,
  p_lng double precision
) returns table (shop_name text, distance_m integer, checked_in boolean)
language plpgsql
security definer
set search_path = ''
as $$
#variable_conflict use_column
declare
  v_uid uuid := auth.uid();
  v_shop public.shops;
  v_dist integer;
begin
  if v_uid is null or not exists (select 1 from public.profiles where user_id = v_uid and role = 'worker') then
    raise exception 'worker only' using errcode = '42501';
  end if;
  if p_lat not between -90 and 90 or p_lng not between -180 and 180 then
    raise exception 'invalid location' using errcode = '22023';
  end if;

  insert into public.shifts (id, worker_id, zone_id, started_at)
  values (p_shift_id, v_uid, p_zone_id, p_started_at)
  on conflict (id) do nothing;
  if not exists (select 1 from public.shifts where id = p_shift_id and worker_id = v_uid) then
    raise exception 'not your shift' using errcode = '42501';
  end if;

  select * into v_shop from public.shops where qr_code = p_qr_code;
  if not found then
    return;
  end if;

  -- 하버사인 거리(m)
  v_dist := round(2 * 6371000 * asin(sqrt(
    power(sin(radians(v_shop.lat - p_lat) / 2), 2)
    + cos(radians(p_lat)) * cos(radians(v_shop.lat)) * power(sin(radians(v_shop.lng - p_lng) / 2), 2)
  )));

  if v_dist <= 50 then
    insert into public.checkins (shift_id, shop_id, lat, lng, distance_m)
    values (p_shift_id, v_shop.id, p_lat, p_lng, v_dist)
    on conflict (shift_id, shop_id) do nothing;
  end if;

  return query select v_shop.name, v_dist, v_dist <= 50;
end;
$$;

revoke execute on function public.check_in(uuid, text, timestamptz, text, double precision, double precision) from public, anon;
grant execute on function public.check_in(uuid, text, timestamptz, text, double precision, double precision) to authenticated;

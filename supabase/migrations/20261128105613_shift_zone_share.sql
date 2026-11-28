-- 칸 중심 좌표(analysis/zones.py 결과). 250m 칸이라 경계는 중심 ±125m
alter table public.zones add column lat double precision, add column lng double precision;

update public.zones z set lat = v.lat, lng = v.lng from (values ('G00', 37.540346, 126.989206), ('G01', 37.540358, 126.992035), ('G02', 37.54037, 126.994865), ('G03', 37.540382, 126.997695), ('G04', 37.540394, 127.000525), ('G05', 37.540406, 127.003355), ('G06', 37.538093, 126.989221), ('G07', 37.538105, 126.992051), ('G08', 37.538117, 126.99488), ('G09', 37.538129, 126.99771), ('G10', 37.538141, 127.00054), ('G11', 37.538153, 127.00337), ('G12', 37.535839, 126.989236), ('G13', 37.535852, 126.992066), ('G14', 37.535864, 126.994896), ('G15', 37.535876, 126.997725), ('G16', 37.535888, 127.000555), ('G17', 37.5359, 127.003384), ('G18', 37.533586, 126.989252), ('G19', 37.533598, 126.992081), ('G20', 37.53361, 126.994911), ('G21', 37.533623, 126.99774), ('G22', 37.533635, 127.00057), ('G23', 37.533646, 127.003399), ('G24', 37.531333, 126.989267), ('G25', 37.531345, 126.992097), ('G26', 37.531357, 126.994926), ('G27', 37.531369, 126.997755), ('G28', 37.531381, 127.000585), ('G29', 37.531393, 127.003414), ('G30', 37.529079, 126.989283), ('G31', 37.529092, 126.992112), ('G32', 37.529104, 126.994941), ('G33', 37.529116, 126.997771), ('G34', 37.529128, 127.0006), ('G35', 37.52914, 127.003429)) as v(id, lat, lng) where z.grid_id = v.id;

-- 근무 하나의 GPS 기록 중 배정 구역 칸 안에 든 수(실증 지표 '부정 의심 비율'의 바탕).
-- security invoker라 RLS 그대로: 워커 본인 근무의 기록만 셈
create or replace function public.shift_zone_share(p_shift_id uuid)
returns table (total int, inside int)
language sql
stable
security invoker
set search_path = ''
as $$
  select count(*)::int,
         count(*) filter (
           where abs(tp.lat - z.lat) <= 125 / 111320.0
             and abs(tp.lng - z.lng) <= 125 / (111320.0 * cos(radians(z.lat)))
         )::int
  from public.track_points tp
  join public.shifts s on s.id = tp.shift_id
  join public.zones z on z.grid_id = s.zone_id
  where tp.shift_id = p_shift_id
$$;

revoke execute on function public.shift_zone_share(uuid) from public, anon;
grant execute on function public.shift_zone_share(uuid) to authenticated;

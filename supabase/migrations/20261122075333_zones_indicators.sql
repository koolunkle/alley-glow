alter table public.zones
  add column night_pop integer check (night_pop >= 0),
  add column weekday_pop integer check (weekday_pop >= 0),
  add column bin_dist_m integer check (bin_dist_m >= 0),
  add column night_sales bigint check (night_sales >= 0);
comment on column public.zones.night_pop is '금·토 19~23시 평균 생활인구(명)';
comment on column public.zones.weekday_pop is '평일 19~23시 평균 생활인구(명)';
comment on column public.zones.bin_dist_m is '칸 중심에서 가장 가까운 가로휴지통까지 거리(m)';
comment on column public.zones.night_sales is '21~24시 추정매출, 상권 영역 겹침 면적 비율 배분(원, 2025년 분기 평균)';

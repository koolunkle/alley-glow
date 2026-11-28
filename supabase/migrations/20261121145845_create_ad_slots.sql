-- 근무 중 노출할 가게 광고 슬롯. 시각은 시 단위, 자정 이후는 24~30으로 표기
create table public.ad_slots (
  id bigint generated always as identity primary key,
  shop_name text not null,
  start_hour smallint not null check (start_hour between 0 and 29),
  end_hour smallint not null check (end_hour between 1 and 30 and end_hour > start_hour),
  placement text not null
);
alter table public.ad_slots enable row level security;
create policy "ad_slots read" on public.ad_slots for select to anon, authenticated using (true);
grant select on public.ad_slots to anon, authenticated;

insert into public.ad_slots (shop_name, start_hour, end_hour, placement) values
  ('[가게명 1]', 19, 20, '등판 조끼 · 수거통 옆면'),
  ('[가게명 2]', 20, 21, '등판 조끼 · 수거통 옆면'),
  ('[가게명 3]', 21, 22, '등판 조끼 · 수거통 옆면'),
  ('[가게명 4]', 22, 23, '등판 조끼 · 수거통 옆면'),
  ('[가게명 5]', 23, 24, '등판 조끼 · 수거통 옆면');

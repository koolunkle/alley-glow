create table public.zones (
  grid_id text primary key,
  name text not null,
  score real not null check (score between 0 and 1)
);

alter table public.zones enable row level security;

-- 지도 점수는 공개 정보라 누구나 읽기만 허용. 쓰기는 대시보드/서비스 키로만.
create policy "zones are readable by everyone"
  on public.zones for select
  to anon, authenticated
  using (true);

insert into public.zones (grid_id, name, score)
select format('G%s', lpad((i - 1)::text, 2, '0')), format('격자 %s', i), s
from unnest(array[
  0.1, 0.3, 0.3, 0.1, 0.1, 0.0,
  0.3, 0.6, 0.6, 0.3, 0.1, 0.0,
  0.3, 0.9, 0.9, 0.6, 0.3, 0.1,
  0.1, 0.6, 0.9, 0.8, 0.3, 0.1,
  0.1, 0.3, 0.6, 0.6, 0.3, 0.0,
  0.0, 0.1, 0.3, 0.3, 0.1, 0.0
]::real[]) with ordinality as t(s, i);

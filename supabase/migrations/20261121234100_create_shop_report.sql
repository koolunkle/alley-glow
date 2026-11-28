-- 상인 계정의 가게
alter table public.profiles add column shop_id bigint references public.shops (id);
update public.profiles set shop_id = (select id from public.shops where qr_code = 'NC-SHOP-001')
where role = 'shop';
alter table public.profiles add constraint profiles_shop_role check ((role = 'shop') = (shop_id is not null));
create index profiles_shop_id_idx on public.profiles (shop_id);

-- "클린 보고 왔어요" 손님. 상인이 +1로 기록
create table public.guest_visits (
  id bigint generated always as identity primary key,
  shop_id bigint not null references public.shops (id),
  created_at timestamptz not null default now()
);
create index guest_visits_shop_id_idx on public.guest_visits (shop_id, created_at);
alter table public.guest_visits enable row level security;
create policy "guest_visits insert own shop" on public.guest_visits for insert to authenticated
  with check (exists (select 1 from public.profiles p where p.user_id = (select auth.uid()) and p.role = 'shop' and p.shop_id = guest_visits.shop_id));
create policy "guest_visits read own shop" on public.guest_visits for select to authenticated
  using (exists (select 1 from public.profiles p where p.user_id = (select auth.uid()) and p.role = 'shop' and p.shop_id = guest_visits.shop_id));
grant select, insert on public.guest_visits to authenticated;

-- 상인 리포트: 호출한 상인 가게의 이번 달 집계(서울 시각). 상인이 아니면 null
-- 밤 기준 날짜 = 서울 시각 - 6시간(자정 넘은 체크인은 전날 밤)
create function public.shop_report() returns json
language plpgsql
stable
security definer
set search_path = ''
as $$
declare
  v_shop public.shops;
  v_month_start date := date_trunc('month', now() at time zone 'Asia/Seoul')::date;
begin
  select s.* into v_shop
  from public.profiles p join public.shops s on s.id = p.shop_id
  where p.user_id = auth.uid() and p.role = 'shop';
  if not found then
    return null;
  end if;

  return (
    with c as (
      select ch.shift_id,
             ch.checked_at at time zone 'Asia/Seoul' as local_at,
             ((ch.checked_at at time zone 'Asia/Seoul') - interval '6 hours')::date as night
      from public.checkins ch
      where ch.shop_id = v_shop.id
    ),
    month_c as (
      select * from c where night >= v_month_start and night < (v_month_start + interval '1 month')::date
    ),
    nights as (
      select night, count(*) as checkins,
             to_char(min(local_at), 'HH24:MI') as first_at,
             to_char(max(local_at), 'HH24:MI') as last_at
      from c group by night order by night desc limit 5
    )
    select json_build_object(
      'shop_id', v_shop.id,
      'shop_name', v_shop.name,
      'month', extract(month from v_month_start)::int,
      'nights', (select count(distinct night) from month_c),
      'collected_kg', (
        select coalesce(round(sum(co.general_kg + co.recycle_kg + co.butts_g / 1000), 1), 0)
        from public.collections co
        where co.shift_id in (select shift_id from month_c)
      ),
      'guests', (
        select count(*) from public.guest_visits g
        where g.shop_id = v_shop.id
          and (g.created_at at time zone 'Asia/Seoul')::date >= v_month_start
          and (g.created_at at time zone 'Asia/Seoul')::date < (v_month_start + interval '1 month')::date
      ),
      'recent', coalesce((
        select json_agg(json_build_object('night', night, 'checkins', checkins, 'first_at', first_at, 'last_at', last_at) order by night desc)
        from nights
      ), '[]'::json)
    )
  );
end;
$$;

revoke execute on function public.shop_report() from public, anon;
grant execute on function public.shop_report() to authenticated;

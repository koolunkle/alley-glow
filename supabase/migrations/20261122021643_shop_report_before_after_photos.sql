-- 상인이 읽을 수 있는 사진: 자기 가게에 체크인한 근무의 청소 전후 사진만(봉투·저울 사진 제외)
-- 정책 안에서 워커 테이블을 조회하면 상인의 RLS에 막히므로 security definer 함수로 판정
create function public.shop_can_read_photo(p_name text) returns boolean
language sql
stable
security definer
set search_path = ''
as $$
  select exists (
    select 1
    from public.profiles p
    join public.checkins ch on ch.shop_id = p.shop_id
    join public.collections c on c.shift_id = ch.shift_id
    where p.user_id = auth.uid()
      and p.role = 'shop'
      and (p_name = any (c.before_photo_paths) or p_name = any (c.after_photo_paths))
  );
$$;
revoke execute on function public.shop_can_read_photo(text) from public, anon;
grant execute on function public.shop_can_read_photo(text) to authenticated;

create policy "collection photos read by shop" on storage.objects for select to authenticated
  using (bucket_id = 'collection-photos' and public.shop_can_read_photo(name));

-- 리포트의 최근 밤마다 청소 전·후 사진 1장씩(그 밤 이 가게에 체크인한 근무 중 사진이 있는 최신 기록)
create or replace function public.shop_report() returns json
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
             to_char(max(local_at), 'HH24:MI') as last_at,
             array_agg(distinct shift_id) as shift_ids
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
        select json_agg(json_build_object(
                 'night', n.night, 'checkins', n.checkins, 'first_at', n.first_at, 'last_at', n.last_at,
                 'before_photo', ph.before_photo, 'after_photo', ph.after_photo
               ) order by n.night desc)
        from nights n
        left join lateral (
          select co.before_photo_paths[1] as before_photo, co.after_photo_paths[1] as after_photo
          from public.collections co
          where co.shift_id = any (n.shift_ids)
            and (cardinality(co.before_photo_paths) > 0 or cardinality(co.after_photo_paths) > 0)
          order by co.created_at desc
          limit 1
        ) ph on true
      ), '[]'::json)
    )
  );
end;
$$;

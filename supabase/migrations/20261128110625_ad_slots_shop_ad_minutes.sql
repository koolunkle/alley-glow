-- 광고 슬롯을 가게와 연결(지금 예시 행은 가게 이름이 같아 이름으로 연결). 슬롯은 여전히 전 구역 공통
alter table public.ad_slots add column shop_id bigint references public.shops(id);
update public.ad_slots a set shop_id = s.id from public.shops s where s.name = a.shop_name;

-- 상인 리포트에 광고 노출 시간(분) 추가: 이번 달 끝난 근무마다 근무 시간과 이 가게 슬롯이 겹친 시간의 합(근무자 기준)
create or replace function public.shop_report()
 returns json
 language plpgsql
 stable security definer
 set search_path to ''
as $function$
declare
  v_shop public.shops;
  v_month_start date := date_trunc('month', now() at time zone 'Asia/Seoul')::date;
  v_month_end date := (date_trunc('month', now() at time zone 'Asia/Seoul') + interval '1 month')::date;
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
      select * from c where night >= v_month_start and night < v_month_end
    ),
    month_scans as (
      select q.score from public.qr_scans q
      where q.shop_id = v_shop.id
        and (q.scanned_at at time zone 'Asia/Seoul')::date >= v_month_start
        and (q.scanned_at at time zone 'Asia/Seoul')::date < v_month_end
    ),
    month_shifts as (
      select sh.started_at at time zone 'Asia/Seoul' as s_at,
             sh.ended_at at time zone 'Asia/Seoul' as e_at,
             ((sh.started_at at time zone 'Asia/Seoul') - interval '6 hours')::date as night
      from public.shifts sh
      where sh.ended_at is not null
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
      -- v1.0 앱이 필수로 읽음. 새 앱은 collected_bags를 씀
      'collected_kg', (
        select coalesce(round(sum(co.general_kg + co.recycle_kg + co.butts_g / 1000), 1), 0)
        from public.collections co
        where co.shift_id in (select shift_id from month_c)
      ),
      -- 들른 근무들의 채운 봉투(100L) 합계. 무게는 선택 입력이라 봉투 수가 늘 기록되는 지표
      'collected_bags', (
        select coalesce(sum(b), 0)::int
        from public.collections co, unnest(co.bags_by_hour) b
        where co.shift_id in (select distinct shift_id from month_c)
      ),
      -- 슬롯 시각은 밤 날짜 기준 시(24 이상은 자정 넘김)
      'ad_minutes', (
        select coalesce(sum(greatest(0, extract(epoch from
                 least(ms.e_at, ms.night + make_interval(hours => a.end_hour))
                 - greatest(ms.s_at, ms.night + make_interval(hours => a.start_hour))) / 60)), 0)::int
        from month_shifts ms
        join public.ad_slots a on a.shop_id = v_shop.id
        where ms.night >= v_month_start and ms.night < v_month_end
      ),
      'guests', (
        select count(*) from public.guest_visits g
        where g.shop_id = v_shop.id
          and (g.created_at at time zone 'Asia/Seoul')::date >= v_month_start
          and (g.created_at at time zone 'Asia/Seoul')::date < v_month_end
      ),
      'qr_scans', (select count(*) from month_scans),
      'clean_score', (select round(avg(score), 1) from month_scans where score is not null),
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
$function$;

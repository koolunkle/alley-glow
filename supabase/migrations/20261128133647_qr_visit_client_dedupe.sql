-- 랜딩 웹이 브라우저에 만든 무작위 기기 id(개인정보 아님). 새로고침으로 스캔·평가를 부풀리지 못하게 씀
alter table public.qr_scans
  add column client_id text check (client_id is null or length(client_id) between 8 and 64);
create index qr_scans_client_recent on public.qr_scans (shop_id, client_id, scanned_at desc) where client_id is not null;

-- p_client를 받는 새 형태로 교체(예전 웹처럼 p_code만 보내도 동작)
drop function public.qr_visit(text);
create function public.qr_visit(p_code text, p_client text default null)
returns json
language plpgsql
security definer
set search_path to ''
as $function$
declare
  v_shop public.shops;
  v_scan uuid;
  v_client text := case when length(p_client) between 8 and 64 then p_client end;
begin
  select * into v_shop from public.shops where qr_code = p_code;
  if not found then
    return null;
  end if;
  -- 같은 기기가 같은 가게를 30분 안에 다시 열면 이전 스캔을 돌려줌(스캔 수·평가 1회 유지)
  -- shortcut: 브라우저 저장소를 지우거나 시크릿 창이면 새 기기로 보임, 공개 홍보 뒤 남용이 보이면 IP 기준 제한이나 캡차 추가
  if v_client is not null then
    select id into v_scan from public.qr_scans
    where shop_id = v_shop.id and client_id = v_client and scanned_at > now() - interval '30 minutes'
    order by scanned_at desc limit 1;
  end if;
  if v_scan is null then
    insert into public.qr_scans (shop_id, client_id) values (v_shop.id, v_client) returning id into v_scan;
  end if;
  return json_build_object(
    'scan_id', v_scan, 'name', v_shop.name, 'intro', v_shop.intro, 'menu', v_shop.menu,
    'benefit', v_shop.benefit, 'lat', v_shop.lat, 'lng', v_shop.lng
  );
end;
$function$;

revoke execute on function public.qr_visit(text, text) from public;
grant execute on function public.qr_visit(text, text) to anon, authenticated;

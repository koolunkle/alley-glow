-- QR 랜딩에 보여 줄 가게 정보(섭외 때 받는 메뉴·혜택으로 채움, 없으면 화면에서 생략)
alter table public.shops
  add column intro text,
  add column menu text,
  add column benefit text;
update public.shops set intro = '[가게 소개 한 줄]', menu = '[대표 메뉴]', benefit = '[혜택]';

-- 시민의 QR 스캔과 청결도 평가(1~5, 스캔당 1번). 익명은 테이블에 직접 접근하지 못하고 아래 함수로만 씀
create table public.qr_scans (
  id uuid primary key default gen_random_uuid(),
  shop_id bigint not null references public.shops (id),
  scanned_at timestamptz not null default now(),
  score smallint check (score between 1 and 5)
);
create index qr_scans_shop_idx on public.qr_scans (shop_id, scanned_at);
alter table public.qr_scans enable row level security;

-- 랜딩을 열 때: 스캔 기록 + 가게 정보. 모르는 코드면 null
create function public.qr_visit(p_code text) returns json
language plpgsql
volatile
security definer
set search_path = ''
as $$
declare
  v_shop public.shops;
  v_scan uuid;
begin
  select * into v_shop from public.shops where qr_code = p_code;
  if not found then
    return null;
  end if;
  insert into public.qr_scans (shop_id) values (v_shop.id) returning id into v_scan;
  return json_build_object(
    'scan_id', v_scan, 'name', v_shop.name, 'intro', v_shop.intro, 'menu', v_shop.menu,
    'benefit', v_shop.benefit, 'lat', v_shop.lat, 'lng', v_shop.lng
  );
end;
$$;

-- 청결도 평가: 그 스캔에 아직 점수가 없고 하루 안일 때만
create function public.qr_rate(p_scan_id uuid, p_score int) returns boolean
language plpgsql
volatile
security definer
set search_path = ''
as $$
begin
  if p_score not between 1 and 5 then
    return false;
  end if;
  update public.qr_scans set score = p_score
  where id = p_scan_id and score is null and scanned_at > now() - interval '1 day';
  return found;
end;
$$;

revoke execute on function public.qr_visit(text) from public;
revoke execute on function public.qr_rate(uuid, int) from public;
grant execute on function public.qr_visit(text) to anon, authenticated;
grant execute on function public.qr_rate(uuid, int) to anon, authenticated;

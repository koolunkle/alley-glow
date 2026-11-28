-- 앱 사용자 역할. 계정은 관리자가 대시보드에서 만들고 이 테이블에 역할을 넣는다(회원가입 없음)
create table public.profiles (
  user_id uuid primary key references auth.users (id) on delete cascade,
  role text not null check (role in ('worker', 'shop')),
  display_name text not null
);
alter table public.profiles enable row level security;
create policy "profiles read own" on public.profiles for select to authenticated using ((select auth.uid()) = user_id);
grant select on public.profiles to authenticated;

-- 저울이 없을 수 있어 무게는 선택. weighed=false면 무게를 비워 두고, 추정치는 저장하지 않음(화면에만 표시)
alter table public.collections rename column scale_photo_paths to photo_paths;
alter table public.collections add column weighed boolean not null default true;
alter table public.collections
  alter column general_kg drop not null,
  alter column recycle_kg drop not null,
  alter column butts_g drop not null;
alter table public.collections add constraint collections_weighed_values check (
  (weighed and general_kg is not null and recycle_kg is not null and butts_g is not null)
  or (not weighed and general_kg is null and recycle_kg is null and butts_g is null)
);
-- 기존 행(시연용 예시)은 실측으로 두고, 이후로는 앱이 반드시 보냄
alter table public.collections alter column weighed drop default;
comment on column public.collections.photo_paths is '근무 종료 사진 Storage 경로. 저울을 쓰면 저울 눈금, 아니면 채운 봉투';

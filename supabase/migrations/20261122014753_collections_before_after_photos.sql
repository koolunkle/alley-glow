-- 청소 전후 사진(같은 자리, 같은 각도). 선택, 각 최대 2장. 경로: {user_id}/{shift_id}/before-{n}.jpg, after-{n}.jpg
alter table public.collections
  add column before_photo_paths text[] not null default '{}' check (cardinality(before_photo_paths) <= 2),
  add column after_photo_paths text[] not null default '{}' check (cardinality(after_photo_paths) <= 2);

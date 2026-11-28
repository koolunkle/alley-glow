-- 근무 종료 사진(저울). 비공개, JPEG 5MB까지. 경로: {user_id}/{shift_id}/scale-{n}.jpg
insert into storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
values ('collection-photos', 'collection-photos', false, 5242880, array['image/jpeg']);

-- 워커 본인 폴더만. 같은 경로 재업로드(upsert)를 위해 update도 허용
create policy "collection photos insert own" on storage.objects for insert to authenticated
  with check (
    bucket_id = 'collection-photos'
    and (storage.foldername(name))[1] = (select auth.uid())::text
    and exists (select 1 from public.profiles p where p.user_id = (select auth.uid()) and p.role = 'worker')
  );
create policy "collection photos read own" on storage.objects for select to authenticated
  using (bucket_id = 'collection-photos' and (storage.foldername(name))[1] = (select auth.uid())::text);
create policy "collection photos update own" on storage.objects for update to authenticated
  using (bucket_id = 'collection-photos' and (storage.foldername(name))[1] = (select auth.uid())::text)
  with check (bucket_id = 'collection-photos' and (storage.foldername(name))[1] = (select auth.uid())::text);

-- 저울 사진 Storage 경로(버킷 제외). 앱은 1장 이상을 요구, 시연용 예시 행은 비어 있음
alter table public.collections
  add column scale_photo_paths text[] not null default '{}' check (cardinality(scale_photo_paths) <= 3);

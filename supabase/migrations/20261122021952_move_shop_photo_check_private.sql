-- 정책 전용 함수는 API(/rpc)에 노출되지 않는 스키마로
create schema if not exists private;
grant usage on schema private to authenticated;

create function private.shop_can_read_photo(p_name text) returns boolean
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
revoke execute on function private.shop_can_read_photo(text) from public, anon;
grant execute on function private.shop_can_read_photo(text) to authenticated;

drop policy "collection photos read by shop" on storage.objects;
create policy "collection photos read by shop" on storage.objects for select to authenticated
  using (bucket_id = 'collection-photos' and private.shop_can_read_photo(name));

drop function public.shop_can_read_photo(text);

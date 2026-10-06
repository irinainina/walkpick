-- Photos of finds. Private: a photo is read through the owner's session only.
-- The phone uploads a compressed JPEG to <user id>/<find id>.jpg.
insert into storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
values ('photos', 'photos', false, 5 * 1024 * 1024, array['image/jpeg']);

create policy "select own photos" on storage.objects
    for select to authenticated
    using (bucket_id = 'photos' and (storage.foldername(name))[1] = (select auth.uid())::text);

create policy "insert own photos" on storage.objects
    for insert to authenticated
    with check (bucket_id = 'photos' and (storage.foldername(name))[1] = (select auth.uid())::text);

create policy "delete own photos" on storage.objects
    for delete to authenticated
    using (bucket_id = 'photos' and (storage.foldername(name))[1] = (select auth.uid())::text);

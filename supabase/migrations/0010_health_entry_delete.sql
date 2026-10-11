-- An entry made by mistake - 642 instead of 64.2 - may be taken back: until now
-- single entries could only be added.
grant delete on public.health_entries to authenticated;
create policy "delete own health_entries" on public.health_entries
    for delete to authenticated using ((select auth.uid()) = user_id);

-- Each table is open to its owner and to nobody else. The app ships a publishable
-- key, so these policies are the whole of the protection.
-- Removing an account goes through the delete-account function: the rows hang off
-- auth.users with on delete cascade.

grant select, insert, update on public.profiles to authenticated;
create policy "select own profiles" on public.profiles
    for select to authenticated using ((select auth.uid()) = id);
create policy "insert own profiles" on public.profiles
    for insert to authenticated with check ((select auth.uid()) = id);
create policy "update own profiles" on public.profiles
    for update to authenticated using ((select auth.uid()) = id) with check ((select auth.uid()) = id);

grant select, insert, update on public.settings to authenticated;
create policy "select own settings" on public.settings
    for select to authenticated using ((select auth.uid()) = user_id);
create policy "insert own settings" on public.settings
    for insert to authenticated with check ((select auth.uid()) = user_id);
create policy "update own settings" on public.settings
    for update to authenticated using ((select auth.uid()) = user_id) with check ((select auth.uid()) = user_id);

grant select, insert, update on public.daily_steps to authenticated;
create policy "select own daily_steps" on public.daily_steps
    for select to authenticated using ((select auth.uid()) = user_id);
create policy "insert own daily_steps" on public.daily_steps
    for insert to authenticated with check ((select auth.uid()) = user_id);
create policy "update own daily_steps" on public.daily_steps
    for update to authenticated using ((select auth.uid()) = user_id) with check ((select auth.uid()) = user_id);

grant select, insert, update, delete on public.walks to authenticated;
create policy "select own walks" on public.walks
    for select to authenticated using ((select auth.uid()) = user_id);
create policy "insert own walks" on public.walks
    for insert to authenticated with check ((select auth.uid()) = user_id);
create policy "update own walks" on public.walks
    for update to authenticated using ((select auth.uid()) = user_id) with check ((select auth.uid()) = user_id);
create policy "delete own walks" on public.walks
    for delete to authenticated using ((select auth.uid()) = user_id);

grant select, insert, update, delete on public.finds to authenticated;
create policy "select own finds" on public.finds
    for select to authenticated using ((select auth.uid()) = user_id);
create policy "insert own finds" on public.finds
    for insert to authenticated with check ((select auth.uid()) = user_id);
create policy "update own finds" on public.finds
    for update to authenticated using ((select auth.uid()) = user_id) with check ((select auth.uid()) = user_id);
create policy "delete own finds" on public.finds
    for delete to authenticated using ((select auth.uid()) = user_id);

-- Single entries are never edited or removed, only the whole account is.
grant select, insert on public.health_entries to authenticated;
create policy "select own health_entries" on public.health_entries
    for select to authenticated using ((select auth.uid()) = user_id);
create policy "insert own health_entries" on public.health_entries
    for insert to authenticated with check ((select auth.uid()) = user_id);

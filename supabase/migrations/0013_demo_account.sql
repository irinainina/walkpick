-- The trial history the debug seed wrote into the author's account (ids from 5eed, step days
-- before 2026-10-06) becomes the read-only demo account behind "View demo".
do $$
declare
    author uuid := (select user_id from public.walks where id::text like '5eed%' limit 1);
    demo uuid := (select id from auth.users where email = 'demo@walkpick.app');
begin
    if author is null then
        return;
    end if;
    if demo is null then
        raise exception 'add the user demo@walkpick.app in Supabase Auth first';
    end if;
    insert into public.profiles (id, email, name) values (demo, 'demo@walkpick.app', 'Demo')
        on conflict (id) do nothing;
    insert into public.settings (user_id, pressure_tab, glucose_tab) values (demo, true, true)
        on conflict (user_id) do nothing;
    update public.walks set user_id = demo, updated_at = now() where id::text like '5eed%';
    update public.finds set user_id = demo, updated_at = now() where id::text like '5eed%';
    update public.health_entries set user_id = demo, updated_at = now() where id::text like '5eed%';
    update public.daily_steps set user_id = demo, updated_at = now()
        where user_id = author and day < '2026-10-06';
end $$;

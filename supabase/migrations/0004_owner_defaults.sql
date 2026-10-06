-- The phone sends rows without an owner: the server writes in whoever is signed in.
alter table public.profiles alter column id set default auth.uid();
alter table public.settings alter column user_id set default auth.uid();
alter table public.daily_steps alter column user_id set default auth.uid();
alter table public.walks alter column user_id set default auth.uid();
alter table public.finds alter column user_id set default auth.uid();
alter table public.health_entries alter column user_id set default auth.uid();

-- A find is one point and the map is drawn on the phone: two numbers travel as
-- plain JSON, a PostGIS point comes back as a hex string.
alter table public.finds
    drop column location,
    add column latitude double precision not null check (latitude between -90 and 90),
    add column longitude double precision not null check (longitude between -180 and 180);

-- A row deleted on one device has to reach the other, so it is marked and not removed; the
-- service stops showing it and erases it for good later. A health entry can now be edited.
alter table public.walks add column deleted_at timestamptz;
alter table public.finds add column deleted_at timestamptz;
alter table public.health_entries
    add column deleted_at timestamptz,
    add column updated_at timestamptz not null default now();

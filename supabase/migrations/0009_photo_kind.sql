-- A find may be a photo taken for its own sake: nothing is asked about it and
-- it is not a failed recognition, so it gets a kind of its own.
alter table public.finds drop constraint if exists finds_kind_check;
alter table public.finds
  add constraint finds_kind_check check (kind in ('plant', 'bird', 'place', 'photo', 'unknown'));

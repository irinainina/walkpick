-- The name of a walk: made up by the phone from the weather and the time of
-- day, and changed by the walker at will. Older walks have none and are shown
-- under a name made up from their start time.
alter table public.walks add column name text;

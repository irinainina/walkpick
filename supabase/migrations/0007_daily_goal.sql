-- The steps of a full day are set in Settings now and start at 5,000: steps are
-- counted on walks only. Nobody could choose 10,000 before, so every 10,000 is
-- the old default.
alter table public.profiles alter column daily_goal set default 5000;
update public.profiles set daily_goal = 5000 where daily_goal = 10000;

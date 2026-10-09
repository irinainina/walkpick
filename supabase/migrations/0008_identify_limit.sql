-- The number of a walk is counted from the walks kept on the phone; the column
-- that was to remember it on the server was never read or written.
alter table public.profiles drop column last_walk_number;

-- Pl@ntNet gives the whole project 500 answers a day. Each caller of the
-- identify function is counted per day, so one account cannot spend them all.
-- Row level security is on and has no policies: the rows are reached only
-- through the function below.
create table public.identify_calls (
  user_id uuid not null references auth.users (id) on delete cascade,
  day date not null default current_date,
  calls integer not null default 0,
  primary key (user_id, day)
);

alter table public.identify_calls enable row level security;

-- Counts one more call of the caller today; false when that is over the limit.
create function public.count_identify_call(day_limit integer)
returns boolean
language sql
security definer
set search_path = ''
as $$
  insert into public.identify_calls as counted (user_id, calls)
  values ((select auth.uid()), 1)
  on conflict (user_id, day) do update set calls = counted.calls + 1
  returning calls <= day_limit;
$$;

revoke all on function public.count_identify_call(integer) from public, anon;
grant execute on function public.count_identify_call(integer) to authenticated;

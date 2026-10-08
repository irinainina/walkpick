-- The emails left on the landing before the release. Written only by the
-- waitlist function with the service role: row level security is on and has
-- no policies, so neither the app nor a browser can read or change the list.
create table public.waitlist (
  email text primary key,
  created_at timestamptz not null default now()
);

alter table public.waitlist enable row level security;

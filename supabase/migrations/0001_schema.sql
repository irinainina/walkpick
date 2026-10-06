-- Everything the phone keeps is copied here, so a new phone can get it back.
-- Rows are written by the phone, which is why ids are uuids it makes itself.
-- Values are stored metric; km / mi and the rest are a matter of display.

create extension if not exists postgis with schema extensions;

create table public.profiles (
    id uuid primary key references auth.users on delete cascade,
    email text,
    name text,
    sex text check (sex in ('female', 'male')),
    birth_date date,
    height_cm smallint check (height_cm between 50 and 260),
    daily_goal integer not null default 10000 check (daily_goal > 0),
    target_weight_kg numeric(4, 1) check (target_weight_kg > 0),
    target_systolic smallint not null default 120 check (target_systolic > 0),
    target_diastolic smallint not null default 80 check (target_diastolic > 0),
    target_glucose_mmol numeric(3, 1) not null default 5.5 check (target_glucose_mmol > 0),
    -- a deleted walk keeps its number, so the next one cannot be counted from the rows
    last_walk_number integer not null default 0,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create table public.settings (
    user_id uuid primary key references auth.users on delete cascade,
    mood_tab boolean not null default true,
    weight_tab boolean not null default true,
    pressure_tab boolean not null default false,
    glucose_tab boolean not null default false,
    distance_unit text not null default 'km' check (distance_unit in ('km', 'mi')),
    weight_unit text not null default 'kg' check (weight_unit in ('kg', 'lb')),
    glucose_unit text not null default 'mmol' check (glucose_unit in ('mmol', 'mgdl')),
    reminders_enabled boolean not null default false,
    reminder_time time not null default '18:00',
    -- ISO days of the week, 1 is Monday
    reminder_days smallint[] not null default '{1,2,3,4,5,6,7}',
    updated_at timestamptz not null default now(),
    check (mood_tab or weight_tab or pressure_tab or glucose_tab)
);

create table public.daily_steps (
    user_id uuid not null references auth.users on delete cascade,
    day date not null,
    steps integer not null check (steps >= 0),
    distance_m integer not null default 0 check (distance_m >= 0),
    active_minutes integer not null default 0 check (active_minutes >= 0),
    -- the goal of that day: changing it later must not repaint the calendar
    goal integer not null check (goal > 0),
    updated_at timestamptz not null default now(),
    primary key (user_id, day)
);

create table public.walks (
    id uuid primary key,
    user_id uuid not null references auth.users on delete cascade,
    number integer not null check (number > 0),
    started_at timestamptz not null,
    ended_at timestamptz not null,
    -- time on the move, pauses left out
    duration_s integer not null check (duration_s >= 0),
    steps integer not null default 0 check (steps >= 0),
    distance_m integer not null default 0 check (distance_m >= 0),
    calories integer not null default 0 check (calories >= 0),
    route extensions.geography(LineString, 4326),
    note text,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    unique (user_id, number),
    check (ended_at >= started_at)
);

create index walks_user_started on public.walks (user_id, started_at desc);

-- A photo and what was recognised in it: every photo is a find, named or unknown.
create table public.finds (
    id uuid primary key,
    user_id uuid not null references auth.users on delete cascade,
    walk_id uuid not null references public.walks on delete cascade,
    taken_at timestamptz not null,
    location extensions.geography(Point, 4326) not null,
    -- path inside the photos bucket
    photo_path text not null,
    kind text not null default 'unknown' check (kind in ('plant', 'bird', 'place', 'unknown')),
    name text,
    latin_name text,
    -- 0..1 from the service; null when the name was typed by hand
    match real check (match between 0 and 1),
    -- the other answers of the service, for "Not this?": [{name, latin_name, match}]
    alternatives jsonb not null default '[]',
    description text,
    wikipedia_url text,
    is_best boolean not null default false,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create index finds_user_taken on public.finds (user_id, taken_at desc);
create index finds_walk on public.finds (walk_id);

create table public.health_entries (
    id uuid primary key,
    user_id uuid not null references auth.users on delete cascade,
    kind text not null check (kind in ('mood', 'weight', 'pressure', 'glucose')),
    measured_at timestamptz not null,
    -- mood 1 (awful) .. 5 (great), weight in kg, systolic, glucose in mmol/L
    value numeric(5, 1) not null check (value > 0),
    diastolic smallint check (diastolic > 0),
    created_at timestamptz not null default now(),
    check ((kind = 'pressure') = (diastolic is not null)),
    check (kind <> 'mood' or value in (1, 2, 3, 4, 5))
);

create index health_entries_user_kind_measured
    on public.health_entries (user_id, kind, measured_at desc);

-- Closed until 0002 opens each table to its owner: the app ships a publishable key.
alter table public.profiles enable row level security;
alter table public.settings enable row level security;
alter table public.daily_steps enable row level security;
alter table public.walks enable row level security;
alter table public.finds enable row level security;
alter table public.health_entries enable row level security;

-- A pause cuts a route into legs: the way between them was not walked and is
-- drawn with dashes. The route stays one line; breaks holds the indexes of the
-- points a leg begins at, "153,201".
alter table public.walks add column breaks text;

-- How the walker felt setting out and coming back, 1 (awful) to 5 (great): the
-- line of the walk is coloured from one to the other.
alter table public.walks add column mood_before smallint check (mood_before between 1 and 5);
alter table public.walks add column mood_after smallint check (mood_after between 1 and 5);

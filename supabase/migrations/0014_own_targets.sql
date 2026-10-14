-- Targets of pressure and glucose are the walker's own or none: the old defaults 120/80 and 5.5
-- were never chosen by anyone, so they become "not set".
alter table public.profiles
    alter column target_systolic drop not null,
    alter column target_systolic drop default,
    alter column target_diastolic drop not null,
    alter column target_diastolic drop default,
    alter column target_glucose_mmol drop not null,
    alter column target_glucose_mmol drop default;

update public.profiles
    set target_systolic = null, target_diastolic = null, updated_at = now()
    where target_systolic = 120 and target_diastolic = 80;

update public.profiles
    set target_glucose_mmol = null, updated_at = now()
    where target_glucose_mmol = 5.5;

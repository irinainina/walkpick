import {
  boolean,
  customType,
  date,
  doublePrecision,
  integer,
  jsonb,
  numeric,
  pgTable,
  primaryKey,
  real,
  smallint,
  text,
  time,
  timestamp,
  uuid,
} from 'drizzle-orm/pg-core';

// The tables as supabase/migrations made them. Field names are the column names: the API gives
// rows as the database keeps them, so nothing is renamed on the way.

const instant = (name: string) =>
  timestamp(name, { withTimezone: true, mode: 'date' });

// PostGIS keeps the route; it is read through ST_AsGeoJSON and never as this raw value.
const lineString = customType<{ data: string }>({
  dataType: () => 'geography(LineString, 4326)',
});

export const profiles = pgTable('profiles', {
  id: uuid('id').primaryKey(),
  email: text('email'),
  name: text('name'),
  sex: text('sex', { enum: ['female', 'male'] }),
  birth_date: date('birth_date'),
  height_cm: smallint('height_cm'),
  daily_goal: integer('daily_goal').notNull(),
  target_weight_kg: numeric('target_weight_kg', {
    precision: 4,
    scale: 1,
    mode: 'number',
  }),
  target_systolic: smallint('target_systolic').notNull(),
  target_diastolic: smallint('target_diastolic').notNull(),
  target_glucose_mmol: numeric('target_glucose_mmol', {
    precision: 3,
    scale: 1,
    mode: 'number',
  }).notNull(),
  created_at: instant('created_at').notNull(),
  updated_at: instant('updated_at').notNull(),
});

export const settings = pgTable('settings', {
  user_id: uuid('user_id').primaryKey(),
  mood_tab: boolean('mood_tab').notNull(),
  weight_tab: boolean('weight_tab').notNull(),
  pressure_tab: boolean('pressure_tab').notNull(),
  glucose_tab: boolean('glucose_tab').notNull(),
  distance_unit: text('distance_unit', { enum: ['km', 'mi'] }).notNull(),
  weight_unit: text('weight_unit', { enum: ['kg', 'lb'] }).notNull(),
  glucose_unit: text('glucose_unit', { enum: ['mmol', 'mgdl'] }).notNull(),
  reminders_enabled: boolean('reminders_enabled').notNull(),
  reminder_time: time('reminder_time').notNull(),
  reminder_days: smallint('reminder_days').array().notNull(),
  updated_at: instant('updated_at').notNull(),
});

export const dailySteps = pgTable(
  'daily_steps',
  {
    user_id: uuid('user_id').notNull(),
    day: date('day').notNull(),
    steps: integer('steps').notNull(),
    distance_m: integer('distance_m').notNull(),
    active_minutes: integer('active_minutes').notNull(),
    goal: integer('goal').notNull(),
    updated_at: instant('updated_at').notNull(),
  },
  (table) => [primaryKey({ columns: [table.user_id, table.day] })],
);

export const walks = pgTable('walks', {
  id: uuid('id').primaryKey(),
  user_id: uuid('user_id').notNull(),
  number: integer('number').notNull(),
  started_at: instant('started_at').notNull(),
  ended_at: instant('ended_at').notNull(),
  duration_s: integer('duration_s').notNull(),
  steps: integer('steps').notNull(),
  distance_m: integer('distance_m').notNull(),
  calories: integer('calories').notNull(),
  route: lineString('route'),
  note: text('note'),
  name: text('name'),
  breaks: text('breaks'),
  mood_before: smallint('mood_before'),
  mood_after: smallint('mood_after'),
  created_at: instant('created_at').notNull(),
  updated_at: instant('updated_at').notNull(),
});

export const finds = pgTable('finds', {
  id: uuid('id').primaryKey(),
  user_id: uuid('user_id').notNull(),
  walk_id: uuid('walk_id').notNull(),
  taken_at: instant('taken_at').notNull(),
  latitude: doublePrecision('latitude').notNull(),
  longitude: doublePrecision('longitude').notNull(),
  photo_path: text('photo_path').notNull(),
  kind: text('kind', {
    enum: ['plant', 'bird', 'place', 'photo', 'unknown'],
  }).notNull(),
  name: text('name'),
  latin_name: text('latin_name'),
  match: real('match'),
  alternatives: jsonb('alternatives').notNull(),
  description: text('description'),
  wikipedia_url: text('wikipedia_url'),
  is_best: boolean('is_best').notNull(),
  created_at: instant('created_at').notNull(),
  updated_at: instant('updated_at').notNull(),
});

export const healthEntries = pgTable('health_entries', {
  id: uuid('id').primaryKey(),
  user_id: uuid('user_id').notNull(),
  kind: text('kind', {
    enum: ['mood', 'weight', 'pressure', 'glucose'],
  }).notNull(),
  measured_at: instant('measured_at').notNull(),
  value: numeric('value', { precision: 5, scale: 1, mode: 'number' }).notNull(),
  diastolic: smallint('diastolic'),
  created_at: instant('created_at').notNull(),
});

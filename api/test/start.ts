import { PGlite } from '@electric-sql/pglite';
import { INestApplication } from '@nestjs/common';
import { Test } from '@nestjs/testing';
import { drizzle } from 'drizzle-orm/pglite';
import request from 'supertest';
import { App } from 'supertest/types';
import { AppModule } from '../src/app.module';
import { SigningKeys } from '../src/auth/signing-keys';
import { Database } from '../src/db/database';
import * as schema from '../src/db/schema';
import { ISSUER, publicKey, signToken } from './tokens';

// The tables the service reads, as the migrations leave them. The in-memory Postgres has no
// PostGIS: a route is kept as its GeoJSON text and the two functions give it back untouched.
const TABLES = `
  create domain geography as text;
  create domain geometry as text;
  create function st_asgeojson(text) returns text language sql as 'select $1';
  create function st_simplify(text, double precision) returns text language sql as 'select $1';
  create table profiles (id uuid primary key, email text, name text, sex text, birth_date date,
    height_cm smallint, daily_goal integer not null default 5000, target_weight_kg numeric(4, 1),
    target_systolic smallint not null default 120, target_diastolic smallint not null default 80,
    target_glucose_mmol numeric(3, 1) not null default 5.5,
    created_at timestamptz not null default now(), updated_at timestamptz not null default now());
  create table settings (user_id uuid primary key, mood_tab boolean not null default true,
    weight_tab boolean not null default true, pressure_tab boolean not null default false,
    glucose_tab boolean not null default false, distance_unit text not null default 'km',
    weight_unit text not null default 'kg', glucose_unit text not null default 'mmol',
    reminders_enabled boolean not null default false, reminder_time time not null default '18:00',
    reminder_days smallint[] not null default '{1,2,3,4,5,6,7}',
    updated_at timestamptz not null default now());
  create table walks (id uuid primary key, user_id uuid not null, number integer not null,
    started_at timestamptz not null, ended_at timestamptz not null, duration_s integer not null,
    steps integer not null default 0, distance_m integer not null default 0,
    calories integer not null default 0, route geography, note text, name text, breaks text,
    mood_before smallint, mood_after smallint, deleted_at timestamptz,
    created_at timestamptz not null default now(), updated_at timestamptz not null default now());
  create table finds (id uuid primary key, user_id uuid not null, walk_id uuid not null,
    taken_at timestamptz not null, latitude double precision not null,
    longitude double precision not null, photo_path text not null, kind text not null default 'unknown',
    name text, latin_name text, match real, alternatives jsonb not null default '[]',
    description text, wikipedia_url text, is_best boolean not null default false, deleted_at timestamptz,
    created_at timestamptz not null default now(), updated_at timestamptz not null default now());
  create table health_entries (id uuid primary key, user_id uuid not null, kind text not null,
    measured_at timestamptz not null, value numeric(5, 1) not null, diastolic smallint,
    deleted_at timestamptz, created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    check ((kind = 'pressure') = (diastolic is not null)));
`;

// Supabase Storage as the service calls it: every asked path gets a link, except a file named "lost".
const storage: typeof fetch = (address, asked) => {
  const { paths } = JSON.parse(asked?.body as string) as { paths: string[] };
  const links = paths.map((path) =>
    path.includes('lost')
      ? { path, signedURL: null }
      : { path, signedURL: `/object/sign/photos/${path}?token=t` },
  );
  const known =
    (address as string) ===
    'https://project.supabase.co/storage/v1/object/sign/photos';
  return Promise.resolve(
    new Response(JSON.stringify(links), { status: known ? 200 : 404 }),
  );
};

export interface Started {
  app: INestApplication<App>;
  sql: PGlite;
  /** A GET with the token of a user; without a user - without a token. */
  get: (path: string, user?: string) => request.Test;
  /** Any other request with the token of a user and, when given, a body. */
  send: (
    method: 'post' | 'patch' | 'delete',
    path: string,
    user: string,
    body?: object,
  ) => request.Test;
}

/** The whole service over an empty in-memory database, with tokens the tests sign themselves. */
export async function start(): Promise<Started> {
  process.env.SUPABASE_URL = 'https://project.supabase.co';
  process.env.DATABASE_URL = 'postgresql://nobody@localhost:1/none';
  process.env.SUPABASE_SECRET_KEY = 'secret';
  jest.spyOn(global, 'fetch').mockImplementation(storage);
  const sql = new PGlite();
  await sql.exec(TABLES);
  const module = await Test.createTestingModule({ imports: [AppModule] })
    .overrideProvider(SigningKeys)
    .useValue({ issuer: ISSUER, find: () => Promise.resolve(publicKey) })
    .overrideProvider(Database)
    .useValue({
      rows: drizzle(sql, { schema }),
      onModuleDestroy: () => sql.close(),
    })
    .compile();
  const app = module.createNestApplication<INestApplication<App>>();
  await app.init();
  const token = (sub: string) =>
    signToken({
      iss: ISSUER,
      aud: 'authenticated',
      sub,
      exp: Date.now() / 1000 + 60,
    });
  const bearer = (user?: string) =>
    user ? { Authorization: `Bearer ${token(user)}` } : {};
  const get = (path: string, user?: string) =>
    request(app.getHttpServer()).get(path).set(bearer(user));
  const send: Started['send'] = (method, path, user, body) =>
    request(app.getHttpServer())[method](path).set(bearer(user)).send(body);
  return { app, sql, get, send };
}

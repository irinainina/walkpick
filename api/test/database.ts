import { PGlite } from '@electric-sql/pglite';
import { postgis } from '@electric-sql/pglite-postgis';
import { readdirSync, readFileSync, writeFileSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import { pending } from '../src/db/migrations';
import { A, B, C } from './tokens';

// What Supabase has before the migrations run: its roles, the users, the photo storage.
const SUPABASE = `
  create role anon; create role authenticated; create role service_role;
  create schema auth; create schema storage; create schema extensions;
  create table auth.users (id uuid primary key);
  create function auth.uid() returns uuid language sql stable
    as $$ select nullif(current_setting('request.jwt.claim.sub', true), '')::uuid $$;
  create table storage.buckets (id text primary key, name text not null, public boolean,
    file_size_limit bigint, allowed_mime_types text[]);
  create table storage.objects (id uuid primary key default gen_random_uuid(), bucket_id text,
    name text, owner uuid);
  alter table storage.objects enable row level security;
  create function storage.foldername(name text) returns text[] language sql immutable
    as $$ select (string_to_array(name, '/'))[:cardinality(string_to_array(name, '/')) - 1] $$;
`;

const MIGRATIONS = join(__dirname, '..', '..', 'supabase', 'migrations');

export const SNAPSHOT = join(tmpdir(), 'walkpick-test-database.tar');

/**
 * The database of the project as the migrations leave it, with the users the tests sign in as.
 * It is built once per run; every service of the tests starts from its snapshot.
 */
export default async function build(): Promise<void> {
  const sql = new PGlite({ extensions: { postgis } });
  await sql.exec(SUPABASE);
  for (const name of pending(readdirSync(MIGRATIONS), []))
    await sql.exec(readFileSync(join(MIGRATIONS, name), 'utf8'));
  await sql.exec(
    `insert into auth.users (id) values ('${A}'), ('${B}'), ('${C}')`,
  );
  const snapshot = await sql.dumpDataDir('none');
  writeFileSync(SNAPSHOT, Buffer.from(await snapshot.arrayBuffer()));
  await sql.close();
}

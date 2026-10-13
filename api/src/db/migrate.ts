import { readdirSync, readFileSync } from 'node:fs';
import { join } from 'node:path';
import { Client } from 'pg';
import { pending, upTo } from './migrations';

// npm run migrate                       applies what is not applied yet
// npm run migrate -- --baseline 0011    marks the migrations up to 0011 as applied without running
const FOLDER = join(__dirname, '..', '..', '..', 'supabase', 'migrations');

async function migrate(): Promise<void> {
  const files = readdirSync(FOLDER);
  const client = new Client({
    connectionString: process.env.DATABASE_URL,
    ssl: { rejectUnauthorized: false },
  });
  await client.connect();
  try {
    await client.query(
      'create table if not exists public.schema_migrations (name text primary key, applied_at timestamptz not null default now())',
    );
    await client.query(
      'alter table public.schema_migrations enable row level security',
    );
    const mark = (name: string) =>
      client.query(
        'insert into public.schema_migrations (name) values ($1) on conflict do nothing',
        [name],
      );
    const baseline = process.argv.indexOf('--baseline');
    if (baseline > 0) {
      for (const name of upTo(files, process.argv[baseline + 1]))
        await mark(name);
    }
    const applied = await client.query<{ name: string }>(
      'select name from public.schema_migrations',
    );
    const todo = pending(
      files,
      applied.rows.map((row) => row.name),
    );
    for (const name of todo) {
      // A migration is applied whole or not at all.
      await client.query('begin');
      try {
        await client.query(readFileSync(join(FOLDER, name), 'utf8'));
        await mark(name);
        await client.query('commit');
        console.log(`applied ${name}`);
      } catch (problem) {
        await client.query('rollback');
        throw problem;
      }
    }
    console.log(
      `${applied.rows.length + todo.length} migrations applied, ${todo.length} of them now`,
    );
  } finally {
    await client.end();
  }
}

void migrate().catch((problem) => {
  console.error(problem);
  process.exitCode = 1;
});

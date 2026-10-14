import { PGlite } from '@electric-sql/pglite';
import { postgis } from '@electric-sql/pglite-postgis';
import { INestApplication } from '@nestjs/common';
import { Test } from '@nestjs/testing';
import { drizzle } from 'drizzle-orm/pglite';
import { readFileSync } from 'node:fs';
import request from 'supertest';
import { App } from 'supertest/types';
import { AppModule } from '../src/app.module';
import { SigningKeys } from '../src/auth/signing-keys';
import { Database } from '../src/db/database';
import { describeApi } from '../src/docs';
import * as schema from '../src/db/schema';
import { SNAPSHOT } from './database';
import { DEMO, ISSUER, publicKey, signToken } from './tokens';

// Supabase as the service calls it. Auth signs the demo in; Storage gives every asked path a link,
// except a file named "lost".
const supabase: typeof fetch = (address, asked) => {
  if ((address as string).endsWith('/auth/v1/token?grant_type=password')) {
    const session = {
      access_token: `demo.${Date.now()}`,
      expires_at: Math.floor(Date.now() / 1000) + 3600,
    };
    return Promise.resolve(new Response(JSON.stringify(session)));
  }
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

/** The whole service over the project's database in memory, with tokens the tests sign themselves. */
export async function start(): Promise<Started> {
  process.env.SUPABASE_URL = 'https://project.supabase.co';
  process.env.DATABASE_URL = 'postgresql://nobody@localhost:1/none';
  process.env.SUPABASE_SECRET_KEY = 'secret';
  process.env.DEMO_USER_ID = DEMO;
  process.env.DEMO_EMAIL = 'demo@walkpick.app';
  process.env.DEMO_PASSWORD = 'demo';
  jest.spyOn(global, 'fetch').mockImplementation(supabase);
  const sql = new PGlite({
    extensions: { postgis },
    loadDataDir: new Blob([readFileSync(SNAPSHOT)]),
  });
  // Supabase finds the PostGIS functions on this path; the service calls them unqualified.
  await sql.exec('set search_path = public, extensions');
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
  describeApi(app);
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

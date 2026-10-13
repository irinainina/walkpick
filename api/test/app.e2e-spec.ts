import { Test } from '@nestjs/testing';
import { INestApplication } from '@nestjs/common';
import request from 'supertest';
import { App } from 'supertest/types';
import { configure } from './../src/app';
import { AppModule } from './../src/app.module';
import { SigningKeys } from './../src/auth/signing-keys';
import { ISSUER, publicKey, signToken } from './tokens';

const token = (sub: string, exp = Date.now() / 1000 + 60) =>
  signToken({ iss: ISSUER, aud: 'authenticated', sub, exp });

describe('the service', () => {
  let app: INestApplication<App>;
  const get = (path: string, bearer?: string) =>
    request(app.getHttpServer())
      .get(path)
      .set(bearer ? { Authorization: `Bearer ${bearer}` } : {});

  beforeAll(async () => {
    process.env.SUPABASE_URL = 'https://project.supabase.co';
    const module = await Test.createTestingModule({ imports: [AppModule] })
      .overrideProvider(SigningKeys)
      .useValue({ issuer: ISSUER, find: () => Promise.resolve(publicKey) })
      .compile();
    app = module.createNestApplication();
    configure(app);
    await app.init();
  });

  afterAll(() => app.close());

  it('says it is alive without a token', () =>
    get('/health').expect(200).expect({ status: 'ok' }));

  it('answers 401 without a token, with a wrong one and with an expired one', async () => {
    const unauthorized = {
      error: { code: 'unauthorized', message: 'a valid token is required' },
    };
    await get('/v1/me').expect(401).expect(unauthorized);
    await get('/v1/me', 'a.b.c').expect(401);
    await get('/v1/me', token('user-a', 1)).expect(401);
  });

  it('takes the user from the token', () =>
    get('/v1/me', token('user-a')).expect(200).expect({ id: 'user-a' }));
});

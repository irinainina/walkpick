import request from 'supertest';
import { start, Started } from './start';
import { ISSUER, signToken } from './tokens';

const USER = '00000000-0000-4000-8000-00000000000a';

describe('the service', () => {
  let it_: Started;

  beforeAll(async () => {
    it_ = await start();
    await it_.sql
      .exec(`insert into profiles (id, name) values ('${USER}', 'Ira');
      insert into settings (user_id, distance_unit) values ('${USER}', 'mi');`);
  });

  afterAll(() => it_.app.close());

  it('says it is alive without a token', () =>
    it_.get('/health').expect(200).expect({ status: 'ok' }));

  it('answers 401 without a token, with a wrong one and with an expired one', async () => {
    const unauthorized = {
      error: { code: 'unauthorized', message: 'a valid token is required' },
    };
    const expired = signToken({
      iss: ISSUER,
      aud: 'authenticated',
      sub: USER,
      exp: 1,
    });
    const server = request(it_.app.getHttpServer());
    await it_.get('/v1/me').expect(401).expect(unauthorized);
    await it_.get('/v1/walks').expect(401);
    await server.get('/v1/me').set('Authorization', 'Bearer a.b.c').expect(401);
    await server
      .get('/v1/me')
      .set('Authorization', `Bearer ${expired}`)
      .expect(401);
  });

  it('gives the profile and the settings of the user of the token', async () => {
    const { body } = await it_.get('/v1/me', USER).expect(200);
    expect(body).toMatchObject({
      id: USER,
      profile: { name: 'Ira', daily_goal: 5000 },
      settings: { distance_unit: 'mi' },
    });
  });

  it('gives nulls to a user the phone has written nothing of yet', () =>
    it_
      .get('/v1/me', '00000000-0000-4000-8000-00000000000c')
      .expect(200)
      .expect({
        id: '00000000-0000-4000-8000-00000000000c',
        profile: null,
        settings: null,
      }));
});

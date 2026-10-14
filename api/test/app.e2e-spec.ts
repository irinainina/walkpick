import request from 'supertest';
import { start, Started } from './start';
import { A as USER, C, ISSUER, signToken } from './tokens';

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

  it('answers what Nest and Express refuse in the one error body', async () => {
    const server = request(it_.app.getHttpServer());
    await it_
      .get('/v1/nowhere', USER)
      .expect(404)
      .expect({
        error: { code: 'not_found', message: 'there is no such address' },
      });
    await server
      .post('/v1/health')
      .set('Content-Type', 'application/json')
      .send('{bad')
      .expect(400)
      .expect({
        error: {
          code: 'validation_failed',
          message: 'body is not_json',
          fields: [{ field: 'body', problem: 'not_json' }],
        },
      });
    await server
      .post('/v1/health')
      .set('Content-Type', 'application/json')
      .send(JSON.stringify({ note: 'x'.repeat(200_000) }))
      .expect(413)
      .expect({
        error: { code: 'too_large', message: 'the body is too large' },
      });
  });

  it('describes itself without a token, bodies from the same checks the requests use', async () => {
    const { body } = await it_.get('/docs-json').expect(200);
    const patch = body.paths['/v1/walks/{id}'].patch;
    expect(patch.security).toEqual([{ bearer: [] }]);
    expect(patch.requestBody.content['application/json'].schema).toEqual({
      type: 'object',
      properties: {
        name: { type: 'string', maxLength: 80, nullable: true },
        note: { type: 'string', maxLength: 2000, nullable: true },
      },
      required: [],
      additionalProperties: false,
    });
    expect(
      body.paths['/v1/health/summary'].get.parameters.map(
        ({ name, required }: { name: string; required: boolean }) =>
          `${name}${required ? '!' : ''}`,
      ),
    ).toEqual(['kind!', 'period!', 'at!', 'tz']);
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
    it_.get('/v1/me', C).expect(200).expect({
      id: C,
      profile: null,
      settings: null,
    }));
});

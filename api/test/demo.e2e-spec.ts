import { readFileSync } from 'node:fs';
import { join } from 'node:path';
import request from 'supertest';
import { start, Started } from './start';
import { A, DEMO } from './tokens';

const WALK = '10000000-0000-4000-8000-000000000001';
const MOVE = join(
  __dirname,
  '..',
  '..',
  'supabase',
  'migrations',
  '0013_demo_account.sql',
);

describe('the demo account', () => {
  let it_: Started;

  beforeAll(async () => {
    it_ = await start();
    await it_.sql
      .exec(`insert into auth.users (id, email) values ('${DEMO}', 'demo@walkpick.app');
      insert into walks (id, user_id, number, name, started_at, ended_at, duration_s) values
        ('${WALK}', '${DEMO}', 1, 'Demo Walk', '2026-10-05T16:00:00Z', '2026-10-05T16:40:00Z', 2400);`);
  });

  afterAll(() => it_.app.close());

  it('gives a token without one and signs in once for every visitor', async () => {
    const server = request(it_.app.getHttpServer());
    const signIns = () =>
      (fetch as jest.Mock).mock.calls.filter(([address]) =>
        (address as string).includes('/auth/v1/token'),
      ).length;
    const before = signIns();
    const first = await server.post('/v1/demo/session').expect(200);
    const second = await server.post('/v1/demo/session').expect(200);
    expect(first.body.access_token).toMatch(/^demo\./);
    expect(first.body.expires_at).toBeGreaterThan(Date.now() / 1000);
    expect(second.body).toEqual(first.body);
    expect(signIns() - before).toBe(1);
  });

  it('reads and exports, but changes nothing', async () => {
    const readOnly = {
      error: {
        code: 'demo_read_only',
        message: 'the demo account is read-only',
      },
    };
    await it_.get(`/v1/walks/${WALK}`, DEMO).expect(200);
    await it_.send('post', '/v1/me/export', DEMO).expect(200);
    await it_
      .send('patch', `/v1/walks/${WALK}`, DEMO, { name: 'Mine' })
      .expect(403)
      .expect(readOnly);
    await it_.send('delete', `/v1/walks/${WALK}`, DEMO).expect(403);
    await it_
      .send('post', '/v1/health', DEMO, {
        id: '30000000-0000-4000-8000-000000000001',
        kind: 'weight',
        measured_at: '2026-10-07T06:00:00Z',
        value: 60,
      })
      .expect(403);
    expect((await it_.get(`/v1/walks/${WALK}`, DEMO)).body.name).toBe(
      'Demo Walk',
    );
  });

  it('takes over the trial history of the author and leaves her own walks', async () => {
    const seed = (n: number) => `5eed000${n}-0000-4000-8000-000000000001`;
    await it_.sql.exec(`
      insert into walks (id, user_id, number, started_at, ended_at, duration_s) values
        ('${seed(1)}', '${A}', 7, '2026-09-01T16:00:00Z', '2026-09-01T16:40:00Z', 2400),
        ('10000000-0000-4000-8000-000000000002', '${A}', 2, '2026-10-10T16:00:00Z', '2026-10-10T16:40:00Z', 2400);
      insert into finds (id, user_id, walk_id, taken_at, latitude, longitude, photo_path) values
        ('${seed(2)}', '${A}', '${seed(1)}', '2026-09-01T16:10:00Z', 50.58, 34.48, 'seed/1.jpg');
      insert into health_entries (id, user_id, kind, measured_at, value) values
        ('${seed(3)}', '${A}', 'weight', '2026-09-01T06:00:00Z', 64.0);
      insert into daily_steps (user_id, day, steps, distance_m, active_minutes, goal) values
        ('${A}', '2026-09-01', 6000, 4200, 50, 5000), ('${A}', '2026-10-10', 3000, 2100, 20, 5000);`);
    await it_.sql.exec(readFileSync(MOVE, 'utf8'));
    const owners = async (table: string) =>
      (
        await it_.sql.query<{ user_id: string }>(
          `select user_id from ${table} order by 1`,
        )
      ).rows.map((row) => row.user_id);
    expect(await owners(`walks where id::text like '5eed%'`)).toEqual([DEMO]);
    expect(await owners(`walks where id::text not like '5eed%'`)).toContain(A);
    expect(await owners('finds')).toEqual([DEMO]);
    expect(await owners('health_entries')).toEqual([DEMO]);
    expect(await owners(`daily_steps where day = '2026-09-01'`)).toEqual([
      DEMO,
    ]);
    expect(await owners(`daily_steps where day = '2026-10-10'`)).toEqual([A]);
    expect((await it_.get('/v1/me', DEMO)).body.profile).toMatchObject({
      name: 'Demo',
    });
  });
});

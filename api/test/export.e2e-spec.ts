import { start, Started } from './start';
import { A, B } from './tokens';

const ROUTE =
  '{"type":"LineString","coordinates":[[34.48,50.58],[34.49,50.59]]}';
const WALK = '10000000-0000-4000-8000-000000000001';

// A has a walk with a photo, a deleted walk, a day of steps and a weight; B has a walk of her own.
const ROWS = `
  insert into profiles (id, name) values ('${A}', 'Ira');
  insert into daily_steps (user_id, day, steps, distance_m, active_minutes, goal) values
    ('${A}', '2026-10-05', 6000, 4200, 50, 5000),
    ('${B}', '2026-10-05', 9000, 7000, 80, 5000);
  insert into walks (id, user_id, number, name, started_at, ended_at, duration_s, route, deleted_at) values
    ('${WALK}', '${A}', 1, 'Misty Evening', '2026-10-05T16:00:00Z', '2026-10-05T16:40:00Z', 2400, st_geomfromgeojson('${ROUTE}'), null),
    ('10000000-0000-4000-8000-000000000002', '${A}', 2, 'Gone', '2026-10-06T16:00:00Z', '2026-10-06T16:40:00Z', 2400, null, now()),
    ('10000000-0000-4000-8000-000000000009', '${B}', 1, 'Foreign Walk', '2026-10-06T10:00:00Z', '2026-10-06T11:00:00Z', 3600, null, null);
  insert into finds (id, user_id, walk_id, taken_at, latitude, longitude, photo_path, kind, name) values
    ('20000000-0000-4000-8000-000000000001', '${A}', '${WALK}', '2026-10-05T16:10:00Z', 50.58, 34.48, 'a/1.jpg', 'plant', 'Common yarrow');
  insert into health_entries (id, user_id, kind, measured_at, value) values
    ('30000000-0000-4000-8000-000000000001', '${A}', 'weight', '2026-10-05T06:00:00Z', 64.0);
`;

describe('export my data', () => {
  let it_: Started;

  beforeAll(async () => {
    it_ = await start();
    await it_.sql.exec(ROWS);
  });

  afterAll(() => it_.app.close());

  it('gives everything the user wrote, without deleted rows and without the rows of others', async () => {
    const { body } = await it_.send('post', '/v1/me/export', A).expect(200);
    expect(body).toMatchObject({
      id: A,
      profile: { name: 'Ira' },
      daily_steps: [{ day: '2026-10-05', steps: 6000 }],
      walks: [{ id: WALK, name: 'Misty Evening', route: JSON.parse(ROUTE) }],
      photos: [
        {
          name: 'Common yarrow',
          url: 'https://project.supabase.co/storage/v1/object/sign/photos/a/1.jpg?token=t',
        },
      ],
      health_entries: [{ kind: 'weight', value: 64 }],
    });
    expect(body.walks).toHaveLength(1);
    for (const rows of [body.daily_steps, body.walks, body.photos])
      for (const row of rows) {
        expect(row).not.toHaveProperty('user_id');
        expect(row).not.toHaveProperty('deleted_at');
        expect(row).not.toHaveProperty('photo_path');
      }
  });

  it('signs the photo links for a week, as the file is opened later', async () => {
    await it_.send('post', '/v1/me/export', A).expect(200);
    const asked = (fetch as jest.Mock).mock.calls.at(-1) as [
      string,
      RequestInit,
    ];
    expect(JSON.parse(asked[1].body as string)).toMatchObject({
      expiresIn: 7 * 24 * 3600,
    });
  });

  it('answers 401 without a token', () =>
    it_.send('post', '/v1/me/export', '').expect(401));
});

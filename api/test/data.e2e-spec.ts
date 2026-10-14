import { start, Started } from './start';
import { A, B } from './tokens';

const walk = (n: number) => `10000000-0000-4000-8000-00000000000${n}`;
const photo = (n: number) => `20000000-0000-4000-8000-00000000000${n}`;
const ROUTE =
  '{"type":"LineString","coordinates":[[34.48,50.58],[34.49,50.59]]}';
const KYIV = 'tz=Europe/Kyiv';

// A walks three times; the late Sunday walk is already Monday in Kyiv. B has one walk of her own.
const ROWS = `
  insert into walks (id, user_id, number, name, started_at, ended_at, duration_s, steps, distance_m, route, breaks, mood_before, mood_after) values
    ('${walk(1)}', '${A}', 1, 'Misty Evening', '2026-10-05T16:00:00Z', '2026-10-05T16:40:00Z', 2400, 4000, 3000, st_geomfromgeojson('${ROUTE}'), '600-900', 2, 4),
    ('${walk(2)}', '${A}', 2, 'Sunny Morning', '2026-10-07T07:00:00Z', '2026-10-07T07:20:00Z', 1200, 2000, 1500, null, null, null, null),
    ('${walk(3)}', '${A}', 3, 'Late Walk', '2026-10-11T21:30:00Z', '2026-10-11T22:00:00Z', 1800, 2500, 2000, null, null, null, null),
    ('${walk(9)}', '${B}', 1, 'Foreign Walk', '2026-10-06T10:00:00Z', '2026-10-06T11:00:00Z', 3600, 9000, 7000, st_geomfromgeojson('${ROUTE}'), null, null, null);
  insert into finds (id, user_id, walk_id, taken_at, latitude, longitude, photo_path, kind, name, is_best) values
    ('${photo(1)}', '${A}', '${walk(1)}', '2026-10-05T16:10:00Z', 50.58, 34.48, 'a/1.jpg', 'plant', 'Common yarrow', true),
    ('${photo(2)}', '${A}', '${walk(1)}', '2026-10-05T16:20:00Z', 50.59, 34.49, 'a/2.jpg', 'bird', 'Great tit', false),
    ('${photo(3)}', '${A}', '${walk(2)}', '2026-09-07T07:10:00Z', 50.60, 34.50, 'a/3.jpg', 'plant', 'Common yarrow', false),
    ('${photo(9)}', '${B}', '${walk(9)}', '2026-10-06T10:30:00Z', 48.00, 30.00, 'b/9.jpg', 'plant', 'Common yarrow', true);
  insert into health_entries (id, user_id, kind, measured_at, value, diastolic) values
    ('30000000-0000-4000-8000-000000000001', '${A}', 'weight', '2026-10-05T06:00:00Z', 64.0, null),
    ('30000000-0000-4000-8000-000000000002', '${A}', 'weight', '2026-10-07T06:00:00Z', 65.0, null),
    ('30000000-0000-4000-8000-000000000003', '${A}', 'pressure', '2026-10-07T06:00:00Z', 122, 78),
    ('30000000-0000-4000-8000-000000000009', '${B}', 'weight', '2026-10-06T06:00:00Z', 90.0, null);
`;

describe('reading', () => {
  let it_: Started;
  const read = async (path: string, user = A) =>
    (await it_.get(path, user).expect(200)).body as Record<string, any>;
  const ids = (body: Record<string, any>) =>
    (body.items as { id: string }[]).map((item) => item.id);

  beforeAll(async () => {
    it_ = await start();
    await it_.sql.exec(ROWS);
  });

  afterAll(() => it_.app.close());

  describe('walks', () => {
    it('lists the walks of the user, the latest first, with the route and the number of photos', async () => {
      const body = await read('/v1/walks');
      expect(ids(body)).toEqual([walk(3), walk(2), walk(1)]);
      expect(body.items[2]).toMatchObject({
        name: 'Misty Evening',
        distance_m: 3000,
        photos: 2,
        route: JSON.parse(ROUTE),
      });
      expect(body.items[1]).toMatchObject({
        photos: 1,
        route: null,
        started_at: '2026-10-07T07:00:00.000Z',
      });
      expect(body.next).toBeNull();
    });

    it('cuts the list by time and gives it page by page', async () => {
      expect(
        ids(
          await read(
            '/v1/walks?from=2026-10-06T00:00:00Z&to=2026-10-08T00:00:00Z',
          ),
        ),
      ).toEqual([walk(2)]);
      const first = await read('/v1/walks?limit=2');
      expect(ids(first)).toEqual([walk(3), walk(2)]);
      expect(ids(await read(`/v1/walks?limit=2&cursor=${first.next}`))).toEqual(
        [walk(1)],
      );
    });

    it('adds a week up by the days of the calendar of the user', async () => {
      const body = await read(
        `/v1/walks/summary?period=week&at=2026-10-07&${KYIV}`,
      );
      expect(body).toMatchObject({
        from: '2026-10-05',
        to: '2026-10-12',
        walks: 2,
        distance_m: 4500,
        steps: 6000,
        photos: 2,
      });
      expect(body.bars).toEqual([
        {
          key: '2026-10-05',
          walks: 1,
          distance_m: 3000,
          duration_s: 2400,
          steps: 4000,
        },
        {
          key: '2026-10-07',
          walks: 1,
          distance_m: 1500,
          duration_s: 1200,
          steps: 2000,
        },
      ]);
      // In UTC the late walk is still on Sunday of the same week.
      expect(
        (await read('/v1/walks/summary?period=week&at=2026-10-07')).walks,
      ).toBe(3);
    });

    it('adds a year up by months and all the time by years', async () => {
      const year = await read(
        `/v1/walks/summary?period=year&at=2026-03-01&${KYIV}`,
      );
      expect(year.bars).toEqual([
        {
          key: '2026-10',
          walks: 3,
          distance_m: 6500,
          duration_s: 5400,
          steps: 8500,
        },
      ]);
      const all = await read(
        `/v1/walks/summary?period=all&at=2026-03-01&${KYIV}`,
      );
      expect(all).toMatchObject({ from: null, to: null, walks: 3, photos: 3 });
      expect(all.bars[0].key).toBe('2026');
    });

    it('gives the metres of each day of the last 53 weeks', async () => {
      const body = await read(`/v1/walks/calendar?to=2026-10-11&${KYIV}`);
      expect(body).toMatchObject({ from: '2025-10-06', to: '2026-10-11' });
      // The late walk belongs to October 12 there and is outside.
      expect(body.days).toEqual([
        { day: '2026-10-05', distance_m: 3000 },
        { day: '2026-10-07', distance_m: 1500 },
      ]);
    });

    it('gives one walk with the whole route, the breaks, the moods and its photos', async () => {
      const body = await read(`/v1/walks/${walk(1)}`);
      expect(body).toMatchObject({
        id: walk(1),
        breaks: '600-900',
        mood_before: 2,
        mood_after: 4,
        route: JSON.parse(ROUTE),
      });
      expect((body.photos as { id: string }[]).map((item) => item.id)).toEqual([
        photo(1),
        photo(2),
      ]);
      expect(body.photos[0]).not.toHaveProperty('photo_path');
    });
  });

  describe('photos', () => {
    it('lists the photos, the latest first, and filters them', async () => {
      expect(ids(await read('/v1/photos'))).toEqual([
        photo(2),
        photo(1),
        photo(3),
      ]);
      expect(ids(await read('/v1/photos?mode=best'))).toEqual([photo(1)]);
      expect(ids(await read('/v1/photos?kind=bird'))).toEqual([photo(2)]);
      expect(ids(await read('/v1/photos?q=YARR'))).toEqual([
        photo(1),
        photo(3),
      ]);
      expect(ids(await read('/v1/photos?q=%25'))).toEqual([]);
      expect(
        ids(await read('/v1/photos?from=2026-10-01T00:00:00Z&kind=plant')),
      ).toEqual([photo(1)]);
    });

    it('gives the marks of the map without the rest', async () => {
      const body = await read('/v1/photos/map?kind=plant');
      expect(body.items).toHaveLength(2);
      expect(body.items).toContainEqual({
        id: photo(1),
        latitude: 50.58,
        longitude: 34.48,
        kind: 'plant',
      });
    });

    it('gives one photo with its walk and the months its name was shot in', async () => {
      const body = await read(`/v1/photos/${photo(1)}?${KYIV}`);
      expect(body).toMatchObject({
        id: photo(1),
        name: 'Common yarrow',
        walk: { id: walk(1), name: 'Misty Evening', number: 1 },
        months: ['2026-09', '2026-10'],
      });
      expect(body).not.toHaveProperty('photo_path');
      expect(body).not.toHaveProperty('user_id');
    });
  });

  describe('health', () => {
    it('lists the entries of one kind', async () => {
      const body = await read('/v1/health?kind=weight');
      expect(body.items).toMatchObject([
        { value: 65, diastolic: null },
        { value: 64 },
      ]);
      expect((await read('/v1/health?kind=pressure')).items).toMatchObject([
        { value: 122, diastolic: 78 },
      ]);
    });

    it('gives the points of a week and the walks under them', async () => {
      const body = await read(
        `/v1/health/summary?kind=weight&period=week&at=2026-10-07&${KYIV}`,
      );
      expect(body.points).toMatchObject([{ value: 64 }, { value: 65 }]);
      expect((body.walks as { key: string }[]).map((bar) => bar.key)).toEqual([
        '2026-10-05',
        '2026-10-07',
      ]);
    });

    it('gives the mean of each month for a year', async () => {
      const body = await read(
        `/v1/health/summary?kind=weight&period=year&at=2026-10-07&${KYIV}`,
      );
      expect(body.points).toEqual([
        { key: '2026-10', value: 64.5, diastolic: null, entries: 2 },
      ]);
      expect(body.walks).toMatchObject([{ key: '2026-10', walks: 3 }]);
    });
  });

  describe('another user', () => {
    it('sees only her own rows in every list and every sum', async () => {
      expect(ids(await read('/v1/walks', B))).toEqual([walk(9)]);
      expect(
        await read(`/v1/walks/summary?period=all&at=2026-10-07`, B),
      ).toMatchObject({ walks: 1, distance_m: 7000, photos: 1 });
      expect((await read('/v1/walks/calendar?to=2026-10-11', B)).days).toEqual([
        { day: '2026-10-06', distance_m: 7000 },
      ]);
      expect(ids(await read('/v1/photos', B))).toEqual([photo(9)]);
      expect(ids(await read('/v1/photos/map', B))).toEqual([photo(9)]);
      expect((await read(`/v1/photos/${photo(9)}`, B)).months).toEqual([
        '2026-10',
      ]);
      expect((await read('/v1/health?kind=weight', B)).items).toMatchObject([
        { value: 90 },
      ]);
      const chart = await read(
        '/v1/health/summary?kind=weight&period=year&at=2026-10-07',
        B,
      );
      expect(chart.points).toMatchObject([{ value: 90, entries: 1 }]);
      expect(chart.walks).toMatchObject([{ walks: 1 }]);
    });

    it('gets 404 for a walk and a photo that are not hers, as for ones that do not exist', async () => {
      const missing = {
        error: { code: 'not_found', message: 'there is no such row' },
      };
      await it_
        .get(`/v1/walks/${walk(1)}`, B)
        .expect(404)
        .expect(missing);
      await it_
        .get(`/v1/photos/${photo(1)}`, B)
        .expect(404)
        .expect(missing);
      await it_.get(`/v1/walks/${walk(7)}`, A).expect(404);
      await it_.get('/v1/walks/not-an-id', A).expect(404);
    });

    it('does not see the photos of a walk that is not hers even by its id', async () => {
      await it_.sql.exec(
        `update finds set walk_id = '${walk(9)}' where id = '${photo(3)}'`,
      );
      const body = await read(`/v1/walks/${walk(9)}`, B);
      expect((body.photos as { id: string }[]).map((item) => item.id)).toEqual([
        photo(9),
      ]);
      await it_.sql.exec(
        `update finds set walk_id = '${walk(2)}' where id = '${photo(3)}'`,
      );
    });
  });

  it('refuses wrong parameters with their names', async () => {
    const wrong = async (path: string, field: string, problem: string) => {
      const { body } = await it_.get(path, A).expect(400);
      expect(body).toEqual({
        error: {
          code: 'validation_failed',
          message: `${field} is ${problem}`,
          fields: [{ field, problem }],
        },
      });
    };
    await wrong('/v1/walks?from=yesterday', 'from', 'not_a_time');
    await wrong('/v1/walks?limit=500', 'limit', 'out_of_range');
    await wrong('/v1/walks/summary?at=2026-10-07', 'period', 'required');
    await wrong(
      '/v1/walks/summary?period=decade&at=2026-10-07',
      'period',
      'not_allowed',
    );
    await wrong(
      '/v1/walks/summary?period=week&at=7.10.2026',
      'at',
      'not_a_day',
    );
    await wrong(
      '/v1/walks/calendar?to=2026-10-11&tz=Mars/Olympus',
      'tz',
      'not_a_time_zone',
    );
    await wrong('/v1/photos?kind=fish', 'kind', 'not_allowed');
    await wrong('/v1/health', 'kind', 'required');
  });
});

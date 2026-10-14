import { start, Started } from './start';
import { A, B } from './tokens';

const WALK = '10000000-0000-4000-8000-000000000001';
const FOREIGN_WALK = '10000000-0000-4000-8000-000000000009';
const photo = (n: number) => `20000000-0000-4000-8000-00000000000${n}`;
const entry = (n: number) => `30000000-0000-4000-8000-00000000000${n}`;

const ROWS = `
  insert into walks (id, user_id, number, name, started_at, ended_at, duration_s) values
    ('${WALK}', '${A}', 1, 'Misty Evening', '2026-10-05T16:00:00Z', '2026-10-05T16:40:00Z', 2400),
    ('${FOREIGN_WALK}', '${B}', 1, 'Foreign Walk', '2026-10-06T10:00:00Z', '2026-10-06T11:00:00Z', 3600);
  insert into finds (id, user_id, walk_id, taken_at, latitude, longitude, photo_path, kind, name) values
    ('${photo(1)}', '${A}', '${WALK}', '2026-10-05T16:10:00Z', 50.58, 34.48, 'a/1.jpg', 'plant', 'Common yarrow'),
    ('${photo(2)}', '${A}', '${WALK}', '2026-10-05T16:20:00Z', 50.59, 34.49, 'a/lost.jpg', 'unknown', null),
    ('${photo(3)}', '${A}', '${WALK}', '2026-10-05T16:30:00Z', 50.59, 34.49, 'a/3.jpg', 'bird', 'Great tit'),
    ('${photo(9)}', '${B}', '${FOREIGN_WALK}', '2026-10-06T10:30:00Z', 48.00, 30.00, 'b/9.jpg', 'plant', 'Oak');
  insert into health_entries (id, user_id, kind, measured_at, value, diastolic) values
    ('${entry(1)}', '${A}', 'weight', '2026-10-05T06:00:00Z', 64.0, null),
    ('${entry(2)}', '${A}', 'pressure', '2026-10-05T06:00:00Z', 120, 80),
    ('${entry(9)}', '${B}', 'weight', '2026-10-06T06:00:00Z', 90.0, null);
`;
const MISSING = {
  error: { code: 'not_found', message: 'there is no such row' },
};

describe('writing', () => {
  let it_: Started;
  const read = async (path: string, user = A) =>
    (await it_.get(path, user).expect(200)).body as Record<string, any>;
  const refused = async (
    method: 'post' | 'patch',
    path: string,
    body: object,
    message: string,
  ) => {
    const answer = await it_.send(method, path, A, body).expect(400);
    expect(answer.body.error).toMatchObject({
      code: 'validation_failed',
      message,
    });
  };

  beforeEach(async () => {
    it_ = await start();
    await it_.sql.exec(ROWS);
  });

  afterEach(() => it_.app.close());

  it('gives a one-time link to the file of a photo and null for a file that is lost', async () => {
    const link = (path: string) =>
      `https://project.supabase.co/storage/v1/object/sign/photos/${path}?token=t`;
    const grid = await read('/v1/photos');
    expect(grid.items.map((item: { url: string }) => item.url)).toEqual([
      link('a/3.jpg'),
      null,
      link('a/1.jpg'),
    ]);
    expect((await read(`/v1/photos/${photo(1)}`)).url).toBe(link('a/1.jpg'));
    expect((await read(`/v1/walks/${WALK}`)).photos[0].url).toBe(
      link('a/1.jpg'),
    );
  });

  it('renames a walk and changes its note', async () => {
    const { body } = await it_
      .send('patch', `/v1/walks/${WALK}`, A, {
        name: '  By the river ',
        note: 'Cold',
      })
      .expect(200);
    expect(body).toMatchObject({
      id: WALK,
      name: 'By the river',
      note: 'Cold',
    });
    expect((await read(`/v1/walks/${WALK}`)).name).toBe('By the river');
    await it_.send('patch', `/v1/walks/${WALK}`, A, { note: '' }).expect(200);
    expect((await read(`/v1/walks/${WALK}`)).note).toBeNull();
  });

  it('refuses a change of a walk that is empty, too long or touches another field', async () => {
    await refused('patch', `/v1/walks/${WALK}`, {}, 'body is empty');
    await refused(
      'patch',
      `/v1/walks/${WALK}`,
      { name: 'x'.repeat(81) },
      'name is too_long',
    );
    await refused(
      'patch',
      `/v1/walks/${WALK}`,
      { distance_m: 99 },
      'distance_m is unknown',
    );
    await refused(
      'patch',
      `/v1/walks/${WALK}`,
      { user_id: B },
      'user_id is unknown',
    );
  });

  it('deletes a walk with its photos: they are gone from every answer', async () => {
    await it_.send('delete', `/v1/walks/${WALK}`, A).expect(204);
    await it_.get(`/v1/walks/${WALK}`, A).expect(404);
    expect((await read('/v1/walks')).items).toEqual([]);
    expect((await read('/v1/photos')).items).toEqual([]);
    expect(
      await read('/v1/walks/summary?period=all&at=2026-10-07'),
    ).toMatchObject({ walks: 0, photos: 0 });
    await it_.send('delete', `/v1/walks/${WALK}`, A).expect(404);
    // Marked, not removed: the phone has to learn about it.
    const kept = await it_.sql.query(
      `select count(*)::int n from finds where walk_id = '${WALK}' and deleted_at is not null`,
    );
    expect(kept.rows).toEqual([{ n: 3 }]);
  });

  it('changes the name, the kind and the Best mark of a photo', async () => {
    const patch = {
      name: 'Yarrow',
      latin_name: 'Achillea millefolium',
      kind: 'plant',
      is_best: true,
    };
    const { body } = await it_
      .send('patch', `/v1/photos/${photo(2)}`, A, patch)
      .expect(200);
    expect(body).toMatchObject({ id: photo(2), ...patch, walk: { id: WALK } });
    expect((await read('/v1/photos?mode=best')).items).toHaveLength(1);
    await refused(
      'patch',
      `/v1/photos/${photo(2)}`,
      { kind: 'fish' },
      'kind is not_allowed',
    );
    await refused(
      'patch',
      `/v1/photos/${photo(2)}`,
      { is_best: 1 },
      'is_best is not_a_flag',
    );
    await refused(
      'patch',
      `/v1/photos/${photo(2)}`,
      { photo_path: 'b/9.jpg' },
      'photo_path is unknown',
    );
  });

  it('marks, unmarks and deletes several photos at once', async () => {
    const ids = [photo(1), photo(3)];
    await it_
      .send('post', '/v1/photos/batch', A, { ids, action: 'best' })
      .expect(200)
      .expect({ changed: 2 });
    expect((await read('/v1/photos?mode=best')).items).toHaveLength(2);
    await it_
      .send('post', '/v1/photos/batch', A, {
        ids: [photo(1)],
        action: 'unbest',
      })
      .expect({ changed: 1 });
    await it_
      .send('post', '/v1/photos/batch', A, { ids, action: 'delete' })
      .expect({ changed: 2 });
    expect(
      (await read('/v1/photos')).items.map((item: { id: string }) => item.id),
    ).toEqual([photo(2)]);
    await refused(
      'post',
      '/v1/photos/batch',
      { ids: [], action: 'best' },
      'ids is out_of_range',
    );
    await refused(
      'post',
      '/v1/photos/batch',
      { ids, action: 'burn' },
      'action is not_allowed',
    );
    await refused('post', '/v1/photos/batch', { ids }, 'action is required');
  });

  it('deletes one photo', async () => {
    await it_.send('delete', `/v1/photos/${photo(1)}`, A).expect(204);
    await it_.get(`/v1/photos/${photo(1)}`, A).expect(404);
    expect((await read(`/v1/walks/${WALK}`)).photos).toHaveLength(2);
  });

  it('adds a health entry once, however many times the request is sent', async () => {
    const added = {
      id: entry(5),
      kind: 'glucose',
      measured_at: '2026-10-07T06:00:00Z',
      value: 5.4,
    };
    const { body } = await it_.send('post', '/v1/health', A, added).expect(201);
    expect(body).toEqual({
      ...added,
      measured_at: '2026-10-07T06:00:00.000Z',
      diastolic: null,
    });
    await it_.send('post', '/v1/health', A, added).expect(201);
    expect((await read('/v1/health?kind=glucose')).items).toHaveLength(1);
  });

  it('refuses a mood, a pressure without its second number and a wrong value', async () => {
    const at = { measured_at: '2026-10-07T06:00:00Z' };
    await refused(
      'post',
      '/v1/health',
      { id: entry(5), kind: 'mood', value: 4, ...at },
      'kind is not_allowed',
    );
    await refused(
      'post',
      '/v1/health',
      { id: entry(5), kind: 'pressure', value: 120, ...at },
      'diastolic is required',
    );
    await refused(
      'post',
      '/v1/health',
      { id: entry(5), kind: 'weight', value: 64, diastolic: 80, ...at },
      'diastolic is not_allowed',
    );
    await refused(
      'post',
      '/v1/health',
      { id: entry(5), kind: 'weight', value: -1, ...at },
      'value is out_of_range',
    );
    await refused(
      'post',
      '/v1/health',
      { kind: 'weight', value: 64, ...at },
      'id is required',
    );
    await refused(
      'post',
      '/v1/health',
      { id: entry(9), kind: 'weight', value: 64, ...at },
      'id is taken',
    );
  });

  it('changes and deletes a health entry', async () => {
    const { body } = await it_
      .send('patch', `/v1/health/${entry(2)}`, A, { value: 125, diastolic: 82 })
      .expect(200);
    expect(body).toMatchObject({ id: entry(2), value: 125, diastolic: 82 });
    await refused(
      'patch',
      `/v1/health/${entry(1)}`,
      { diastolic: 80 },
      'diastolic is not_allowed',
    );
    await refused(
      'patch',
      `/v1/health/${entry(2)}`,
      { diastolic: null },
      'diastolic is required',
    );
    await it_.send('delete', `/v1/health/${entry(1)}`, A).expect(204);
    expect((await read('/v1/health?kind=weight')).items).toEqual([]);
  });

  it('lets nobody change or delete the rows of another user', async () => {
    await it_
      .send('patch', `/v1/walks/${WALK}`, B, { name: 'Mine now' })
      .expect(404)
      .expect(MISSING);
    await it_.send('delete', `/v1/walks/${WALK}`, B).expect(404);
    await it_
      .send('patch', `/v1/photos/${photo(1)}`, B, { is_best: true })
      .expect(404);
    await it_.send('delete', `/v1/photos/${photo(1)}`, B).expect(404);
    await it_
      .send('post', '/v1/photos/batch', B, {
        ids: [photo(1), photo(9)],
        action: 'delete',
      })
      .expect({ changed: 1 });
    await it_
      .send('patch', `/v1/health/${entry(1)}`, B, { value: 1 })
      .expect(404);
    await it_.send('delete', `/v1/health/${entry(1)}`, B).expect(404);
    expect(await read(`/v1/walks/${WALK}`)).toMatchObject({
      name: 'Misty Evening',
    });
    expect((await read('/v1/photos')).items).toHaveLength(3);
    expect((await read(`/v1/photos/${photo(1)}`)).is_best).toBe(false);
    expect((await read('/v1/health?kind=weight')).items).toMatchObject([
      { value: 64 },
    ]);
  });

  it('answers 401 to every change without a token', async () => {
    const server = it_.app.getHttpServer();
    const request = (await import('supertest')).default;
    await request(server)
      .patch(`/v1/walks/${WALK}`)
      .send({ name: 'x' })
      .expect(401);
    await request(server)
      .delete(`/v1/photos/${photo(1)}`)
      .expect(401);
    await request(server).post('/v1/health').send({}).expect(401);
  });
});

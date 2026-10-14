import { readSettings } from './config';

const env = {
  SUPABASE_URL: 'https://project.supabase.co/',
  DATABASE_URL: 'postgresql://user:secret@host:6543/postgres',
  SUPABASE_SECRET_KEY: 'key',
};

describe('readSettings', () => {
  it('takes the port, the project, the database and the key from the environment', () => {
    expect(readSettings({ PORT: '3000', ...env })).toEqual({
      port: 3000,
      supabaseUrl: 'https://project.supabase.co',
      databaseUrl: env.DATABASE_URL,
      supabaseKey: 'key',
      demo: null,
      forecaKey: null,
    });
  });

  it('takes the demo account only whole', () => {
    const demo = {
      DEMO_USER_ID: '00000000-0000-4000-8000-00000000000d',
      DEMO_EMAIL: 'demo@walkpick.app',
      DEMO_PASSWORD: 'p',
    };
    expect(readSettings({ ...env, ...demo }).demo).toEqual({
      userId: demo.DEMO_USER_ID,
      email: demo.DEMO_EMAIL,
      password: 'p',
    });
    expect(() =>
      readSettings({ ...env, ...demo, DEMO_PASSWORD: undefined }),
    ).toThrow('all together');
  });

  it('falls back to the port Cloud Run expects', () => {
    expect(readSettings(env).port).toBe(8080);
  });

  it('refuses a port that is not a number', () => {
    expect(() => readSettings({ PORT: 'http', ...env })).toThrow('PORT');
  });

  it('refuses to start without any of its three settings', () => {
    for (const name of Object.keys(env)) {
      expect(() => readSettings({ ...env, [name]: undefined })).toThrow(name);
    }
  });
});

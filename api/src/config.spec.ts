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
    });
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

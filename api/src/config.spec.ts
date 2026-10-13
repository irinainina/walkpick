import { readSettings } from './config';

const SUPABASE_URL = 'https://project.supabase.co/';
const DATABASE_URL = 'postgresql://user:secret@host:6543/postgres';

describe('readSettings', () => {
  it('takes the port, the project and the database from the environment', () => {
    expect(readSettings({ PORT: '3000', SUPABASE_URL, DATABASE_URL })).toEqual({
      port: 3000,
      supabaseUrl: 'https://project.supabase.co',
      databaseUrl: DATABASE_URL,
    });
  });

  it('falls back to the port Cloud Run expects', () => {
    expect(readSettings({ SUPABASE_URL, DATABASE_URL }).port).toBe(8080);
  });

  it('refuses a port that is not a number', () => {
    expect(() =>
      readSettings({ PORT: 'http', SUPABASE_URL, DATABASE_URL }),
    ).toThrow('PORT');
  });

  it('refuses to start without the Supabase project or the database', () => {
    expect(() => readSettings({ DATABASE_URL })).toThrow('SUPABASE_URL');
    expect(() => readSettings({ SUPABASE_URL })).toThrow('DATABASE_URL');
  });
});

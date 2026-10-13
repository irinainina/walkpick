import { readSettings } from './config';

const SUPABASE_URL = 'https://project.supabase.co/';

describe('readSettings', () => {
  it('takes the port and the project from the environment', () => {
    expect(readSettings({ PORT: '3000', SUPABASE_URL })).toEqual({
      port: 3000,
      supabaseUrl: 'https://project.supabase.co',
    });
  });

  it('falls back to the port Cloud Run expects', () => {
    expect(readSettings({ SUPABASE_URL }).port).toBe(8080);
  });

  it('refuses a port that is not a number', () => {
    expect(() => readSettings({ PORT: 'http', SUPABASE_URL })).toThrow('PORT');
  });

  it('refuses to start without the Supabase project', () => {
    expect(() => readSettings({})).toThrow('SUPABASE_URL');
  });
});

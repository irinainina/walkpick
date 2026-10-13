import { readSettings } from './config';

describe('readSettings', () => {
  it('takes the port from the environment', () => {
    expect(readSettings({ PORT: '3000' })).toEqual({ port: 3000 });
  });

  it('falls back to the port Cloud Run expects', () => {
    expect(readSettings({})).toEqual({ port: 8080 });
  });

  it('refuses a port that is not a number', () => {
    expect(() => readSettings({ PORT: 'http' })).toThrow('PORT');
  });
});

import { Settings } from '../config';
import { SigningKeys } from './signing-keys';
import { TokenError } from './token';

const settings = new Settings(
  8080,
  'https://project.supabase.co',
  'postgres://x',
  'key',
);
const answer = () =>
  Promise.resolve(
    new Response(JSON.stringify({ keys: [{ kid: 'k1', kty: 'EC' }] })),
  );

describe('SigningKeys', () => {
  afterEach(() => jest.restoreAllMocks());

  it('asks Supabase once for requests that come together and answers them all', async () => {
    const asked = jest.spyOn(global, 'fetch').mockImplementation(answer);
    const keys = new SigningKeys(settings);

    const found = await Promise.all([1, 2, 3, 4, 5].map(() => keys.find('k1')));

    expect(found).toHaveLength(5);
    expect(found.every((key) => key.kid === 'k1')).toBe(true);
    expect(asked).toHaveBeenCalledTimes(1);
    await keys.find('k1');
    expect(asked).toHaveBeenCalledTimes(1);
  });

  it('refuses an unknown key without asking Supabase again within a minute', async () => {
    const asked = jest.spyOn(global, 'fetch').mockImplementation(answer);
    const keys = new SigningKeys(settings);

    await expect(keys.find('forged')).rejects.toThrow(TokenError);
    await expect(keys.find('forged')).rejects.toThrow(TokenError);

    expect(asked).toHaveBeenCalledTimes(1);
  });
});

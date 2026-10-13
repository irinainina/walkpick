import { generateKeyPairSync } from 'node:crypto';
import { ISSUER, part, publicKey as key, signToken } from '../../test/tokens';
import { keyIdOf, TokenError, userOf } from './token';

const NOW = new Date('2026-10-13T12:00:00Z');
const SOON = NOW.getTime() / 1000 + 60;
const claims = { iss: ISSUER, aud: 'authenticated', sub: 'user-a', exp: SOON };
const read = (token: string) => () => userOf(token, key, ISSUER, NOW);

describe('userOf', () => {
  it('gives the user of a token signed by the project', () => {
    expect(read(signToken(claims))()).toBe('user-a');
    expect(keyIdOf(signToken(claims))).toBe('k1');
  });

  it('refuses a token signed by somebody else', () => {
    const stranger = generateKeyPairSync('ec', {
      namedCurve: 'P-256',
    }).privateKey;
    expect(read(signToken(claims, undefined, stranger))).toThrow(TokenError);
  });

  it('refuses a token whose user was changed after signing', () => {
    const [head, , signature] = signToken(claims).split('.');
    expect(
      read(`${head}.${part({ ...claims, sub: 'user-b' })}.${signature}`),
    ).toThrow('signature');
  });

  it('refuses an expired token, another project, an anonymous key and an unsigned token', () => {
    expect(read(signToken({ ...claims, exp: NOW.getTime() / 1000 }))).toThrow(
      'expired',
    );
    expect(read(signToken({ ...claims, iss: 'https://other' }))).toThrow(
      'project',
    );
    expect(read(signToken({ ...claims, aud: 'anon' }))).toThrow('signed-in');
    expect(read(signToken(claims, { alg: 'none' }))).toThrow('ES256');
    expect(read('not.a.token')).toThrow(TokenError);
  });
});

import { generateKeyPairSync, KeyObject, sign } from 'node:crypto';
import { SigningKey } from '../src/auth/token';

export const A = '00000000-0000-4000-8000-00000000000a';
export const B = '00000000-0000-4000-8000-00000000000b';
/** A user who has signed in and written nothing yet. */
export const C = '00000000-0000-4000-8000-00000000000c';
/** The read-only account behind "View demo". */
export const DEMO = '00000000-0000-4000-8000-00000000000d';

export const ISSUER = 'https://project.supabase.co/auth/v1';

const pair = generateKeyPairSync('ec', { namedCurve: 'P-256' });

export const publicKey: SigningKey = pair.publicKey.export({ format: 'jwk' });

export const part = (value: object) =>
  Buffer.from(JSON.stringify(value)).toString('base64url');

/** A token as Supabase signs it, for the tests. */
export function signToken(
  claims: object,
  head: object = { alg: 'ES256', kid: 'k1' },
  by: KeyObject = pair.privateKey,
): string {
  const signed = `${part(head)}.${part(claims)}`;
  const signature = sign('sha256', Buffer.from(signed), {
    key: by,
    dsaEncoding: 'ieee-p1363',
  });
  return `${signed}.${signature.toString('base64url')}`;
}

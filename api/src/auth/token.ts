import { createPublicKey, JsonWebKey, verify } from 'node:crypto';

export interface SigningKey extends JsonWebKey {
  kid?: string;
}

/** Why a token was refused; the text goes to the log, never to the user. */
export class TokenError extends Error {}

function decode(part: string): Record<string, unknown> {
  try {
    return JSON.parse(Buffer.from(part, 'base64url').toString()) as Record<
      string,
      unknown
    >;
  } catch {
    throw new TokenError('the token is not a JWT');
  }
}

/** The id of the key that signed the token. */
export function keyIdOf(token: string): string {
  const kid = decode(token.split('.')[0] ?? '').kid;
  if (typeof kid !== 'string') throw new TokenError('the token names no key');
  return kid;
}

/** The user a Supabase access token belongs to; throws TokenError on anything else. */
export function userOf(
  token: string,
  key: SigningKey,
  issuer: string,
  now: Date,
): string {
  const [head, body, signature, ...rest] = token.split('.');
  if (!head || !body || !signature || rest.length > 0)
    throw new TokenError('the token is not a JWT');
  // Supabase signs with ES256: a header naming another algorithm is a forgery.
  if (decode(head).alg !== 'ES256')
    throw new TokenError('the token is not signed with ES256');
  const signed = verify(
    'sha256',
    Buffer.from(`${head}.${body}`),
    { key: createPublicKey({ key, format: 'jwk' }), dsaEncoding: 'ieee-p1363' },
    Buffer.from(signature, 'base64url'),
  );
  if (!signed) throw new TokenError('the signature is wrong');
  const claims = decode(body);
  if (claims.iss !== issuer)
    throw new TokenError('the token is from another project');
  if (claims.aud !== 'authenticated')
    throw new TokenError('the token is not of a signed-in user');
  if (typeof claims.exp !== 'number' || claims.exp * 1000 <= now.getTime()) {
    throw new TokenError('the token has expired');
  }
  if (typeof claims.sub !== 'string' || !claims.sub)
    throw new TokenError('the token names no user');
  return claims.sub;
}

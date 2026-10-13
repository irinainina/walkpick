import { Injectable } from '@nestjs/common';
import { Settings } from '../config';
import { SigningKey, TokenError } from './token';

// An unknown key id is asked from Supabase at most this often: a flood of forged tokens must not
// turn into a flood of requests.
const ASK_AGAIN_MS = 60_000;

/** The public keys of the Supabase project, fetched once and again when a new key appears. */
@Injectable()
export class SigningKeys {
  private keys = new Map<string, SigningKey>();
  private askedAt = 0;

  constructor(private readonly settings: Settings) {}

  get issuer(): string {
    return `${this.settings.supabaseUrl}/auth/v1`;
  }

  async find(kid: string): Promise<SigningKey> {
    if (!this.keys.has(kid) && Date.now() - this.askedAt > ASK_AGAIN_MS) {
      this.askedAt = Date.now();
      const answer = await fetch(`${this.issuer}/.well-known/jwks.json`);
      if (!answer.ok)
        throw new Error(`Supabase did not give its keys: ${answer.status}`);
      const { keys } = (await answer.json()) as { keys: SigningKey[] };
      this.keys = new Map(keys.map((key) => [key.kid ?? '', key]));
    }
    const key = this.keys.get(kid);
    if (!key) throw new TokenError('the token is signed with an unknown key');
    return key;
  }
}

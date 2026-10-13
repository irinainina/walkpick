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
  private asking?: Promise<void>;

  constructor(private readonly settings: Settings) {}

  get issuer(): string {
    return `${this.settings.supabaseUrl}/auth/v1`;
  }

  async find(kid: string): Promise<SigningKey> {
    if (!this.keys.has(kid)) await this.ask();
    const key = this.keys.get(kid);
    if (!key) throw new TokenError('the token is signed with an unknown key');
    return key;
  }

  // Requests that come while the keys are on their way wait for the same answer: a page sends
  // several at once, and all but the first would be refused.
  private ask(): Promise<void> {
    if (this.asking) return this.asking;
    if (Date.now() - this.askedAt <= ASK_AGAIN_MS) return Promise.resolve();
    this.askedAt = Date.now();
    this.asking = this.load().finally(() => (this.asking = undefined));
    return this.asking;
  }

  private async load(): Promise<void> {
    const answer = await fetch(`${this.issuer}/.well-known/jwks.json`);
    if (!answer.ok)
      throw new Error(`Supabase did not give its keys: ${answer.status}`);
    const { keys } = (await answer.json()) as { keys: SigningKey[] };
    this.keys = new Map(keys.map((key) => [key.kid ?? '', key]));
  }
}

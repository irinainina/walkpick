import { Injectable } from '@nestjs/common';
import { eq } from 'drizzle-orm';
import { Database } from './db/database';
import { profiles, settings } from './db/schema';

export interface Account {
  id: string;
  profile: typeof profiles.$inferSelect | null;
  settings: typeof settings.$inferSelect | null;
}

/** The profile and the settings of a user; either is null until the phone has written it. */
@Injectable()
export class Accounts {
  constructor(private readonly database: Database) {}

  async find(userId: string): Promise<Account> {
    const { rows } = this.database;
    const [[profile], [chosen]] = await Promise.all([
      rows.select().from(profiles).where(eq(profiles.id, userId)),
      rows.select().from(settings).where(eq(settings.user_id, userId)),
    ]);
    return { id: userId, profile: profile ?? null, settings: chosen ?? null };
  }
}

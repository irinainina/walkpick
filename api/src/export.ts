import { Injectable } from '@nestjs/common';
import { asc, getTableColumns, sql } from 'drizzle-orm';
import { Accounts } from './accounts';
import { Database } from './db/database';
import { ownedBy } from './db/owned';
import { dailySteps, finds, healthEntries, walks } from './db/schema';
import { PhotoLinks } from './photos/photo-links';
import { geoJson } from './walks/walks';

// The file is kept and opened later; the photos in it have to outlive a page of the site.
const LINK_SECONDS = 7 * 24 * 3600;

/** The columns a user's own copy carries: not the owner, not the deletion mark. */
function shown<T extends Record<string, unknown>>(columns: T) {
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  const { user_id, deleted_at, ...rest } = columns;
  return rest;
}

/** Everything the user has written, in one JSON; photo files are links that live a week. */
@Injectable()
export class Export {
  constructor(
    private readonly database: Database,
    private readonly accounts: Accounts,
    private readonly links: PhotoLinks,
  ) {}

  async of(userId: string) {
    const { rows } = this.database;
    const [account, steps, walked, photos, health] = await Promise.all([
      this.accounts.find(userId),
      rows
        .select(shown(getTableColumns(dailySteps)))
        .from(dailySteps)
        .where(ownedBy(dailySteps, userId))
        .orderBy(asc(dailySteps.day)),
      rows
        .select({
          ...shown(getTableColumns(walks)),
          route: sql<string | null>`st_asgeojson(${walks.route})`,
        })
        .from(walks)
        .where(ownedBy(walks, userId))
        .orderBy(asc(walks.started_at)),
      rows
        .select(shown(getTableColumns(finds)))
        .from(finds)
        .where(ownedBy(finds, userId))
        .orderBy(asc(finds.taken_at)),
      rows
        .select(shown(getTableColumns(healthEntries)))
        .from(healthEntries)
        .where(ownedBy(healthEntries, userId))
        .orderBy(asc(healthEntries.measured_at)),
    ]);
    return {
      exported_at: new Date(),
      ...account,
      daily_steps: steps,
      walks: walked.map((walk) => ({ ...walk, route: geoJson(walk.route) })),
      photos: await this.links.attach(photos, LINK_SECONDS),
      health_entries: health,
    };
  }
}

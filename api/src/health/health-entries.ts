import { Injectable } from '@nestjs/common';
import { and, asc, desc, eq, gte, lt, sql } from 'drizzle-orm';
import { Database } from '../db/database';
import { ownedBy } from '../db/owned';
import { healthEntries } from '../db/schema';
import { localKey, Period, spanOf, within } from '../period';
import { invalid, page } from '../query';
import { Walks } from '../walks/walks';

export const HEALTH_KINDS = healthEntries.kind.enumValues;
export type HealthKind = (typeof HEALTH_KINDS)[number];

const entry = {
  id: healthEntries.id,
  kind: healthEntries.kind,
  measured_at: healthEntries.measured_at,
  value: healthEntries.value,
  diastolic: healthEntries.diastolic,
};

export interface NewEntry {
  id: string;
  kind: HealthKind;
  measured_at: Date;
  value: number;
  diastolic: number | null;
}

export type EntryChange = Partial<
  Pick<NewEntry, 'measured_at' | 'value' | 'diastolic'>
>;

// Only a pressure has a second number, and it always has one.
function checkDiastolic(
  kind: HealthKind,
  diastolic: number | null | undefined,
): void {
  if (diastolic === undefined) return;
  if (kind === 'pressure' && diastolic === null)
    invalid('diastolic', 'required');
  if (kind !== 'pressure' && diastolic !== null)
    invalid('diastolic', 'not_allowed');
}

@Injectable()
export class HealthEntries {
  constructor(
    private readonly database: Database,
    private readonly walks: Walks,
  ) {}

  private one(userId: string, id: string) {
    return this.database.rows
      .select(entry)
      .from(healthEntries)
      .where(and(ownedBy(healthEntries, userId), eq(healthEntries.id, id)))
      .then(([found]) => found ?? null);
  }

  async list(
    userId: string,
    kind: HealthKind,
    from: Date | undefined,
    to: Date | undefined,
    size: number,
    before?: Date,
  ) {
    const rows = await this.database.rows
      .select(entry)
      .from(healthEntries)
      .where(
        and(
          ownedBy(healthEntries, userId),
          eq(healthEntries.kind, kind),
          from && gte(healthEntries.measured_at, from),
          to && lt(healthEntries.measured_at, to),
          before && lt(healthEntries.measured_at, before),
        ),
      )
      .orderBy(desc(healthEntries.measured_at))
      .limit(size + 1);
    return page(rows, size, (last) => last.measured_at);
  }

  /** The points of a chart and the walks under them; a year and all the time go by month. */
  async summary(
    userId: string,
    kind: HealthKind,
    period: Period,
    at: string,
    zone: string,
  ) {
    const span = spanOf(period, at);
    const byMonth = span.step !== 'day';
    const { rows } = this.database;
    const mine = and(
      ownedBy(healthEntries, userId),
      eq(healthEntries.kind, kind),
      within(healthEntries.measured_at, zone, span),
    );
    const points = byMonth
      ? rows
          .select({
            key: localKey(healthEntries.measured_at, zone, 'month'),
            value: sql<number>`round(avg(${healthEntries.value}), 1)::float8`,
            diastolic: sql<
              number | null
            >`round(avg(${healthEntries.diastolic}))::int`,
            entries: sql<number>`count(*)::int`,
          })
          .from(healthEntries)
          .where(mine)
          .groupBy(sql`1`)
          .orderBy(sql`1`)
      : rows
          .select(entry)
          .from(healthEntries)
          .where(mine)
          .orderBy(asc(healthEntries.measured_at));
    const [found, walks] = await Promise.all([
      points,
      this.walks.bars(userId, span, zone, byMonth ? 'month' : 'day'),
    ]);
    return { kind, period, from: span.from, to: span.to, points: found, walks };
  }

  /** Writes a new entry; the same request sent twice leaves one entry. */
  async add(userId: string, added: NewEntry) {
    // A mood is marked at the ends of a walk and nowhere else.
    if (added.kind === 'mood') invalid('kind', 'not_allowed');
    checkDiastolic(added.kind, added.diastolic);
    await this.database.rows
      .insert(healthEntries)
      .values({
        ...added,
        user_id: userId,
        created_at: new Date(),
        updated_at: new Date(),
      })
      .onConflictDoNothing();
    // An id that is somebody else's or was deleted cannot be taken again.
    return (await this.one(userId, added.id)) ?? invalid('id', 'taken');
  }

  /** Null when the entry is not this user's. */
  async change(userId: string, id: string, patch: EntryChange) {
    const before = await this.one(userId, id);
    if (!before) return null;
    checkDiastolic(before.kind, patch.diastolic);
    await this.database.rows
      .update(healthEntries)
      .set({ ...patch, updated_at: new Date() })
      .where(and(ownedBy(healthEntries, userId), eq(healthEntries.id, id)));
    return this.one(userId, id);
  }

  async remove(userId: string, id: string): Promise<boolean> {
    const now = new Date();
    const gone = await this.database.rows
      .update(healthEntries)
      .set({ deleted_at: now, updated_at: now })
      .where(and(ownedBy(healthEntries, userId), eq(healthEntries.id, id)))
      .returning({ id: healthEntries.id });
    return gone.length > 0;
  }
}

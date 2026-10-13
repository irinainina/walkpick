import { Injectable } from '@nestjs/common';
import { and, asc, desc, eq, gte, lt, sql } from 'drizzle-orm';
import { Database } from '../db/database';
import { ownedBy } from '../db/owned';
import { healthEntries } from '../db/schema';
import { localKey, Period, spanOf, within } from '../period';
import { page } from '../query';
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

@Injectable()
export class HealthEntries {
  constructor(
    private readonly database: Database,
    private readonly walks: Walks,
  ) {}

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
}

import { Injectable } from '@nestjs/common';
import { and, asc, desc, eq, gte, lt, lte, sql } from 'drizzle-orm';
import { PgColumn } from 'drizzle-orm/pg-core';
import { Database } from '../db/database';
import { ownedBy } from '../db/owned';
import { finds, walks } from '../db/schema';
import {
  localKey,
  Period,
  shiftDay,
  Span,
  spanOf,
  Step,
  within,
} from '../period';
import { page } from '../query';

// Degrees, about five metres: a list of walks must not carry every point of every route.
const SIMPLIFY = 0.00005;
// The squares of a year: 53 weeks ending with the asked day.
const CALENDAR_DAYS = 53 * 7;

const card = {
  id: walks.id,
  number: walks.number,
  name: walks.name,
  note: walks.note,
  started_at: walks.started_at,
  ended_at: walks.ended_at,
  duration_s: walks.duration_s,
  steps: walks.steps,
  distance_m: walks.distance_m,
  calories: walks.calories,
  mood_before: walks.mood_before,
  mood_after: walks.mood_after,
};

export const photoCard = {
  id: finds.id,
  walk_id: finds.walk_id,
  taken_at: finds.taken_at,
  latitude: finds.latitude,
  longitude: finds.longitude,
  kind: finds.kind,
  name: finds.name,
  latin_name: finds.latin_name,
  is_best: finds.is_best,
};

const total = (column: PgColumn) =>
  sql<number>`coalesce(sum(${column}), 0)::int`;
const geoJson = (route: string | null): unknown =>
  route ? JSON.parse(route) : null;

export interface Bar {
  key: string;
  walks: number;
  distance_m: number;
  duration_s: number;
  steps: number;
}

@Injectable()
export class Walks {
  constructor(private readonly database: Database) {}

  async list(
    userId: string,
    from: Date | undefined,
    to: Date | undefined,
    size: number,
    before?: Date,
  ) {
    const rows = await this.database.rows
      .select({
        ...card,
        route: sql<
          string | null
        >`st_asgeojson(st_simplify(${walks.route}::geometry, ${SIMPLIFY}))`,
        // Written out: inside a sub-query the query builder drops the table from a column's name.
        photos: sql<number>`(select count(*)::int from finds where finds.walk_id = walks.id)`,
      })
      .from(walks)
      .where(
        and(
          ownedBy(walks, userId),
          from && gte(walks.started_at, from),
          to && lt(walks.started_at, to),
          before && lt(walks.started_at, before),
        ),
      )
      .orderBy(desc(walks.started_at))
      .limit(size + 1);
    return page(
      rows.map((row) => ({ ...row, route: geoJson(row.route) })),
      size,
      (last) => last.started_at,
    );
  }

  /** The walks of a span added up by day, month or year; a key without walks is left out. */
  bars(
    userId: string,
    span: Span,
    zone: string,
    step: Step = span.step,
  ): Promise<Bar[]> {
    return this.database.rows
      .select({
        key: localKey(walks.started_at, zone, step),
        walks: sql<number>`count(*)::int`,
        distance_m: total(walks.distance_m),
        duration_s: total(walks.duration_s),
        steps: total(walks.steps),
      })
      .from(walks)
      .where(and(ownedBy(walks, userId), within(walks.started_at, zone, span)))
      .groupBy(sql`1`)
      .orderBy(sql`1`);
  }

  async summary(userId: string, period: Period, at: string, zone: string) {
    const span = spanOf(period, at);
    const [bars, [{ photos }]] = await Promise.all([
      this.bars(userId, span, zone),
      this.database.rows
        .select({ photos: sql<number>`count(*)::int` })
        .from(finds)
        .where(and(ownedBy(finds, userId), within(finds.taken_at, zone, span))),
    ]);
    const sum = (field: 'walks' | 'distance_m' | 'duration_s' | 'steps') =>
      bars.reduce((all, bar) => all + bar[field], 0);
    return {
      period,
      from: span.from,
      to: span.to,
      walks: sum('walks'),
      distance_m: sum('distance_m'),
      duration_s: sum('duration_s'),
      steps: sum('steps'),
      photos,
      bars,
    };
  }

  async calendar(userId: string, to: string, zone: string) {
    const from = shiftDay(to, 1 - CALENDAR_DAYS);
    const day = localKey(walks.started_at, zone);
    const days = await this.database.rows
      .select({ day, distance_m: total(walks.distance_m) })
      .from(walks)
      .where(and(ownedBy(walks, userId), gte(day, from), lte(day, to)))
      .groupBy(sql`1`)
      .orderBy(sql`1`);
    return { from, to, days };
  }

  /** One walk with its whole route and its photos; null when it is not this user's. */
  async find(userId: string, id: string) {
    const { rows } = this.database;
    const [[walk], photos] = await Promise.all([
      rows
        .select({
          ...card,
          breaks: walks.breaks,
          route: sql<string | null>`st_asgeojson(${walks.route})`,
        })
        .from(walks)
        .where(and(ownedBy(walks, userId), eq(walks.id, id))),
      rows
        .select(photoCard)
        .from(finds)
        .where(and(ownedBy(finds, userId), eq(finds.walk_id, id)))
        .orderBy(asc(finds.taken_at)),
    ]);
    return walk ? { ...walk, route: geoJson(walk.route), photos } : null;
  }
}

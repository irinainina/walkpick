import { Injectable } from '@nestjs/common';
import {
  and,
  desc,
  eq,
  getTableColumns,
  gte,
  ilike,
  lt,
  sql,
} from 'drizzle-orm';
import { Database } from '../db/database';
import { ownedBy } from '../db/owned';
import { finds, walks } from '../db/schema';
import { localKey } from '../period';
import { page } from '../query';
import { photoCard } from '../walks/walks';

export const MODES = ['all', 'best'] as const;
export const KINDS = finds.kind.enumValues;

export interface PhotoFilter {
  mode?: (typeof MODES)[number];
  kind?: (typeof KINDS)[number];
  /** A part of the name. */
  q?: string;
  from?: Date;
  to?: Date;
}

// The path of the file stays inside: a client gets a one-time link instead.
// eslint-disable-next-line @typescript-eslint/no-unused-vars
const { user_id, photo_path, ...shown } = getTableColumns(finds);

@Injectable()
export class Photos {
  constructor(private readonly database: Database) {}

  private matching(userId: string, filter: PhotoFilter) {
    return and(
      ownedBy(finds, userId),
      filter.mode === 'best' ? eq(finds.is_best, true) : undefined,
      filter.kind && eq(finds.kind, filter.kind),
      // The signs of a pattern are searched as themselves.
      filter.q
        ? ilike(finds.name, `%${filter.q.replace(/[\\%_]/g, '\\$&')}%`)
        : undefined,
      filter.from && gte(finds.taken_at, filter.from),
      filter.to && lt(finds.taken_at, filter.to),
    );
  }

  async list(userId: string, filter: PhotoFilter, size: number, before?: Date) {
    const rows = await this.database.rows
      .select(photoCard)
      .from(finds)
      .where(
        and(
          this.matching(userId, filter),
          before && lt(finds.taken_at, before),
        ),
      )
      .orderBy(desc(finds.taken_at))
      .limit(size + 1);
    return page(rows, size, (last) => last.taken_at);
  }

  /** Only what a mark of the map needs: the grid may hold hundreds of photos. */
  async map(userId: string, filter: PhotoFilter) {
    const items = await this.database.rows
      .select({
        id: finds.id,
        latitude: finds.latitude,
        longitude: finds.longitude,
        kind: finds.kind,
      })
      .from(finds)
      .where(this.matching(userId, filter));
    return { items };
  }

  /** One photo, its walk and the months this name was shot in; null when it is not this user's. */
  async find(userId: string, id: string, zone: string) {
    const { rows } = this.database;
    const [photo] = await rows
      .select({
        ...shown,
        walk: {
          id: walks.id,
          number: walks.number,
          name: walks.name,
          started_at: walks.started_at,
        },
      })
      .from(finds)
      .innerJoin(walks, eq(walks.id, finds.walk_id))
      .where(and(ownedBy(finds, userId), eq(finds.id, id)));
    if (!photo) return null;
    const months = photo.name
      ? await rows
          .selectDistinct({ month: localKey(finds.taken_at, zone, 'month') })
          .from(finds)
          .where(and(ownedBy(finds, userId), eq(finds.name, photo.name)))
          .orderBy(sql`1`)
      : [];
    return { ...photo, months: months.map((row) => row.month) };
  }
}

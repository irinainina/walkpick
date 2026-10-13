import { and, eq, isNull, SQL } from 'drizzle-orm';
import { PgColumn } from 'drizzle-orm/pg-core';

/**
 * The condition every query of a user's rows carries. The service's role sees all rows, so this
 * is the only thing between one user and the data of another. A row marked as deleted is not
 * anybody's any more.
 */
export const ownedBy = (
  table: { user_id: PgColumn; deleted_at?: PgColumn },
  userId: string,
): SQL | undefined =>
  and(eq(table.user_id, userId), table.deleted_at && isNull(table.deleted_at));

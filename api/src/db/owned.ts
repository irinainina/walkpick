import { eq, SQL } from 'drizzle-orm';
import { PgColumn } from 'drizzle-orm/pg-core';

/**
 * The condition every query of a user's rows carries. The service's role sees all rows, so this
 * is the only thing between one user and the data of another.
 */
export const ownedBy = (table: { user_id: PgColumn }, userId: string): SQL =>
  eq(table.user_id, userId);

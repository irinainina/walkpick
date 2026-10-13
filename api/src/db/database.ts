import { Injectable, OnModuleDestroy } from '@nestjs/common';
import { drizzle, NodePgDatabase } from 'drizzle-orm/node-postgres';
import { Pool } from 'pg';
import { Settings } from '../config';
import * as schema from './schema';

// One instance of the service serves one person at a time: a few connections are plenty, and the
// free Supabase pooler has few to give.
const MAX_CONNECTIONS = 5;

/**
 * The database. The service comes in with a role the row-level rules do not hold, so every query
 * of a user's rows must carry "user_id = the user of the token" itself.
 */
@Injectable()
export class Database implements OnModuleDestroy {
  private readonly pool: Pool;
  readonly rows: NodePgDatabase<typeof schema>;

  constructor(settings: Settings) {
    this.pool = new Pool({
      connectionString: settings.databaseUrl,
      max: MAX_CONNECTIONS,
      // The pooler presents the certificate of Supabase's own authority, unknown to Node.
      ssl: { rejectUnauthorized: false },
    });
    this.rows = drizzle(this.pool, { schema });
  }

  onModuleDestroy(): Promise<void> {
    return this.pool.end();
  }
}

/** What the service is told from outside; injected wherever a setting is needed. */
export class Settings {
  constructor(
    readonly port: number,
    /** The Supabase project that signs the users in, without a slash at the end. */
    readonly supabaseUrl: string,
    /** The Postgres of the same project, through its connection pooler. */
    readonly databaseUrl: string,
    /** The secret key of the project: the service signs the links to photo files with it. */
    readonly supabaseKey: string,
  ) {}
}

// Cloud Run tells the port through PORT; 8080 is what it expects when nothing is said.
const DEFAULT_PORT = 8080;

/** Reads the settings from the environment and refuses to start on a wrong one. */
export function readSettings(
  env: Record<string, string | undefined>,
): Settings {
  const port = env.PORT === undefined ? DEFAULT_PORT : Number(env.PORT);
  if (!Number.isInteger(port) || port <= 0) {
    throw new Error(`PORT must be a port number, got "${env.PORT}"`);
  }
  const supabaseUrl = env.SUPABASE_URL?.replace(/\/+$/, '');
  if (!supabaseUrl?.startsWith('https://')) {
    throw new Error(
      'SUPABASE_URL must be the https address of the Supabase project',
    );
  }
  const databaseUrl = env.DATABASE_URL;
  if (!databaseUrl?.startsWith('postgres')) {
    throw new Error(
      'DATABASE_URL must be the postgres:// address of the database',
    );
  }
  const supabaseKey = env.SUPABASE_SECRET_KEY;
  if (!supabaseKey)
    throw new Error(
      'SUPABASE_SECRET_KEY must be the secret key of the Supabase project',
    );
  return new Settings(port, supabaseUrl, databaseUrl, supabaseKey);
}

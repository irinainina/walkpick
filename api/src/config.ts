/** What the service is told from outside; injected wherever a setting is needed. */
export class Settings {
  constructor(readonly port: number) {}
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
  return new Settings(port);
}

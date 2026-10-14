import { readFileSync } from 'node:fs';
import { join } from 'node:path';

/** The pooler's certificate is checked against Supabase's own root, which Node does not know. */
export const SSL = {
  ca: readFileSync(join(__dirname, '..', '..', 'supabase-ca.crt'), 'utf8'),
};

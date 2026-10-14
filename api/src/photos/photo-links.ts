import { Injectable } from '@nestjs/common';
import { Settings } from '../config';

const BUCKET = 'photos';
// Long enough to look through a page of photos, short enough for a link that got away to die.
const LINK_SECONDS = 3600;

interface Signed {
  path: string | null;
  signedURL: string | null;
}

/** One-time addresses of photo files: the bucket is closed, and a client never sees a path. */
@Injectable()
export class PhotoLinks {
  constructor(private readonly settings: Settings) {}

  /** Gives each row the address of its file in place of the path; null when the file is not there. */
  async attach<T extends { photo_path: string }>(
    rows: T[],
    seconds = LINK_SECONDS,
  ): Promise<(Omit<T, 'photo_path'> & { url: string | null })[]> {
    const links = await this.sign(
      rows.map((row) => row.photo_path),
      seconds,
    );
    return rows.map(({ photo_path, ...row }) => ({
      ...row,
      url: links.get(photo_path) ?? null,
    }));
  }

  private async sign(
    paths: string[],
    seconds: number,
  ): Promise<Map<string, string>> {
    if (paths.length === 0) return new Map();
    const storage = `${this.settings.supabaseUrl}/storage/v1`;
    const key = this.settings.supabaseKey;
    const answer = await fetch(`${storage}/object/sign/${BUCKET}`, {
      method: 'POST',
      headers: {
        apikey: key,
        Authorization: `Bearer ${key}`,
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({ expiresIn: seconds, paths }),
    });
    if (!answer.ok)
      throw new Error(
        `Supabase did not sign the photo links: ${answer.status}`,
      );
    const signed = (await answer.json()) as Signed[];
    return new Map(
      signed.flatMap((link) =>
        link.path && link.signedURL
          ? [[link.path, storage + link.signedURL]]
          : [],
      ),
    );
  }
}

import { Controller, HttpCode, Post } from '@nestjs/common';
import { Public } from './auth/auth.guard';
import { Settings } from './config';
import { notFound } from './query';

interface Session {
  access_token: string;
  expires_at: number;
}

// A token is given out while it has this long to live: a visitor gets at least five minutes.
const MARGIN_S = 5 * 60;

/** "View demo": the service signs in as the demo user itself, a visitor never sees the password. */
@Public()
@Controller('v1/demo')
export class DemoController {
  private session: Session | null = null;

  constructor(private readonly settings: Settings) {}

  /** One session is shared by all visitors, so pressing the button costs Supabase nothing. */
  @Post('session')
  @HttpCode(200)
  async start(): Promise<Session> {
    const now = Date.now() / 1000;
    if (!this.session || this.session.expires_at - MARGIN_S < now)
      this.session = await this.signIn();
    return this.session;
  }

  private async signIn(): Promise<Session> {
    const demo = this.settings.demo ?? notFound();
    const key = this.settings.supabaseKey;
    const answer = await fetch(
      `${this.settings.supabaseUrl}/auth/v1/token?grant_type=password`,
      {
        method: 'POST',
        headers: { apikey: key, 'Content-Type': 'application/json' },
        body: JSON.stringify({ email: demo.email, password: demo.password }),
      },
    );
    if (!answer.ok)
      throw new Error(`Supabase did not sign the demo in: ${answer.status}`);
    const { access_token, expires_at } = (await answer.json()) as Session;
    return { access_token, expires_at };
  }
}

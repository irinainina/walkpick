import { Controller, Get } from '@nestjs/common';
import { Public } from './auth/auth.guard';

/** What the hosting asks to learn that the service is alive; outside /v1 and without a token. */
@Public()
@Controller('health')
export class AliveController {
  @Get()
  check(): { status: 'ok' } {
    return { status: 'ok' };
  }
}

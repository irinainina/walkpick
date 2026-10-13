import { Controller, Get } from '@nestjs/common';
import { Public } from './auth/auth.guard';

/** What the hosting asks to learn that the service is alive. */
@Public()
@Controller('health')
export class HealthController {
  @Get()
  check(): { status: 'ok' } {
    return { status: 'ok' };
  }
}

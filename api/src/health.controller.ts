import { Controller, Get } from '@nestjs/common';

/** What the hosting asks to learn that the service is alive. */
@Controller('health')
export class HealthController {
  @Get()
  check(): { status: 'ok' } {
    return { status: 'ok' };
  }
}

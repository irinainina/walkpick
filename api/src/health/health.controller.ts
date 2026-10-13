import { Controller, Get, Query } from '@nestjs/common';
import { UserId } from '../auth/auth.guard';
import { PERIODS } from '../period';
import { day, instant, limit, oneOf, required, zone } from '../query';
import { HEALTH_KINDS, HealthEntries } from './health-entries';

type Asked = Record<string, string | undefined>;

const kindOf = (asked: Asked) =>
  required('kind', oneOf('kind', asked.kind, HEALTH_KINDS));

@Controller('v1/health')
export class HealthController {
  constructor(private readonly entries: HealthEntries) {}

  @Get()
  list(@UserId() userId: string, @Query() asked: Asked) {
    return this.entries.list(
      userId,
      kindOf(asked),
      instant('from', asked.from),
      instant('to', asked.to),
      limit(asked.limit),
      instant('cursor', asked.cursor),
    );
  }

  @Get('summary')
  summary(@UserId() userId: string, @Query() asked: Asked) {
    const period = required('period', oneOf('period', asked.period, PERIODS));
    return this.entries.summary(
      userId,
      kindOf(asked),
      period,
      required('at', day('at', asked.at)),
      zone(asked.tz),
    );
  }
}

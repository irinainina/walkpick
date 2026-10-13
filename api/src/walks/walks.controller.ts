import { Controller, Get, Param, Query } from '@nestjs/common';
import { UserId } from '../auth/auth.guard';
import { PERIODS } from '../period';
import {
  day,
  id,
  instant,
  limit,
  notFound,
  oneOf,
  required,
  zone,
} from '../query';
import { Walks } from './walks';

type Asked = Record<string, string | undefined>;

@Controller('v1/walks')
export class WalksController {
  constructor(private readonly walks: Walks) {}

  @Get()
  list(@UserId() userId: string, @Query() asked: Asked) {
    return this.walks.list(
      userId,
      instant('from', asked.from),
      instant('to', asked.to),
      limit(asked.limit),
      instant('cursor', asked.cursor),
    );
  }

  @Get('summary')
  summary(@UserId() userId: string, @Query() asked: Asked) {
    const period = required('period', oneOf('period', asked.period, PERIODS));
    return this.walks.summary(
      userId,
      period,
      required('at', day('at', asked.at)),
      zone(asked.tz),
    );
  }

  @Get('calendar')
  calendar(@UserId() userId: string, @Query() asked: Asked) {
    return this.walks.calendar(
      userId,
      required('to', day('to', asked.to)),
      zone(asked.tz),
    );
  }

  @Get(':id')
  async one(@UserId() userId: string, @Param('id') walkId: string) {
    return (await this.walks.find(userId, id('id', walkId))) ?? notFound();
  }
}

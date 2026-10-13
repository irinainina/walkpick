import {
  Body,
  Controller,
  Delete,
  Get,
  HttpCode,
  Param,
  Patch,
  Post,
  Query,
} from '@nestjs/common';
import { UserId } from '../auth/auth.guard';
import { amount, change, choice, fields, moment, orNull, uuid } from '../body';
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
import { HEALTH_KINDS, HealthEntries } from './health-entries';

type Asked = Record<string, string | undefined>;

// What numeric(5, 1) and a smallint can hold; whether a number makes sense is the client's care.
const VALUE = amount(9999.9);
const DIASTOLIC = orNull(amount(999));

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

  @Post()
  add(@UserId() userId: string, @Body() body: unknown) {
    const added = fields(body, {
      id: uuid,
      kind: choice(HEALTH_KINDS),
      measured_at: moment,
      value: VALUE,
      diastolic: DIASTOLIC,
    });
    return this.entries.add(userId, {
      id: required('id', added.id),
      kind: required('kind', added.kind),
      measured_at: required('measured_at', added.measured_at),
      value: required('value', added.value),
      diastolic: added.diastolic ?? null,
    });
  }

  @Patch(':id')
  async change(
    @UserId() userId: string,
    @Param('id') entryId: string,
    @Body() body: unknown,
  ) {
    const patch = change(
      fields(body, { measured_at: moment, value: VALUE, diastolic: DIASTOLIC }),
    );
    return (
      (await this.entries.change(userId, id('id', entryId), patch)) ??
      notFound()
    );
  }

  @Delete(':id')
  @HttpCode(204)
  async remove(
    @UserId() userId: string,
    @Param('id') entryId: string,
  ): Promise<void> {
    if (!(await this.entries.remove(userId, id('id', entryId)))) notFound();
  }
}

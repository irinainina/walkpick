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
import { change, choice, fields, flag, listOf, text, uuid } from '../body';
import { Asks, Guarded, Takes } from '../docs';
import { id, instant, limit, notFound, oneOf, required, zone } from '../query';
import { KINDS, MODES, PhotoChange, PhotoFilter, Photos } from './photos';

type Asked = Record<string, string | undefined>;

const NAME_LENGTH = 80;
const BATCH = 100;
const ACTIONS = ['best', 'unbest', 'delete'] as const;
const BATCH_BODY = { ids: listOf(uuid, BATCH), action: choice(ACTIONS) };
const CHANGE = {
  name: text(NAME_LENGTH),
  latin_name: text(NAME_LENGTH),
  kind: choice(KINDS),
  is_best: flag,
};
const FILTER = Asks([
  ['mode', { type: 'string', enum: [...MODES] }],
  ['kind', { type: 'string', enum: [...KINDS] }],
  'q',
  'from',
  'to',
]);

const filterOf = (asked: Asked): PhotoFilter => ({
  mode: oneOf('mode', asked.mode, MODES),
  kind: oneOf('kind', asked.kind, KINDS),
  q: asked.q?.trim() || undefined,
  from: instant('from', asked.from),
  to: instant('to', asked.to),
});

const DONE: Record<
  (typeof ACTIONS)[number],
  () => PhotoChange & { deleted_at?: Date }
> = {
  best: () => ({ is_best: true }),
  unbest: () => ({ is_best: false }),
  delete: () => ({ deleted_at: new Date() }),
};

@Guarded('photos')
@Controller('v1/photos')
export class PhotosController {
  constructor(private readonly photos: Photos) {}

  @Get()
  @FILTER
  @Asks(['limit', 'cursor'])
  list(@UserId() userId: string, @Query() asked: Asked) {
    return this.photos.list(
      userId,
      filterOf(asked),
      limit(asked.limit),
      instant('cursor', asked.cursor),
    );
  }

  @Get('map')
  @FILTER
  map(@UserId() userId: string, @Query() asked: Asked) {
    return this.photos.map(userId, filterOf(asked));
  }

  /** The selection mode of the grid: one action for many photos. */
  @Post('batch')
  @HttpCode(200)
  @Takes(BATCH_BODY, ['ids', 'action'])
  async batch(@UserId() userId: string, @Body() body: unknown) {
    const asked = fields(body, BATCH_BODY);
    const patch = DONE[required('action', asked.action)]();
    return {
      changed: await this.photos.change(
        userId,
        required('ids', asked.ids),
        patch,
      ),
    };
  }

  @Get(':id')
  @Asks(['tz'])
  async one(
    @UserId() userId: string,
    @Param('id') photoId: string,
    @Query() asked: Asked,
  ) {
    return (
      (await this.photos.find(userId, id('id', photoId), zone(asked.tz))) ??
      notFound()
    );
  }

  @Patch(':id')
  @Asks(['tz'])
  @Takes(CHANGE)
  async change(
    @UserId() userId: string,
    @Param('id') photoId: string,
    @Query() asked: Asked,
    @Body() body: unknown,
  ) {
    const patch = change(fields(body, CHANGE));
    if ((await this.photos.change(userId, [id('id', photoId)], patch)) === 0)
      notFound();
    return this.one(userId, photoId, asked);
  }

  @Delete(':id')
  @HttpCode(204)
  async remove(
    @UserId() userId: string,
    @Param('id') photoId: string,
  ): Promise<void> {
    if (
      (await this.photos.change(userId, [id('id', photoId)], DONE.delete())) ===
      0
    )
      notFound();
  }
}

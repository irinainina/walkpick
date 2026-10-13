import { Controller, Get, Param, Query } from '@nestjs/common';
import { UserId } from '../auth/auth.guard';
import { id, instant, limit, notFound, oneOf, zone } from '../query';
import { KINDS, MODES, PhotoFilter, Photos } from './photos';

type Asked = Record<string, string | undefined>;

const filterOf = (asked: Asked): PhotoFilter => ({
  mode: oneOf('mode', asked.mode, MODES),
  kind: oneOf('kind', asked.kind, KINDS),
  q: asked.q?.trim() || undefined,
  from: instant('from', asked.from),
  to: instant('to', asked.to),
});

@Controller('v1/photos')
export class PhotosController {
  constructor(private readonly photos: Photos) {}

  @Get()
  list(@UserId() userId: string, @Query() asked: Asked) {
    return this.photos.list(
      userId,
      filterOf(asked),
      limit(asked.limit),
      instant('cursor', asked.cursor),
    );
  }

  @Get('map')
  map(@UserId() userId: string, @Query() asked: Asked) {
    return this.photos.map(userId, filterOf(asked));
  }

  @Get(':id')
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
}

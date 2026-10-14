import { Module } from '@nestjs/common';
import { APP_FILTER, APP_GUARD } from '@nestjs/core';
import { Accounts } from './accounts';
import { AliveController } from './alive.controller';
import { AuthGuard } from './auth/auth.guard';
import { SigningKeys } from './auth/signing-keys';
import { readSettings, Settings } from './config';
import { Database } from './db/database';
import { ErrorShape } from './errors';
import { HealthEntries } from './health/health-entries';
import { HealthController } from './health/health.controller';
import { MeController } from './me.controller';
import { PhotoLinks } from './photos/photo-links';
import { Photos } from './photos/photos';
import { PhotosController } from './photos/photos.controller';
import { Walks } from './walks/walks';
import { WalksController } from './walks/walks.controller';

@Module({
  controllers: [
    AliveController,
    MeController,
    WalksController,
    PhotosController,
    HealthController,
  ],
  providers: [
    { provide: Settings, useFactory: () => readSettings(process.env) },
    SigningKeys,
    Database,
    Accounts,
    PhotoLinks,
    Walks,
    Photos,
    HealthEntries,
    { provide: APP_GUARD, useClass: AuthGuard },
    { provide: APP_FILTER, useClass: ErrorShape },
  ],
})
export class AppModule {}

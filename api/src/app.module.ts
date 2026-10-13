import { Module } from '@nestjs/common';
import { APP_GUARD } from '@nestjs/core';
import { Accounts } from './accounts';
import { AuthGuard } from './auth/auth.guard';
import { SigningKeys } from './auth/signing-keys';
import { readSettings, Settings } from './config';
import { Database } from './db/database';
import { HealthController } from './health.controller';
import { MeController } from './me.controller';

@Module({
  controllers: [HealthController, MeController],
  providers: [
    { provide: Settings, useFactory: () => readSettings(process.env) },
    SigningKeys,
    Database,
    Accounts,
    { provide: APP_GUARD, useClass: AuthGuard },
  ],
})
export class AppModule {}

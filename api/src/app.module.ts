import { Module } from '@nestjs/common';
import { APP_GUARD } from '@nestjs/core';
import { AuthGuard } from './auth/auth.guard';
import { SigningKeys } from './auth/signing-keys';
import { readSettings, Settings } from './config';
import { HealthController } from './health.controller';
import { MeController } from './me.controller';

@Module({
  controllers: [HealthController, MeController],
  providers: [
    { provide: Settings, useFactory: () => readSettings(process.env) },
    SigningKeys,
    { provide: APP_GUARD, useClass: AuthGuard },
  ],
})
export class AppModule {}

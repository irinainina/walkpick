import { Module } from '@nestjs/common';
import { readSettings, Settings } from './config';
import { HealthController } from './health.controller';

@Module({
  controllers: [HealthController],
  providers: [
    { provide: Settings, useFactory: () => readSettings(process.env) },
  ],
})
export class AppModule {}

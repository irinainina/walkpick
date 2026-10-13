import { NestFactory } from '@nestjs/core';
import { AppModule } from './app.module';
import { Settings } from './config';

async function bootstrap() {
  const app = await NestFactory.create(AppModule);
  // The site is served from another address; every request is guarded by its token, not by a cookie.
  app.enableCors();
  app.enableShutdownHooks();
  await app.listen(app.get(Settings).port);
}
void bootstrap();

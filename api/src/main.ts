import { NestFactory } from '@nestjs/core';
import { AppModule } from './app.module';
import { Settings } from './config';
import { describeApi } from './docs';

async function bootstrap() {
  const app = await NestFactory.create(AppModule);
  // The site is served from another address; every request is guarded by its token, not by a cookie.
  app.enableCors();
  app.enableShutdownHooks();
  describeApi(app);
  await app.listen(app.get(Settings).port);
}
void bootstrap();

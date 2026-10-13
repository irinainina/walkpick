import { NestFactory } from '@nestjs/core';
import { AppModule } from './app.module';
import { Settings } from './config';

async function bootstrap() {
  const app = await NestFactory.create(AppModule);
  app.enableShutdownHooks();
  await app.listen(app.get(Settings).port);
}
void bootstrap();

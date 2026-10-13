import { NestFactory } from '@nestjs/core';
import { configure } from './app';
import { AppModule } from './app.module';
import { Settings } from './config';

async function bootstrap() {
  const app = await NestFactory.create(AppModule);
  configure(app);
  app.enableShutdownHooks();
  await app.listen(app.get(Settings).port);
}
void bootstrap();

import { INestApplication, RequestMethod } from '@nestjs/common';

/** What the tests and the real start set up alike. */
export function configure(app: INestApplication): void {
  app.setGlobalPrefix('v1', {
    exclude: [{ path: 'health', method: RequestMethod.GET }],
  });
}

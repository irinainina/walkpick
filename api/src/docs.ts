import { applyDecorators, INestApplication } from '@nestjs/common';
import {
  ApiBearerAuth,
  ApiBody,
  ApiDefaultResponse,
  ApiQuery,
  ApiTags,
  DocumentBuilder,
  SwaggerModule,
} from '@nestjs/swagger';
import { Check, SchemaObject, schemaOf } from './body';
import { PERIODS } from './period';

const ERROR: SchemaObject = {
  type: 'object',
  required: ['error'],
  properties: {
    error: {
      type: 'object',
      required: ['code', 'message'],
      properties: {
        code: {
          type: 'string',
          enum: [
            'validation_failed',
            'unauthorized',
            'not_found',
            'too_large',
            'internal',
          ],
        },
        message: { type: 'string' },
        fields: {
          type: 'array',
          items: {
            type: 'object',
            properties: {
              field: { type: 'string' },
              problem: { type: 'string' },
            },
          },
        },
      },
    },
  },
};

const QUERY: Record<string, SchemaObject> = {
  from: { type: 'string', format: 'date-time' },
  to: { type: 'string', format: 'date-time' },
  cursor: { type: 'string', format: 'date-time' },
  limit: { type: 'integer', minimum: 1, maximum: 100, default: 50 },
  period: { type: 'string', enum: [...PERIODS] },
  at: { type: 'string', format: 'date' },
  tz: { type: 'string', default: 'UTC', example: 'Europe/Kyiv' },
  q: { type: 'string' },
};

/** A group of requests that need the user's token and answer a refusal with the one error body. */
export const Guarded = (tag: string) =>
  applyDecorators(
    ApiTags(tag),
    ApiBearerAuth(),
    ApiDefaultResponse({ description: 'A refusal', schema: ERROR }),
  );

/** The query parameters a request reads; a name not in QUERY comes with its own schema. */
export const Asks = (
  names: (string | [string, SchemaObject])[],
  required: string[] = [],
) =>
  applyDecorators(
    ...names.map((asked) => {
      const [name, schema] =
        typeof asked === 'string' ? [asked, QUERY[asked]] : asked;
      return ApiQuery({ name, schema, required: required.includes(name) });
    }),
  );

export const Takes = (
  spec: Record<string, Check<unknown>>,
  required?: string[],
) => ApiBody({ schema: schemaOf(spec, required) });

/** The description is built from the controllers and served at /docs, its JSON at /docs-json. */
export function describeApi(app: INestApplication): void {
  const config = new DocumentBuilder()
    .setTitle('WalkPick API')
    .setVersion('0.2')
    .addBearerAuth()
    .build();
  SwaggerModule.setup('docs', app, SwaggerModule.createDocument(app, config));
}

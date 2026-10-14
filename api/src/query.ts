import { BadRequestException, NotFoundException } from '@nestjs/common';

const DEFAULT_LIMIT = 50;
const MAX_LIMIT = 100;
const DAY = /^\d{4}-\d{2}-\d{2}$/;
const UUID = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

/** Refuses a request for one wrong parameter, in the one shape all errors have. */
export function invalid(field: string, problem: string): never {
  throw new BadRequestException({
    error: {
      code: 'validation_failed',
      message: `${field} is ${problem}`,
      fields: [{ field, problem }],
    },
  });
}

/** A row that is not there and a row of another user are answered alike. */
export function notFound(): never {
  throw new NotFoundException({
    error: { code: 'not_found', message: 'there is no such row' },
  });
}

export function required<T>(field: string, value: T | undefined): T {
  return value === undefined ? invalid(field, 'required') : value;
}

/** A latitude or a longitude: a number no further from zero than [max] degrees. */
export function coordinate(
  field: string,
  value: string | undefined,
  max: number,
): number {
  const number = Number(required(field, value));
  return Number.isFinite(number) && Math.abs(number) <= max
    ? number
    : invalid(field, 'out_of_range');
}

/** A moment in ISO 8601. */
export function instant(field: string, value?: string): Date | undefined {
  if (value === undefined) return undefined;
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? invalid(field, 'not_a_time') : date;
}

/** A calendar day, 2026-10-13. */
export function day(field: string, value?: string): string | undefined {
  if (value === undefined) return undefined;
  return DAY.test(value) && !Number.isNaN(Date.parse(value))
    ? value
    : invalid(field, 'not_a_day');
}

export function oneOf<T extends string>(
  field: string,
  value: string | undefined,
  allowed: readonly T[],
): T | undefined {
  if (value === undefined) return undefined;
  return allowed.includes(value as T)
    ? (value as T)
    : invalid(field, 'not_allowed');
}

export function id(field: string, value: string): string {
  return UUID.test(value) ? value : notFound();
}

/** The time zone the user's days are counted in; UTC when the client does not say. */
export function zone(value?: string): string {
  if (value === undefined) return 'UTC';
  try {
    new Intl.DateTimeFormat('en', { timeZone: value });
    return value;
  } catch {
    return invalid('tz', 'not_a_time_zone');
  }
}

export function limit(value?: string): number {
  if (value === undefined) return DEFAULT_LIMIT;
  const count = Number(value);
  return Number.isInteger(count) && count > 0 && count <= MAX_LIMIT
    ? count
    : invalid('limit', 'out_of_range');
}

/** A page of a list: one row more than asked was read to learn whether there is a next page. */
export function page<T>(
  rows: T[],
  size: number,
  cursorOf: (last: T) => Date,
): { items: T[]; next: string | null } {
  const items = rows.slice(0, size);
  return {
    items,
    next: rows.length > size ? cursorOf(items[size - 1]).toISOString() : null,
  };
}

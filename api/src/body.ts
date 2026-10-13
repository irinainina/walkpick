import { invalid } from './query';

const UUID = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

/** Reads one field of a body or refuses the request. */
export type Check<T> = (field: string, value: unknown) => T;

/** Text up to a length; spaces around are cut and nothing left is null. */
export const text =
  (max: number): Check<string | null> =>
  (field, value) => {
    if (value === null) return null;
    if (typeof value !== 'string') return invalid(field, 'not_text');
    const cut = value.trim();
    return cut.length > max ? invalid(field, 'too_long') : cut || null;
  };

export const flag: Check<boolean> = (field, value) =>
  typeof value === 'boolean' ? value : invalid(field, 'not_a_flag');

export const choice =
  <T extends string>(allowed: readonly T[]): Check<T> =>
  (field, value) =>
    allowed.includes(value as T) ? (value as T) : invalid(field, 'not_allowed');

export const moment: Check<Date> = (field, value) => {
  const date = typeof value === 'string' ? new Date(value) : new Date(NaN);
  return Number.isNaN(date.getTime()) ? invalid(field, 'not_a_time') : date;
};

export const amount =
  (max: number): Check<number> =>
  (field, value) =>
    typeof value === 'number' && value > 0 && value <= max
      ? value
      : invalid(field, 'out_of_range');

export const orNull =
  <T>(check: Check<T>): Check<T | null> =>
  (field, value) =>
    value === null ? null : check(field, value);

export const uuid: Check<string> = (field, value) =>
  typeof value === 'string' && UUID.test(value)
    ? value
    : invalid(field, 'not_an_id');

export const listOf =
  <T>(check: Check<T>, max: number): Check<T[]> =>
  (field, value) =>
    Array.isArray(value) && value.length > 0 && value.length <= max
      ? value.map((item) => check(field, item))
      : invalid(field, 'out_of_range');

type Read<S> = { [K in keyof S]?: S[K] extends Check<infer T> ? T : never };

/** The fields a body may carry. A field nobody expects is a mistake of the client, not ignored. */
export function fields<S extends Record<string, Check<unknown>>>(
  body: unknown,
  spec: S,
): Read<S> {
  if (typeof body !== 'object' || body === null || Array.isArray(body))
    return invalid('body', 'not_an_object');
  const read: Record<string, unknown> = {};
  for (const [field, value] of Object.entries(body)) {
    if (!(field in spec)) invalid(field, 'unknown');
    read[field] = spec[field](field, value);
  }
  return read as Read<S>;
}

/** A change that changes nothing is refused. */
export function change<T extends object>(patch: T): T {
  return Object.keys(patch).length === 0 ? invalid('body', 'empty') : patch;
}

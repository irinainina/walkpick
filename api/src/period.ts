import { and, gte, lt, SQL, sql } from 'drizzle-orm';
import { PgColumn } from 'drizzle-orm/pg-core';

export const PERIODS = ['week', 'month', 'year', 'all'] as const;
export type Period = (typeof PERIODS)[number];
export type Step = 'day' | 'month' | 'year';

/** A period in the days of the user's calendar; [to] is the first day after it. */
export interface Span {
  from: string | null;
  to: string | null;
  step: Step;
}

const DAY_MS = 86_400_000;
const WEEK_DAYS = 7;
const iso = (date: Date) => date.toISOString().slice(0, 10);

/** The day [days] away from an ISO day. */
export function shiftDay(day: string, days: number): string {
  return iso(new Date(Date.parse(day) + days * DAY_MS));
}

/** The period around a day: the week starts on Monday, the bars of a year are its months. */
export function spanOf(period: Period, at: string): Span {
  const [year, month] = at.split('-').map(Number);
  switch (period) {
    case 'week': {
      // getUTCDay counts from Sunday.
      const monday = shiftDay(
        at,
        -((new Date(at).getUTCDay() + 6) % WEEK_DAYS),
      );
      return { from: monday, to: shiftDay(monday, WEEK_DAYS), step: 'day' };
    }
    case 'month':
      return {
        from: iso(new Date(Date.UTC(year, month - 1, 1))),
        to: iso(new Date(Date.UTC(year, month, 1))),
        step: 'day',
      };
    case 'year':
      return { from: `${year}-01-01`, to: `${year + 1}-01-01`, step: 'month' };
    case 'all':
      return { from: null, to: null, step: 'year' };
  }
}

const FORMAT: Record<Step, string> = {
  day: 'YYYY-MM-DD',
  month: 'YYYY-MM',
  year: 'YYYY',
};

/** The day, month or year of a moment on the user's calendar, as text that sorts by time. */
export const localKey = (column: PgColumn, zone: string, step: Step = 'day') =>
  sql<string>`to_char(${column} at time zone ${zone}, ${FORMAT[step]})`;

/** The rows whose moment falls into the span on the user's calendar. */
export function within(
  column: PgColumn,
  zone: string,
  span: Span,
): SQL | undefined {
  if (!span.from || !span.to) return undefined;
  return and(
    gte(localKey(column, zone), span.from),
    lt(localKey(column, zone), span.to),
  );
}

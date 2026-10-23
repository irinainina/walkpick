import { day } from "./format";

export const PERIODS = { week: "Week", month: "Month", year: "Year", all: "All" };
export type Period = keyof typeof PERIODS;

// Week, Month and Year are the current calendar ones, as in the app
export const periodStart = (period: Period) => {
  const start = new Date();
  start.setHours(0, 0, 0, 0);
  if (period === "week") start.setDate(start.getDate() - (start.getDay() + 6) % 7);
  if (period === "month") start.setDate(1);
  if (period === "year") start.setMonth(0, 1);
  return period === "all" ? new Date(0) : start;
};

export const inPeriod = <T>(items: T[], period: Period, at: (item: T) => string) =>
  items.filter(item => new Date(at(item)) >= periodStart(period));

export interface Bucket {
  from: Date;
  to: Date;
  label: string;
  title: string;
}

/** The bars of a period: days for a week and a month, months for a year and for everything since the first item. */
export const buckets = (period: Period, first: string) => {
  const byMonth = period === "year" || period === "all";
  const cursor = period === "all" ? new Date(first) : periodStart(period);
  cursor.setHours(0, 0, 0, 0);
  if (byMonth) cursor.setDate(1);
  const end = new Date(cursor);
  if (period === "week") end.setDate(end.getDate() + 6);
  if (period === "month") end.setMonth(end.getMonth() + 1, 0);
  if (period === "year") end.setMonth(11, 31);
  if (period === "all") end.setTime(Date.now());
  const list: Bucket[] = [];
  while (cursor <= end) {
    const from = new Date(cursor);
    if (byMonth) cursor.setMonth(cursor.getMonth() + 1);
    else cursor.setDate(cursor.getDate() + 1);
    const label = period === "week" ? day(from, { weekday: "short" }) : period === "month" ? String(from.getDate()) : day(from, { month: "narrow" });
    list.push({ from, to: new Date(cursor), label, title: byMonth ? day(from, { month: "long", year: "numeric" }) : day(from) });
  }
  return list;
};

export const within = <T>(items: T[], bucket: Bucket, at: (item: T) => string) =>
  items.filter(item => new Date(at(item)) >= bucket.from && new Date(at(item)) < bucket.to);

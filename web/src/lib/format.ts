// English only until the item "Два языка" brings the dictionaries
const LOCALE = "en-US";

export const number = (value: number, digits = 0) =>
  value.toLocaleString(LOCALE, { minimumFractionDigits: digits, maximumFractionDigits: digits });

export const km = (metres: number) => `${number(metres / 1000, 1)} km`;

export const duration = (seconds: number) => {
  const min = Math.round(seconds / 60);
  return min < 60 ? `${min} min` : `${Math.floor(min / 60)} h ${min % 60} min`;
};

export const day = (date: Date | string, options: Intl.DateTimeFormatOptions = { weekday: "long", month: "long", day: "numeric" }) =>
  new Date(date).toLocaleDateString(LOCALE, options);

export const sum = <T>(items: T[], value: (item: T) => number) => items.reduce((total, item) => total + value(item), 0);

export const clock = (date: Date | string) => new Date(date).toLocaleTimeString(LOCALE, { hour: "2-digit", minute: "2-digit" });

// A local calendar day as YYYY-MM-DD, to group by day
export const dayKey = (date: Date | string) => new Date(date).toLocaleDateString("sv-SE");

export const walkName = (walk: { name: string | null; number: number }) => walk.name || `Walk ${walk.number}`;

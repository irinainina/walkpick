import type { Walk } from "@/lib/api";
import { day, dayKey, km, sum } from "@/lib/format";
import { periodStart } from "@/lib/period";

/** The last 53 weeks, a column per week from Monday; the darker the square, the longer the walks of that day. */
export default function YearGrid({ walks }: { walks: Walk[] }) {
  const perDay: Record<string, number> = {};
  walks.forEach(walk => { perDay[dayKey(walk.start)] = (perDay[dayKey(walk.start)] ?? 0) + walk.m; });
  const first = periodStart("week");
  first.setDate(first.getDate() - 52 * 7);
  const cells = Array.from({ length: 53 * 7 }, (_, index) => {
    const date = new Date(first);
    date.setDate(first.getDate() + index);
    return { date, metres: perDay[dayKey(date)] ?? 0 };
  });
  const year = walks.filter(walk => new Date(walk.start) >= first);
  return (
    <>
      <div className="year-grid year-light" aria-hidden="true">
        {cells.map(({ date, metres }) => (
          // A level per 1.5 km, the fourth for 4.5 km and more
          <i key={date.getTime()} data-level={metres ? Math.min(4, Math.ceil(metres / 1500)) : undefined} title={`${day(date)}${metres ? `: ${km(metres)}` : ""}`} />
        ))}
      </div>
      <p>{year.length} walks · {km(sum(year, walk => walk.m))} in the last twelve months. The darker the square, the longer the walk.</p>
    </>
  );
}

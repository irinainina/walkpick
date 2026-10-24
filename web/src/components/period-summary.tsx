import type { Walk } from "@/lib/api";
import { day, duration, km, number, sum } from "@/lib/format";
import { buckets, inPeriod, PERIODS, within, type Period } from "@/lib/period";
import Bars from "./bars";
import PeriodSwitch from "./period-switch";
import Stats from "./stats";

export const started = (walk: Walk) => walk.start;

// Opens on the shortest period that has walks
export const firstPeriod = (walks: Walk[]) =>
  (Object.keys(PERIODS) as Period[]).find(id => inPeriod(walks, id, started).length) ?? "week";

const titles: Record<Period, () => string> = {
  week: () => "This week",
  month: () => day(new Date(), { month: "long", year: "numeric" }),
  year: () => String(new Date().getFullYear()),
  all: () => "All walks",
};

/** The totals of a period and its distance by day or month. */
export default function PeriodSummary({ walks, period, onChange }: { walks: Walk[]; period: Period; onChange: (period: Period) => void }) {
  const inside = inPeriod(walks, period, started);
  const bars = walks.length
    ? buckets(period, walks[walks.length - 1].start).map(bucket => ({ ...bucket, value: sum(within(inside, bucket, started), walk => walk.m) }))
    : [];
  return (
    <section className="period">
      <div className="page-head">
        <h2>{titles[period]()}</h2>
        <PeriodSwitch chosen={period} onChange={onChange} />
      </div>
      <div className="columns columns-even">
        <div className="card">
          <Stats items={[
            [inside.length, "walks"],
            [number(sum(inside, walk => walk.m) / 1000, 1), "km"],
            [duration(sum(inside, walk => walk.sec)), "on foot"],
            [number(sum(inside, walk => walk.steps)), "steps"],
          ]} />
        </div>
        <div className="card">
          <Bars bars={bars} unit={km} />
        </div>
      </div>
    </section>
  );
}

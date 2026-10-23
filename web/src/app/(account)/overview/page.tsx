"use client";

import { useState } from "react";
import Bars from "@/components/bars";
import PeriodSwitch from "@/components/period-switch";
import Stats from "@/components/stats";
import { useWalks, type Walk } from "@/lib/api";
import { day, duration, km, number, sum } from "@/lib/format";
import { buckets, inPeriod, PERIODS, within, type Period } from "@/lib/period";

const started = (walk: Walk) => walk.start;
const titles: Record<Period, () => string> = {
  week: () => "This week",
  month: () => day(new Date(), { month: "long", year: "numeric" }),
  year: () => String(new Date().getFullYear()),
  all: () => "All walks",
};

export default function Overview() {
  const { walks, problem } = useWalks();
  const [chosen, setChosen] = useState<Period | null>(null);
  if (!walks) return <p className="empty">{problem}</p>;

  // Opens on the shortest period that has walks
  const period = chosen ?? (Object.keys(PERIODS) as Period[]).find(id => inPeriod(walks, id, started).length) ?? "week";
  const inside = inPeriod(walks, period, started);
  const bars = walks.length
    ? buckets(period, walks[walks.length - 1].start).map(bucket => ({ ...bucket, value: sum(within(inside, bucket, started), walk => walk.m) }))
    : [];

  return (
    <>
      <div className="page-head">
        <div>
          <h1>Overview</h1>
          <p>{day(new Date(), { weekday: "long", month: "long", day: "numeric", year: "numeric" })}</p>
        </div>
      </div>
      <section className="period">
        <div className="page-head">
          <h2>{titles[period]()}</h2>
          <PeriodSwitch chosen={period} onChange={setChosen} />
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
    </>
  );
}

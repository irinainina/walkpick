"use client";

import { useState } from "react";
import Icon from "@/components/icon";
import MoodFace from "@/components/mood-face";
import MoodPair from "@/components/mood-pair";
import PeriodSummary, { firstPeriod } from "@/components/period-summary";
import RouteThumb from "@/components/route-thumb";
import Stats from "@/components/stats";
import YearGrid from "@/components/year-grid";
import { useFinds, useWalks, type Walk } from "@/lib/api";
import { clock, day, dayKey, duration, km, number, sum, walkName } from "@/lib/format";
import { KINDS, MOODS } from "@/lib/kinds";
import type { Period } from "@/lib/period";

const shortDay = (date: string) => day(date, { month: "short", day: "numeric", year: "numeric" });
const most = (walks: Walk[], value: (walk: Walk) => number) => walks.reduce((top, walk) => (value(walk) > value(top) ? walk : top));

// The most days in a row with at least one walk
const longestStreak = (walks: Walk[]) => {
  const days = [...new Set(walks.map(walk => dayKey(walk.start)))].sort();
  let longest = 0;
  let run = 0;
  days.forEach((key, index) => {
    const next = new Date(`${days[index - 1]}T12:00`);
    next.setDate(next.getDate() + 1);
    run = index && dayKey(next) === key ? run + 1 : 1;
    longest = Math.max(longest, run);
  });
  return longest;
};

function MoodCard({ walks }: { walks: Walk[] }) {
  const asked = walks.flatMap(walk => (walk.mood ? [walk.mood] : []));
  if (!asked.length) return <div className="card"><h3>Mood</h3><p>The app asks about your mood at the start and the end of every walk.</p></div>;
  const better = asked.filter(([before, after]) => after > before).length;
  return (
    <div className="card">
      <h3>Mood</h3>
      <div className="mood-scale">
        {["Before the walk", "After the walk"].map((label, index) => {
          const average = sum(asked, mood => mood[index]) / asked.length;
          return (
            <div key={label}>
              <MoodFace value={Math.round(average)} />
              <span>{label}<strong>{MOODS[Math.round(average) - 1].name} · {number(average, 1)}</strong></span>
            </div>
          );
        })}
      </div>
      <p>{better} of {asked.length} walks ended in a better mood.</p>
    </div>
  );
}

function WalkRow({ walk }: { walk: Walk }) {
  const pace = walk.sec / (walk.m / 1000);
  return (
    <a className="walk-row" href={`/walk-page.html?id=${walk.id}`}>
      <RouteThumb route={walk.route} />
      <span><strong>{walkName(walk)}</strong><small>{shortDay(walk.start)} · {clock(walk.start)}</small></span>
      <span>{km(walk.m)}</span>
      <span>{duration(walk.sec)}</span>
      <span>{number(walk.steps)} steps</span>
      {walk.mood ? <MoodPair mood={walk.mood} /> : (
        <span>{Math.floor(pace / 60)}:{String(Math.floor(pace % 60)).padStart(2, "0")} min/km</span>
      )}
    </a>
  );
}

export default function Overview() {
  const { walks, problem } = useWalks();
  const { finds } = useFinds();
  const [chosen, setChosen] = useState<Period | null>(null);
  if (!walks) return <main className="app container"><p className="empty">{problem}</p></main>;

  const longest = walks.length ? most(walks, walk => walk.m) : null;
  const steps = walks.length ? most(walks, walk => walk.steps) : null;
  const shots = finds?.filter(find => find.photo).slice(0, 12) ?? [];

  return (
    <main className="app container">
      <div className="page-head">
        <div>
          <h1>Overview</h1>
          <p>{day(new Date(), { weekday: "long", month: "long", day: "numeric", year: "numeric" })}</p>
        </div>
      </div>
      <PeriodSummary walks={walks} period={chosen ?? firstPeriod(walks)} onChange={setChosen} />
      {longest && steps && (
        <>
          <section className="card">
            <h3>A year of walks</h3>
            <YearGrid walks={walks} />
          </section>
          <div className="columns columns-even">
            <div className="card">
              <h3>Records</h3>
              <Stats items={[
                [km(longest.m), `longest walk · ${shortDay(longest.start)}`],
                [number(steps.steps), `most steps · ${shortDay(steps.start)}`],
                [`${longestStreak(walks)} days`, "longest streak"],
              ]} />
            </div>
            <MoodCard walks={walks} />
          </div>
          <section className="section">
            <div className="page-head">
              <h2>Last walks</h2>
              <a className="link" href="/walks">All walks <Icon name="arrow" /></a>
            </div>
            <div>{walks.slice(0, 5).map(walk => <WalkRow key={walk.id} walk={walk} />)}</div>
          </section>
        </>
      )}
      {shots.length > 0 && (
        <section className="section">
          <div className="page-head">
            <h2>Last finds</h2>
            <a className="link" href="/photo.html">All photos <Icon name="arrow" /></a>
          </div>
          <div className="tiles">
            {shots.map(find => (
              <a key={find.id} className="tile" href={`/photo-page.html?id=${find.id}`}>
                {/* eslint-disable-next-line @next/next/no-img-element -- signed storage links change every hour */}
                <img src={find.photo!} alt="" loading="lazy" />
                <span className="tile-text"><strong>{find.name || KINDS[find.kind].name}</strong>{shortDay(find.at)}</span>
              </a>
            ))}
          </div>
        </section>
      )}
    </main>
  );
}

"use client";

import { useState } from "react";
import Icon from "@/components/icon";
import MoodPair from "@/components/mood-pair";
import RouteThumb from "@/components/route-thumb";
import { useFinds, useWalks, type Find, type Walk } from "@/lib/api";
import { clock, day, duration, km, number, sum, walkName } from "@/lib/format";
import styles from "./walks.module.css";

const SORTS = {
  newest: { name: "Newest", value: (walk: Walk) => Date.parse(walk.start) },
  longest: { name: "Longest", value: (walk: Walk) => walk.m },
  steps: { name: "Most steps", value: (walk: Walk) => walk.steps },
};
type Sort = keyof typeof SORTS;

const PAGE = 12;
const month = (walk: Walk) => day(walk.start, { month: "long", year: "numeric" });
const totals = (walks: Walk[]) => `${walks.length} ${walks.length === 1 ? "walk" : "walks"} · ${km(sum(walks, walk => walk.m))} · ${duration(sum(walks, walk => walk.sec))}`;

// The latest photos as a collage, as on komoot: one large and four small
function Collage({ shots }: { shots: Find[] }) {
  // eslint-disable-next-line @next/next/no-img-element -- the same plain img as the photos beside it
  if (shots.length < 5) return <div className={styles.collage}><img src="/assets/images/hero.webp" alt="" /></div>;
  return (
    <div className={styles.collage}>
      {shots.slice(0, 5).map(find => (
        // eslint-disable-next-line @next/next/no-img-element -- signed storage links change every hour
        <img key={find.id} src={find.photo!} alt="" />
      ))}
      <a className={`button button-small ${styles.all}`} href="/photo.html">All photos</a>
    </div>
  );
}

function WalkCard({ walk, shots }: { walk: Walk; shots: Find[] }) {
  return (
    <a className={styles.card} href={`/walk-page.html?id=${walk.id}`}>
      <div className={styles.cover}>
        <RouteThumb route={walk.route} />
        {shots.slice(0, 2).map(find => (
          // eslint-disable-next-line @next/next/no-img-element -- signed storage links change every hour
          <img key={find.id} src={find.photo!} alt="" loading="lazy" />
        ))}
        {shots.length > 2 && <span className={styles.more}>+{shots.length - 2}</span>}
      </div>
      <div className={styles.text}>
        <strong>{walkName(walk)}</strong>
        <small>{day(walk.start, { weekday: "long", month: "long", day: "numeric" })} · {clock(walk.start)}</small>
      </div>
      <dl className={styles.numbers}>
        <div><dt>Distance</dt><dd>{km(walk.m)}</dd></div>
        <div><dt>Time</dt><dd>{duration(walk.sec)}</dd></div>
        <div><dt>Steps</dt><dd>{number(walk.steps)}</dd></div>
      </dl>
      {walk.mood && <MoodPair mood={walk.mood} />}
    </a>
  );
}

export default function Walks() {
  const { walks, problem } = useWalks();
  const { finds } = useFinds();
  const [search, setSearch] = useState("");
  const [sort, setSort] = useState<Sort>("newest");
  const [shown, setShown] = useState(PAGE);
  if (!walks) return <main className="app container"><p className="empty">{problem}</p></main>;

  const shots = finds?.filter(find => find.photo) ?? [];
  const query = search.trim().toLowerCase();
  const found = walks
    .filter(walk => walkName(walk).toLowerCase().includes(query))
    .sort((a, b) => SORTS[sort].value(b) - SORTS[sort].value(a));
  const page = found.slice(0, shown);
  // Months only make sense while the walks go by date
  const groups = sort === "newest" ? [...new Set(page.map(month))].map(name => ({ name, walks: page.filter(walk => month(walk) === name) })) : [{ name: "", walks: page }];
  const card = (walk: Walk) => <WalkCard key={walk.id} walk={walk} shots={shots.filter(find => find.walk === walk.id)} />;

  return (
    <main className={`container ${styles.page}`}>
      <Collage shots={shots} />
      <div className={styles.head}>
        <div>
          <h1>Walks</h1>
          {walks.length > 0 && <p>Since {day(walks[walks.length - 1].start, { month: "long", day: "numeric", year: "numeric" })}</p>}
        </div>
        <dl className={styles.numbers}>
          <div><dt>Walks</dt><dd>{walks.length}</dd></div>
          <div><dt>Distance</dt><dd>{km(sum(walks, walk => walk.m))}</dd></div>
          <div><dt>On foot</dt><dd>{duration(sum(walks, walk => walk.sec))}</dd></div>
          <div><dt>Steps</dt><dd>{number(sum(walks, walk => walk.steps))}</dd></div>
        </dl>
      </div>
      {walks.length === 0 ? <p className="empty">The walks recorded by the app will appear here.</p> : (
        <>
          <div className="toolbar">
            <label className="field search">
              <Icon name="search" />
              <input type="search" placeholder="Search by name" value={search} onChange={event => { setSearch(event.target.value); setShown(PAGE); }} />
            </label>
            <div className="segmented" role="group" aria-label="Sort">
              {(Object.keys(SORTS) as Sort[]).map(id => (
                <button key={id} type="button" aria-pressed={id === sort} onClick={() => { setSort(id); setShown(PAGE); }}>{SORTS[id].name}</button>
              ))}
            </div>
          </div>
          {found.length === 0 && <p className="empty">No walks with this name.</p>}
          {groups.map(group => (
            <section key={group.name} className={styles.month}>
              {group.name && (
                <h2>{group.name}<span>{totals(found.filter(walk => month(walk) === group.name))}</span></h2>
              )}
              <div className={styles.grid}>{group.walks.map(card)}</div>
            </section>
          ))}
          {shown < found.length && (
            <button className={`button button-ghost ${styles.next}`} type="button" onClick={() => setShown(shown + PAGE)}>
              Show more · {found.length - shown}
            </button>
          )}
        </>
      )}
    </main>
  );
}

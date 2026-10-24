"use client";

import { useEffect, useState } from "react";
import type { Kind } from "./kinds";
import { API, leave, token } from "./session";

export interface Walk {
  id: string;
  number: number;
  name: string | null;
  start: string;
  sec: number;
  steps: number;
  m: number;
  route: [number, number][];
  mood: [number, number] | null;
}

interface WalkRow {
  id: string;
  number: number;
  name: string | null;
  started_at: string;
  duration_s: number;
  steps: number;
  distance_m: number;
  route: { coordinates: [number, number][] } | null;
  mood_before: number | null;
  mood_after: number | null;
}

export interface Find {
  id: string;
  walk: string | null;
  at: string;
  lon: number;
  lat: number;
  kind: Kind;
  name: string | null;
  best: boolean;
  photo: string | null;
}

interface FindRow {
  id: string;
  walk_id: string | null;
  taken_at: string;
  longitude: number;
  latitude: number;
  kind: Kind;
  name: string | null;
  is_best: boolean;
  url: string | null;
}

export const api = async <T>(path: string, method = "GET", body?: object): Promise<T> => {
  const access = await token();
  if (!access) leave();
  const answer = await fetch(API + path, {
    method,
    headers: { Authorization: `Bearer ${access}`, ...(body && { "Content-Type": "application/json" }) },
    body: body && JSON.stringify(body),
  });
  if (answer.status === 401) leave();
  if (!answer.ok) throw new Error(`${method} ${path}: ${answer.status}`);
  return answer.status === 204 ? (null as T) : answer.json();
};

// A list is read page by page to its end
const all = async <T>(path: string) => {
  const items: T[] = [];
  let cursor = "";
  do {
    const page = await api<{ items: T[]; next: string | null }>(`${path}${path.includes("?") ? "&" : "?"}limit=100${cursor}`);
    items.push(...page.items);
    cursor = page.next ? `&cursor=${encodeURIComponent(page.next)}` : "";
  } while (cursor);
  return items;
};

// A whole list in the page's shape; null while it loads
const useAll = <Row, Item>(path: string, shape: (row: Row) => Item) => {
  const [items, setItems] = useState<Item[] | null>(null);
  const [problem, setProblem] = useState("");
  useEffect(() => {
    all<Row>(path).then(rows => setItems(rows.map(shape))).catch((error: Error) => setProblem(error.message));
  }, [path, shape]);
  return { items, problem };
};

const walkOf = (walk: WalkRow): Walk => ({
  id: walk.id, number: walk.number, name: walk.name, start: walk.started_at, sec: walk.duration_s, steps: walk.steps,
  m: walk.distance_m, route: walk.route ? walk.route.coordinates : [],
  mood: walk.mood_before && walk.mood_after ? [walk.mood_before, walk.mood_after] : null,
});

const findOf = (find: FindRow): Find => ({
  id: find.id, walk: find.walk_id, at: find.taken_at, lon: find.longitude, lat: find.latitude, kind: find.kind,
  name: find.name, best: find.is_best, photo: find.url,
});

/** The walks of the account, the latest first. */
export const useWalks = () => {
  const { items, problem } = useAll("/walks", walkOf);
  return { walks: items, problem };
};

/** The photos of the account, the latest first. */
export const useFinds = () => {
  const { items, problem } = useAll("/photos", findOf);
  return { finds: items, problem };
};

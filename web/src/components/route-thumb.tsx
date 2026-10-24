import type { Walk } from "@/lib/api";

/** A walk drawn as a small line; longitudes are squeezed by cos(latitude) to keep the shape true. */
export default function RouteThumb({ route }: { route: Walk["route"] }) {
  if (!route.length) return <svg className="route-thumb" viewBox="0 0 64 64" />;
  const squeeze = Math.cos(route[0][1] * Math.PI / 180);
  const xs = route.map(([lon]) => lon * squeeze);
  const ys = route.map(([, lat]) => -lat);
  const [left, top] = [Math.min(...xs), Math.min(...ys)];
  const size = Math.max(Math.max(...xs) - left, Math.max(...ys) - top) || 1;
  const points = xs.map((x, index) => `${((x - left) / size * 48 + 8).toFixed(1)},${((ys[index] - top) / size * 48 + 8).toFixed(1)}`).join(" ");
  return (
    <svg className="route-thumb" viewBox="0 0 64 64" aria-hidden="true">
      <polyline points={points} fill="none" stroke="currentColor" strokeWidth="4" vectorEffect="non-scaling-stroke" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  );
}

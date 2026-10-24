"use client";

import "maplibre-gl/dist/maplibre-gl.css";
import type { Map as MapLibre } from "maplibre-gl";
import { useCallback, useEffect, useRef } from "react";
import type { Find, Walk } from "@/lib/api";
import { KINDS, moodColor } from "@/lib/kinds";

type Lib = typeof import("maplibre-gl");
type Draw = (lib: Lib, container: HTMLDivElement) => MapLibre;

const STYLE = "https://tiles.openfreemap.org/styles/positron";
const ROUTE_COLOR = "#2ecc8f";
const photoPage = (find: Find) => `/photo-page.html?id=${find.id}`;

const newMap = ({ Map, LngLatBounds, NavigationControl }: Lib, container: HTMLDivElement, points: [number, number][], padding: number) => {
  const bounds = points.reduce((all, point) => all.extend(point), new LngLatBounds(points[0], points[0]));
  const map = new Map({ container, style: STYLE, bounds, fitBoundsOptions: { padding, maxZoom: 16 }, cooperativeGestures: true });
  map.addControl(new NavigationControl({ showCompass: false }), "top-right");
  return map;
};

const pin = (find: Find) => {
  const link = document.createElement("a");
  link.className = find.photo ? "pin" : "pin pin-dot";
  link.href = photoPage(find);
  link.style.background = find.photo ? `center / cover url("${find.photo}")` : KINDS[find.kind].color;
  return link;
};

// maplibre touches the window, so it loads in the browser only; the map goes away with the component
function MapView({ draw, className }: { draw: Draw; className?: string }) {
  const box = useRef<HTMLDivElement>(null);
  useEffect(() => {
    let map: MapLibre | undefined;
    let gone = false;
    import("maplibre-gl").then(lib => {
      if (!gone && box.current) map = draw(lib, box.current);
    });
    return () => {
      gone = true;
      map?.remove();
    };
  }, [draw]);
  return <div ref={box} className={`map${className ? ` ${className}` : ""}`} />;
}

/** One walk: its route from the mood before to the mood after, its start and its photos. */
export function WalkMap({ walk, finds, padding = 72, className }: { walk: Walk; finds: Find[]; padding?: number; className?: string }) {
  const draw = useCallback<Draw>((lib, container) => {
    const map = newMap(lib, container, walk.route, padding);
    map.on("load", () => {
      map.addSource("route", { type: "geojson", lineMetrics: true, data: { type: "Feature", properties: {}, geometry: { type: "LineString", coordinates: walk.route } } });
      const line = { type: "line", source: "route", layout: { "line-cap": "round", "line-join": "round" } } as const;
      map.addLayer({ ...line, id: "route-edge", paint: { "line-color": "#ffffff", "line-width": 10 } });
      map.addLayer({
        ...line, id: "route",
        paint: {
          "line-width": 6,
          "line-gradient": ["interpolate", ["linear"], ["line-progress"], 0, walk.mood ? moodColor(walk.mood[0]) : ROUTE_COLOR, 1, walk.mood ? moodColor(walk.mood[1]) : ROUTE_COLOR],
        },
      });
    });
    const start = document.createElement("span");
    start.className = "pin-start";
    new lib.Marker({ element: start }).setLngLat(walk.route[0]).addTo(map);
    // The marker writes its own label when added, so the name goes on after
    finds.forEach(find => new lib.Marker({ element: pin(find) }).setLngLat([find.lon, find.lat]).addTo(map)
      .getElement().setAttribute("aria-label", find.name || KINDS[find.kind].name));
    return map;
  }, [walk, finds, padding]);
  return <MapView draw={draw} className={className} />;
}

/** Many photos: neighbours merge into a circle with their number, a single one opens its page. */
export function FindsMap({ finds, className }: { finds: Find[]; className?: string }) {
  const draw = useCallback<Draw>((lib, container) => {
    const map = newMap(lib, container, finds.map(find => [find.lon, find.lat]), 96);
    map.on("load", () => {
      map.addSource("finds", {
        type: "geojson", cluster: true, clusterRadius: 48,
        data: { type: "FeatureCollection", features: finds.map(find => ({ type: "Feature", properties: { index: finds.indexOf(find), color: KINDS[find.kind].color }, geometry: { type: "Point", coordinates: [find.lon, find.lat] } })) },
      });
      map.addLayer({
        id: "groups", type: "circle", source: "finds", filter: ["has", "point_count"],
        paint: { "circle-color": "#0e2233", "circle-radius": ["step", ["get", "point_count"], 18, 10, 24, 50, 32], "circle-stroke-width": 4, "circle-stroke-color": "#ffffff" },
      });
      map.addLayer({
        id: "group-count", type: "symbol", source: "finds", filter: ["has", "point_count"],
        layout: { "text-field": ["get", "point_count_abbreviated"], "text-font": ["Noto Sans Bold"], "text-size": 14 }, paint: { "text-color": "#ffffff" },
      });
      map.addLayer({
        id: "single", type: "circle", source: "finds", filter: ["!", ["has", "point_count"]],
        paint: {
          "circle-color": ["get", "color"],
          "circle-radius": 10, "circle-stroke-width": 4, "circle-stroke-color": "#ffffff",
        },
      });
      map.on("click", "single", event => { location.href = photoPage(finds[event.features![0].properties.index]); });
      map.on("click", "groups", event => {
        const [lon, lat] = (event.features![0].geometry as GeoJSON.Point).coordinates;
        map.easeTo({ center: [lon, lat], zoom: map.getZoom() + 2 });
      });
      ["single", "groups"].forEach(layer => {
        map.on("mouseenter", layer, () => { map.getCanvas().style.cursor = "pointer"; });
        map.on("mouseleave", layer, () => { map.getCanvas().style.cursor = ""; });
      });
    });
    return map;
  }, [finds]);
  return <MapView draw={draw} className={className} />;
}

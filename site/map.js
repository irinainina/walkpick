const MAP_STYLE = 'https://tiles.openfreemap.org/styles/positron';
const kindColors = { plant: '#2ecc8f', bird: '#0470dc', place: '#c8762d', photo: '#566879', unknown: '#8a97a5' };
const boundsOf = points => points.reduce((bounds, point) => bounds.extend(point), new maplibregl.LngLatBounds(points[0], points[0]));
const newMap = (container, points, padding, interactive = true) => {
  const map = new maplibregl.Map({ container, style: MAP_STYLE, bounds: boundsOf(points), fitBoundsOptions: { padding, maxZoom: 16 }, interactive, cooperativeGestures: interactive });
  if (interactive) map.addControl(new maplibregl.NavigationControl({ showCompass: false }), 'top-right');
  return map;
};

const findPin = find => {
  const pin = document.createElement('a');
  pin.className = find.photo ? 'pin' : 'pin pin-dot';
  pin.href = `photo-page.html?id=${find.id}`;
  pin.dataset.find = find.id;
  pin.style.background = find.photo ? `center / cover url(assets/photos/${find.id}.jpg)` : kindColors[find.kind];
  pin.setAttribute('aria-label', findName(find));
  return pin;
};

// One walk: its route from the mood before to the mood after (plain green when the mood was not asked) and its photos
const walkMap = (container, walk, pins, padding = 72) => {
  const map = newMap(container, walk.route, padding);
  map.on('load', () => {
    map.addSource('route', { type: 'geojson', lineMetrics: true, data: { type: 'Feature', geometry: { type: 'LineString', coordinates: walk.route } } });
    map.addLayer({ id: 'route-edge', type: 'line', source: 'route', layout: { 'line-cap': 'round', 'line-join': 'round' }, paint: { 'line-color': '#ffffff', 'line-width': 10 } });
    map.addLayer({
      id: 'route', type: 'line', source: 'route', layout: { 'line-cap': 'round', 'line-join': 'round' },
      paint: { 'line-width': 6, 'line-gradient': ['interpolate', ['linear'], ['line-progress'], 0, walk.mood ? moodColor(walk.mood[0]) : '#2ecc8f', 1, walk.mood ? moodColor(walk.mood[1]) : '#2ecc8f'] },
    });
  });
  const start = document.createElement('span');
  start.className = 'pin-start';
  new maplibregl.Marker({ element: start }).setLngLat(walk.route[0]).addTo(map);
  pins.forEach(find => new maplibregl.Marker({ element: findPin(find) }).setLngLat([find.lon, find.lat]).addTo(map));
  return map;
};

// Many photos: neighbours merge into a circle with their number, a single one opens its page
const findsMap = (container, list) => {
  const map = newMap(container, list.map(find => [find.lon, find.lat]), 96);
  map.on('load', () => {
    map.addSource('finds', {
      type: 'geojson', cluster: true, clusterRadius: 48,
      data: { type: 'FeatureCollection', features: list.map(find => ({ type: 'Feature', properties: { id: find.id, kind: find.kind }, geometry: { type: 'Point', coordinates: [find.lon, find.lat] } })) },
    });
    map.addLayer({ id: 'groups', type: 'circle', source: 'finds', filter: ['has', 'point_count'], paint: { 'circle-color': '#0e2233', 'circle-radius': ['step', ['get', 'point_count'], 18, 10, 24, 50, 32], 'circle-stroke-width': 4, 'circle-stroke-color': '#ffffff' } });
    map.addLayer({ id: 'group-count', type: 'symbol', source: 'finds', filter: ['has', 'point_count'], layout: { 'text-field': ['get', 'point_count_abbreviated'], 'text-font': ['Noto Sans Bold'], 'text-size': 14 }, paint: { 'text-color': '#ffffff' } });
    map.addLayer({
      id: 'single', type: 'circle', source: 'finds', filter: ['!', ['has', 'point_count']],
      paint: { 'circle-color': ['match', ['get', 'kind'], ...Object.entries(kindColors).flat(), '#8a97a5'], 'circle-radius': 10, 'circle-stroke-width': 4, 'circle-stroke-color': '#ffffff' },
    });
    map.on('click', 'single', event => { location.href = `photo-page.html?id=${event.features[0].properties.id}`; });
    map.on('click', 'groups', event => map.easeTo({ center: event.features[0].geometry.coordinates, zoom: map.getZoom() + 2 }));
    ['single', 'groups'].forEach(layer => {
      map.on('mouseenter', layer, () => { map.getCanvas().style.cursor = 'pointer'; });
      map.on('mouseleave', layer, () => { map.getCanvas().style.cursor = ''; });
    });
  });
  return map;
};

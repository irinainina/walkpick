import { distanceM, merged, NearPlace } from './places';

const place = (
  name: string,
  source: NearPlace['source'],
  latitude: number,
  longitude: number,
): NearPlace => ({
  name,
  kind: 'Monument',
  distance_m: distanceM(
    { latitude: 50.58089, longitude: 34.47494 },
    { latitude, longitude },
  ),
  source,
  latitude,
  longitude,
});

describe('places near a photo', () => {
  it('measures metres between two points', () => {
    expect(
      distanceM(
        { latitude: 50, longitude: 34 },
        { latitude: 50.001, longitude: 34 },
      ),
    ).toBe(111);
  });

  it('keeps the name of Wikidata for a place both know and adds what only Google knows', () => {
    const wikidata = [
      place("Пам'ятник Тарасові Шевченку", 'wikidata', 50.58093, 34.47497),
    ];
    const google = [
      place("Pam'yatnyk T. H. Shevchenku", 'google', 50.58089, 34.47481),
      place('Памятник Лебедин', 'google', 50.58121, 34.47565),
    ];
    expect(merged(wikidata, google).map((it) => it.name)).toEqual([
      "Пам'ятник Тарасові Шевченку",
      'Памятник Лебедин',
    ]);
  });
});

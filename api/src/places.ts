import { Controller, Get, Injectable, Query } from '@nestjs/common';
import { Settings } from './config';
import { Asks, Guarded } from './docs';
import { coordinate } from './query';

// A photo of a place is taken from close by.
const RADIUS_M = 300;
const SHOWN = 10;
// A place of Google this close to one of Wikidata is the same place: Wikidata names it.
const SAME_M = 30;
const EARTH_M = 6_371_000;

const WIKIDATA = 'https://query.wikidata.org/sparql';
const GOOGLE = 'https://places.googleapis.com/v1/places:searchNearby';
// Without the choice of types Google answers with shops, taxis and chemists.
const GOOGLE_TYPES = [
  'historical_landmark',
  'historical_place',
  'cultural_landmark',
  'monument',
  'sculpture',
  'church',
  'museum',
  'art_gallery',
  'park',
  'tourist_attraction',
];

// What a type of Google is called on the sheet; the names Google gives follow the language asked.
const GOOGLE_KINDS: Record<string, string> = {
  historical_landmark: 'Landmark',
  historical_place: 'Landmark',
  cultural_landmark: 'Landmark',
  monument: 'Monument',
  sculpture: 'Sculpture',
  church: 'Church',
  museum: 'Museum',
  art_gallery: 'Gallery',
  park: 'Park',
  tourist_attraction: 'Sight',
};

/** A place near a photo, for the sheet "Which one is it?". */
export interface NearPlace {
  name: string;
  /** What it is, in English: "Church", "Monument". */
  kind: string;
  distance_m: number;
  source: 'wikidata' | 'google';
  latitude: number;
  longitude: number;
  /** Its article in Wikipedia: in the language of the country, else in English. */
  article?: { language: string; title: string };
}

/** Metres between two points, near enough for a few hundred metres. */
export function distanceM(
  a: { latitude: number; longitude: number },
  b: { latitude: number; longitude: number },
): number {
  const rad = Math.PI / 180;
  const x =
    (b.longitude - a.longitude) *
    rad *
    Math.cos(((a.latitude + b.latitude) / 2) * rad);
  const y = (b.latitude - a.latitude) * rad;
  return Math.round(Math.hypot(x, y) * EARTH_M);
}

/** Both lists as one, the nearest first; a place known to both keeps the name of Wikidata. */
export function merged(
  wikidata: NearPlace[],
  google: NearPlace[],
): NearPlace[] {
  const own = google.filter((place) =>
    wikidata.every((known) => distanceM(known, place) > SAME_M),
  );
  return [...wikidata, ...own]
    .sort((a, b) => a.distance_m - b.distance_m)
    .slice(0, SHOWN);
}

function article(row: Binding): Pick<NearPlace, 'article'> {
  const local = row.localTitle?.value;
  const code = row.code?.value;
  if (local && code) return { article: { language: code, title: local } };
  const en = row.enTitle?.value;
  return en ? { article: { language: 'en', title: en } } : {};
}

const capital = (text: string) => text.charAt(0).toUpperCase() + text.slice(1);

interface Binding {
  [name: string]: { value: string } | undefined;
}

@Injectable()
export class Places {
  constructor(private readonly settings: Settings) {}

  async near(latitude: number, longitude: number): Promise<NearPlace[]> {
    const here = { latitude, longitude };
    const { places, language } = await this.wikidata(here);
    const google = await this.google(here, language).catch(() => []);
    return merged(places, google);
  }

  // Named in the language of the country, as people there call the place; English when there is none.
  private async wikidata(here: { latitude: number; longitude: number }) {
    const query = `SELECT ?item ?local ?en ?type ?code ?loc ?localTitle ?enTitle WHERE {
  SERVICE wikibase:around { ?item wdt:P625 ?loc .
    bd:serviceParam wikibase:center "Point(${here.longitude} ${here.latitude})"^^geo:wktLiteral ;
      wikibase:radius "${RADIUS_M / 1000}" . }
  FILTER NOT EXISTS { ?item wdt:P1082 [] }
  OPTIONAL { ?item wdt:P31/rdfs:label ?type . FILTER(LANG(?type) = "en") }
  OPTIONAL { ?item rdfs:label ?en . FILTER(LANG(?en) = "en") }
  OPTIONAL { ?item wdt:P17/wdt:P37/wdt:P424 ?code . ?item rdfs:label ?local . FILTER(LANG(?local) = ?code)
    OPTIONAL { ?localPage schema:about ?item ; schema:inLanguage ?code ; schema:name ?localTitle ;
      schema:isPartOf/wikibase:wikiGroup "wikipedia" . } }
  OPTIONAL { ?enPage schema:about ?item ; schema:isPartOf <https://en.wikipedia.org/> ; schema:name ?enTitle . }
}`;
    const answer = await fetch(
      `${WIKIDATA}?format=json&query=${encodeURIComponent(query)}`,
      {
        headers: {
          'User-Agent':
            'WalkPick/0.2 (https://walkpick-406273949742.europe-west1.run.app)',
        },
      },
    );
    if (!answer.ok)
      throw new Error(`Wikidata did not answer: ${answer.status}`);
    const rows = ((await answer.json()) as { results: { bindings: Binding[] } })
      .results.bindings;
    const byItem = new Map<string, NearPlace>();
    let language: string | undefined;
    for (const row of rows) {
      language ??= row.code?.value;
      const name = row.local?.value ?? row.en?.value;
      const point = /Point\(([-\d.]+) ([-\d.]+)\)/.exec(row.loc?.value ?? '');
      const item = row.item?.value;
      if (!name || !point || !item || byItem.has(item)) continue;
      const at = { latitude: Number(point[2]), longitude: Number(point[1]) };
      byItem.set(item, {
        name,
        kind: capital(row.type?.value ?? 'place'),
        distance_m: distanceM(here, at),
        source: 'wikidata',
        ...at,
        ...article(row),
      });
    }
    return { places: [...byItem.values()], language };
  }

  private async google(
    here: { latitude: number; longitude: number },
    language: string | undefined,
  ): Promise<NearPlace[]> {
    const key = this.settings.googlePlacesKey;
    if (!key) return [];
    const answer = await fetch(GOOGLE, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'X-Goog-Api-Key': key,
        'X-Goog-FieldMask':
          'places.displayName,places.location,places.primaryType',
      },
      body: JSON.stringify({
        includedTypes: GOOGLE_TYPES,
        maxResultCount: 20,
        rankPreference: 'DISTANCE',
        ...(language ? { languageCode: language } : {}),
        locationRestriction: { circle: { center: here, radius: RADIUS_M } },
      }),
    });
    if (!answer.ok)
      throw new Error(`Google Places did not answer: ${answer.status}`);
    const { places = [] } = (await answer.json()) as {
      places?: {
        displayName?: { text: string };
        location: { latitude: number; longitude: number };
        primaryType?: string;
      }[];
    };
    return places
      .filter((place) => place.displayName?.text)
      .map((place) => ({
        name: place.displayName!.text,
        kind: GOOGLE_KINDS[place.primaryType ?? ''] ?? 'Place',
        distance_m: distanceM(here, place.location),
        source: 'google' as const,
        ...place.location,
      }));
  }
}

@Guarded('places')
@Controller('v1/places')
export class PlacesController {
  constructor(private readonly places: Places) {}

  /** Places near a photo of a place: the sheet "Which one is it?" offers them. */
  @Get()
  @Asks(
    [
      ['lat', { type: 'number', minimum: -90, maximum: 90 }],
      ['lon', { type: 'number', minimum: -180, maximum: 180 }],
    ],
    ['lat', 'lon'],
  )
  async near(@Query() asked: Record<string, string | undefined>) {
    const places = await this.places.near(
      coordinate('lat', asked.lat, 90),
      coordinate('lon', asked.lon, 180),
    );
    return { places };
  }
}

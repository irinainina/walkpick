import { Controller, Get, Injectable, Query } from '@nestjs/common';
import { Settings } from './config';
import { Asks, Guarded } from './docs';
import { coordinate, notFound } from './query';

const FORECA = 'https://weatherapi.foreca.net/api/v1/current';
// One answer serves a place about 5 km across for a quarter of an hour: the free plan allows
// 2,000 requests a day for all the users together.
const CELL_DEGREES = 0.05;
const KEEP_MS = 15 * 60 * 1000;
// Above this many mm an hour rain is heavy in the WMO table the app reads.
const HEAVY_MM_H = 7.6;

/** The weather now, in the fields and the codes of Open-Meteo, which the app already reads. */
export interface WeatherNow {
  weather_code: number;
  temperature_2m: number;
  wind_speed_10m: number;
}

interface ForecaNow {
  symbol: string;
  temperature: number;
  windSpeed: number;
  precipRate: number;
}

// The first digit of a Foreca symbol: clear, almost clear, half cloudy, broken, overcast, thin
// high clouds, fog.
const CLOUDS = [0, 1, 2, 2, 3, 1, 45];
// By the second digit - slight, showers, precipitation, thunder - for rain, sleet and snow.
const FALLING = [
  [61, 80, 63, 95],
  [66, 66, 67, 95],
  [71, 85, 73, 95],
];

/** Foreca's "d420" as the WMO code of the same weather: showers of rain. */
export function wmoCode(symbol: string, precipMmH: number): number {
  const [clouds, rate, kind] = symbol.slice(1).split('').map(Number);
  if (!rate) return CLOUDS[clouds] ?? 3;
  if (kind === 0 && rate < 4 && precipMmH >= HEAVY_MM_H)
    return rate === 2 ? 82 : 65;
  return FALLING[kind]?.[rate - 1] ?? 63;
}

@Injectable()
export class Weather {
  private readonly kept = new Map<string, { at: number; now: WeatherNow }>();

  constructor(private readonly settings: Settings) {}

  async at(lat: number, lon: number): Promise<WeatherNow> {
    const key = this.settings.forecaKey ?? notFound();
    const cell = [lat, lon]
      .map((degrees) => Math.round(degrees / CELL_DEGREES) * CELL_DEGREES)
      .map((degrees) => degrees.toFixed(2));
    const name = cell.join(',');
    const known = this.kept.get(name);
    if (known && Date.now() - known.at < KEEP_MS) return known.now;
    const answer = await fetch(`${FORECA}/${cell[1]},${cell[0]}`, {
      headers: { Authorization: `Bearer ${key}` },
    });
    if (!answer.ok) throw new Error(`Foreca did not answer: ${answer.status}`);
    const { current } = (await answer.json()) as { current: ForecaNow };
    const now = {
      weather_code: wmoCode(current.symbol, current.precipRate),
      temperature_2m: current.temperature,
      wind_speed_10m: current.windSpeed,
    };
    this.kept.set(name, { at: Date.now(), now });
    return now;
  }
}

@Guarded('weather')
@Controller('v1/weather')
export class WeatherController {
  constructor(private readonly weather: Weather) {}

  /** The weather now at a place; the app shows it in the header of Walk and names walks by it. */
  @Get()
  @Asks(
    [
      ['lat', { type: 'number', minimum: -90, maximum: 90 }],
      ['lon', { type: 'number', minimum: -180, maximum: 180 }],
    ],
    ['lat', 'lon'],
  )
  now(@Query() asked: Record<string, string | undefined>): Promise<WeatherNow> {
    return this.weather.at(
      coordinate('lat', asked.lat, 90),
      coordinate('lon', asked.lon, 180),
    );
  }
}

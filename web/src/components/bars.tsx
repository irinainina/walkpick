export interface Bar {
  label: string;
  title: string;
  value: number;
}

/** Bars against the largest one; a bar with nothing in it stays a grey stub. */
export default function Bars({ bars, unit }: { bars: Bar[]; unit: (value: number) => string }) {
  const top = Math.max(...bars.map(bar => bar.value), 1);
  return (
    <div className={`bars${bars.length > 14 ? " bars-dense" : ""}`}>
      {bars.map(bar => (
        <div key={bar.title} title={`${bar.title}: ${unit(bar.value)}`}>
          <i style={{ height: `${bar.value / top * 100}%` }} data-walked={bar.value ? "" : undefined} />
          {bar.label}
        </div>
      ))}
    </div>
  );
}

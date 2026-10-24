import { MOODS } from "@/lib/kinds";

/** The mood before and after a walk as two coloured dots with names. */
export default function MoodPair({ mood }: { mood: [number, number] }) {
  const [before, after] = mood.map(value => MOODS[value - 1]);
  return (
    <span className="mood">
      <i style={{ background: before.color }} />{before.name} → <i style={{ background: after.color }} />{after.name}
    </span>
  );
}

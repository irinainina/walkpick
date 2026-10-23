import { PERIODS, type Period } from "@/lib/period";

export default function PeriodSwitch({ chosen, onChange }: { chosen: Period; onChange: (period: Period) => void }) {
  return (
    <div className="segmented" role="group" aria-label="Period">
      {(Object.entries(PERIODS) as [Period, string][]).map(([id, name]) => (
        <button key={id} type="button" aria-pressed={id === chosen} onClick={() => onChange(id)}>{name}</button>
      ))}
    </div>
  );
}

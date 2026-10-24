import { MOODS } from "@/lib/kinds";

const EYES = "M22.5 25.6c1.436 0 2.6-1.612 2.6-3.6s-1.164-3.6-2.6-3.6-2.6 1.612-2.6 3.6 1.164 3.6 2.6 3.6zM35.5 25.6c1.436 0 2.6-1.612 2.6-3.6s-1.164-3.6-2.6-3.6-2.6 1.612-2.6 3.6 1.164 3.6 2.6 3.6z";
// The mouths from Awful to Great, the same faces as in the app
const MOUTHS = [
  "M19.5 24.5l6-4M32.5 20.5l6 4M20.5 38c5.667-6 11.333-6 17 0",
  "M21.5 37c5-4 10-4 15 0",
  "M22 35.5l14-3",
  "M21 32c5.333 4.333 10.667 4.333 16 0",
  "M21 33.5c5.333 5 10.667 5 16 0",
];

/** One of the five mood faces, value 1 to 5. */
export default function MoodFace({ value }: { value: number }) {
  return (
    <svg viewBox="5 3 48 48" fill="none" stroke={MOODS[value - 1].color} strokeLinecap="round" aria-hidden="true">
      <path d="M29 48.5a21.5 21.5 0 1 0 0-43a21.5 21.5 0 0 0 0 43z" fill="#fff" strokeWidth="3" />
      {value > 1 && <path stroke="none" fill={MOODS[value - 1].color} d={EYES} />}
      <path strokeWidth="3" d={MOUTHS[value - 1]} />
      {value === 5 && <path strokeWidth="2.5" d="M16.4 28.6c-.8 1.333-.733 2.667.2 4M41.6 28.6c.8 1.333.733 2.667-.2 4" />}
    </svg>
  );
}

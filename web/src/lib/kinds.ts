export type Kind = "plant" | "bird" | "place" | "photo" | "unknown";

export const KINDS: Record<Kind, { name: string; color: string }> = {
  plant: { name: "Plant", color: "#2ecc8f" },
  bird: { name: "Bird", color: "#0470dc" },
  place: { name: "Place", color: "#c8762d" },
  photo: { name: "Photo", color: "#566879" },
  unknown: { name: "Unknown", color: "#8a97a5" },
};

// The five answers of the app from the worst to the best
export const MOODS = [
  { name: "Awful", color: "#ff6b6b" },
  { name: "Bad", color: "#ff9f43" },
  { name: "Neutral", color: "#f5b82e" },
  { name: "Good", color: "#8fd16a" },
  { name: "Great", color: "#2ecc8f" },
];

export const moodColor = (value: number) => MOODS[value - 1].color;

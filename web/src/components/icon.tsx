const PATHS = {
  arrow: "M7 17L17 7M9 7h8v8",
  mail: "M4 6h16v12H4z M4 7l8 6 8-6",
  search: "M11 18a7 7 0 1 0 0-14 7 7 0 0 0 0 14z M16 16l4 4",
};

export default function Icon({ name }: { name: keyof typeof PATHS }) {
  return (
    <svg className="icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <path d={PATHS[name]} />
    </svg>
  );
}

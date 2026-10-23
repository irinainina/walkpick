const PATHS = {
  arrow: "M7 17L17 7M9 7h8v8",
  mail: "M4 6h16v12H4z M4 7l8 6 8-6",
};

export default function Icon({ name }: { name: keyof typeof PATHS }) {
  return (
    <svg className="icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <path d={PATHS[name]} />
    </svg>
  );
}

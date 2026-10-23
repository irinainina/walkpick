/** Large numbers with their labels under them, two to a row. */
export default function Stats({ items }: { items: [string | number, string][] }) {
  return (
    <dl className="stats">
      {items.map(([value, label]) => (
        <div key={label}>
          <dt>{label}</dt>
          <dd>{value}</dd>
        </div>
      ))}
    </dl>
  );
}

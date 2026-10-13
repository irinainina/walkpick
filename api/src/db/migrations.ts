/** The migrations that are in the folder and not in the database yet, in their order. */
export function pending(files: string[], applied: string[]): string[] {
  const done = new Set(applied);
  return files
    .filter((file) => file.endsWith('.sql') && !done.has(file))
    .sort((a, b) => a.localeCompare(b));
}

/** The migrations up to the named number: the ones applied by hand before the service kept count. */
export function upTo(files: string[], last: string): string[] {
  return pending(files, []).filter(
    (file) => file.slice(0, last.length) <= last,
  );
}

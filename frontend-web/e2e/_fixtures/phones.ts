/**
 * Random Moroccan phone-number generator for E2E fixtures.
 *
 * Format: +2126XXXXXXXX (12 chars after the +). We bias the first digit after
 * `+2126` to a digit ≥ 0 so every generated number is a plausible MA mobile.
 * Each call returns a unique value within the process by seeding from a
 * monotonic counter combined with the current timestamp millis.
 */
let counter = 0;

export function randomMoroccanPhone(): string {
  counter += 1;
  // 8 digits after +2126
  const millis = Date.now() % 10_000_000;
  const noise = Math.floor(Math.random() * 10);
  const tail = `${millis}${counter % 10}${noise}`.slice(-8).padStart(8, '0');
  return `+2126${tail}`;
}

export function randomEmail(prefix = 'e2e'): string {
  counter += 1;
  return `${prefix}-${Date.now()}-${counter}@naja7i.test`;
}

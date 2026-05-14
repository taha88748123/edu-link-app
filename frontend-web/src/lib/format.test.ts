import { describe, expect, it } from 'vitest';

import { formatMoneyMad, initials } from './format';

describe('formatMoneyMad', () => {
  it('formats 15000 centimes as 150,00 MAD in fr-MA', () => {
    const formatted = formatMoneyMad(15000, 'fr');
    // Different ICU versions may emit U+00A0 (NBSP) or U+202F (narrow NBSP) as the group separator.
    // We normalise to a regular space before asserting.
    const normalised = formatted.replace(/ | /g, ' ');
    expect(normalised).toMatch(/^150,00\s*MAD$/);
  });

  it('returns a string for zero', () => {
    expect(typeof formatMoneyMad(0, 'fr')).toBe('string');
  });
});

describe('initials', () => {
  it('produces the first letter of each word', () => {
    expect(initials('Jane Doe')).toBe('JD');
    expect(initials('hassan')).toBe('H');
    expect(initials('  ')).toBe('?');
    expect(initials(null)).toBe('?');
  });
});

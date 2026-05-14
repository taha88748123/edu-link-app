import { describe, expect, it } from 'vitest';

import fr from './locales/fr.json';
import ar from './locales/ar.json';
import en from './locales/en.json';

type JsonRecord = Record<string, unknown>;

function flatten(obj: unknown, prefix = ''): string[] {
  if (obj === null || typeof obj !== 'object') return [prefix];
  const out: string[] = [];
  for (const [k, v] of Object.entries(obj as JsonRecord)) {
    const next = prefix ? `${prefix}.${k}` : k;
    if (v !== null && typeof v === 'object') {
      out.push(...flatten(v, next));
    } else {
      out.push(next);
    }
  }
  return out;
}

describe('i18n locale files', () => {
  const frKeys = new Set(flatten(fr));
  const arKeys = new Set(flatten(ar));
  const enKeys = new Set(flatten(en));

  it('fr/ar/en expose the same set of translation keys', () => {
    const diffArMissing = [...frKeys].filter((k) => !arKeys.has(k));
    const diffArExtra = [...arKeys].filter((k) => !frKeys.has(k));
    const diffEnMissing = [...frKeys].filter((k) => !enKeys.has(k));
    const diffEnExtra = [...enKeys].filter((k) => !frKeys.has(k));

    expect(diffArMissing, `Keys missing in ar.json: ${diffArMissing.join(', ')}`).toEqual([]);
    expect(diffArExtra, `Extra keys in ar.json: ${diffArExtra.join(', ')}`).toEqual([]);
    expect(diffEnMissing, `Keys missing in en.json: ${diffEnMissing.join(', ')}`).toEqual([]);
    expect(diffEnExtra, `Extra keys in en.json: ${diffEnExtra.join(', ')}`).toEqual([]);
  });
});

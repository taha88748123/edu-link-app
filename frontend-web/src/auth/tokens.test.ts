import { describe, expect, it } from 'vitest';

import { decodeJwt, isJwtExpired } from './tokens';

// Helper that produces a deterministic JWT-like token (header.payload.signature).
function makeJwt(payload: Record<string, unknown>): string {
  const header = btoa(JSON.stringify({ alg: 'HS256', typ: 'JWT' }))
    .replace(/=+$/, '')
    .replace(/\+/g, '-')
    .replace(/\//g, '_');
  const body = btoa(JSON.stringify(payload))
    .replace(/=+$/, '')
    .replace(/\+/g, '-')
    .replace(/\//g, '_');
  return `${header}.${body}.signature`;
}

describe('decodeJwt', () => {
  it('parses sub / role / locale / exp claims', () => {
    const exp = Math.floor(Date.now() / 1000) + 3600;
    const token = makeJwt({
      sub: '11111111-2222-3333-4444-555555555555',
      role: 'STUDENT',
      locale: 'fr',
      exp,
    });
    const claims = decodeJwt(token);
    expect(claims).not.toBeNull();
    expect(claims?.sub).toBe('11111111-2222-3333-4444-555555555555');
    expect(claims?.role).toBe('STUDENT');
    expect(claims?.locale).toBe('fr');
    expect(claims?.exp).toBe(exp);
  });

  it('returns null for malformed tokens', () => {
    expect(decodeJwt('')).toBeNull();
    expect(decodeJwt('not-a-jwt')).toBeNull();
    expect(decodeJwt('a.b')).toBeNull(); // payload "b" is not valid base64-JSON
    expect(decodeJwt('a.b.c')).toBeNull(); // header/payload still not base64-JSON
  });

  it('detects expiration correctly', () => {
    const past = makeJwt({ sub: 'x', role: 'STUDENT', exp: 1 });
    const future = makeJwt({
      sub: 'x',
      role: 'STUDENT',
      exp: Math.floor(Date.now() / 1000) + 3600,
    });
    expect(isJwtExpired(past)).toBe(true);
    expect(isJwtExpired(future)).toBe(false);
  });
});

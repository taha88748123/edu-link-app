import type { Locale, Role } from '@/api/types';

/**
 * Claims we expect on the JWT issued by `auth-service`.
 * The frontend does NOT verify the signature — it just reads claims for routing.
 */
export interface JwtClaims {
  sub: string;
  role: Role;
  locale?: Locale;
  exp: number;
  iat?: number;
  iss?: string;
}

/** Decode the base64url payload of a JWT. Returns `null` if the token is malformed. */
export function decodeJwt(token: string): JwtClaims | null {
  if (!token || typeof token !== 'string') return null;
  const parts = token.split('.');
  if (parts.length < 2) return null;

  try {
    const payload = parts[1];
    // base64url -> base64
    const b64 = payload.replace(/-/g, '+').replace(/_/g, '/');
    const padded = b64 + '='.repeat((4 - (b64.length % 4)) % 4);

    const decoded =
      typeof atob === 'function'
        ? atob(padded)
        : Buffer.from(padded, 'base64').toString('binary');

    // Re-decode as UTF-8 (atob produces a binary string).
    const json = decodeURIComponent(
      decoded
        .split('')
        .map((c) => '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2))
        .join(''),
    );

    return JSON.parse(json) as JwtClaims;
  } catch {
    return null;
  }
}

/** Returns true if the JWT `exp` claim is in the past (with a small skew). */
export function isJwtExpired(token: string, skewSec = 5): boolean {
  const claims = decodeJwt(token);
  if (!claims || typeof claims.exp !== 'number') return true;
  return claims.exp * 1000 < Date.now() - skewSec * 1000;
}

import type { MeResponse, Role, TokenResponse } from '@/api/types';

export interface AuthState {
  accessToken: string | null;
  refreshToken: string | null;
  user: MeResponse | null;
  hydrated: boolean;

  hydrate(): void;
  setTokens(t: TokenResponse): void;
  setUser(u: MeResponse | null): void;
  clear(): void;
}

export interface AuthSnapshot {
  user: MeResponse | null;
  role: Role | null;
  isAuthenticated: boolean;
}

export const STORAGE_KEYS = {
  access: 'naja7i.access',
  refresh: 'naja7i.refresh',
  locale: 'naja7i.locale',
  theme: 'naja7i.theme',
} as const;

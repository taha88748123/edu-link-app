import { create } from 'zustand';

import type { MeResponse, TokenResponse } from '@/api/types';
import { type AuthState, STORAGE_KEYS } from './auth.types';

function readStorage(key: string): string | null {
  try {
    return typeof window === 'undefined' ? null : window.localStorage.getItem(key);
  } catch {
    return null;
  }
}

function writeStorage(key: string, value: string | null) {
  try {
    if (typeof window === 'undefined') return;
    if (value === null) {
      window.localStorage.removeItem(key);
    } else {
      window.localStorage.setItem(key, value);
    }
  } catch {
    /* ignore quota errors etc. */
  }
}

/**
 * Single source of truth for auth tokens + the in-memory user echo.
 * Tokens are persisted to localStorage; `user` is intentionally NOT persisted —
 * it must come from `GET /api/v1/auth/me` after rehydration so we never trust
 * a stale role saved on disk.
 */
export const useAuthStore = create<AuthState>((set) => ({
  accessToken: null,
  refreshToken: null,
  user: null,
  hydrated: false,

  hydrate() {
    const access = readStorage(STORAGE_KEYS.access);
    const refresh = readStorage(STORAGE_KEYS.refresh);
    set({
      accessToken: access,
      refreshToken: refresh,
      hydrated: true,
    });
  },

  setTokens(t: TokenResponse) {
    writeStorage(STORAGE_KEYS.access, t.accessToken);
    writeStorage(STORAGE_KEYS.refresh, t.refreshToken);
    set({ accessToken: t.accessToken, refreshToken: t.refreshToken });
  },

  setUser(u: MeResponse | null) {
    set({ user: u });
  },

  clear() {
    writeStorage(STORAGE_KEYS.access, null);
    writeStorage(STORAGE_KEYS.refresh, null);
    set({ accessToken: null, refreshToken: null, user: null });
  },
}));

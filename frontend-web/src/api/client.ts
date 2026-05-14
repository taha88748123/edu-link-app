import axios, {
  AxiosError,
  AxiosInstance,
  AxiosRequestConfig,
  InternalAxiosRequestConfig,
} from 'axios';

import { useAuthStore } from '@/auth/auth.store';
import type { ApiError, ApiResponse, TokenResponse } from './types';

const BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';

/**
 * Axios instance used by every typed API call.
 * - Request interceptor: injects `Authorization: Bearer <access>`.
 * - Response interceptor: on HTTP 401 (and only once per request), attempts a refresh
 *   via `/api/v1/auth/refresh`, stores the new tokens, and retries the original call.
 *   On refresh failure: clears auth store + redirects to `/login?next=...`.
 */
export const apiClient: AxiosInstance = axios.create({
  baseURL: BASE_URL,
  headers: {
    'Content-Type': 'application/json',
    Accept: 'application/json',
  },
  // No credentials/cookies — we are JWT-only.
  withCredentials: false,
});

// Marker so we only retry once per request.
interface RetriableConfig extends InternalAxiosRequestConfig {
  _retried?: boolean;
  _skipAuth?: boolean;
}

apiClient.interceptors.request.use((config) => {
  const cfg = config as RetriableConfig;
  if (cfg._skipAuth) return cfg;
  const token = useAuthStore.getState().accessToken;
  if (token) {
    cfg.headers.set('Authorization', `Bearer ${token}`);
  }
  return cfg;
});

// ---- Refresh single-flight ----
let refreshInFlight: Promise<TokenResponse | null> | null = null;

async function performRefresh(): Promise<TokenResponse | null> {
  const refreshToken = useAuthStore.getState().refreshToken;
  if (!refreshToken) return null;
  try {
    const resp = await axios.post<ApiResponse<TokenResponse>>(
      `${BASE_URL}/api/v1/auth/refresh`,
      { refreshToken },
      { headers: { 'Content-Type': 'application/json' } },
    );
    const tokens = resp.data?.data;
    if (!tokens) return null;
    useAuthStore.getState().setTokens(tokens);
    return tokens;
  } catch {
    return null;
  }
}

function redirectToLogin() {
  const here = window.location.pathname + window.location.search;
  const next = encodeURIComponent(here);
  // Use replace to avoid stacking history entries on token expiry.
  window.location.replace(`/login?next=${next}`);
}

apiClient.interceptors.response.use(
  (resp) => resp,
  async (error: AxiosError<ApiResponse<unknown>>) => {
    const original = error.config as RetriableConfig | undefined;
    const status = error.response?.status;

    // Auto-refresh on 401 (only for authenticated calls, only once).
    if (status === 401 && original && !original._retried && !original._skipAuth) {
      original._retried = true;

      if (!refreshInFlight) {
        refreshInFlight = performRefresh().finally(() => {
          refreshInFlight = null;
        });
      }
      const tokens = await refreshInFlight;

      if (tokens) {
        original.headers.set('Authorization', `Bearer ${tokens.accessToken}`);
        return apiClient(original);
      }

      // Refresh failed: nuke auth + bounce to login.
      useAuthStore.getState().clear();
      if (typeof window !== 'undefined' && !window.location.pathname.startsWith('/login')) {
        redirectToLogin();
      }
    }

    return Promise.reject(error);
  },
);

/**
 * Unwrap an `ApiResponse<T>` envelope or throw a typed {@link ApiClientError}.
 */
export class ApiClientError extends Error {
  readonly code: string;
  readonly messageKey?: string;
  readonly details?: unknown;
  readonly traceId?: string;
  readonly httpStatus?: number;

  constructor(err: ApiError, httpStatus?: number) {
    super(err.message);
    this.code = err.code;
    this.messageKey = err.messageKey;
    this.details = err.details;
    this.traceId = err.traceId;
    this.httpStatus = httpStatus;
    this.name = 'ApiClientError';
  }
}

export async function unwrap<T>(promise: Promise<{ data: ApiResponse<T> }>): Promise<T> {
  try {
    const resp = await promise;
    if (resp.data?.error) {
      throw new ApiClientError(resp.data.error);
    }
    // `data` may legitimately be null on 204-like responses.
    return resp.data?.data as T;
  } catch (e) {
    if (e instanceof ApiClientError) throw e;
    const ax = e as AxiosError<ApiResponse<unknown>>;
    const apiErr = ax.response?.data?.error;
    if (apiErr) {
      throw new ApiClientError(apiErr, ax.response?.status);
    }
    // Network / unknown error.
    throw new ApiClientError(
      {
        code: 'INTERNAL_ERROR',
        message: ax.message || 'Network error',
      },
      ax.response?.status,
    );
  }
}

/** Helper to call public endpoints (skips Authorization header). */
export function publicRequest<T>(config: AxiosRequestConfig): Promise<T> {
  // We tag the request config with `_skipAuth` so the request interceptor knows
  // not to attach the Authorization header. The field is not part of axios's
  // public type, so we cast through `unknown` to add it without TS complaints.
  const tagged = { ...config, _skipAuth: true } as unknown as AxiosRequestConfig;
  return unwrap<T>(apiClient.request(tagged));
}

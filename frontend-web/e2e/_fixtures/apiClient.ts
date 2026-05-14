/**
 * Backend API helper for E2E fixtures.
 *
 * Uses Playwright's `request` context (fetch under the hood). Two base URLs:
 *   - GATEWAY (default 8080) for all normal user-facing endpoints
 *   - AUTH_DIRECT (default 8101) for the dev-only OTP backdoor, which is
 *     intentionally NOT routed through the gateway.
 *
 * All "seed*" helpers are idempotent in the sense that they generate unique
 * phones per call — running them twice on a clean DB will create two distinct
 * accounts. They never expect a specific pre-existing state.
 */
import type { APIRequestContext } from '@playwright/test';

import { randomMoroccanPhone, randomEmail } from './phones';

export const GATEWAY_URL =
  process.env.E2E_API_URL || 'http://localhost:8080';
export const AUTH_DIRECT_URL =
  process.env.E2E_AUTH_DIRECT_URL || 'http://localhost:8101';

type Role = 'STUDENT' | 'PROF' | 'PARENT' | 'ADMIN';

interface ApiEnvelope<T> {
  data?: T | null;
  error?: { code: string; message: string } | null;
}

async function unwrap<T>(resp: { ok(): boolean; status(): number; text(): Promise<string> }): Promise<T> {
  const body = await resp.text();
  if (!resp.ok()) {
    throw new Error(`HTTP ${resp.status()} — ${body}`);
  }
  if (!body) return undefined as T;
  const parsed = JSON.parse(body) as ApiEnvelope<T>;
  if (parsed.error) {
    throw new Error(`API error ${parsed.error.code}: ${parsed.error.message}`);
  }
  return parsed.data as T;
}

export interface SeededUser {
  userId: string;
  phone: string;
  email: string;
  password: string;
  role: Role;
  accessToken: string;
  refreshToken: string;
}

export class ApiClient {
  constructor(private readonly request: APIRequestContext) {}

  // ---------------------------------------------------------------- low-level

  async registerUser(input: {
    phone?: string;
    email?: string;
    password?: string;
    role: Role;
  }): Promise<{ userId: string; phone: string; email: string; password: string }> {
    const phone = input.phone ?? randomMoroccanPhone();
    const email = input.email ?? randomEmail('user');
    const password = input.password ?? 'StrongTestPass!123';
    if (input.role === 'ADMIN') {
      throw new Error('Cannot self-register an ADMIN. Use seedAdmin with env credentials.');
    }
    const resp = await this.request.post(`${GATEWAY_URL}/api/v1/auth/register`, {
      data: { phone, email, password, role: input.role, locale: 'fr' },
      headers: { 'content-type': 'application/json' },
    });
    const data = await unwrap<{ userId: string }>(resp);
    return { userId: data.userId, phone, email, password };
  }

  /** DEV-ONLY: reads the plain OTP code from auth-service via the X-Test-Mode backdoor. */
  async fetchOtp(userId: string): Promise<{ code: string; channel: string; expiresAt: string }> {
    const resp = await this.request.get(
      `${AUTH_DIRECT_URL}/api/v1/dev/auth/otp/${userId}`,
      { headers: { 'X-Test-Mode': 'true' } },
    );
    return unwrap<{ code: string; channel: string; expiresAt: string }>(resp);
  }

  async verifyOtp(userId: string, code: string): Promise<{ accessToken: string; refreshToken: string; userId: string; role: Role }> {
    const resp = await this.request.post(`${GATEWAY_URL}/api/v1/auth/verify-otp`, {
      data: { userId, code },
      headers: { 'content-type': 'application/json' },
    });
    return unwrap<{ accessToken: string; refreshToken: string; userId: string; role: Role }>(resp);
  }

  async loginViaApi(phoneOrEmail: string, password: string): Promise<{ accessToken: string; refreshToken: string; userId: string; role: Role }> {
    const resp = await this.request.post(`${GATEWAY_URL}/api/v1/auth/login`, {
      data: { phoneOrEmail, password },
      headers: { 'content-type': 'application/json' },
    });
    return unwrap<{ accessToken: string; refreshToken: string; userId: string; role: Role }>(resp);
  }

  // ---------------------------------------------------------------- composite seeds

  /**
   * Register + verify OTP. Returns the full token pair so the caller can drop
   * straight into `storageState`.
   */
  async seedVerifiedUser(role: Role): Promise<SeededUser> {
    const { userId, phone, email, password } = await this.registerUser({ role });
    const { code } = await this.fetchOtp(userId);
    const { accessToken, refreshToken } = await this.verifyOtp(userId, code);
    return { userId, phone, email, password, role, accessToken, refreshToken };
  }

  /** Convenience wrapper: student with verified OTP. Wallet top-up should be
   *  done via seedTopupApproved using an admin token. */
  async seedStudent(opts: { phone?: string; password?: string } = {}): Promise<SeededUser> {
    const { userId, phone, email, password } = await this.registerUser({
      role: 'STUDENT',
      phone: opts.phone,
      password: opts.password,
    });
    const { code } = await this.fetchOtp(userId);
    const { accessToken, refreshToken } = await this.verifyOtp(userId, code);
    return { userId, phone, email, password, role: 'STUDENT', accessToken, refreshToken };
  }

  /**
   * Register + verify a PROF. Note: profile/pricing/calendar wiring lives in
   * other services and is left to the caller (or the seedProf composite if
   * the brief required it). The brief mentions calling `seedProf({
   * phone, password, subjects[], levels[], modalities[] })` — but those
   * service endpoints aren't part of auth-service. We expose the primitive
   * here; tests stitch on the per-service helpers as needed.
   */
  async seedProf(opts: { phone?: string; password?: string } = {}): Promise<SeededUser> {
    const { userId, phone, email, password } = await this.registerUser({
      role: 'PROF',
      phone: opts.phone,
      password: opts.password,
    });
    const { code } = await this.fetchOtp(userId);
    const { accessToken, refreshToken } = await this.verifyOtp(userId, code);
    return { userId, phone, email, password, role: 'PROF', accessToken, refreshToken };
  }

  /**
   * Resolve an ADMIN identity. Self-registration is forbidden by AuthService,
   * so we require env-provided credentials for a pre-seeded admin account.
   *
   * Required env vars:
   *   E2E_ADMIN_PHONE     — phone or email
   *   E2E_ADMIN_PASSWORD  — plain password
   */
  async seedAdmin(): Promise<{ accessToken: string; refreshToken: string; userId: string; role: Role; phone: string; password: string }> {
    const phone = process.env.E2E_ADMIN_PHONE;
    const password = process.env.E2E_ADMIN_PASSWORD;
    if (!phone || !password) {
      throw new Error(
        'E2E_ADMIN_PHONE / E2E_ADMIN_PASSWORD must be set — admins cannot self-register.',
      );
    }
    const tokens = await this.loginViaApi(phone, password);
    return { phone, password, ...tokens };
  }
}

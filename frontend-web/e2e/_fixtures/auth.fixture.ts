/**
 * Playwright fixtures for seeded + logged-in users.
 *
 * Usage in a spec:
 *   import { test, expect } from './_fixtures/auth.fixture';
 *   test('discover', async ({ studentAuth }) => {
 *     await studentAuth.page.goto('/student/discover');
 *     ...
 *   });
 *
 * Implementation note: rather than driving the login form, we seed the user
 * via the API and inject the access/refresh tokens directly into localStorage.
 * This is dramatically faster and avoids a flaky 6-digit OTP UI step on every
 * test. The auth-register-login spec still exercises the UI path end-to-end.
 */
import { test as base, type Page, type APIRequestContext } from '@playwright/test';

import { ApiClient, type SeededUser } from './apiClient';

export const STORAGE_KEYS = {
  access: 'naja7i.access',
  refresh: 'naja7i.refresh',
} as const;

export interface AuthedSession {
  page: Page;
  user: SeededUser;
}

/**
 * Inject tokens into the page's localStorage before any app code runs. We
 * navigate to the root once first so localStorage has the right origin.
 */
async function primeTokens(page: Page, user: SeededUser, baseURL: string): Promise<void> {
  // Loading the (light) root page is the cheapest way to attach an origin to
  // localStorage. The app's RequireAuth then sees the tokens on first render.
  await page.goto(baseURL, { waitUntil: 'domcontentloaded' });
  await page.evaluate(
    ([accessKey, refreshKey, access, refresh]) => {
      window.localStorage.setItem(accessKey, access);
      window.localStorage.setItem(refreshKey, refresh);
    },
    [STORAGE_KEYS.access, STORAGE_KEYS.refresh, user.accessToken, user.refreshToken] as const,
  );
}

interface Fixtures {
  api: ApiClient;
  studentAuth: AuthedSession;
  profAuth: AuthedSession;
  adminAuth: AuthedSession;
}

export const test = base.extend<Fixtures>({
  api: async ({ request }: { request: APIRequestContext }, use) => {
    await use(new ApiClient(request));
  },

  studentAuth: async ({ page, api, baseURL }, use) => {
    const user = await api.seedStudent();
    await primeTokens(page, user, baseURL ?? 'http://localhost:5173');
    await use({ page, user });
  },

  profAuth: async ({ page, api, baseURL }, use) => {
    const user = await api.seedProf();
    await primeTokens(page, user, baseURL ?? 'http://localhost:5173');
    await use({ page, user });
  },

  adminAuth: async ({ page, api, baseURL }, use) => {
    const tokens = await api.seedAdmin();
    const user: SeededUser = {
      userId: tokens.userId,
      phone: tokens.phone,
      email: '',
      password: tokens.password,
      role: 'ADMIN',
      accessToken: tokens.accessToken,
      refreshToken: tokens.refreshToken,
    };
    await primeTokens(page, user, baseURL ?? 'http://localhost:5173');
    await use({ page, user });
  },
});

export { expect } from '@playwright/test';

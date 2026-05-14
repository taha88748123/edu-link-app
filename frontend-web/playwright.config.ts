import { defineConfig, devices } from '@playwright/test';

/**
 * Playwright config — kept at the project root (not under e2e/) so the
 * `playwright` CLI auto-discovers it from `npm run e2e` no matter the cwd.
 *
 * Test runs are serialised by design: the golden paths share backend state
 * (users, bookings, topups) and rely on unique-per-run phone numbers to stay
 * idempotent. Parallelism + retries would flake against the shared DB.
 *
 * Environment variables:
 *   E2E_BASE_URL          — frontend URL (default http://localhost:5173)
 *   E2E_API_URL           — backend gateway (default http://localhost:8080)
 *   E2E_AUTH_DIRECT_URL   — auth-service direct port (default http://localhost:8101)
 *                           used only by the dev OTP backdoor
 *   NAJA7I_AUTH_TEST_MODE — must be `true` server-side (sets the property
 *                           that enables the TestSupportController bean)
 */
export default defineConfig({
  testDir: './e2e',
  timeout: 60_000,
  expect: { timeout: 10_000 },
  fullyParallel: false,
  workers: 1,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 2 : 0,
  reporter: [['list'], ['html', { open: 'never' }]],
  use: {
    baseURL: process.env.E2E_BASE_URL || 'http://localhost:5173',
    trace: 'on-first-retry',
    screenshot: 'only-on-failure',
    video: 'retain-on-failure',
    actionTimeout: 10_000,
    navigationTimeout: 30_000,
  },
  projects: [
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'] },
    },
    // Firefox + WebKit intentionally disabled in v1 to keep CI fast.
    // Re-enable once Chromium-only flows are stable.
  ],
  webServer: {
    command: 'npm run dev',
    port: 5173,
    reuseExistingServer: !process.env.CI,
    timeout: 120_000,
    stdout: 'ignore',
    stderr: 'pipe',
  },
});

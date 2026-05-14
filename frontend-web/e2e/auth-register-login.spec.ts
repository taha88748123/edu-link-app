import { test, expect, STORAGE_KEYS } from './_fixtures/auth.fixture';
import { randomMoroccanPhone } from './_fixtures/phones';

/**
 * Golden path #1 — register → verify OTP → land on a role home → logout → login.
 *
 * This is the one spec that exercises the auth UI end-to-end. The other specs
 * use API seeding for speed; this one validates the UI plumbing.
 */
test.describe('auth: register + login', () => {
  test('registers a STUDENT, verifies via API-peeked OTP, then logs in via form', async ({
    page,
    api,
  }) => {
    const phone = randomMoroccanPhone();
    const password = 'StrongTestPass!123';

    let userId: string | null = null;

    await test.step('open /register and fill the form', async () => {
      await page.goto('/register');
      await page.getByTestId('register-phone').fill(phone);
      await page.getByTestId('register-password').fill(password);
      // confirm password input has no testid — locate by its label.
      await page.getByLabel(/confirmer le mot de passe|confirm password|تأكيد/i).fill(password);
      // STUDENT is the default role; we still click the select to make the test
      // explicit about the choice in case defaults change later.
      await page.getByTestId('register-role-select').click();
      await page.getByRole('option', { name: /étudiant|student|طالب/i }).click();
      // Accept the T&Cs checkbox (zod requires it).
      await page.locator('#accept-terms').check();
      await page.getByTestId('register-submit').click();
    });

    await test.step('lands on /auth/verify-otp with userId in the URL', async () => {
      await page.waitForURL(/\/auth\/verify-otp\?userId=.+/, { timeout: 15_000 });
      const url = new URL(page.url());
      userId = url.searchParams.get('userId');
      expect(userId, 'userId query param must be present after register redirect').toBeTruthy();
    });

    await test.step('fetch OTP via the dev backdoor and type it', async () => {
      expect(userId).not.toBeNull();
      const { code } = await api.fetchOtp(userId!);
      expect(code).toMatch(/^\d{6}$/);
      // Fill each digit input — the page auto-advances focus on input.
      for (let i = 0; i < 6; i++) {
        await page.getByTestId(`otp-digit-${i}`).fill(code[i]);
      }
      await page.getByTestId('otp-submit').click();
    });

    await test.step('lands on /student after OTP success and tokens are stored', async () => {
      await page.waitForURL(/\/student(\/|$)/, { timeout: 15_000 });
      const access = await page.evaluate(
        (k) => window.localStorage.getItem(k),
        STORAGE_KEYS.access,
      );
      const refresh = await page.evaluate(
        (k) => window.localStorage.getItem(k),
        STORAGE_KEYS.refresh,
      );
      expect(access, 'access token must be persisted to localStorage').toBeTruthy();
      expect(refresh, 'refresh token must be persisted to localStorage').toBeTruthy();
    });

    await test.step('logout, then log back in via the form', async () => {
      // Clear tokens to simulate logout (UI offers a menu but its labels vary
      // by locale; tearing down storage is the deterministic equivalent).
      await page.evaluate(([a, r]) => {
        window.localStorage.removeItem(a);
        window.localStorage.removeItem(r);
      }, [STORAGE_KEYS.access, STORAGE_KEYS.refresh] as const);

      await page.goto('/login');
      await page.getByTestId('login-phone-or-email').fill(phone);
      await page.getByTestId('login-password').fill(password);
      await page.getByTestId('login-submit').click();
      await page.waitForURL(/\/student(\/|$)|\/home$/, { timeout: 15_000 });
    });
  });
});

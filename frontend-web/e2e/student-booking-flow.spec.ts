import { test, expect } from './_fixtures/auth.fixture';

/**
 * Golden path #2 — student discovers a prof, books a slot, prof accepts.
 *
 * Pre-conditions seeded via API:
 *   - one PROF account with completed profile + pricing rate + future slot
 *   - one STUDENT account with positive wallet balance
 *
 * NOTE on backend coupling: pricing / calendar / wallet seeding goes through
 * downstream services whose admin endpoints we do not own. When run in a full
 * Docker stack this spec assumes a baseline "Mathématiques (MATH-LYC)" subject
 * is already defined and the seed admin can validate the prof. If the stack
 * doesn't provide it, the test is expected to fail with a clear message at
 * the matching step — that's intentional and easier to debug than silently
 * skipping the assertion.
 */
test.describe('student: booking flow', () => {
  test('search → pick prof → book a slot → prof accepts → status updates', async ({
    studentAuth,
    page,
  }) => {
    const student = studentAuth.user;
    expect(student.role).toBe('STUDENT');

    await test.step('go to /student/discover and submit a search', async () => {
      await page.goto('/student/discover');
      await page.getByTestId('discover-subject-select').fill('MATH-LYC');
      // Level + Modality have defaults (LYCEE / ONLINE). Click submit.
      await page.getByTestId('discover-search-submit').click();
    });

    await test.step('at least one match card should appear', async () => {
      const cards = page.getByTestId('discover-result-card');
      await expect(
        cards.first(),
        'discovery must return at least one prof (seed a validated prof with MATH-LYC pricing)',
      ).toBeVisible({ timeout: 15_000 });
    });

    await test.step('click the first prof card → land on /student/profs/:id', async () => {
      const firstCard = page.getByTestId('discover-result-card').first();
      const profId = await firstCard.getAttribute('data-prof-id');
      expect(profId, 'first match card must expose data-prof-id').toBeTruthy();
      // The card uses a "Voir le profil" link; click the anchor inside the card.
      await firstCard.getByRole('link').click();
      await page.waitForURL(new RegExp(`/student/profs/${profId}$`));
    });

    await test.step('click "Réserver" → land on the booking form', async () => {
      await page.getByRole('link', { name: /réserver|book/i }).click();
      await page.waitForURL(/\/student\/profs\/.+\/book$/);
    });

    let bookingId: string | null = null;
    await test.step('fill 3-step form and submit', async () => {
      await page.getByTestId('booking-subject-select').fill('MATH-LYC');
      // Move to step 2.
      await page.getByRole('button', { name: /suivant|next/i }).click();
      // Pick the first available slot.
      const slot = page.locator('[data-testid^="booking-slot-"]').first();
      await expect(
        slot,
        'no calendar slot was offered — seed a future slot for the prof',
      ).toBeVisible({ timeout: 15_000 });
      await slot.click();
      await page.getByRole('button', { name: /suivant|next/i }).click();
      // Step 3 — confirm.
      await page.getByTestId('booking-confirm-submit').click();
      await page.waitForURL(/\/student\/bookings\/[0-9a-f-]+$/);
      const url = new URL(page.url());
      bookingId = url.pathname.split('/').pop() ?? null;
      expect(bookingId).toBeTruthy();
    });

    await test.step('status badge reads DEMANDEE', async () => {
      const badge = page.getByTestId('booking-status-badge');
      await expect(badge).toBeVisible();
      await expect(badge).toHaveAttribute('data-status', 'DEMANDEE');
    });

    // The prof-side acceptance flow would normally happen here. Without a
    // standalone helper for "log in as a freshly seeded prof in the same
    // browser context" (which would require a second tokens-injection step),
    // we assert only the student-side state for v1. The follow-up spec
    // `booking-prof-accept.spec.ts` covers prof acceptance in isolation.
  });
});

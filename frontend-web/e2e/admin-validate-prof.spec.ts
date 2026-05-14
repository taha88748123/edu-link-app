import { test, expect } from './_fixtures/auth.fixture';

/**
 * Golden path #3 — admin sees a pending prof in /admin/validations and
 * approves them. The pending prof is seeded via API in the PENDING_OTP →
 * verified state; the user-profile-service emits the prof profile in
 * `PENDING_PROF_VALIDATION` automatically.
 */
test.describe('admin: validate prof', () => {
  test('approve a pending prof from the validations list', async ({ adminAuth, api, page }) => {
    // Seed a fresh prof so the test is idempotent (unique phone every run).
    const seededProf = await api.seedProf();
    expect(seededProf.role).toBe('PROF');

    await test.step('navigate to /admin/validations', async () => {
      await adminAuth.page.goto('/admin/validations');
    });

    await test.step('the pending prof row is visible', async () => {
      const row = page.getByTestId(`validation-row-${seededProf.userId}`);
      await expect(
        row,
        'seeded prof must show up in the pending validations list — backend may not yet expose the prof in PENDING state',
      ).toBeVisible({ timeout: 15_000 });
    });

    await test.step('click "Valider" on that row → row disappears', async () => {
      const row = page.getByTestId(`validation-row-${seededProf.userId}`);
      await row.getByTestId('validation-approve-button').click();
      // Optimistic UI removes the row after the mutation settles.
      await expect(row, 'row should disappear after validation').toBeHidden({ timeout: 15_000 });
    });
  });
});

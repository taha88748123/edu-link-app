import { test, expect } from './_fixtures/auth.fixture';

/**
 * Golden path #5 — money flow.
 *
 *   1. Student submits a 200 MAD top-up request (CASHPLUS with proof URL).
 *   2. Admin approves it from /admin/topups.
 *   3. Student refreshes /student/wallet; balance is +200 MAD (20000 minor).
 */
test.describe('admin: topup approve', () => {
  test('student submits a topup, admin approves, student balance increases by 200 MAD', async ({
    studentAuth,
    adminAuth,
    page,
  }) => {
    const student = studentAuth.user;

    // Capture starting balance from /student/wallet so we can diff later.
    let initialMinor = 0;
    await test.step('student: open wallet, read starting balance', async () => {
      await page.goto('/student/wallet');
      // The big balance card uses formatMoneyMad — we read the live DOM rather
      // than re-parsing the locale-aware string, so we use the API instead.
      const resp = await page.request.get('http://localhost:8080/api/v1/payment/me/balance', {
        headers: { Authorization: `Bearer ${student.accessToken}` },
      });
      if (resp.ok()) {
        const body = await resp.json();
        initialMinor = body?.data?.balanceMinor ?? 0;
      }
    });

    await test.step('student: submit a 200 MAD top-up via the dialog', async () => {
      await page.getByRole('button', { name: /recharge|top.?up|إيداع/i }).first().click();
      // Dialog is now open — fill amount + proof. Method default CASHPLUS.
      await page.getByLabel(/montant|amount|مبلغ/i).fill('200');
      await page.getByLabel(/justificatif|proof|إثبات/i).fill('https://example.com/proof.jpg');
      await page.getByRole('button', { name: /soumettre|submit|envoyer/i }).click();
    });

    let topupId: string | null = null;
    await test.step('admin: locate the new pending topup row', async () => {
      await adminAuth.page.goto('/admin/topups');
      // The latest topup belongs to our student. Find it by walking the
      // list — we don't have its id client-side, so we just take the first
      // visible row (newest first by createdAt) that points at our student.
      const candidate = adminAuth.page.locator('[data-testid^="topup-row-"]').first();
      await expect(
        candidate,
        'no pending topup row visible — payment-service may not have ingested the request',
      ).toBeVisible({ timeout: 15_000 });
      const testId = await candidate.getAttribute('data-testid');
      topupId = testId?.replace('topup-row-', '') ?? null;
      expect(topupId).toBeTruthy();
    });

    await test.step('admin: approve the topup', async () => {
      const row = adminAuth.page.getByTestId(`topup-row-${topupId}`);
      await row.getByTestId('topup-approve-button').click();
      // Confirmation dialog → submit.
      await adminAuth.page.getByRole('button', { name: /confirmer|confirm/i }).click();
      await expect(row).toBeHidden({ timeout: 15_000 });
    });

    await test.step('student: balance increased by 200_00 minor', async () => {
      const resp = await page.request.get('http://localhost:8080/api/v1/payment/me/balance', {
        headers: { Authorization: `Bearer ${student.accessToken}` },
      });
      expect(resp.ok(), 'wallet balance endpoint must return 200').toBe(true);
      const body = await resp.json();
      const after: number = body?.data?.balanceMinor ?? 0;
      expect(
        after - initialMinor,
        'balance must have grown by exactly 200 MAD (20000 minor units)',
      ).toBe(20000);
    });
  });
});

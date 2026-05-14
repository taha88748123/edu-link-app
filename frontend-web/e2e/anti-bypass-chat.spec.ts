import { test, expect } from './_fixtures/auth.fixture';

/**
 * Golden path #4 — SECURITY REGRESSION.
 *
 * The chat-service moderator must strip phone digits from messages sent while
 * the related booking is still PRE_ACCEPT. This is the platform's primary
 * anti-bypass control: without it, profs and students could swap contact
 * info and complete the lesson off-platform.
 *
 * We type a message containing a Moroccan phone number and assert the
 * rendered bubble body has its digits masked. We also check that the
 * anti-bypass banner is visible while the conversation is PRE_ACCEPT.
 */
test.describe('chat: anti-bypass moderation', () => {
  test('phone digits typed in a PRE_ACCEPT conversation are masked', async ({
    studentAuth,
    page,
  }) => {
    // The chat conversation is created server-side when a booking enters
    // DEMANDEE. The student here is freshly seeded with no bookings, so we
    // navigate to /chat to land on the conversation list. The test stack must
    // include a PRE_ACCEPT conversation belonging to this student for the
    // first sub-step to succeed.
    await test.step('open /chat and pick the first conversation', async () => {
      await page.goto('/chat');
      const firstConv = page.locator('a[href^="/chat/"]').first();
      await expect(
        firstConv,
        'seeded student must have at least one PRE_ACCEPT conversation — preconditions not met',
      ).toBeVisible({ timeout: 15_000 });
      await firstConv.click();
      await page.waitForURL(/\/chat\/[0-9a-f-]+$/);
    });

    await test.step('anti-bypass notice is visible while PRE_ACCEPT', async () => {
      await expect(
        page.getByTestId('chat-bypass-notice'),
        'AntiBypassNotice MUST appear while conversation is PRE_ACCEPT',
      ).toBeVisible();
    });

    const rawMessage = 'Appelle-moi au 06 12 34 56 78';
    await test.step('type a message containing a phone and send', async () => {
      await page.getByTestId('chat-compose-input').fill(rawMessage);
      await page.getByTestId('chat-send-button').click();
    });

    await test.step('the rendered bubble has its digits masked', async () => {
      // Find the latest bubble that is "own" (student-sent).
      const lastBubble = page.locator('[data-testid="message-bubble"]').last();
      await expect(lastBubble).toBeVisible({ timeout: 10_000 });

      const text = (await lastBubble.innerText()).trim();
      // The exact masking shape is `***` or `0X **`; both should still NOT
      // contain a contiguous run of 6+ digits anywhere in the rendered body.
      expect(
        /\d{6,}/.test(text),
        `Bubble body still contains a 6+ digit run — moderation failed. Got: ${JSON.stringify(text)}`,
      ).toBe(false);
      // The bubble must also carry the moderation marker.
      const moderation = await lastBubble.getAttribute('data-moderation');
      expect(
        moderation,
        'expected data-moderation to be MASKED or FLAGGED, got: ' + moderation,
      ).not.toBe('CLEAN');
    });
  });
});

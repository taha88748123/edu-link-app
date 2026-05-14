import { describe, expect, it } from 'vitest';
import { render } from '@testing-library/react';
import { I18nextProvider, initReactI18next } from 'react-i18next';
import { createInstance } from 'i18next';

import type { MessageView } from '@/api/chat.api';
import fr from '@/i18n/locales/fr.json';
import { MessageBubble } from './MessageBubble';

const testI18n = createInstance();
void testI18n.use(initReactI18next).init({
  lng: 'fr',
  fallbackLng: 'fr',
  resources: { fr: { translation: fr } },
  interpolation: { escapeValue: false },
});

function makeMessage(overrides: Partial<MessageView> = {}): MessageView {
  return {
    id: 'm-1',
    conversationId: 'c-1',
    senderId: 's-1',
    senderRole: 'STUDENT',
    type: 'TEXT',
    body: 'bonjour',
    attachments: null,
    moderation: { status: 'CLEAN', flags: null },
    createdAt: '2024-09-01T10:00:00Z',
    ...overrides,
  };
}

describe('MessageBubble', () => {
  it('does NOT render the moderation hint for CLEAN messages', () => {
    const { container } = render(
      <I18nextProvider i18n={testI18n}>
        <MessageBubble
          message={makeMessage({ moderation: { status: 'CLEAN', flags: null } })}
          isOwn={false}
          locale="fr"
        />
      </I18nextProvider>,
    );
    const bubble = container.querySelector('[data-testid="message-bubble"]');
    expect(bubble?.getAttribute('data-moderation')).toBe('CLEAN');
    expect(container.querySelectorAll('[aria-label="Modération automatique appliquée"]')).toHaveLength(0);
  });

  it('renders the MASKED moderation hint with tooltip aria-label', () => {
    const { container } = render(
      <I18nextProvider i18n={testI18n}>
        <MessageBubble
          message={makeMessage({
            body: '06 ** ** ** **',
            moderation: { status: 'MASKED', flags: ['phone'] },
          })}
          isOwn
          locale="fr"
        />
      </I18nextProvider>,
    );
    const bubble = container.querySelector('[data-testid="message-bubble"]');
    expect(bubble?.getAttribute('data-moderation')).toBe('MASKED');
    expect(
      container.querySelector('[aria-label="Modération automatique appliquée"]'),
    ).toBeTruthy();
  });

  it('renders the FLAGGED moderation hint too', () => {
    const { container } = render(
      <I18nextProvider i18n={testI18n}>
        <MessageBubble
          message={makeMessage({
            moderation: { status: 'FLAGGED', flags: ['bypass'] },
          })}
          isOwn={false}
          locale="fr"
        />
      </I18nextProvider>,
    );
    expect(
      container.querySelector('[aria-label="Modération automatique appliquée"]'),
    ).toBeTruthy();
  });

  it('aligns own messages on the end side', () => {
    const { container } = render(
      <I18nextProvider i18n={testI18n}>
        <MessageBubble message={makeMessage()} isOwn locale="fr" />
      </I18nextProvider>,
    );
    const bubble = container.querySelector('[data-testid="message-bubble"]');
    expect(bubble?.className).toContain('justify-end');
  });
});

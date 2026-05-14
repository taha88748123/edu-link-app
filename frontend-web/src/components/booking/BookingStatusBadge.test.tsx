import { describe, expect, it } from 'vitest';
import { render } from '@testing-library/react';
import { I18nextProvider, initReactI18next } from 'react-i18next';
import { createInstance } from 'i18next';

import { BookingStatusBadge, STATUS_VARIANT } from './BookingStatusBadge';
import type { BookingStatus } from '@/api/enums';
import fr from '@/i18n/locales/fr.json';

// Isolated i18n instance — avoids interfering with the shared singleton used
// elsewhere in the test suite (`@/i18n/config`).
const testI18n = createInstance();
testI18n.use(initReactI18next).init({
  lng: 'fr',
  fallbackLng: 'fr',
  resources: { fr: { translation: fr } },
  interpolation: { escapeValue: false },
});

describe('STATUS_VARIANT', () => {
  it('maps every BookingStatus to a non-empty variant', () => {
    const statuses: BookingStatus[] = [
      'DEMANDEE',
      'ACCEPTEE_PAR_PROF',
      'EN_COURS',
      'TERMINEE',
      'CONFIRMEE',
      'ANNULEE',
      'LITIGE',
    ];
    for (const s of statuses) {
      expect(STATUS_VARIANT[s]).toBeTruthy();
    }
  });

  it('uses success for CONFIRMEE, danger for LITIGE, warning for DEMANDEE', () => {
    expect(STATUS_VARIANT.CONFIRMEE).toBe('success');
    expect(STATUS_VARIANT.LITIGE).toBe('danger');
    expect(STATUS_VARIANT.DEMANDEE).toBe('warning');
  });
});

describe('BookingStatusBadge', () => {
  it('renders the localized label for a status', () => {
    const { getByText } = render(
      <I18nextProvider i18n={testI18n}>
        <BookingStatusBadge status="DEMANDEE" />
      </I18nextProvider>,
    );
    expect(getByText('Demandée')).toBeInTheDocument();
  });

  it('renders the localized label for CONFIRMEE', () => {
    const { getByText } = render(
      <I18nextProvider i18n={testI18n}>
        <BookingStatusBadge status="CONFIRMEE" />
      </I18nextProvider>,
    );
    expect(getByText('Confirmée')).toBeInTheDocument();
  });
});

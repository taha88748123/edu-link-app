import { describe, expect, it } from 'vitest';

import {
  allowedActions,
  bookingTab,
  isContactVisible,
  isStartWithinWindow,
} from './booking';

describe('isContactVisible (anti-bypass)', () => {
  it('hides contacts while a booking is just DEMANDEE', () => {
    expect(isContactVisible('DEMANDEE')).toBe(false);
  });

  it('hides contacts for cancelled bookings (never accepted)', () => {
    expect(isContactVisible('ANNULEE')).toBe(false);
  });

  it.each(['ACCEPTEE_PAR_PROF', 'EN_COURS', 'TERMINEE', 'CONFIRMEE', 'LITIGE'] as const)(
    'allows contacts once status reaches %s',
    (status) => {
      expect(isContactVisible(status)).toBe(true);
    },
  );

  it('treats null / undefined as hidden', () => {
    expect(isContactVisible(null)).toBe(false);
    expect(isContactVisible(undefined)).toBe(false);
  });
});

describe('bookingTab', () => {
  it('groups in-flight statuses under "upcoming"', () => {
    expect(bookingTab('DEMANDEE')).toBe('upcoming');
    expect(bookingTab('ACCEPTEE_PAR_PROF')).toBe('upcoming');
    expect(bookingTab('EN_COURS')).toBe('upcoming');
  });

  it('groups completed statuses under "past"', () => {
    expect(bookingTab('TERMINEE')).toBe('past');
    expect(bookingTab('CONFIRMEE')).toBe('past');
  });

  it('groups cancelled / disputed under "cancelled"', () => {
    expect(bookingTab('ANNULEE')).toBe('cancelled');
    expect(bookingTab('LITIGE')).toBe('cancelled');
  });
});

describe('allowedActions', () => {
  it('STUDENT can cancel a DEMANDEE or ACCEPTEE booking', () => {
    expect(allowedActions('DEMANDEE', 'STUDENT')).toEqual(['cancel']);
    expect(allowedActions('ACCEPTEE_PAR_PROF', 'STUDENT')).toEqual(['cancel']);
  });

  it('STUDENT can confirm or dispute a TERMINEE booking', () => {
    expect(allowedActions('TERMINEE', 'STUDENT').sort()).toEqual(['confirm', 'dispute']);
  });

  it('PROF can accept or decline a DEMANDEE booking', () => {
    expect(allowedActions('DEMANDEE', 'PROF').sort()).toEqual(['accept', 'decline']);
  });

  it('PROF can start or cancel an ACCEPTEE booking', () => {
    expect(allowedActions('ACCEPTEE_PAR_PROF', 'PROF').sort()).toEqual(['cancel', 'start']);
  });

  it('returns no actions for CONFIRMEE / ANNULEE bookings', () => {
    expect(allowedActions('CONFIRMEE', 'STUDENT')).toEqual([]);
    expect(allowedActions('ANNULEE', 'PROF')).toEqual([]);
  });
});

describe('isStartWithinWindow', () => {
  it('returns true when scheduledStart is in the past', () => {
    const past = new Date(Date.now() - 60_000);
    expect(isStartWithinWindow(past)).toBe(true);
  });

  it('returns true when scheduledStart is within 15 min', () => {
    const soon = new Date(Date.now() + 10 * 60 * 1000);
    expect(isStartWithinWindow(soon)).toBe(true);
  });

  it('returns false when scheduledStart is more than 15 min away', () => {
    const later = new Date(Date.now() + 30 * 60 * 1000);
    expect(isStartWithinWindow(later)).toBe(false);
  });

  it('returns false for an invalid date string', () => {
    expect(isStartWithinWindow('not-a-date')).toBe(false);
  });
});

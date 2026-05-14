import type { BookingStatus } from '@/api/enums';

/**
 * Statuses after which contact information (phone / email / display name)
 * may be exposed in booking-related views. Anti-bypass invariant: before
 * the prof has accepted, the platform must NOT leak any contact info.
 */
export const CONTACT_VISIBLE_STATUSES: BookingStatus[] = [
  'ACCEPTEE_PAR_PROF',
  'EN_COURS',
  'TERMINEE',
  'CONFIRMEE',
  'LITIGE',
];

/** Anti-bypass guard for any UI component about to render contact info. */
export function isContactVisible(status: BookingStatus | null | undefined): boolean {
  if (!status) return false;
  return CONTACT_VISIBLE_STATUSES.includes(status);
}

/** Logical tab buckets used by the bookings list. */
export type BookingTab = 'upcoming' | 'past' | 'cancelled';

export function bookingTab(status: BookingStatus): BookingTab {
  switch (status) {
    case 'DEMANDEE':
    case 'ACCEPTEE_PAR_PROF':
    case 'EN_COURS':
      return 'upcoming';
    case 'TERMINEE':
    case 'CONFIRMEE':
      return 'past';
    case 'ANNULEE':
    case 'LITIGE':
    default:
      return 'cancelled';
  }
}

/** Caller-role-aware list of actions allowed on a booking given its status. */
export type BookingAction = 'cancel' | 'confirm' | 'dispute' | 'accept' | 'decline' | 'start';
export type CallerRole = 'STUDENT' | 'PROF';

/**
 * Compute the list of actions a caller may take on a booking in this status.
 *
 * Pure / synchronous — used both by the UI to decide which buttons to render
 * and by the unit test `src/lib/booking.test.ts` to lock down the matrix.
 *
 * NB: `start` is gated by additional time-window logic (scheduled_start within
 * the next 15 minutes); the helper exposes it as soon as the status allows
 * but the UI MUST re-check `isStartWithinWindow()` before enabling the button.
 */
export function allowedActions(status: BookingStatus, role: CallerRole): BookingAction[] {
  if (role === 'STUDENT') {
    switch (status) {
      case 'DEMANDEE':
        return ['cancel'];
      case 'ACCEPTEE_PAR_PROF':
        return ['cancel'];
      case 'TERMINEE':
        return ['confirm', 'dispute'];
      default:
        return [];
    }
  }
  // PROF
  switch (status) {
    case 'DEMANDEE':
      return ['accept', 'decline'];
    case 'ACCEPTEE_PAR_PROF':
      return ['start', 'cancel'];
    case 'TERMINEE':
      return ['dispute'];
    default:
      return [];
  }
}

const START_WINDOW_MS = 15 * 60 * 1000;

/** Returns true when scheduledStart is within the next 15 minutes (or already started). */
export function isStartWithinWindow(scheduledStart: string | Date, now: Date = new Date()): boolean {
  const start = typeof scheduledStart === 'string' ? new Date(scheduledStart) : scheduledStart;
  if (Number.isNaN(start.getTime())) return false;
  return start.getTime() - now.getTime() <= START_WINDOW_MS;
}

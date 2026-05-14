/**
 * TypeScript mirrors of the shared backend enums living in
 * `backend/shared-libs/shared-dto/src/main/java/ma/naja7i/shared/dto/*`.
 *
 * Keep these in sync — they're used by the booking / calendar / pricing /
 * payment / matching domain modules.
 */

export type Modality = 'ONLINE' | 'DOMICILE' | 'LOCAL';
export const MODALITIES: Modality[] = ['ONLINE', 'DOMICILE', 'LOCAL'];

export type SchoolLevel = 'PRIMAIRE' | 'COLLEGE' | 'LYCEE' | 'SUPERIEUR';
export const SCHOOL_LEVELS: SchoolLevel[] = ['PRIMAIRE', 'COLLEGE', 'LYCEE', 'SUPERIEUR'];

export type BookingStatus =
  | 'DEMANDEE'
  | 'ACCEPTEE_PAR_PROF'
  | 'EN_COURS'
  | 'TERMINEE'
  | 'CONFIRMEE'
  | 'ANNULEE'
  | 'LITIGE';

export const BOOKING_STATUSES: BookingStatus[] = [
  'DEMANDEE',
  'ACCEPTEE_PAR_PROF',
  'EN_COURS',
  'TERMINEE',
  'CONFIRMEE',
  'ANNULEE',
  'LITIGE',
];

/** Calendar slot status (matches `calendar-service` domain). */
export type SlotStatus = 'AVAILABLE' | 'RESERVED_PENDING' | 'BOOKED' | 'BLOCKED';

/** Availability rule recurrence. */
export type Recurrence = 'WEEKLY' | 'ONE_OFF';

/** Top-up methods accepted by payment-service. */
export type TopupMethod = 'CASHPLUS' | 'BANK_TRANSFER' | 'WHATSAPP' | 'OTHER';
export const TOPUP_METHODS: TopupMethod[] = ['CASHPLUS', 'BANK_TRANSFER', 'WHATSAPP', 'OTHER'];

export type TopupStatus = 'EN_ATTENTE_ADMIN' | 'APPROVED' | 'MODIFIED' | 'REJECTED';

export type PayoutStatus = 'REQUESTED' | 'APPROVED' | 'PAID' | 'REJECTED';

/** Prof validation status (used in dashboards). */
export type ProfStatus =
  | 'EN_ATTENTE'
  | 'EN_TEST_IA'
  | 'EN_ENTRETIEN_ADMIN'
  | 'VALIDE'
  | 'SUSPENDU'
  | 'REJETE';

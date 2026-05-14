import { apiClient, unwrap } from './client';
import type { BookingStatus, Modality, SchoolLevel } from './enums';

const BASE = '/api/v1/bookings';

// ---------- DTO mirrors (booking-service/.../web/dto) ----------

export interface BookingView {
  id: string;
  studentId: string;
  profId: string;
  slotId: string | null;
  subjectCode: string;
  level: SchoolLevel;
  modality: Modality;
  durationMinutes: number;
  totalAmountMinor: number;
  currency: string;
  status: BookingStatus;
  scheduledStart: string;
  scheduledEnd: string;
  createdAt: string;
}

export interface BookingDetailedView {
  id: string;
  studentId: string;
  profId: string;
  slotId: string | null;
  subjectCode: string;
  level: SchoolLevel;
  modality: Modality;
  durationMinutes: number;
  amountMinor: number;
  platformFeeMinor: number;
  totalAmountMinor: number;
  currency: string;
  escrowId: string | null;
  status: BookingStatus;
  scheduledStart: string;
  scheduledEnd: string;
  actualStart: string | null;
  actualEnd: string | null;
  cancelReason: string | null;
  cancelledBy: string | null;
  cancelledAt: string | null;
  confirmedAt: string | null;
  declineReason: string | null;
  declinedAt: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface DisputeView {
  id: string;
  bookingId: string;
  openedBy: string;
  cause: string;
  evidenceUrls: string | null;
  status: string;
  resolutionNotes: string | null;
  resolvedBy: string | null;
  resolvedAt: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface CreateBookingRequest {
  profId: string;
  slotId: string;
  subjectCode: string;
  level: SchoolLevel;
  modality: Modality;
  durationMinutes: number;
  packId?: string | null;
}

export interface CancelRequest {
  reason?: string;
}

export interface DeclineRequest {
  reason?: string;
}

export interface DisputeRequest {
  cause: string;
  evidenceUrls?: string;
}

export interface ListMyBookingsParams {
  status?: BookingStatus;
  page?: number;
  size?: number;
}

// ---------- API ----------

export const bookingsApi = {
  listMyBookings(params: ListMyBookingsParams = {}): Promise<BookingView[]> {
    return unwrap<BookingView[]>(
      apiClient.get(`${BASE}/me`, {
        params: {
          status: params.status,
          page: params.page ?? 0,
          size: params.size ?? 20,
        },
      }),
    );
  },

  getBooking(id: string): Promise<BookingDetailedView> {
    return unwrap<BookingDetailedView>(apiClient.get(`${BASE}/${id}`));
  },

  createBooking(body: CreateBookingRequest): Promise<BookingDetailedView> {
    return unwrap<BookingDetailedView>(apiClient.post(BASE, body));
  },

  cancelBooking(id: string, body: CancelRequest = {}): Promise<BookingDetailedView> {
    return unwrap<BookingDetailedView>(apiClient.post(`${BASE}/${id}:cancel`, body));
  },

  confirmBooking(id: string): Promise<BookingDetailedView> {
    return unwrap<BookingDetailedView>(apiClient.post(`${BASE}/${id}:confirm`));
  },

  disputeBooking(id: string, body: DisputeRequest): Promise<DisputeView> {
    return unwrap<DisputeView>(apiClient.post(`${BASE}/${id}:dispute`, body));
  },

  acceptBooking(id: string): Promise<BookingDetailedView> {
    return unwrap<BookingDetailedView>(apiClient.post(`${BASE}/${id}:accept`));
  },

  declineBooking(id: string, body: DeclineRequest = {}): Promise<BookingDetailedView> {
    return unwrap<BookingDetailedView>(apiClient.post(`${BASE}/${id}:decline`, body));
  },

  startBooking(id: string): Promise<BookingDetailedView> {
    return unwrap<BookingDetailedView>(apiClient.post(`${BASE}/${id}:start`));
  },
};

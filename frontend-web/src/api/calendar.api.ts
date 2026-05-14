import { apiClient, unwrap } from './client';
import type { Modality, Recurrence, SlotStatus } from './enums';

const BASE = '/api/v1/calendar';

// ---------- DTOs ----------

export interface SlotView {
  id: string;
  profId: string;
  startAt: string;
  endAt: string;
  modality: Modality;
  status: SlotStatus;
  bookingId: string | null;
  reservedUntil: string | null;
}

export interface AvailabilityRuleView {
  id: string;
  profId: string;
  recurrence: Recurrence;
  /** 0=Mon..6=Sun (WEEKLY only). */
  dayOfWeek: number | null;
  /** ISO local date (ONE_OFF only). */
  date: string | null;
  /** "HH:mm:ss" or "HH:mm". */
  startTime: string;
  endTime: string;
  timezone: string | null;
  active: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface CreateAvailabilityRuleRequest {
  recurrence: Recurrence;
  dayOfWeek?: number | null;
  date?: string | null;
  startTime: string;
  endTime: string;
  timezone?: string;
}

export interface UpdateAvailabilityRuleRequest {
  recurrence?: Recurrence;
  dayOfWeek?: number | null;
  date?: string | null;
  startTime?: string;
  endTime?: string;
  timezone?: string;
  active?: boolean;
}

export interface GenerateSlotsResponse {
  profId: string;
  created: number;
  skipped: number;
}

export interface ListProfSlotsParams {
  profId: string;
  from: string;
  to: string;
  modality?: Modality;
  page?: number;
  size?: number;
}

export interface ListMySlotsParams {
  from: string;
  to: string;
  status?: SlotStatus;
  page?: number;
  size?: number;
}

// ---------- API ----------

export const calendarApi = {
  listMyRules(): Promise<AvailabilityRuleView[]> {
    return unwrap<AvailabilityRuleView[]>(apiClient.get(`${BASE}/availability-rules/me`));
  },

  createRule(body: CreateAvailabilityRuleRequest): Promise<AvailabilityRuleView> {
    return unwrap<AvailabilityRuleView>(apiClient.post(`${BASE}/availability-rules`, body));
  },

  updateRule(id: string, body: UpdateAvailabilityRuleRequest): Promise<AvailabilityRuleView> {
    return unwrap<AvailabilityRuleView>(apiClient.put(`${BASE}/availability-rules/${id}`, body));
  },

  deleteRule(id: string): Promise<void> {
    return unwrap<void>(apiClient.delete(`${BASE}/availability-rules/${id}`));
  },

  generateSlots(): Promise<GenerateSlotsResponse> {
    return unwrap<GenerateSlotsResponse>(apiClient.post(`${BASE}/slots:generate`));
  },

  listMySlots(params: ListMySlotsParams): Promise<SlotView[]> {
    return unwrap<SlotView[]>(
      apiClient.get(`${BASE}/slots/me`, {
        params: {
          from: params.from,
          to: params.to,
          status: params.status,
          page: params.page ?? 0,
          size: params.size ?? 200,
        },
      }),
    );
  },

  listProfSlots(params: ListProfSlotsParams): Promise<SlotView[]> {
    return unwrap<SlotView[]>(
      apiClient.get(`${BASE}/profs/${params.profId}/slots`, {
        params: {
          from: params.from,
          to: params.to,
          modality: params.modality,
          page: params.page ?? 0,
          size: params.size ?? 100,
        },
      }),
    );
  },

  blockSlot(id: string): Promise<SlotView> {
    return unwrap<SlotView>(apiClient.post(`${BASE}/slots/${id}:block`));
  },

  unblockSlot(id: string): Promise<SlotView> {
    return unwrap<SlotView>(apiClient.post(`${BASE}/slots/${id}:unblock`));
  },
};

import { apiClient, unwrap } from './client';
import type { Modality, SchoolLevel } from './enums';

const BASE = '/api/v1/pricing';

// ---------- DTOs ----------

export interface RateView {
  id: string;
  profId: string;
  subjectCode: string;
  level: SchoolLevel;
  modality: Modality;
  durationMinutes: number;
  dayOfWeek: number | null;
  amountMinor: number;
  amountMad: string;
  currency: string;
  active: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface PackView {
  id: string;
  profId: string;
  subjectCode: string;
  level: SchoolLevel;
  modality: Modality;
  sessionsCount: number;
  discountPct: string | number;
  totalAmountMinor: number;
  totalAmountMad: string;
  currency: string;
  active: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface MyPricingView {
  rates: RateView[];
  packs: PackView[];
}

export interface CreateRateRequest {
  subjectCode: string;
  level: SchoolLevel;
  modality: Modality;
  durationMinutes: number;
  dayOfWeek?: number | null;
  amountMad: string;
}

export interface UpdateRateRequest extends CreateRateRequest {
  active?: boolean;
}

export interface CreatePackRequest {
  subjectCode: string;
  level: SchoolLevel;
  modality: Modality;
  sessionsCount: number;
  /** Decimal 0..100 (sent as string by Jackson, accepted as number by backend). */
  discountPct: string | number;
  totalAmountMad: string;
}

export interface UpdatePackRequest extends CreatePackRequest {
  active?: boolean;
}

export interface QuoteRequest {
  profId: string;
  subjectCode: string;
  level: SchoolLevel;
  modality: Modality;
  durationMinutes: number;
  dayOfWeek?: number | null;
  packId?: string | null;
}

export interface QuoteBreakdown {
  source: string;
  rateId: string | null;
  packId: string | null;
  sessionsCount: number | null;
  platformFeePct: string | null;
  platformFeeFixedMinor: number;
}

export interface QuoteResponse {
  baseAmountMinor: number;
  baseAmountMad: string;
  platformFeeMinor: number;
  platformFeeMad: string;
  totalAmountMinor: number;
  totalAmountMad: string;
  currency: string;
  breakdown: QuoteBreakdown;
}

export interface ListProfRatesParams {
  profId: string;
  subjectCode?: string;
  level?: SchoolLevel;
  modality?: Modality;
}

// ---------- API ----------

export const pricingApi = {
  getMyPricing(): Promise<MyPricingView> {
    return unwrap<MyPricingView>(apiClient.get(`${BASE}/me`));
  },

  createRate(body: CreateRateRequest): Promise<RateView> {
    return unwrap<RateView>(apiClient.post(`${BASE}/rates`, body));
  },

  updateRate(id: string, body: UpdateRateRequest): Promise<RateView> {
    return unwrap<RateView>(apiClient.put(`${BASE}/rates/${id}`, body));
  },

  deleteRate(id: string): Promise<void> {
    return unwrap<void>(apiClient.delete(`${BASE}/rates/${id}`));
  },

  createPack(body: CreatePackRequest): Promise<PackView> {
    return unwrap<PackView>(apiClient.post(`${BASE}/packs`, body));
  },

  updatePack(id: string, body: UpdatePackRequest): Promise<PackView> {
    return unwrap<PackView>(apiClient.put(`${BASE}/packs/${id}`, body));
  },

  deletePack(id: string): Promise<void> {
    return unwrap<void>(apiClient.delete(`${BASE}/packs/${id}`));
  },

  quote(body: QuoteRequest): Promise<QuoteResponse> {
    return unwrap<QuoteResponse>(apiClient.post(`${BASE}/quote`, body));
  },

  listProfRates(params: ListProfRatesParams): Promise<RateView[]> {
    return unwrap<RateView[]>(
      apiClient.get(`${BASE}/profs/${params.profId}/rates`, {
        params: {
          subjectCode: params.subjectCode,
          level: params.level,
          modality: params.modality,
        },
      }),
    );
  },

  listProfPacks(params: ListProfRatesParams): Promise<PackView[]> {
    return unwrap<PackView[]>(
      apiClient.get(`${BASE}/profs/${params.profId}/packs`, {
        params: {
          subjectCode: params.subjectCode,
          level: params.level,
          modality: params.modality,
        },
      }),
    );
  },
};

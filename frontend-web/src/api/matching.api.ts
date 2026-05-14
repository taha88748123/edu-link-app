import { apiClient, unwrap } from './client';

const BASE = '/api/v1/ai';

export interface MatchingRequest {
  subjectCode: string;
  /** Sent as a free-form string per backend DTO (NotBlank). */
  level: string;
  /** Sent as a free-form string per backend DTO (NotBlank). */
  modality: string;
  /** Decimal MAD as string to be JSON-safe with BigDecimal on the backend. */
  budgetMaxMad?: string | null;
  city?: string | null;
  dayOfWeek?: number | null;
  durationMinutes?: number | null;
  limit?: number | null;
}

export interface MatchSuggestion {
  profId: string;
  score: number;
  rationale: string | null;
  basePriceMad: string | null;
  totalPriceMad: string | null;
}

export interface MatchingResponse {
  results: MatchSuggestion[];
  computedAt: string;
  agent: string;
  cacheHit: boolean;
  filterHash: string;
}

export const matchingApi = {
  getMatches(body: MatchingRequest): Promise<MatchingResponse> {
    return unwrap<MatchingResponse>(apiClient.post(`${BASE}/matching/profs`, body));
  },
};

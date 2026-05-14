import { apiClient, unwrap } from './client';
import type { PayoutStatus, TopupMethod, TopupStatus } from './enums';

const BASE = '/api/v1/payment';

// ---------- DTOs ----------

export interface BalanceView {
  balanceMad: string;
  balanceMinor: number;
  currency: string;
}

export interface AuditEntryView {
  id: number;
  userId: string;
  actorId: string | null;
  /** AuditOperation enum name (TOPUP, ESCROW_LOCK, ESCROW_RELEASE, ...). */
  operation: string;
  amountMinor: number;
  balanceBefore: number;
  balanceAfter: number;
  referenceType: string | null;
  referenceId: string | null;
  reason: string | null;
  traceId: string | null;
  createdAt: string;
}

export interface TopupView {
  id: string;
  studentId: string;
  method: TopupMethod;
  amountMinor: number;
  approvedAmountMinor: number | null;
  currency: string;
  proofUrl: string | null;
  status: TopupStatus;
  adminNotes: string | null;
  decidedBy: string | null;
  decidedAt: string | null;
  createdAt: string;
}

export interface PayoutView {
  id: string;
  profId: string;
  requestedAmountMinor: number;
  approvedAmountMinor: number | null;
  currency: string;
  status: PayoutStatus;
  ibanRef: string | null;
  paymentRef: string | null;
  adminNotes: string | null;
  decidedBy: string | null;
  requestedAt: string;
  decidedAt: string | null;
  paidAt: string | null;
}

export interface CreateTopupRequest {
  method: TopupMethod;
  /** Decimal MAD as string (matches backend BigDecimal). */
  amountMad: string;
  proofUrl?: string;
}

export interface CreatePayoutRequest {
  amountMad: string;
  ibanRef?: string;
}

export interface ListAuditParams {
  from?: string;
  to?: string;
  page?: number;
  size?: number;
}

export interface ListTopupsParams {
  status?: TopupStatus;
  page?: number;
  size?: number;
}

export interface ListPayoutsParams {
  status?: PayoutStatus;
  page?: number;
  size?: number;
}

// ---------- API ----------

export const paymentApi = {
  getMyBalance(): Promise<BalanceView> {
    return unwrap<BalanceView>(apiClient.get(`${BASE}/balance/me`));
  },

  getMyAuditLog(params: ListAuditParams = {}): Promise<AuditEntryView[]> {
    return unwrap<AuditEntryView[]>(
      apiClient.get(`${BASE}/audit/me`, {
        params: {
          from: params.from,
          to: params.to,
          page: params.page ?? 0,
          size: params.size ?? 20,
        },
      }),
    );
  },

  submitTopup(body: CreateTopupRequest): Promise<TopupView> {
    return unwrap<TopupView>(apiClient.post(`${BASE}/topups`, body));
  },

  listMyTopups(params: ListTopupsParams = {}): Promise<TopupView[]> {
    return unwrap<TopupView[]>(
      apiClient.get(`${BASE}/topups/me`, {
        params: {
          status: params.status,
          page: params.page ?? 0,
          size: params.size ?? 20,
        },
      }),
    );
  },

  requestPayout(body: CreatePayoutRequest): Promise<PayoutView> {
    return unwrap<PayoutView>(apiClient.post(`${BASE}/payouts`, body));
  },

  listMyPayouts(params: ListPayoutsParams = {}): Promise<PayoutView[]> {
    return unwrap<PayoutView[]>(
      apiClient.get(`${BASE}/payouts/me`, {
        params: {
          status: params.status,
          page: params.page ?? 0,
          size: params.size ?? 20,
        },
      }),
    );
  },
};

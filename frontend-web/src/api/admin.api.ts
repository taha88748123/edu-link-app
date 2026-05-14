import { apiClient, unwrap } from './client';
import type { ProfPrivateView } from './profile.api';
import type { BookingView } from './bookings.api';
import type {
  AuditEntryView,
  PayoutView,
  TopupView,
} from './payment.api';
import type { BookingStatus, PayoutStatus, TopupStatus } from './enums';

/**
 * Combined admin facade. Delegates to the admin endpoints across services so
 * `AdminDashboardPage` and the per-resource pages only depend on a single api.
 *
 * Endpoint matrix:
 *  - GET  /api/v1/admin/profs/pending
 *  - POST /api/v1/admin/profs/{id}/validate | /reject | /suspend
 *  - GET  /api/v1/payment/admin/topups          + :decide
 *  - GET  /api/v1/payment/admin/payouts         + :decide | :markPaid
 *  - GET  /api/v1/bookings/admin                + :resolve dispute
 *  - GET  /api/v1/payment/admin/audit/{userId}  + /export
 *  - POST /api/v1/payment/admin/balance/{userId}:adjust
 */

export interface PageMeta {
  page: number;
  size: number;
  total: number;
}

/**
 * Same shape as the backend `ApiResponse` envelope but the calling code wants
 * BOTH the body and meta.total. We use a custom unwrap-equivalent that pulls
 * data + meta from the raw axios response.
 */
async function pageFetch<T>(
  url: string,
  params: Record<string, unknown>,
): Promise<{ items: T[]; meta: PageMeta | null }> {
  const resp = await apiClient.get<{
    data?: T[] | null;
    meta?: PageMeta | null;
    error?: { code: string; message: string; messageKey?: string; traceId?: string } | null;
  }>(url, { params });
  if (resp.data?.error) {
    // Defer error wrapping to caller via unwrap-style throw; mimic ApiClientError.
    throw new Error(resp.data.error.messageKey || resp.data.error.message);
  }
  return {
    items: resp.data?.data ?? [],
    meta: resp.data?.meta ?? null,
  };
}

// ---------- Pending prof validations ----------

export interface ListProfsAdminParams {
  page?: number;
  size?: number;
}

export interface ProfDecisionRequest {
  notes?: string;
  reason?: string;
}

// ---------- Topup decide ----------

export interface TopupDecideRequest {
  decision: 'APPROVE' | 'MODIFY' | 'REJECT';
  approvedAmountMad?: string;
  notes?: string;
}

// ---------- Payout decide / markPaid ----------

export interface PayoutDecideRequest {
  decision: 'APPROVE' | 'REJECT';
  notes?: string;
}

export interface PayoutMarkPaidRequest {
  paymentRef: string;
}

// ---------- Dispute resolution ----------

export type DisputeDecision = 'REFUND' | 'PAY_PROF' | 'SPLIT';

export interface ResolveDisputeRequest {
  decision: DisputeDecision;
  profShareMad?: string;
  studentRefundMad?: string;
  notes?: string;
}

export interface ListBookingsAdminParams {
  status?: BookingStatus;
  from?: string;
  to?: string;
  page?: number;
  size?: number;
}

// ---------- Audit ----------

export interface ListAuditAdminParams {
  from?: string;
  to?: string;
  page?: number;
  size?: number;
}

// ---------- Balance adjustments ----------

export type BalanceAdjustOperation = 'ADMIN_CREDIT' | 'ADMIN_DEBIT' | 'ADMIN_ADJUST';

export interface AdminAdjustRequest {
  /** Signed delta in MAD (BigDecimal string). */
  deltaMad: string;
  operation: BalanceAdjustOperation;
  reason: string;
}

// ---------- API ----------

export const adminApi = {
  // ---- Prof validations ----
  listPendingProfs(
    params: ListProfsAdminParams = {},
  ): Promise<{ items: ProfPrivateView[]; meta: PageMeta | null }> {
    return pageFetch<ProfPrivateView>('/api/v1/admin/profs/pending', {
      page: params.page ?? 0,
      size: params.size ?? 20,
    });
  },

  validateProf(profId: string, body: ProfDecisionRequest = {}): Promise<ProfPrivateView> {
    return unwrap<ProfPrivateView>(
      apiClient.post(`/api/v1/admin/profs/${profId}/validate`, body),
    );
  },

  rejectProf(profId: string, body: ProfDecisionRequest = {}): Promise<ProfPrivateView> {
    return unwrap<ProfPrivateView>(
      apiClient.post(`/api/v1/admin/profs/${profId}/reject`, body),
    );
  },

  suspendProf(profId: string, body: ProfDecisionRequest = {}): Promise<ProfPrivateView> {
    return unwrap<ProfPrivateView>(
      apiClient.post(`/api/v1/admin/profs/${profId}/suspend`, body),
    );
  },

  // ---- Top-ups ----
  listTopups(
    status: TopupStatus = 'EN_ATTENTE_ADMIN',
    page = 0,
    size = 20,
  ): Promise<{ items: TopupView[]; meta: PageMeta | null }> {
    return pageFetch<TopupView>('/api/v1/payment/admin/topups', { status, page, size });
  },

  decideTopup(topupId: string, body: TopupDecideRequest): Promise<TopupView> {
    return unwrap<TopupView>(apiClient.post(`/api/v1/payment/admin/topups/${topupId}:decide`, body));
  },

  // ---- Payouts ----
  listPayouts(
    status: PayoutStatus = 'REQUESTED',
    page = 0,
    size = 20,
  ): Promise<{ items: PayoutView[]; meta: PageMeta | null }> {
    return pageFetch<PayoutView>('/api/v1/payment/admin/payouts', { status, page, size });
  },

  decidePayout(payoutId: string, body: PayoutDecideRequest): Promise<PayoutView> {
    return unwrap<PayoutView>(apiClient.post(`/api/v1/payment/admin/payouts/${payoutId}:decide`, body));
  },

  markPaidPayout(payoutId: string, body: PayoutMarkPaidRequest): Promise<PayoutView> {
    return unwrap<PayoutView>(
      apiClient.post(`/api/v1/payment/admin/payouts/${payoutId}:markPaid`, body),
    );
  },

  // ---- Disputes (booking-service) ----
  listBookingsForAdmin(
    params: ListBookingsAdminParams = {},
  ): Promise<{ items: BookingView[]; meta: PageMeta | null }> {
    return pageFetch<BookingView>('/api/v1/bookings/admin', {
      status: params.status,
      from: params.from,
      to: params.to,
      page: params.page ?? 0,
      size: params.size ?? 50,
    });
  },

  resolveDispute(
    bookingId: string,
    disputeId: string,
    body: ResolveDisputeRequest,
  ): Promise<unknown> {
    return unwrap<unknown>(
      apiClient.post(`/api/v1/bookings/admin/${bookingId}/disputes/${disputeId}:resolve`, body),
    );
  },

  // ---- Audit log ----
  getUserAudit(
    userId: string,
    params: ListAuditAdminParams = {},
  ): Promise<{ items: AuditEntryView[]; meta: PageMeta | null }> {
    return pageFetch<AuditEntryView>(`/api/v1/payment/admin/audit/${userId}`, {
      from: params.from,
      to: params.to,
      page: params.page ?? 0,
      size: params.size ?? 20,
    });
  },

  /**
   * Streams the audit CSV as a Blob. Caller is responsible for triggering the
   * download (createObjectURL + anchor click).
   */
  exportAuditCsv(params: {
    userId?: string;
    from?: string;
    to?: string;
  }): Promise<Blob> {
    return apiClient
      .post(
        `/api/v1/payment/admin/audit/export`,
        null,
        {
          params: {
            userId: params.userId,
            from: params.from,
            to: params.to,
            format: 'CSV',
          },
          responseType: 'blob',
        },
      )
      .then((r) => r.data as Blob);
  },

  // ---- Manual balance adjustment ----
  adjustBalance(userId: string, body: AdminAdjustRequest): Promise<AuditEntryView> {
    return unwrap<AuditEntryView>(
      apiClient.post(`/api/v1/payment/admin/balance/${userId}:adjust`, body),
    );
  },
};

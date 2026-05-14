import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';

import {
  adminApi,
  type AdminAdjustRequest,
  type ListAuditAdminParams,
  type ListBookingsAdminParams,
  type ListProfsAdminParams,
  type PayoutDecideRequest,
  type PayoutMarkPaidRequest,
  type ProfDecisionRequest,
  type ResolveDisputeRequest,
  type TopupDecideRequest,
} from '@/api/admin.api';
import type { PayoutStatus, TopupStatus } from '@/api/enums';

export const adminKeys = {
  all: ['admin'] as const,
  pendingProfs: (filters: ListProfsAdminParams) => ['admin', 'profs', 'pending', filters] as const,
  topups: (status: TopupStatus, page: number) => ['admin', 'topups', status, page] as const,
  payouts: (status: PayoutStatus, page: number) => ['admin', 'payouts', status, page] as const,
  bookings: (filters: ListBookingsAdminParams) => ['admin', 'bookings', filters] as const,
  audit: (userId: string, filters: ListAuditAdminParams) =>
    ['admin', 'audit', userId, filters] as const,
};

// ---- Pending profs ----

export function usePendingProfs(params: ListProfsAdminParams = {}) {
  return useQuery({
    queryKey: adminKeys.pendingProfs(params),
    queryFn: () => adminApi.listPendingProfs(params),
  });
}

export function useValidateProf() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ profId, body }: { profId: string; body?: ProfDecisionRequest }) =>
      adminApi.validateProf(profId, body ?? {}),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['admin', 'profs'] }),
  });
}

export function useRejectProf() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ profId, body }: { profId: string; body?: ProfDecisionRequest }) =>
      adminApi.rejectProf(profId, body ?? {}),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['admin', 'profs'] }),
  });
}

export function useSuspendProf() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ profId, body }: { profId: string; body?: ProfDecisionRequest }) =>
      adminApi.suspendProf(profId, body ?? {}),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['admin', 'profs'] }),
  });
}

// ---- Topups ----

export function useTopupsAdmin(status: TopupStatus = 'EN_ATTENTE_ADMIN', page = 0) {
  return useQuery({
    queryKey: adminKeys.topups(status, page),
    queryFn: () => adminApi.listTopups(status, page),
  });
}

export function useDecideTopup() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ topupId, body }: { topupId: string; body: TopupDecideRequest }) =>
      adminApi.decideTopup(topupId, body),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['admin', 'topups'] }),
  });
}

// ---- Payouts ----

export function usePayoutsAdmin(status: PayoutStatus = 'REQUESTED', page = 0) {
  return useQuery({
    queryKey: adminKeys.payouts(status, page),
    queryFn: () => adminApi.listPayouts(status, page),
  });
}

export function useDecidePayout() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ payoutId, body }: { payoutId: string; body: PayoutDecideRequest }) =>
      adminApi.decidePayout(payoutId, body),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['admin', 'payouts'] }),
  });
}

export function useMarkPaidPayout() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ payoutId, body }: { payoutId: string; body: PayoutMarkPaidRequest }) =>
      adminApi.markPaidPayout(payoutId, body),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['admin', 'payouts'] }),
  });
}

// ---- Disputes ----

export function useAdminBookings(params: ListBookingsAdminParams = {}) {
  return useQuery({
    queryKey: adminKeys.bookings(params),
    queryFn: () => adminApi.listBookingsForAdmin(params),
  });
}

export function useResolveDispute() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({
      bookingId,
      disputeId,
      body,
    }: {
      bookingId: string;
      disputeId: string;
      body: ResolveDisputeRequest;
    }) => adminApi.resolveDispute(bookingId, disputeId, body),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['admin', 'bookings'] }),
  });
}

// ---- Audit ----

export function useUserAudit(userId: string | undefined, params: ListAuditAdminParams = {}) {
  return useQuery({
    queryKey: adminKeys.audit(userId ?? '', params),
    queryFn: () => adminApi.getUserAudit(userId as string, params),
    enabled: Boolean(userId),
  });
}

// ---- Balance adjust ----

export function useAdjustBalance() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ userId, body }: { userId: string; body: AdminAdjustRequest }) =>
      adminApi.adjustBalance(userId, body),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['admin'] }),
  });
}

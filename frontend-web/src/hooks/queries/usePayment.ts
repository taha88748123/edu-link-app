import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';

import {
  paymentApi,
  type CreatePayoutRequest,
  type CreateTopupRequest,
  type ListAuditParams,
  type ListPayoutsParams,
  type ListTopupsParams,
} from '@/api/payment.api';

export const paymentKeys = {
  balance: ['payment', 'balance'] as const,
  audit: (filters: ListAuditParams) => ['payment', 'audit', filters] as const,
  topups: (filters: ListTopupsParams) => ['payment', 'topups', filters] as const,
  payouts: (filters: ListPayoutsParams) => ['payment', 'payouts', filters] as const,
};

export function useMyBalance() {
  return useQuery({
    queryKey: paymentKeys.balance,
    queryFn: () => paymentApi.getMyBalance(),
  });
}

export function useMyAuditLog(filters: ListAuditParams = {}) {
  return useQuery({
    queryKey: paymentKeys.audit(filters),
    queryFn: () => paymentApi.getMyAuditLog(filters),
  });
}

export function useSubmitTopup() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (body: CreateTopupRequest) => paymentApi.submitTopup(body),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['payment', 'topups'] });
      qc.invalidateQueries({ queryKey: paymentKeys.balance });
    },
  });
}

export function useMyTopups(filters: ListTopupsParams = {}) {
  return useQuery({
    queryKey: paymentKeys.topups(filters),
    queryFn: () => paymentApi.listMyTopups(filters),
  });
}

export function useRequestPayout() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (body: CreatePayoutRequest) => paymentApi.requestPayout(body),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['payment', 'payouts'] });
      qc.invalidateQueries({ queryKey: paymentKeys.balance });
    },
  });
}

export function useMyPayouts(filters: ListPayoutsParams = {}) {
  return useQuery({
    queryKey: paymentKeys.payouts(filters),
    queryFn: () => paymentApi.listMyPayouts(filters),
  });
}

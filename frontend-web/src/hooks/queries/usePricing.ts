import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';

import {
  pricingApi,
  type CreatePackRequest,
  type CreateRateRequest,
  type ListProfRatesParams,
  type QuoteRequest,
  type UpdatePackRequest,
  type UpdateRateRequest,
} from '@/api/pricing.api';

export const pricingKeys = {
  me: ['pricing', 'me'] as const,
  profRates: (filters: ListProfRatesParams) => ['pricing', 'prof', filters.profId, 'rates', filters] as const,
  profPacks: (filters: ListProfRatesParams) => ['pricing', 'prof', filters.profId, 'packs', filters] as const,
  quote: (body: QuoteRequest) => ['pricing', 'quote', body] as const,
};

export function useMyPricing() {
  return useQuery({
    queryKey: pricingKeys.me,
    queryFn: () => pricingApi.getMyPricing(),
  });
}

export function useCreateRate() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (body: CreateRateRequest) => pricingApi.createRate(body),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['pricing'] }),
  });
}

type UpdateRateArgs = { id: string; body: UpdateRateRequest };
export function useUpdateRate() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, body }: UpdateRateArgs) => pricingApi.updateRate(id, body),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['pricing'] }),
  });
}

export function useDeleteRate() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => pricingApi.deleteRate(id),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['pricing'] }),
  });
}

export function useCreatePack() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (body: CreatePackRequest) => pricingApi.createPack(body),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['pricing'] }),
  });
}

type UpdatePackArgs = { id: string; body: UpdatePackRequest };
export function useUpdatePack() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, body }: UpdatePackArgs) => pricingApi.updatePack(id, body),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['pricing'] }),
  });
}

export function useDeletePack() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => pricingApi.deletePack(id),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['pricing'] }),
  });
}

export function useQuote(body: QuoteRequest | null) {
  return useQuery({
    queryKey: body ? pricingKeys.quote(body) : ['pricing', 'quote', 'idle'],
    queryFn: () => pricingApi.quote(body as QuoteRequest),
    enabled: Boolean(body),
  });
}

export function useProfRates(filters: ListProfRatesParams, enabled = true) {
  return useQuery({
    queryKey: pricingKeys.profRates(filters),
    queryFn: () => pricingApi.listProfRates(filters),
    enabled,
  });
}

export function useProfPacks(filters: ListProfRatesParams, enabled = true) {
  return useQuery({
    queryKey: pricingKeys.profPacks(filters),
    queryFn: () => pricingApi.listProfPacks(filters),
    enabled,
  });
}

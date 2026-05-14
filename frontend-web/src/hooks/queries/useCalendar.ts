import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';

import {
  calendarApi,
  type CreateAvailabilityRuleRequest,
  type ListMySlotsParams,
  type ListProfSlotsParams,
  type UpdateAvailabilityRuleRequest,
} from '@/api/calendar.api';

export const calendarKeys = {
  rules: ['calendar', 'rules'] as const,
  mySlots: (filters: ListMySlotsParams) => ['calendar', 'slots', 'me', filters] as const,
  profSlots: (filters: ListProfSlotsParams) => ['calendar', 'slots', 'prof', filters] as const,
};

export function useMyAvailabilityRules() {
  return useQuery({
    queryKey: calendarKeys.rules,
    queryFn: () => calendarApi.listMyRules(),
  });
}

export function useCreateRule() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (body: CreateAvailabilityRuleRequest) => calendarApi.createRule(body),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: calendarKeys.rules });
    },
  });
}

type UpdateRuleArgs = { id: string; body: UpdateAvailabilityRuleRequest };
export function useUpdateRule() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, body }: UpdateRuleArgs) => calendarApi.updateRule(id, body),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: calendarKeys.rules });
    },
  });
}

export function useDeleteRule() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => calendarApi.deleteRule(id),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: calendarKeys.rules });
    },
  });
}

export function useGenerateSlots() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: () => calendarApi.generateSlots(),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['calendar', 'slots'] });
    },
  });
}

export function useMySlots(filters: ListMySlotsParams) {
  return useQuery({
    queryKey: calendarKeys.mySlots(filters),
    queryFn: () => calendarApi.listMySlots(filters),
  });
}

export function useProfSlots(filters: ListProfSlotsParams, enabled = true) {
  return useQuery({
    queryKey: calendarKeys.profSlots(filters),
    queryFn: () => calendarApi.listProfSlots(filters),
    enabled,
  });
}

export function useBlockSlot() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => calendarApi.blockSlot(id),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['calendar', 'slots'] });
    },
  });
}

export function useUnblockSlot() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => calendarApi.unblockSlot(id),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['calendar', 'slots'] });
    },
  });
}

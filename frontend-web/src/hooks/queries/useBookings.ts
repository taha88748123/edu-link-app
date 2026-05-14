import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';

import {
  bookingsApi,
  type CancelRequest,
  type CreateBookingRequest,
  type DeclineRequest,
  type DisputeRequest,
  type ListMyBookingsParams,
} from '@/api/bookings.api';

export const bookingKeys = {
  all: ['bookings'] as const,
  list: (filters: ListMyBookingsParams) => ['bookings', 'list', filters] as const,
  detail: (id: string) => ['bookings', 'detail', id] as const,
};

export function useMyBookings(filters: ListMyBookingsParams = {}) {
  return useQuery({
    queryKey: bookingKeys.list(filters),
    queryFn: () => bookingsApi.listMyBookings(filters),
  });
}

export function useBooking(id: string | undefined) {
  return useQuery({
    queryKey: bookingKeys.detail(id ?? ''),
    queryFn: () => bookingsApi.getBooking(id as string),
    enabled: Boolean(id),
  });
}

export function useCreateBooking() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (body: CreateBookingRequest) => bookingsApi.createBooking(body),
    onSuccess: (created) => {
      qc.invalidateQueries({ queryKey: bookingKeys.all });
      qc.setQueryData(bookingKeys.detail(created.id), created);
    },
  });
}

type CancelArgs = { id: string; body?: CancelRequest };
export function useCancelBooking() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, body }: CancelArgs) => bookingsApi.cancelBooking(id, body ?? {}),
    onSuccess: (updated) => {
      qc.setQueryData(bookingKeys.detail(updated.id), updated);
      qc.invalidateQueries({ queryKey: bookingKeys.all });
    },
  });
}

export function useConfirmBooking() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => bookingsApi.confirmBooking(id),
    onSuccess: (updated) => {
      qc.setQueryData(bookingKeys.detail(updated.id), updated);
      qc.invalidateQueries({ queryKey: bookingKeys.all });
    },
  });
}

type DisputeArgs = { id: string; body: DisputeRequest };
export function useDisputeBooking() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, body }: DisputeArgs) => bookingsApi.disputeBooking(id, body),
    onSuccess: (_dispute, vars) => {
      qc.invalidateQueries({ queryKey: bookingKeys.detail(vars.id) });
      qc.invalidateQueries({ queryKey: bookingKeys.all });
    },
  });
}

export function useAcceptBooking() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => bookingsApi.acceptBooking(id),
    onSuccess: (updated) => {
      qc.setQueryData(bookingKeys.detail(updated.id), updated);
      qc.invalidateQueries({ queryKey: bookingKeys.all });
    },
  });
}

type DeclineArgs = { id: string; body?: DeclineRequest };
export function useDeclineBooking() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, body }: DeclineArgs) => bookingsApi.declineBooking(id, body ?? {}),
    onSuccess: (updated) => {
      qc.setQueryData(bookingKeys.detail(updated.id), updated);
      qc.invalidateQueries({ queryKey: bookingKeys.all });
    },
  });
}

export function useStartBooking() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => bookingsApi.startBooking(id),
    onSuccess: (updated) => {
      qc.setQueryData(bookingKeys.detail(updated.id), updated);
      qc.invalidateQueries({ queryKey: bookingKeys.all });
    },
  });
}

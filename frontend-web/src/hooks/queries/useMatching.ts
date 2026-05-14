import { useMutation } from '@tanstack/react-query';

import { matchingApi, type MatchingRequest } from '@/api/matching.api';

export const matchingKeys = {
  request: (filters: MatchingRequest) => ['matching', filters] as const,
};

/**
 * Matching is a POST endpoint; we expose it as a mutation so the search button
 * triggers a fresh ranking on demand. Components keep the returned data in
 * local state (or via the mutation cache).
 */
export function useMatchingMutation() {
  return useMutation({
    mutationFn: (body: MatchingRequest) => matchingApi.getMatches(body),
  });
}

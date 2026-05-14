import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';

import {
  profileApi,
  type CreateReviewRequest,
  type SearchProfsParams,
  type UpdateProfileRequest,
} from '@/api/profile.api';

export const profileKeys = {
  me: ['profile', 'me'] as const,
  prof: (id: string) => ['profile', 'prof', id] as const,
  search: (filters: SearchProfsParams) => ['profile', 'search', filters] as const,
  reviews: (profId: string, page: number) => ['profile', 'prof', profId, 'reviews', page] as const,
};

export function useMyProfile() {
  return useQuery({
    queryKey: profileKeys.me,
    queryFn: () => profileApi.getMyProfile(),
  });
}

export function useUpdateMyProfile() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (body: UpdateProfileRequest) => profileApi.updateMyProfile(body),
    onSuccess: () => qc.invalidateQueries({ queryKey: profileKeys.me }),
  });
}

export function useProfPublic(profId: string | undefined) {
  return useQuery({
    queryKey: profileKeys.prof(profId ?? ''),
    queryFn: () => profileApi.getProfPublic(profId as string),
    enabled: Boolean(profId),
  });
}

export function useSearchProfs(filters: SearchProfsParams, enabled = true) {
  return useQuery({
    queryKey: profileKeys.search(filters),
    queryFn: () => profileApi.searchProfs(filters),
    enabled,
  });
}

export function useProfReviews(profId: string | undefined, page: number = 0) {
  return useQuery({
    queryKey: profileKeys.reviews(profId ?? '', page),
    queryFn: () => profileApi.getProfReviews(profId as string, page),
    enabled: Boolean(profId),
  });
}

export function useCreateReview() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (body: CreateReviewRequest) => profileApi.createReview(body),
    onSuccess: (_review, vars) => {
      qc.invalidateQueries({ queryKey: profileKeys.prof(vars.profId) });
      qc.invalidateQueries({ queryKey: ['profile', 'prof', vars.profId, 'reviews'] });
    },
  });
}

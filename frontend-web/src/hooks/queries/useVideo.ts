import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';

import {
  videoApi,
  type ConsentRequest,
  type CreateArtifactRequest,
} from '@/api/video.api';

export const videoKeys = {
  all: ['video'] as const,
  session: (id: string) => ['video', 'session', id] as const,
  artifacts: (id: string) => ['video', 'artifacts', id] as const,
};

export function useSession(id: string | undefined, refetchInterval?: number) {
  return useQuery({
    queryKey: videoKeys.session(id ?? ''),
    queryFn: () => videoApi.getSession(id as string),
    enabled: Boolean(id),
    refetchInterval,
  });
}

export function useArtifacts(id: string | undefined) {
  return useQuery({
    queryKey: videoKeys.artifacts(id ?? ''),
    queryFn: () => videoApi.listArtifacts(id as string),
    enabled: Boolean(id),
  });
}

export function useIssueToken() {
  return useMutation({
    mutationFn: (id: string) => videoApi.issueToken(id),
  });
}

export function useStartSession() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => videoApi.startSession(id),
    onSuccess: (updated) => {
      qc.setQueryData(videoKeys.session(updated.id), updated);
    },
  });
}

export function useEndSession() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => videoApi.endSession(id),
    onSuccess: (updated) => {
      qc.setQueryData(videoKeys.session(updated.id), updated);
    },
  });
}

type ConsentArgs = { id: string; body: ConsentRequest };
export function useGrantConsent() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, body }: ConsentArgs) => videoApi.grantConsent(id, body),
    onSuccess: (updated) => {
      qc.setQueryData(videoKeys.session(updated.id), updated);
    },
  });
}

type CreateArtifactArgs = { id: string; body: CreateArtifactRequest };
export function useCreateArtifact() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, body }: CreateArtifactArgs) => videoApi.createArtifact(id, body),
    onSuccess: (_a, vars) => {
      qc.invalidateQueries({ queryKey: videoKeys.artifacts(vars.id) });
    },
  });
}

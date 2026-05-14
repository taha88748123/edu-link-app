import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';

import {
  chatApi,
  type CreateMessageRequest,
  type ListConversationsParams,
  type ListMessagesParams,
} from '@/api/chat.api';

/**
 * Polling intervals (ms). v1 uses TanStack Query refetchInterval — see the
 * TODO note in `ChatPage` about replacing this with SockJS+STOMP via /ws/chat.
 */
const DEFAULT_MESSAGES_POLL_MS = 3_000;
const DEFAULT_PRESENCE_POLL_MS = 10_000;

function readPollMs(): number {
  // Env hook for tests / staging slow-down (VITE_CHAT_POLL_MS).
  const raw = import.meta.env.VITE_CHAT_POLL_MS;
  if (typeof raw === 'string' && raw.length > 0) {
    const n = Number(raw);
    if (Number.isFinite(n) && n > 0) return n;
  }
  return DEFAULT_MESSAGES_POLL_MS;
}

export const chatKeys = {
  all: ['chat'] as const,
  conversations: (filters: ListConversationsParams) => ['chat', 'conversations', filters] as const,
  conversation: (id: string) => ['chat', 'conversation', id] as const,
  messages: (id: string, filters: ListMessagesParams) =>
    ['chat', 'messages', id, filters] as const,
  presence: (userId: string) => ['chat', 'presence', userId] as const,
};

export function useMyConversations(filters: ListConversationsParams = {}) {
  return useQuery({
    queryKey: chatKeys.conversations(filters),
    queryFn: () => chatApi.listMyConversations(filters),
    // Light polling on the list — keeps unread counts fresh.
    refetchInterval: 15_000,
  });
}

export function useConversation(id: string | undefined) {
  return useQuery({
    queryKey: chatKeys.conversation(id ?? ''),
    queryFn: () => chatApi.getConversation(id as string),
    enabled: Boolean(id),
  });
}

export function useMessages(id: string | undefined, filters: ListMessagesParams = {}) {
  return useQuery({
    queryKey: chatKeys.messages(id ?? '', filters),
    queryFn: () => chatApi.listMessages(id as string, filters),
    enabled: Boolean(id),
    refetchInterval: readPollMs(),
  });
}

export function usePresence(userId: string | undefined) {
  return useQuery({
    queryKey: chatKeys.presence(userId ?? ''),
    queryFn: () => chatApi.getPresence(userId as string),
    enabled: Boolean(userId),
    refetchInterval: DEFAULT_PRESENCE_POLL_MS,
  });
}

type SendArgs = { id: string; body: CreateMessageRequest };
export function useSendMessage() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, body }: SendArgs) => chatApi.sendMessage(id, body),
    onSuccess: (_msg, vars) => {
      qc.invalidateQueries({ queryKey: ['chat', 'messages', vars.id] });
      qc.invalidateQueries({ queryKey: ['chat', 'conversations'] });
    },
  });
}

export function useTyping() {
  return useMutation({
    mutationFn: (id: string) => chatApi.typing(id),
  });
}

type ReadArgs = { id: string; lastReadMessageId: string };
export function useMarkRead() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, lastReadMessageId }: ReadArgs) =>
      chatApi.markRead(id, lastReadMessageId),
    onSuccess: (_void, vars) => {
      qc.invalidateQueries({ queryKey: chatKeys.conversation(vars.id) });
      qc.invalidateQueries({ queryKey: ['chat', 'conversations'] });
    },
  });
}

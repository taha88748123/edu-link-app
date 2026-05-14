import { useEffect, useMemo, useRef, useState } from 'react';
import { Client, type IFrame, type IMessage } from '@stomp/stompjs';
import { useQueryClient } from '@tanstack/react-query';

import type { MessageView } from '@/api/chat.api';
import { chatKeys } from '@/hooks/queries/useChat';
import { useAuthStore } from '@/auth/auth.store';

/** Connection state surfaced to the UI badge. */
export type ChatSocketStatus =
  | 'idle'
  | 'connecting'
  | 'connected'
  | 'reconnecting'
  | 'failed';

const MAX_RECONNECT_FAILURES = 5;
const BASE_DELAY_MS = 1_000;
const MAX_DELAY_MS = 30_000;

function backoff(attempt: number): number {
  // 1s → 2s → 4s → 8s → 16s, capped at 30s.
  return Math.min(BASE_DELAY_MS * 2 ** attempt, MAX_DELAY_MS);
}

/** Builds the absolute WS URL — same origin as the SPA, scheme matched to https/http. */
function defaultWsUrl(): string {
  if (typeof window === 'undefined') return 'ws://localhost:5173/ws/chat';
  const proto = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
  return `${proto}//${window.location.host}/ws/chat`;
}

export interface UseChatSocketOptions {
  /** Override the URL (tests). */
  url?: string;
  /** Disable the hook entirely (e.g. when WS is disabled by config). */
  enabled?: boolean;
  /** Test seam: override the underlying STOMP {@link Client} factory. */
  clientFactory?: (cfg: ConstructorParameters<typeof Client>[0]) => Client;
}

export interface UseChatSocketResult {
  status: ChatSocketStatus;
  /** Sends a typing notification to the server (no-op when disconnected). */
  sendTyping: () => void;
}

/**
 * Subscribes to {@code /topic/conversations/{conversationId}/messages} over a
 * STOMP-over-native-WebSocket connection. On incoming messages, the React
 * Query cache for that conversation is invalidated so the
 * {@code useMessages} consumer re-renders with the fresh history.
 *
 * <p>Reconnection is exponential (1s, 2s, 4s, 8s, 16s, 30s cap). After
 * {@link MAX_RECONNECT_FAILURES} consecutive failures we surface {@code failed}
 * so the UI can fall back to REST polling.
 */
export function useChatSocket(
  conversationId: string | undefined,
  opts: UseChatSocketOptions = {},
): UseChatSocketResult {
  const qc = useQueryClient();
  const accessToken = useAuthStore((s) => s.accessToken);

  const [status, setStatus] = useState<ChatSocketStatus>('idle');
  const clientRef = useRef<Client | null>(null);
  const failuresRef = useRef(0);

  const url = useMemo(() => opts.url ?? defaultWsUrl(), [opts.url]);
  const enabled = opts.enabled ?? true;

  useEffect(() => {
    if (!enabled) {
      setStatus('idle');
      return;
    }
    if (!conversationId || !accessToken) {
      setStatus('idle');
      return;
    }

    let cancelled = false;
    const factory =
      opts.clientFactory ??
      ((cfg: ConstructorParameters<typeof Client>[0]) => new Client(cfg));

    const client: Client = factory({
      brokerURL: url,
      connectHeaders: {
        Authorization: `Bearer ${accessToken}`,
      },
      reconnectDelay: 0, // we drive reconnects ourselves to apply the cap and the failure counter.
      debug: () => undefined,
      onConnect: () => {
        if (cancelled) return;
        failuresRef.current = 0;
        setStatus('connected');
        client.subscribe(`/topic/conversations/${conversationId}/messages`, (frame: IMessage) => {
          try {
            const payload = JSON.parse(frame.body) as MessageView;
            // Push into ALL cached pages of this conversation by invalidation
            // (cheap, correct). A direct setQueryData merge would be smarter
            // but requires knowing the exact ListMessagesParams key.
            qc.invalidateQueries({ queryKey: ['chat', 'messages', conversationId] });
            qc.invalidateQueries({ queryKey: chatKeys.conversations({}) });
            // Optional: optimistic patch — exposed for future refinement.
            void payload;
          } catch {
            // Bad frame — ignore; REST will reconcile on next poll.
          }
        });
      },
      onWebSocketClose: () => {
        if (cancelled) return;
        failuresRef.current += 1;
        if (failuresRef.current >= MAX_RECONNECT_FAILURES) {
          setStatus('failed');
          return;
        }
        setStatus('reconnecting');
        const delay = backoff(failuresRef.current - 1);
        window.setTimeout(() => {
          if (cancelled) return;
          try {
            client.activate();
          } catch {
            setStatus('failed');
          }
        }, delay);
      },
      onStompError: (frame: IFrame) => {
        // Backend rejected CONNECT (bad token, etc.) — don't loop; the next
        // close handler counts this as a failure too.
        // eslint-disable-next-line no-console
        console.warn('[chat-ws] STOMP error', frame.headers, frame.body);
      },
    });

    clientRef.current = client;
    setStatus('connecting');
    client.activate();

    return () => {
      cancelled = true;
      clientRef.current = null;
      try {
        void client.deactivate();
      } catch {
        // ignore
      }
      setStatus('idle');
    };
  }, [conversationId, accessToken, url, enabled, qc, opts.clientFactory]);

  return {
    status,
    sendTyping: () => {
      const c = clientRef.current;
      if (!c || !c.connected || !conversationId) return;
      try {
        c.publish({
          destination: `/app/conversations/${conversationId}/typing`,
          body: '',
        });
      } catch {
        // best-effort; the REST /typing endpoint remains as the canonical signal.
      }
    },
  };
}

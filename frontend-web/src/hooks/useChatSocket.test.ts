import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { renderHook, act, waitFor } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import React from 'react';

import { useChatSocket } from './useChatSocket';
import { useAuthStore } from '@/auth/auth.store';

/**
 * Minimal fake STOMP {@code Client} that records lifecycle calls so we can
 * verify the hook's reconnect/subscribe wiring without a real WebSocket.
 */
class FakeStompClient {
  // Match the shape the hook touches; the rest of the real {@code Client} surface is unused.
  public connected = false;
  public activated = 0;
  public deactivated = 0;
  private subscriptions: Array<(frame: { body: string }) => void> = [];

  constructor(public cfg: any) {}

  activate() {
    this.activated += 1;
    // Defer onConnect to a microtask so the hook can observe 'connecting' first.
    queueMicrotask(() => {
      this.connected = true;
      this.cfg.onConnect?.();
    });
  }

  deactivate() {
    this.deactivated += 1;
    this.connected = false;
    return Promise.resolve();
  }

  subscribe(_dest: string, cb: (frame: { body: string }) => void) {
    this.subscriptions.push(cb);
    return { id: 'sub-' + this.subscriptions.length, unsubscribe: () => undefined };
  }

  publish() {
    // no-op
  }

  // Test helper — emit a server-side message.
  push(payload: unknown) {
    const body = JSON.stringify(payload);
    this.subscriptions.forEach((cb) => cb({ body }));
  }

  // Test helper — simulate the socket dropping.
  drop() {
    this.connected = false;
    this.cfg.onWebSocketClose?.();
  }
}

function wrapper(qc: QueryClient): React.FC<{ children: React.ReactNode }> {
  // eslint-disable-next-line react/display-name
  return ({ children }) =>
    React.createElement(QueryClientProvider, { client: qc }, children);
}

describe('useChatSocket', () => {
  let qc: QueryClient;
  let fake: FakeStompClient | null;

  beforeEach(() => {
    qc = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    fake = null;
    // Seed an access token so the hook activates.
    useAuthStore.setState({ accessToken: 'test.jwt.token', refreshToken: null, user: null, hydrated: true });
  });

  afterEach(() => {
    vi.useRealTimers();
    qc.clear();
  });

  it('starts idle when conversationId is missing', () => {
    const { result } = renderHook(() => useChatSocket(undefined), { wrapper: wrapper(qc) });
    expect(result.current.status).toBe('idle');
  });

  it('connects and surfaces "connected" once onConnect fires', async () => {
    const factory = (cfg: any) => {
      fake = new FakeStompClient(cfg);
      return fake as unknown as ReturnType<typeof FakeStompClient.prototype.activate> as any;
    };

    const { result } = renderHook(() => useChatSocket('c1', { clientFactory: factory as any }), {
      wrapper: wrapper(qc),
    });

    await waitFor(() => expect(result.current.status).toBe('connected'));
    expect(fake!.activated).toBe(1);
  });

  it('invalidates the messages query when the server pushes a frame', async () => {
    const factory = (cfg: any) => {
      fake = new FakeStompClient(cfg);
      return fake as any;
    };
    const spy = vi.spyOn(qc, 'invalidateQueries');

    renderHook(() => useChatSocket('c1', { clientFactory: factory as any }), {
      wrapper: wrapper(qc),
    });
    await waitFor(() => expect(fake!.connected).toBe(true));

    act(() => {
      fake!.push({ id: 'm1', conversationId: 'c1', body: 'hi' });
    });

    // Both calls happen synchronously inside the subscription callback.
    expect(spy).toHaveBeenCalledWith(expect.objectContaining({ queryKey: ['chat', 'messages', 'c1'] }));
  });

  it('surfaces "failed" after 5 consecutive disconnects without a successful reconnect', async () => {
    // We disable the auto-reconnect inside the fake by NOT advancing timers
    // between drops — five consecutive failures must trip the kill-switch
    // regardless of whether the next reconnect attempt completes.
    const factory = (cfg: any) => {
      fake = new FakeStompClient(cfg);
      return fake as any;
    };

    const { result } = renderHook(() => useChatSocket('c1', { clientFactory: factory as any }), {
      wrapper: wrapper(qc),
    });
    await waitFor(() => expect(result.current.status).toBe('connected'));

    for (let i = 0; i < 5; i++) {
      act(() => fake!.drop());
    }

    await waitFor(() => expect(result.current.status).toBe('failed'));
  });

  it('does nothing when disabled', () => {
    const factory = vi.fn();
    const { result } = renderHook(
      () => useChatSocket('c1', { enabled: false, clientFactory: factory as any }),
      { wrapper: wrapper(qc) },
    );
    expect(result.current.status).toBe('idle');
    expect(factory).not.toHaveBeenCalled();
  });
});

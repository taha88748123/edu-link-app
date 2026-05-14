import { describe, expect, it } from 'vitest';

import { chatKeys } from './useChat';

describe('chatKeys', () => {
  it('exposes a stable root tuple for invalidation', () => {
    expect(chatKeys.all).toEqual(['chat']);
  });

  it('encodes filters into the conversations key', () => {
    expect(chatKeys.conversations({})).toEqual(['chat', 'conversations', {}]);
    expect(chatKeys.conversations({ status: 'PRE_ACCEPT' })).toEqual([
      'chat',
      'conversations',
      { status: 'PRE_ACCEPT' },
    ]);
  });

  it('keys conversations / messages / presence under chat.* roots', () => {
    expect(chatKeys.conversation('c1')).toEqual(['chat', 'conversation', 'c1']);
    expect(chatKeys.messages('c1', { size: 50 })).toEqual([
      'chat',
      'messages',
      'c1',
      { size: 50 },
    ]);
    expect(chatKeys.presence('u1')).toEqual(['chat', 'presence', 'u1']);
  });

  it('returns different tuples for different filters (so RQ caches them apart)', () => {
    const a = chatKeys.messages('c1', {});
    const b = chatKeys.messages('c1', { before: 'm-100' });
    expect(a).not.toEqual(b);
  });
});

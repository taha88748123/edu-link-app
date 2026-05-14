import { apiClient, unwrap } from './client';

/**
 * TypeScript mirrors of `backend/chat-service/.../web/dto/*`.
 * Keep in sync with `ConversationView`, `MessageView`, `AdminMessageView`,
 * `CreateMessageRequest`, `ReadRequest`, `PresenceView`.
 */

const BASE = '/api/v1/chat';

// ---------- Enums ----------

export type ConversationStatus = 'PRE_ACCEPT' | 'POST_ACCEPT' | 'CLOSED';
export const CONVERSATION_STATUSES: ConversationStatus[] = [
  'PRE_ACCEPT',
  'POST_ACCEPT',
  'CLOSED',
];

export type MessageType = 'TEXT' | 'IMAGE' | 'PDF' | 'VOICE' | 'SMART_REPLY';
export type SenderRole = 'PROF' | 'STUDENT';
export type ModerationStatus = 'CLEAN' | 'MASKED' | 'FLAGGED' | 'BLOCKED';

// ---------- DTOs ----------

export interface ConversationView {
  id: string;
  bookingId: string;
  profId: string;
  studentId: string;
  status: ConversationStatus;
  lastMessageAt: string | null;
  createdAt: string;
  closedAt: string | null;
  unreadCount: number;
}

export interface AttachmentView {
  url: string;
  mime: string;
  sizeBytes: number;
}

export interface MessageModerationView {
  status: ModerationStatus | null;
  flags: string[] | null;
}

export interface MessageView {
  id: string;
  conversationId: string;
  senderId: string;
  senderRole: SenderRole;
  type: MessageType;
  body: string | null;
  attachments: AttachmentView[] | null;
  moderation: MessageModerationView | null;
  createdAt: string;
}

export interface AdminMessageView extends MessageView {
  bookingId: string | null;
  bodyOriginal: string | null;
}

export interface PresenceView {
  userId: string;
  online: boolean;
  ttlSeconds: number | null;
}

export interface CreateMessageRequest {
  type: MessageType;
  body?: string;
  attachments?: AttachmentView[];
}

export interface ListConversationsParams {
  status?: ConversationStatus;
  page?: number;
  size?: number;
}

export interface ListMessagesParams {
  before?: string;
  size?: number;
}

// ---------- API ----------

export const chatApi = {
  listMyConversations(params: ListConversationsParams = {}): Promise<ConversationView[]> {
    return unwrap<ConversationView[]>(
      apiClient.get(`${BASE}/conversations/me`, {
        params: {
          status: params.status,
          page: params.page ?? 0,
          size: params.size ?? 20,
        },
      }),
    );
  },

  getConversation(id: string): Promise<ConversationView> {
    return unwrap<ConversationView>(apiClient.get(`${BASE}/conversations/${id}`));
  },

  listMessages(id: string, params: ListMessagesParams = {}): Promise<MessageView[]> {
    return unwrap<MessageView[]>(
      apiClient.get(`${BASE}/conversations/${id}/messages`, {
        params: {
          before: params.before,
          size: params.size ?? 50,
        },
      }),
    );
  },

  sendMessage(id: string, body: CreateMessageRequest): Promise<MessageView> {
    return unwrap<MessageView>(apiClient.post(`${BASE}/conversations/${id}/messages`, body));
  },

  typing(id: string): Promise<void> {
    return unwrap<void>(apiClient.post(`${BASE}/conversations/${id}/typing`));
  },

  markRead(id: string, lastReadMessageId: string): Promise<void> {
    return unwrap<void>(
      apiClient.post(`${BASE}/conversations/${id}/read`, { lastReadMessageId }),
    );
  },

  getPresence(userId: string): Promise<PresenceView> {
    return unwrap<PresenceView>(apiClient.get(`${BASE}/presence/${userId}`));
  },

  heartbeat(): Promise<PresenceView> {
    return unwrap<PresenceView>(apiClient.post(`${BASE}/presence/heartbeat`));
  },

  // ---------- Admin ----------

  adminGetMessage(messageId: string): Promise<AdminMessageView> {
    return unwrap<AdminMessageView>(apiClient.get(`${BASE}/admin/messages/${messageId}`));
  },

  adminCloseConversation(id: string): Promise<ConversationView> {
    return unwrap<ConversationView>(apiClient.post(`${BASE}/admin/conversations/${id}:close`));
  },
};

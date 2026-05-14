import { apiClient, unwrap } from './client';

/**
 * TypeScript mirrors of `backend/video-session-service/.../web/dto/*`.
 */

const BASE = '/api/v1/video';

// ---------- Enums ----------

export type SessionStatus = 'SCHEDULED' | 'LIVE' | 'ENDED' | 'FAILED' | 'EXPIRED';
export const SESSION_STATUSES: SessionStatus[] = [
  'SCHEDULED',
  'LIVE',
  'ENDED',
  'FAILED',
  'EXPIRED',
];

export type ArtifactType = 'WHITEBOARD' | 'QUIZ' | 'NOTE' | 'CODE';
export type ConsentScope = 'RECORDING' | 'TRANSCRIPT';

// ---------- DTOs ----------

export interface Consent {
  userId: string;
  at: string;
  scope: ConsentScope;
}

export interface SessionView {
  id: string;
  bookingId: string;
  profId: string;
  studentId: string;
  livekitRoom: string | null;
  scheduledStart: string;
  scheduledEnd: string;
  actualStart: string | null;
  actualEnd: string | null;
  durationEffectiveSeconds: number | null;
  recordingEnabled: boolean | null;
  recordingUrl: string | null;
  recordingExpiresAt: string | null;
  consentGivenBy: Consent[] | null;
  status: SessionStatus;
  createdAt: string;
  updatedAt: string;
}

export interface SessionTokenResponse {
  token: string;
  livekitUrl: string;
  roomName: string;
  expiresAt: string;
}

export interface ConsentRequest {
  scope: ConsentScope;
}

export interface CreateArtifactRequest {
  type: ArtifactType;
  content: string;
}

export interface ArtifactView {
  id: string;
  sessionId: string;
  type: ArtifactType;
  content: string;
  createdBy: string;
  createdAt: string;
}

export interface ListSessionsParams {
  status?: SessionStatus;
  from?: string;
  to?: string;
  page?: number;
  size?: number;
}

// ---------- API ----------

export const videoApi = {
  getSession(id: string): Promise<SessionView> {
    return unwrap<SessionView>(apiClient.get(`${BASE}/sessions/${id}`));
  },

  listMySessions(params: ListSessionsParams = {}): Promise<SessionView[]> {
    return unwrap<SessionView[]>(
      apiClient.get(`${BASE}/sessions/me`, {
        params: {
          status: params.status,
          from: params.from,
          to: params.to,
          page: params.page ?? 0,
          size: params.size ?? 20,
        },
      }),
    );
  },

  issueToken(id: string): Promise<SessionTokenResponse> {
    return unwrap<SessionTokenResponse>(apiClient.post(`${BASE}/sessions/${id}/token`));
  },

  startSession(id: string): Promise<SessionView> {
    return unwrap<SessionView>(apiClient.post(`${BASE}/sessions/${id}:start`));
  },

  endSession(id: string): Promise<SessionView> {
    return unwrap<SessionView>(apiClient.post(`${BASE}/sessions/${id}:end`));
  },

  grantConsent(id: string, body: ConsentRequest): Promise<SessionView> {
    return unwrap<SessionView>(apiClient.post(`${BASE}/sessions/${id}/consent`, body));
  },

  createArtifact(id: string, body: CreateArtifactRequest): Promise<ArtifactView> {
    return unwrap<ArtifactView>(apiClient.post(`${BASE}/sessions/${id}/artifacts`, body));
  },

  listArtifacts(id: string): Promise<ArtifactView[]> {
    return unwrap<ArtifactView[]>(apiClient.get(`${BASE}/sessions/${id}/artifacts`));
  },

  // ---------- Admin ----------

  adminListSessions(params: ListSessionsParams = {}): Promise<SessionView[]> {
    return unwrap<SessionView[]>(
      apiClient.get(`${BASE}/admin/sessions`, {
        params: {
          status: params.status,
          from: params.from,
          to: params.to,
          page: params.page ?? 0,
          size: params.size ?? 20,
        },
      }),
    );
  },

  adminExpireSession(id: string): Promise<SessionView> {
    return unwrap<SessionView>(apiClient.post(`${BASE}/admin/sessions/${id}:expire`));
  },
};

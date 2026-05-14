import { apiClient, unwrap } from './client';
import type { Modality, ProfStatus, SchoolLevel } from './enums';

const PROFILE_BASE = '/api/v1/profile';
const CATALOG_BASE = '/api/v1/profiles';

// ---------- DTOs ----------

export interface SubjectView {
  code: string;
  level: SchoolLevel;
}

export interface LocationView {
  city: string;
  region: string | null;
}

export interface ProfPublicView {
  userId: string;
  displayName: string | null;
  photoUrl: string | null;
  bio: string | null;
  yearsExperience: number | null;
  introVideoUrl: string | null;
  subjects: SubjectView[];
  modalities: Modality[];
  locations: LocationView[];
  avgStars: string;
  reviewCount: number;
}

export interface ProfPrivateView {
  userId: string;
  status: ProfStatus;
  yearsExperience: number | null;
  introVideoUrl: string | null;
  aiQcmScore: string | null;
  adminNotes: string | null;
  submittedAt: string | null;
  validatedAt: string | null;
  validatedBy: string | null;
  subjects: SubjectView[];
  modalities: Modality[];
  locations: LocationView[];
  diplomas: unknown[];
  avgStars: string;
  reviewCount: number;
}

export interface StudentSubView {
  userId: string;
  schoolLevel: SchoolLevel | null;
  schoolName: string | null;
  targetExam: string | null;
  goals: string | null;
  interests: string[];
}

export interface ProfileView {
  userId: string;
  role: string;
  displayName: string | null;
  photoUrl: string | null;
  bio: string | null;
  preferredLanguage: string | null;
  notificationPrefs: Record<string, unknown> | null;
  createdAt: string;
  updatedAt: string;
  prof: ProfPrivateView | null;
  student: StudentSubView | null;
}

export interface UpdateProfileRequest {
  displayName?: string;
  bio?: string;
  photoUrl?: string;
  preferredLanguage?: string;
  notificationPrefs?: Record<string, unknown>;
}

export interface ReviewView {
  id: string;
  bookingId: string | null;
  profId: string;
  studentId: string;
  stars: number;
  comment: string | null;
  moderated: boolean;
  createdAt: string;
}

export interface CreateReviewRequest {
  bookingId?: string;
  profId: string;
  stars: number;
  comment?: string;
}

export interface SearchProfsParams {
  subject?: string;
  level?: SchoolLevel;
  modality?: Modality;
  city?: string;
  minStars?: string;
  sort?: string;
  page?: number;
  size?: number;
}

// ---------- API ----------

export const profileApi = {
  getMyProfile(): Promise<ProfileView> {
    return unwrap<ProfileView>(apiClient.get(`${PROFILE_BASE}/me`));
  },

  updateMyProfile(body: UpdateProfileRequest): Promise<ProfileView> {
    return unwrap<ProfileView>(apiClient.put(`${PROFILE_BASE}/me`, body));
  },

  getProfPublic(profId: string): Promise<ProfPublicView> {
    return unwrap<ProfPublicView>(apiClient.get(`${CATALOG_BASE}/profs/${profId}`));
  },

  searchProfs(params: SearchProfsParams = {}): Promise<ProfPublicView[]> {
    return unwrap<ProfPublicView[]>(
      apiClient.get(`${CATALOG_BASE}/profs`, {
        params: {
          subject: params.subject,
          level: params.level,
          modality: params.modality,
          city: params.city,
          minStars: params.minStars,
          sort: params.sort ?? 'updated',
          page: params.page ?? 0,
          size: params.size ?? 20,
        },
      }),
    );
  },

  getProfReviews(
    profId: string,
    page: number = 0,
    size: number = 20,
  ): Promise<ReviewView[]> {
    return unwrap<ReviewView[]>(
      apiClient.get(`${CATALOG_BASE}/profs/${profId}/reviews`, {
        params: { page, size },
      }),
    );
  },

  createReview(body: CreateReviewRequest): Promise<ReviewView> {
    return unwrap<ReviewView>(apiClient.post(`${PROFILE_BASE}/me/reviews`, body));
  },
};

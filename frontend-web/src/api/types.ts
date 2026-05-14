/**
 * TypeScript mirrors of the backend DTOs (auth-service + shared-libs).
 * Keep in sync with `backend/auth-service/src/main/java/.../infrastructure/web/dto/*`
 * and `backend/shared-libs/shared-dto/src/main/java/.../ApiResponse.java`.
 */

export type Role = 'ADMIN' | 'PROF' | 'STUDENT' | 'PARENT';
export type Locale = 'fr' | 'ar' | 'en';
export type OtpChannel = 'SMS' | 'WA';

/** Mirrors {@link UserStatus} on the backend (auth-service). */
export type UserStatus =
  | 'PENDING_OTP'
  | 'ACTIVE'
  | 'SUSPENDED'
  | 'PENDING_PROF_VALIDATION'
  | 'DELETED';

// ----- Requests -----

export interface RegisterRequest {
  phone: string;
  email?: string;
  password: string;
  role: Role;
  locale?: Locale;
}

export interface VerifyOtpRequest {
  userId: string;
  code: string;
}

export interface LoginRequest {
  phoneOrEmail: string;
  password: string;
}

export interface RefreshRequest {
  refreshToken: string;
}

export interface ResendOtpRequest {
  userId: string;
  channel: OtpChannel;
}

export interface PasswordResetRequestDto {
  phoneOrEmail: string;
}

export interface PasswordResetDto {
  resetToken: string;
  newPassword: string;
}

// ----- Responses -----

export interface RegisterResponse {
  userId: string;
  status: string;
  message: string;
}

export interface TokenResponse {
  accessToken: string;
  refreshToken: string;
  expiresInSec: number;
  userId: string;
  role: Role;
}

export interface MeResponse {
  id: string;
  email: string | null;
  phone: string;
  role: Role;
  status: UserStatus;
  locale: Locale;
  emailVerified: boolean;
  phoneVerified: boolean;
  lastLoginAt: string | null;
}

// ----- Envelope -----

export interface ApiError {
  code: string;
  message: string;
  messageKey?: string;
  details?: unknown;
  traceId?: string;
}

export interface PageMeta {
  page: number;
  size: number;
  total: number;
}

export interface ApiResponse<T> {
  data?: T | null;
  meta?: PageMeta | null;
  error?: ApiError | null;
}

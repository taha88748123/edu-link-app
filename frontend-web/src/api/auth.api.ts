import { apiClient, publicRequest, unwrap } from './client';
import type {
  LoginRequest,
  MeResponse,
  PasswordResetDto,
  PasswordResetRequestDto,
  RegisterRequest,
  RegisterResponse,
  ResendOtpRequest,
  TokenResponse,
  VerifyOtpRequest,
} from './types';

const BASE = '/api/v1/auth';

export const authApi = {
  register(body: RegisterRequest): Promise<RegisterResponse> {
    return publicRequest<RegisterResponse>({
      method: 'POST',
      url: `${BASE}/register`,
      data: body,
    });
  },

  verifyOtp(body: VerifyOtpRequest): Promise<TokenResponse> {
    return publicRequest<TokenResponse>({
      method: 'POST',
      url: `${BASE}/verify-otp`,
      data: body,
    });
  },

  login(body: LoginRequest): Promise<TokenResponse> {
    return publicRequest<TokenResponse>({
      method: 'POST',
      url: `${BASE}/login`,
      data: body,
    });
  },

  refresh(refreshToken: string): Promise<TokenResponse> {
    return publicRequest<TokenResponse>({
      method: 'POST',
      url: `${BASE}/refresh`,
      data: { refreshToken },
    });
  },

  resendOtp(body: ResendOtpRequest): Promise<void> {
    return publicRequest<void>({
      method: 'POST',
      url: `${BASE}/resend-otp`,
      data: body,
    });
  },

  requestPasswordReset(body: PasswordResetRequestDto): Promise<void> {
    return publicRequest<void>({
      method: 'POST',
      url: `${BASE}/password/reset-request`,
      data: body,
    });
  },

  confirmPasswordReset(body: PasswordResetDto): Promise<void> {
    return publicRequest<void>({
      method: 'POST',
      url: `${BASE}/password/reset`,
      data: body,
    });
  },

  me(): Promise<MeResponse> {
    return unwrap<MeResponse>(apiClient.get(`${BASE}/me`));
  },

  logout(refreshToken?: string): Promise<void> {
    return unwrap<void>(apiClient.post(`${BASE}/logout`, refreshToken ? { refreshToken } : {}));
  },
};

import { useCallback } from 'react';
import { useMutation } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';

import { authApi } from '@/api/auth.api';
import type {
  LoginRequest,
  RegisterRequest,
  ResendOtpRequest,
  VerifyOtpRequest,
} from '@/api/types';
import { useAuthStore } from './auth.store';

/**
 * High-level auth facade. Wraps the Zustand store + TanStack Query mutations.
 * UI components should depend on this hook rather than calling `authApi` directly,
 * so token side-effects (storage, redirect) are centralised.
 */
export function useAuth() {
  const navigate = useNavigate();
  const accessToken = useAuthStore((s) => s.accessToken);
  const user = useAuthStore((s) => s.user);
  const setTokens = useAuthStore((s) => s.setTokens);
  const setUser = useAuthStore((s) => s.setUser);
  const clear = useAuthStore((s) => s.clear);

  const loginMut = useMutation({
    mutationFn: (body: LoginRequest) => authApi.login(body),
    onSuccess: (tokens) => {
      setTokens(tokens);
    },
  });

  const registerMut = useMutation({
    mutationFn: (body: RegisterRequest) => authApi.register(body),
  });

  const verifyOtpMut = useMutation({
    mutationFn: (body: VerifyOtpRequest) => authApi.verifyOtp(body),
    onSuccess: (tokens) => {
      setTokens(tokens);
    },
  });

  const resendOtpMut = useMutation({
    mutationFn: (body: ResendOtpRequest) => authApi.resendOtp(body),
  });

  const logout = useCallback(async () => {
    const refreshToken = useAuthStore.getState().refreshToken ?? undefined;
    try {
      await authApi.logout(refreshToken);
    } catch {
      /* logout is best-effort; we always clear local state. */
    }
    clear();
    navigate('/login', { replace: true });
  }, [clear, navigate]);

  return {
    user,
    role: user?.role ?? null,
    isAuthenticated: Boolean(accessToken),

    login: loginMut,
    register: registerMut,
    verifyOtp: verifyOtpMut,
    resendOtp: resendOtpMut,

    logout,
    setUser,
  };
}

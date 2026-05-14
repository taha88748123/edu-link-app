import { z } from 'zod';

import type { Role } from '@/api/types';

/**
 * Matches Moroccan phone numbers:
 *   - international form: `+212` + 9 digits where the first is 5/6/7
 *   - local form: `0` + 9 digits where the second is 5/6/7
 */
export const MOROCCAN_PHONE_REGEX = /^(?:\+212|0)[5-7]\d{8}$/;

/** Password: 8-128 chars, at least one letter and one digit. */
export const PASSWORD_REGEX = /^(?=.*[A-Za-z])(?=.*\d).{8,128}$/;

export const ALLOWED_ROLES: Role[] = ['STUDENT', 'PROF', 'PARENT'];

// ------------- Schemas -------------

export const loginSchema = z.object({
  phoneOrEmail: z
    .string()
    .min(1, { message: 'auth.errors.required' })
    .max(255),
  password: z.string().min(1, { message: 'auth.errors.required' }),
});
export type LoginFormValues = z.infer<typeof loginSchema>;

export const registerSchema = z
  .object({
    phone: z
      .string()
      .regex(MOROCCAN_PHONE_REGEX, { message: 'auth.errors.invalid_phone' }),
    email: z
      .union([z.string().email({ message: 'auth.errors.invalid_email' }), z.literal('')])
      .optional()
      .transform((v) => (v === '' ? undefined : v)),
    password: z
      .string()
      .regex(PASSWORD_REGEX, { message: 'auth.errors.weak_password' }),
    confirmPassword: z.string(),
    role: z.enum(['STUDENT', 'PROF', 'PARENT'] as const),
    locale: z.enum(['fr', 'ar', 'en'] as const).optional(),
    acceptTerms: z.literal(true, {
      errorMap: () => ({ message: 'auth.errors.terms_required' }),
    }),
  })
  .refine((v) => v.password === v.confirmPassword, {
    path: ['confirmPassword'],
    message: 'auth.errors.passwords_mismatch',
  });
export type RegisterFormValues = z.infer<typeof registerSchema>;

export const verifyOtpSchema = z.object({
  code: z
    .string()
    .regex(/^\d{4,8}$/, { message: 'auth.errors.invalid_otp' }),
});
export type VerifyOtpFormValues = z.infer<typeof verifyOtpSchema>;

export const passwordResetRequestSchema = z.object({
  phoneOrEmail: z.string().min(1, { message: 'auth.errors.required' }),
});
export type PasswordResetRequestFormValues = z.infer<typeof passwordResetRequestSchema>;

export const passwordResetConfirmSchema = z
  .object({
    token: z.string().min(1, { message: 'auth.errors.required' }),
    newPassword: z.string().regex(PASSWORD_REGEX, { message: 'auth.errors.weak_password' }),
    confirmPassword: z.string(),
  })
  .refine((v) => v.newPassword === v.confirmPassword, {
    path: ['confirmPassword'],
    message: 'auth.errors.passwords_mismatch',
  });
export type PasswordResetConfirmFormValues = z.infer<typeof passwordResetConfirmSchema>;

// ------------- Helpers -------------

/**
 * 0 (empty) → 4 (excellent). Used by the register page strength meter.
 */
export function passwordStrength(pw: string): number {
  if (!pw) return 0;
  let score = 0;
  if (pw.length >= 8) score++;
  if (pw.length >= 12) score++;
  if (/[A-Z]/.test(pw) && /[a-z]/.test(pw)) score++;
  if (/\d/.test(pw)) score++;
  if (/[^A-Za-z0-9]/.test(pw)) score++;
  return Math.min(score, 4);
}

import { useEffect, useRef, useState } from 'react';
import { Navigate, useNavigate, useSearchParams } from 'react-router-dom';
import { useTranslation } from 'react-i18next';

import { Alert, AlertDescription } from '@/components/ui/alert';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';
import { useAuth } from '@/auth/useAuth';
import { useApiError } from '@/hooks/useApiError';
import type { OtpChannel } from '@/api/types';

const OTP_LEN = 6;
const RESEND_COOLDOWN = 30;

export default function VerifyOtpPage() {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const [params] = useSearchParams();
  const userIdParam = params.get('userId');
  const { verifyOtp, resendOtp } = useAuth();
  const renderError = useApiError();

  const [digits, setDigits] = useState<string[]>(() => Array(OTP_LEN).fill(''));
  const [channel, setChannel] = useState<OtpChannel>('SMS');
  const [secondsLeft, setSecondsLeft] = useState(RESEND_COOLDOWN);
  const inputsRef = useRef<Array<HTMLInputElement | null>>([]);

  useEffect(() => {
    if (secondsLeft <= 0) return;
    const t = setInterval(() => setSecondsLeft((s) => Math.max(0, s - 1)), 1000);
    return () => clearInterval(t);
  }, [secondsLeft]);

  if (!userIdParam) {
    return <Navigate to="/register" replace />;
  }
  const userId: string = userIdParam;

  function setDigit(i: number, value: string) {
    const clean = value.replace(/\D/g, '').slice(0, 1);
    setDigits((prev) => {
      const next = [...prev];
      next[i] = clean;
      return next;
    });
    if (clean && i < OTP_LEN - 1) {
      inputsRef.current[i + 1]?.focus();
    }
  }

  function onKeyDown(i: number, e: React.KeyboardEvent<HTMLInputElement>) {
    if (e.key === 'Backspace' && !digits[i] && i > 0) {
      inputsRef.current[i - 1]?.focus();
    }
  }

  function onPaste(e: React.ClipboardEvent<HTMLInputElement>) {
    const pasted = e.clipboardData.getData('text').replace(/\D/g, '').slice(0, OTP_LEN);
    if (!pasted) return;
    e.preventDefault();
    const next = Array(OTP_LEN).fill('');
    for (let i = 0; i < pasted.length; i++) next[i] = pasted[i];
    setDigits(next);
    inputsRef.current[Math.min(pasted.length, OTP_LEN - 1)]?.focus();
  }

  const code = digits.join('');
  const complete = code.length === OTP_LEN;

  function onSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (!complete) return;
    verifyOtp.mutate(
      { userId, code },
      { onSuccess: () => navigate('/home', { replace: true }) },
    );
  }

  function onResend() {
    resendOtp.mutate(
      { userId, channel },
      { onSuccess: () => setSecondsLeft(RESEND_COOLDOWN) },
    );
  }

  return (
    <Card>
      <CardHeader className="text-center">
        <CardTitle>{t('auth.otp.title')}</CardTitle>
        <CardDescription>{t('auth.otp.subtitle')}</CardDescription>
      </CardHeader>
      <CardContent>
        <form className="space-y-4" onSubmit={onSubmit} noValidate>
          <div className="flex justify-center gap-2" dir="ltr">
            {digits.map((d, i) => (
              <input
                key={i}
                ref={(el) => {
                  inputsRef.current[i] = el;
                }}
                value={d}
                onChange={(e) => setDigit(i, e.target.value)}
                onKeyDown={(e) => onKeyDown(i, e)}
                onPaste={onPaste}
                inputMode="numeric"
                autoComplete="one-time-code"
                maxLength={1}
                aria-label={`${t('auth.otp.code')} ${i + 1}`}
                className="h-12 w-10 rounded-md border border-input bg-background text-center text-lg font-semibold focus:outline-none focus:ring-2 focus:ring-ring"
              />
            ))}
          </div>

          {verifyOtp.isError ? (
            <Alert variant="destructive">
              <AlertDescription>{renderError(verifyOtp.error)}</AlertDescription>
            </Alert>
          ) : null}

          <Button type="submit" className="w-full" disabled={!complete || verifyOtp.isPending}>
            {verifyOtp.isPending ? t('common.loading') : t('auth.otp.submit')}
          </Button>

          <div className="space-y-2">
            <label className="text-sm text-muted-foreground">{t('auth.otp.channel')}</label>
            <div className="flex items-center gap-2">
              <Select
                value={channel}
                onValueChange={(v) => setChannel(v as OtpChannel)}
              >
                <SelectTrigger className="w-40">
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="SMS">{t('auth.otp.channel_sms')}</SelectItem>
                  <SelectItem value="WA">{t('auth.otp.channel_wa')}</SelectItem>
                </SelectContent>
              </Select>
              <Button
                type="button"
                variant="outline"
                onClick={onResend}
                disabled={secondsLeft > 0 || resendOtp.isPending}
              >
                {secondsLeft > 0
                  ? t('auth.otp.resend_in', { seconds: secondsLeft })
                  : t('auth.otp.resend')}
              </Button>
            </div>
            {resendOtp.isError ? (
              <Alert variant="destructive">
                <AlertDescription>{renderError(resendOtp.error)}</AlertDescription>
              </Alert>
            ) : null}
          </div>
        </form>
      </CardContent>
    </Card>
  );
}

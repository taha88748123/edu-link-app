import { useEffect } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { useTranslation } from 'react-i18next';

import { Alert, AlertDescription } from '@/components/ui/alert';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import {
  Form,
  FormControl,
  FormField,
  FormItem,
  FormLabel,
  FormMessage,
} from '@/components/ui/form';
import { Input } from '@/components/ui/input';
import { loginSchema, type LoginFormValues } from '@/lib/validation';
import { useAuth } from '@/auth/useAuth';
import { useApiError } from '@/hooks/useApiError';

export default function LoginPage() {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const renderError = useApiError();
  const { login, isAuthenticated } = useAuth();

  const next = searchParams.get('next') || '/home';

  const form = useForm<LoginFormValues>({
    resolver: zodResolver(loginSchema),
    defaultValues: { phoneOrEmail: '', password: '' },
  });

  // If a token is already in storage, skip straight to the dispatcher.
  useEffect(() => {
    if (isAuthenticated) navigate(next, { replace: true });
  }, [isAuthenticated, navigate, next]);

  // Translate inline RHF/Zod errors via the messageKey we use as message.
  function translateMessage(msg?: string): string | undefined {
    if (!msg) return undefined;
    return msg.startsWith('auth.') ? t(msg) : msg;
  }

  function onSubmit(values: LoginFormValues) {
    login.mutate(values, {
      onSuccess: () => navigate(next, { replace: true }),
    });
  }

  return (
    <Card>
      <CardHeader className="text-center">
        <CardTitle>{t('auth.login.title')}</CardTitle>
        <CardDescription>{t('auth.login.subtitle')}</CardDescription>
      </CardHeader>
      <CardContent>
        <Form {...form}>
          <form className="space-y-4" onSubmit={form.handleSubmit(onSubmit)} noValidate>
            <FormField
              control={form.control}
              name="phoneOrEmail"
              render={({ field, fieldState }) => (
                <FormItem>
                  <FormLabel>{t('auth.login.phone_or_email')}</FormLabel>
                  <FormControl>
                    <Input
                      autoComplete="username"
                      data-testid="login-phone-or-email"
                      {...field}
                    />
                  </FormControl>
                  <FormMessage>{translateMessage(fieldState.error?.message)}</FormMessage>
                </FormItem>
              )}
            />
            <FormField
              control={form.control}
              name="password"
              render={({ field, fieldState }) => (
                <FormItem>
                  <FormLabel>{t('auth.login.password')}</FormLabel>
                  <FormControl>
                    <Input
                      type="password"
                      autoComplete="current-password"
                      data-testid="login-password"
                      {...field}
                    />
                  </FormControl>
                  <FormMessage>{translateMessage(fieldState.error?.message)}</FormMessage>
                </FormItem>
              )}
            />

            {login.isError ? (
              <Alert variant="destructive">
                <AlertDescription>{renderError(login.error)}</AlertDescription>
              </Alert>
            ) : null}

            <Button
              type="submit"
              className="w-full"
              disabled={login.isPending}
              data-testid="login-submit"
            >
              {login.isPending ? t('common.loading') : t('auth.login.submit')}
            </Button>

            <div className="flex flex-col items-center gap-2 text-sm">
              <Link to="/auth/forgot-password" className="text-primary hover:underline">
                {t('auth.login.forgot')}
              </Link>
              <span className="text-muted-foreground">
                {t('auth.login.no_account')}{' '}
                <Link to="/register" className="text-primary hover:underline">
                  {t('auth.login.register_link')}
                </Link>
              </span>
            </div>
          </form>
        </Form>
      </CardContent>
    </Card>
  );
}

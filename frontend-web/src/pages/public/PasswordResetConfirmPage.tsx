import { useEffect } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { useMutation } from '@tanstack/react-query';
import { useTranslation } from 'react-i18next';

import { authApi } from '@/api/auth.api';
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
import {
  passwordResetConfirmSchema,
  type PasswordResetConfirmFormValues,
} from '@/lib/validation';
import { useApiError } from '@/hooks/useApiError';

export default function PasswordResetConfirmPage() {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const [params] = useSearchParams();
  const renderError = useApiError();

  const form = useForm<PasswordResetConfirmFormValues>({
    resolver: zodResolver(passwordResetConfirmSchema),
    defaultValues: {
      token: params.get('token') ?? '',
      newPassword: '',
      confirmPassword: '',
    },
  });

  useEffect(() => {
    const t0 = params.get('token');
    if (t0) form.setValue('token', t0);
  }, [params, form]);

  const mutation = useMutation({
    mutationFn: (values: PasswordResetConfirmFormValues) =>
      authApi.confirmPasswordReset({
        resetToken: values.token,
        newPassword: values.newPassword,
      }),
    onSuccess: () => {
      setTimeout(() => navigate('/login', { replace: true }), 1500);
    },
  });

  function translateMessage(msg?: string): string | undefined {
    if (!msg) return undefined;
    return msg.startsWith('auth.') ? t(msg) : msg;
  }

  return (
    <Card>
      <CardHeader className="text-center">
        <CardTitle>{t('auth.reset_confirm.title')}</CardTitle>
        <CardDescription>{t('auth.reset_confirm.subtitle')}</CardDescription>
      </CardHeader>
      <CardContent>
        <Form {...form}>
          <form
            className="space-y-4"
            onSubmit={form.handleSubmit((v) => mutation.mutate(v))}
            noValidate
          >
            <FormField
              control={form.control}
              name="token"
              render={({ field, fieldState }) => (
                <FormItem>
                  <FormLabel>{t('auth.reset_confirm.token')}</FormLabel>
                  <FormControl>
                    <Input {...field} />
                  </FormControl>
                  <FormMessage>{translateMessage(fieldState.error?.message)}</FormMessage>
                </FormItem>
              )}
            />
            <FormField
              control={form.control}
              name="newPassword"
              render={({ field, fieldState }) => (
                <FormItem>
                  <FormLabel>{t('auth.reset_confirm.new_password')}</FormLabel>
                  <FormControl>
                    <Input type="password" autoComplete="new-password" {...field} />
                  </FormControl>
                  <FormMessage>{translateMessage(fieldState.error?.message)}</FormMessage>
                </FormItem>
              )}
            />
            <FormField
              control={form.control}
              name="confirmPassword"
              render={({ field, fieldState }) => (
                <FormItem>
                  <FormLabel>{t('auth.reset_confirm.confirm_password')}</FormLabel>
                  <FormControl>
                    <Input type="password" autoComplete="new-password" {...field} />
                  </FormControl>
                  <FormMessage>{translateMessage(fieldState.error?.message)}</FormMessage>
                </FormItem>
              )}
            />

            {mutation.isSuccess ? (
              <Alert>
                <AlertDescription>{t('auth.reset_confirm.success')}</AlertDescription>
              </Alert>
            ) : null}
            {mutation.isError ? (
              <Alert variant="destructive">
                <AlertDescription>{renderError(mutation.error)}</AlertDescription>
              </Alert>
            ) : null}

            <Button type="submit" className="w-full" disabled={mutation.isPending}>
              {mutation.isPending ? t('common.loading') : t('auth.reset_confirm.submit')}
            </Button>
            <div className="text-center text-sm">
              <Link to="/login" className="text-primary hover:underline">
                {t('auth.reset_request.back_to_login')}
              </Link>
            </div>
          </form>
        </Form>
      </CardContent>
    </Card>
  );
}

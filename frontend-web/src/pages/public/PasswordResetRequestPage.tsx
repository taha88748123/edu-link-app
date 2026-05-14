import { Link } from 'react-router-dom';
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
  passwordResetRequestSchema,
  type PasswordResetRequestFormValues,
} from '@/lib/validation';
import { useApiError } from '@/hooks/useApiError';

export default function PasswordResetRequestPage() {
  const { t } = useTranslation();
  const renderError = useApiError();

  const form = useForm<PasswordResetRequestFormValues>({
    resolver: zodResolver(passwordResetRequestSchema),
    defaultValues: { phoneOrEmail: '' },
  });

  const mutation = useMutation({
    mutationFn: (values: PasswordResetRequestFormValues) =>
      authApi.requestPasswordReset({ phoneOrEmail: values.phoneOrEmail }),
  });

  function translateMessage(msg?: string): string | undefined {
    if (!msg) return undefined;
    return msg.startsWith('auth.') ? t(msg) : msg;
  }

  return (
    <Card>
      <CardHeader className="text-center">
        <CardTitle>{t('auth.reset_request.title')}</CardTitle>
        <CardDescription>{t('auth.reset_request.subtitle')}</CardDescription>
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
              name="phoneOrEmail"
              render={({ field, fieldState }) => (
                <FormItem>
                  <FormLabel>{t('auth.reset_request.phone_or_email')}</FormLabel>
                  <FormControl>
                    <Input autoComplete="username" {...field} />
                  </FormControl>
                  <FormMessage>{translateMessage(fieldState.error?.message)}</FormMessage>
                </FormItem>
              )}
            />

            {mutation.isSuccess ? (
              <Alert>
                <AlertDescription>{t('auth.reset_request.success')}</AlertDescription>
              </Alert>
            ) : null}
            {mutation.isError ? (
              <Alert variant="destructive">
                <AlertDescription>{renderError(mutation.error)}</AlertDescription>
              </Alert>
            ) : null}

            <Button type="submit" className="w-full" disabled={mutation.isPending}>
              {mutation.isPending ? t('common.loading') : t('auth.reset_request.submit')}
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

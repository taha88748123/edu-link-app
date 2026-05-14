import { Link, useNavigate } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { useTranslation } from 'react-i18next';

import { Alert, AlertDescription } from '@/components/ui/alert';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import {
  Form,
  FormControl,
  FormDescription,
  FormField,
  FormItem,
  FormLabel,
  FormMessage,
} from '@/components/ui/form';
import { Input } from '@/components/ui/input';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';
import { useAuth } from '@/auth/useAuth';
import { useApiError } from '@/hooks/useApiError';
import {
  passwordStrength,
  registerSchema,
  type RegisterFormValues,
} from '@/lib/validation';
import type { Locale, Role } from '@/api/types';
import { SUPPORTED_LOCALES } from '@/i18n/config';

const STRENGTH_LABELS = ['weak', 'weak', 'fair', 'good', 'strong'] as const;
const STRENGTH_COLORS = [
  'bg-muted',
  'bg-destructive',
  'bg-amber-500',
  'bg-emerald-500',
  'bg-emerald-600',
];

export default function RegisterPage() {
  const { t, i18n } = useTranslation();
  const navigate = useNavigate();
  const { register: registerMut } = useAuth();
  const renderError = useApiError();

  const form = useForm<RegisterFormValues>({
    resolver: zodResolver(registerSchema),
    defaultValues: {
      phone: '',
      email: '',
      password: '',
      confirmPassword: '',
      role: 'STUDENT',
      locale: (SUPPORTED_LOCALES as readonly string[]).includes(i18n.language)
        ? (i18n.language as Locale)
        : 'fr',
      acceptTerms: false as unknown as true,
    },
  });

  function translateMessage(msg?: string): string | undefined {
    if (!msg) return undefined;
    return msg.startsWith('auth.') ? t(msg) : msg;
  }

  const role = form.watch('role') as Role;
  const password = form.watch('password');
  const strength = passwordStrength(password ?? '');

  function onSubmit(values: RegisterFormValues) {
    registerMut.mutate(
      {
        phone: values.phone,
        email: values.email,
        password: values.password,
        role: values.role,
        locale: values.locale,
      },
      {
        onSuccess: (resp) => {
          navigate(`/auth/verify-otp?userId=${encodeURIComponent(resp.userId)}`, {
            replace: true,
          });
        },
      },
    );
  }

  return (
    <Card>
      <CardHeader className="text-center">
        <CardTitle>{t('auth.register.title')}</CardTitle>
        <CardDescription>{t('auth.register.subtitle')}</CardDescription>
      </CardHeader>
      <CardContent>
        <Form {...form}>
          <form className="space-y-4" onSubmit={form.handleSubmit(onSubmit)} noValidate>
            <FormField
              control={form.control}
              name="phone"
              render={({ field, fieldState }) => (
                <FormItem>
                  <FormLabel>{t('auth.register.phone')}</FormLabel>
                  <FormControl>
                    <Input
                      placeholder="+212XXXXXXXXX"
                      autoComplete="tel"
                      inputMode="tel"
                      {...field}
                    />
                  </FormControl>
                  <FormDescription>{t('auth.register.phone_hint')}</FormDescription>
                  <FormMessage>{translateMessage(fieldState.error?.message)}</FormMessage>
                </FormItem>
              )}
            />

            <FormField
              control={form.control}
              name="email"
              render={({ field, fieldState }) => (
                <FormItem>
                  <FormLabel>{t('auth.register.email')}</FormLabel>
                  <FormControl>
                    <Input type="email" autoComplete="email" {...field} value={field.value ?? ''} />
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
                  <FormLabel>{t('auth.register.password')}</FormLabel>
                  <FormControl>
                    <Input type="password" autoComplete="new-password" {...field} />
                  </FormControl>
                  <div className="flex h-1.5 gap-1">
                    {[0, 1, 2, 3].map((i) => (
                      <div
                        key={i}
                        className={`h-full flex-1 rounded ${
                          strength > i ? STRENGTH_COLORS[strength] : 'bg-muted'
                        }`}
                      />
                    ))}
                  </div>
                  <FormDescription>
                    {t('auth.register.password_hint')}{' '}
                    {strength > 0 ? `(${t(`auth.register.strength.${STRENGTH_LABELS[strength]}`)})` : ''}
                  </FormDescription>
                  <FormMessage>{translateMessage(fieldState.error?.message)}</FormMessage>
                </FormItem>
              )}
            />

            <FormField
              control={form.control}
              name="confirmPassword"
              render={({ field, fieldState }) => (
                <FormItem>
                  <FormLabel>{t('auth.register.confirm_password')}</FormLabel>
                  <FormControl>
                    <Input type="password" autoComplete="new-password" {...field} />
                  </FormControl>
                  <FormMessage>{translateMessage(fieldState.error?.message)}</FormMessage>
                </FormItem>
              )}
            />

            <FormField
              control={form.control}
              name="role"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>{t('auth.register.role')}</FormLabel>
                  <Select onValueChange={field.onChange} value={field.value}>
                    <FormControl>
                      <SelectTrigger>
                        <SelectValue />
                      </SelectTrigger>
                    </FormControl>
                    <SelectContent>
                      <SelectItem value="STUDENT">{t('roles.STUDENT')}</SelectItem>
                      <SelectItem value="PROF">{t('roles.PROF')}</SelectItem>
                      <SelectItem value="PARENT">{t('roles.PARENT')}</SelectItem>
                    </SelectContent>
                  </Select>
                </FormItem>
              )}
            />

            {role === 'PROF' ? (
              <Alert>
                <AlertDescription>{t('auth.register.prof_notice')}</AlertDescription>
              </Alert>
            ) : null}

            <FormField
              control={form.control}
              name="acceptTerms"
              render={({ field, fieldState }) => (
                <FormItem className="flex flex-row items-start gap-2 space-y-0">
                  <FormControl>
                    <input
                      id="accept-terms"
                      type="checkbox"
                      checked={Boolean(field.value)}
                      onChange={(e) => field.onChange(e.target.checked)}
                      className="mt-1 h-4 w-4 rounded border-input"
                    />
                  </FormControl>
                  <div className="space-y-1 leading-none">
                    <FormLabel htmlFor="accept-terms" className="text-sm font-normal">
                      {t('auth.register.accept_terms')}
                    </FormLabel>
                    <FormMessage>{translateMessage(fieldState.error?.message)}</FormMessage>
                  </div>
                </FormItem>
              )}
            />

            {registerMut.isError ? (
              <Alert variant="destructive">
                <AlertDescription>{renderError(registerMut.error)}</AlertDescription>
              </Alert>
            ) : null}

            <Button type="submit" className="w-full" disabled={registerMut.isPending}>
              {registerMut.isPending ? t('common.loading') : t('auth.register.submit')}
            </Button>

            <p className="text-center text-sm text-muted-foreground">
              {t('auth.register.have_account')}{' '}
              <Link to="/login" className="text-primary hover:underline">
                {t('auth.register.login_link')}
              </Link>
            </p>
          </form>
        </Form>
      </CardContent>
    </Card>
  );
}

import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { Search } from 'lucide-react';

import {
  MODALITIES,
  SCHOOL_LEVELS,
  type Modality,
  type SchoolLevel,
} from '@/api/enums';
import type { MatchSuggestion } from '@/api/matching.api';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
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
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';
import { Skeleton } from '@/components/ui/skeleton';
import { MatchCard } from '@/components/matching/MatchCard';
import { useApiError } from '@/hooks/useApiError';
import { useMatchingMutation } from '@/hooks/queries/useMatching';
import { toast } from '@/components/ui/use-toast';

const discoverSchema = z.object({
  subjectCode: z.string().min(1, { message: 'auth.errors.required' }),
  level: z.enum(SCHOOL_LEVELS as [SchoolLevel, ...SchoolLevel[]]),
  modality: z.enum(MODALITIES as [Modality, ...Modality[]]),
  budgetMaxMad: z.string().optional(),
  city: z.string().optional(),
  dayOfWeek: z.string().optional(),
  durationMinutes: z.string().optional(),
});

type DiscoverFormValues = z.infer<typeof discoverSchema>;

export default function DiscoverPage() {
  const { t } = useTranslation();
  const apiError = useApiError();
  const matching = useMatchingMutation();
  const [results, setResults] = useState<MatchSuggestion[] | null>(null);

  const form = useForm<DiscoverFormValues>({
    resolver: zodResolver(discoverSchema),
    defaultValues: {
      subjectCode: '',
      level: 'LYCEE',
      modality: 'ONLINE',
      budgetMaxMad: '',
      city: '',
      dayOfWeek: '',
      durationMinutes: '60',
    },
  });

  const onSubmit = form.handleSubmit(async (values) => {
    try {
      const resp = await matching.mutateAsync({
        subjectCode: values.subjectCode,
        level: values.level,
        modality: values.modality,
        budgetMaxMad: values.budgetMaxMad ? values.budgetMaxMad : null,
        city: values.city || null,
        dayOfWeek: values.dayOfWeek ? Number(values.dayOfWeek) : null,
        durationMinutes: values.durationMinutes ? Number(values.durationMinutes) : null,
        limit: 20,
      });
      setResults(resp.results);
    } catch (err) {
      toast({ variant: 'destructive', description: apiError(err) });
    }
  });

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-semibold">{t('discover.title')}</h1>
        <p className="text-sm text-muted-foreground">{t('discover.subtitle')}</p>
      </div>

      <Card>
        <CardHeader>
          <CardTitle className="text-lg">{t('common.search')}</CardTitle>
        </CardHeader>
        <CardContent>
          <Form {...form}>
            <form onSubmit={onSubmit} className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
              <FormField
                control={form.control}
                name="subjectCode"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>{t('discover.form.subject')}</FormLabel>
                    <FormControl>
                      <Input
                        placeholder={t('discover.form.subject_placeholder') ?? ''}
                        data-testid="discover-subject-select"
                        {...field}
                      />
                    </FormControl>
                    <FormMessage>
                      {form.formState.errors.subjectCode?.message
                        ? t(form.formState.errors.subjectCode.message)
                        : null}
                    </FormMessage>
                  </FormItem>
                )}
              />
              <FormField
                control={form.control}
                name="level"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>{t('discover.form.level')}</FormLabel>
                    <Select value={field.value} onValueChange={field.onChange}>
                      <FormControl>
                        <SelectTrigger data-testid="discover-level-select">
                          <SelectValue />
                        </SelectTrigger>
                      </FormControl>
                      <SelectContent>
                        {SCHOOL_LEVELS.map((l) => (
                          <SelectItem key={l} value={l}>
                            {t(`level.${l}`)}
                          </SelectItem>
                        ))}
                      </SelectContent>
                    </Select>
                  </FormItem>
                )}
              />
              <FormField
                control={form.control}
                name="modality"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>{t('discover.form.modality')}</FormLabel>
                    <Select value={field.value} onValueChange={field.onChange}>
                      <FormControl>
                        <SelectTrigger data-testid="discover-modality-select">
                          <SelectValue />
                        </SelectTrigger>
                      </FormControl>
                      <SelectContent>
                        {MODALITIES.map((m) => (
                          <SelectItem key={m} value={m}>
                            {t(`modality.${m}`)}
                          </SelectItem>
                        ))}
                      </SelectContent>
                    </Select>
                  </FormItem>
                )}
              />
              <FormField
                control={form.control}
                name="budgetMaxMad"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>
                      {t('discover.form.budget')} {t('common.optional')}
                    </FormLabel>
                    <FormControl>
                      <Input type="number" inputMode="decimal" placeholder="200" {...field} />
                    </FormControl>
                  </FormItem>
                )}
              />
              <FormField
                control={form.control}
                name="city"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>
                      {t('discover.form.city')} {t('common.optional')}
                    </FormLabel>
                    <FormControl>
                      <Input placeholder="Casablanca" {...field} />
                    </FormControl>
                  </FormItem>
                )}
              />
              <FormField
                control={form.control}
                name="dayOfWeek"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>
                      {t('discover.form.day')} {t('common.optional')}
                    </FormLabel>
                    <Select value={field.value ?? ''} onValueChange={field.onChange}>
                      <FormControl>
                        <SelectTrigger>
                          <SelectValue placeholder={t('common.all') ?? ''} />
                        </SelectTrigger>
                      </FormControl>
                      <SelectContent>
                        {[0, 1, 2, 3, 4, 5, 6].map((d) => (
                          <SelectItem key={d} value={String(d)}>
                            {t(`weekday.${d}`)}
                          </SelectItem>
                        ))}
                      </SelectContent>
                    </Select>
                  </FormItem>
                )}
              />

              <div className="sm:col-span-2 lg:col-span-3">
                <Button
                  type="submit"
                  disabled={matching.isPending}
                  className="gap-2"
                  data-testid="discover-search-submit"
                >
                  <Search className="h-4 w-4" />
                  {t('discover.form.submit')}
                </Button>
              </div>
            </form>
          </Form>
        </CardContent>
      </Card>

      {/* Results */}
      {matching.isPending && (
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {[0, 1, 2, 3, 4, 5].map((i) => (
            <Skeleton key={i} className="h-40 w-full" />
          ))}
        </div>
      )}

      {results !== null && !matching.isPending && (
        <section className="space-y-4">
          <div className="flex items-center justify-between">
            <h2 className="text-lg font-semibold">{t('discover.results_title')}</h2>
            <span className="text-sm text-muted-foreground">
              {t('discover.match_count', { count: results.length })}
            </span>
          </div>
          {results.length === 0 ? (
            <Card>
              <CardContent className="py-10 text-center text-sm text-muted-foreground">
                {t('discover.empty')}
              </CardContent>
            </Card>
          ) : (
            <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
              {results.map((r) => (
                <MatchCard key={r.profId} match={r} />
              ))}
            </div>
          )}
        </section>
      )}
    </div>
  );
}

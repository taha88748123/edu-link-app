import { useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { useTranslation } from 'react-i18next';

import type { Modality, SchoolLevel } from '@/api/enums';
import { MODALITIES, SCHOOL_LEVELS } from '@/api/enums';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';
import { Skeleton } from '@/components/ui/skeleton';
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table';
import { ProfileSummary } from '@/components/profile/ProfileSummary';
import { useProfPublic } from '@/hooks/queries/useProfile';
import { useProfPacks, useProfRates } from '@/hooks/queries/usePricing';
import { formatMoneyMad } from '@/lib/format';

export default function ProfPublicPage() {
  const { profId = '' } = useParams<{ profId: string }>();
  const { t, i18n } = useTranslation();

  const profQuery = useProfPublic(profId);

  const [subjectCode, setSubjectCode] = useState<string>('');
  const [level, setLevel] = useState<SchoolLevel | ''>('');
  const [modality, setModality] = useState<Modality | ''>('');

  const filters = {
    profId,
    subjectCode: subjectCode || undefined,
    level: (level || undefined) as SchoolLevel | undefined,
    modality: (modality || undefined) as Modality | undefined,
  };
  const ratesQuery = useProfRates(filters);
  const packsQuery = useProfPacks(filters);

  if (profQuery.isLoading) {
    return <Skeleton className="h-64 w-full" />;
  }
  if (!profQuery.data) {
    return <p className="text-sm text-muted-foreground">{t('errors.PROFILE_NOT_FOUND')}</p>;
  }

  const prof = profQuery.data;

  return (
    <div className="space-y-6">
      <ProfileSummary prof={prof} />

      <div className="flex justify-end">
        <Button asChild>
          <Link to={`/student/profs/${prof.userId}/book`}>{t('profile.book_cta')}</Link>
        </Button>
      </div>

      <Card>
        <CardHeader className="space-y-2">
          <CardTitle className="text-lg">{t('profile.rates_title')}</CardTitle>
          <div className="grid gap-2 sm:grid-cols-3">
            <input
              className="h-9 rounded-md border bg-background px-3 text-sm"
              placeholder={t('discover.form.subject_placeholder') ?? ''}
              value={subjectCode}
              onChange={(e) => setSubjectCode(e.target.value)}
            />
            <Select value={level || 'all'} onValueChange={(v) => setLevel(v === 'all' ? '' : (v as SchoolLevel))}>
              <SelectTrigger>
                <SelectValue placeholder={t('common.all') ?? ''} />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="all">{t('common.all')}</SelectItem>
                {SCHOOL_LEVELS.map((l) => (
                  <SelectItem key={l} value={l}>
                    {t(`level.${l}`)}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
            <Select
              value={modality || 'all'}
              onValueChange={(v) => setModality(v === 'all' ? '' : (v as Modality))}
            >
              <SelectTrigger>
                <SelectValue placeholder={t('common.all') ?? ''} />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="all">{t('common.all')}</SelectItem>
                {MODALITIES.map((m) => (
                  <SelectItem key={m} value={m}>
                    {t(`modality.${m}`)}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>
        </CardHeader>
        <CardContent>
          {ratesQuery.isLoading ? (
            <Skeleton className="h-32 w-full" />
          ) : (ratesQuery.data ?? []).length === 0 ? (
            <p className="text-sm text-muted-foreground">{t('profile.no_rates')}</p>
          ) : (
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>{t('pricing.rate.subject')}</TableHead>
                  <TableHead>{t('pricing.rate.level')}</TableHead>
                  <TableHead>{t('pricing.rate.modality')}</TableHead>
                  <TableHead>{t('pricing.rate.duration')}</TableHead>
                  <TableHead className="text-end">{t('pricing.rate.amount')}</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {(ratesQuery.data ?? []).map((r) => (
                  <TableRow key={r.id}>
                    <TableCell>{r.subjectCode}</TableCell>
                    <TableCell>{t(`level.${r.level}`)}</TableCell>
                    <TableCell>{t(`modality.${r.modality}`)}</TableCell>
                    <TableCell>{r.durationMinutes} min</TableCell>
                    <TableCell className="text-end font-medium">
                      {formatMoneyMad(r.amountMinor, i18n.language)}
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          )}
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle className="text-lg">{t('profile.packs_title')}</CardTitle>
        </CardHeader>
        <CardContent>
          {packsQuery.isLoading ? (
            <Skeleton className="h-32 w-full" />
          ) : (packsQuery.data ?? []).length === 0 ? (
            <p className="text-sm text-muted-foreground">{t('profile.no_packs')}</p>
          ) : (
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>{t('pricing.rate.subject')}</TableHead>
                  <TableHead>{t('pricing.rate.level')}</TableHead>
                  <TableHead>{t('pricing.rate.modality')}</TableHead>
                  <TableHead>{t('pricing.pack.sessions')}</TableHead>
                  <TableHead>{t('pricing.pack.discount')}</TableHead>
                  <TableHead className="text-end">{t('pricing.pack.total')}</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {(packsQuery.data ?? []).map((p) => (
                  <TableRow key={p.id}>
                    <TableCell>{p.subjectCode}</TableCell>
                    <TableCell>{t(`level.${p.level}`)}</TableCell>
                    <TableCell>{t(`modality.${p.modality}`)}</TableCell>
                    <TableCell>{p.sessionsCount}</TableCell>
                    <TableCell>{String(p.discountPct)}%</TableCell>
                    <TableCell className="text-end font-medium">
                      {formatMoneyMad(p.totalAmountMinor, i18n.language)}
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          )}
        </CardContent>
      </Card>
    </div>
  );
}

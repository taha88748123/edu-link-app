import { useMemo, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { useTranslation } from 'react-i18next';

import {
  MODALITIES,
  SCHOOL_LEVELS,
  type Modality,
  type SchoolLevel,
} from '@/api/enums';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';
import { Separator } from '@/components/ui/separator';
import { Skeleton } from '@/components/ui/skeleton';
import { PriceBreakdown } from '@/components/booking/PriceBreakdown';
import { useCreateBooking } from '@/hooks/queries/useBookings';
import { useProfSlots } from '@/hooks/queries/useCalendar';
import { useQuote } from '@/hooks/queries/usePricing';
import { useApiError } from '@/hooks/useApiError';
import { useProfPublic } from '@/hooks/queries/useProfile';
import { formatDateTime, formatMoneyMad } from '@/lib/format';
import { toast } from '@/components/ui/use-toast';

type Step = 1 | 2 | 3;

export default function BookingCreatePage() {
  const { profId = '' } = useParams<{ profId: string }>();
  const navigate = useNavigate();
  const { t, i18n } = useTranslation();
  const apiError = useApiError();

  const [step, setStep] = useState<Step>(1);
  const [subjectCode, setSubjectCode] = useState('');
  const [level, setLevel] = useState<SchoolLevel>('LYCEE');
  const [modality, setModality] = useState<Modality>('ONLINE');
  const [durationMinutes, setDurationMinutes] = useState<number>(60);
  const [slotId, setSlotId] = useState<string | null>(null);

  const profQuery = useProfPublic(profId);

  const slotRange = useMemo(() => {
    const from = new Date();
    const to = new Date();
    to.setDate(to.getDate() + 30);
    return { from: from.toISOString(), to: to.toISOString() };
  }, []);

  const slotsQuery = useProfSlots(
    {
      profId,
      from: slotRange.from,
      to: slotRange.to,
      modality,
      size: 100,
    },
    step === 2,
  );

  const quoteBody = step >= 1 && subjectCode
    ? {
        profId,
        subjectCode,
        level,
        modality,
        durationMinutes,
      }
    : null;
  const quoteQuery = useQuote(quoteBody);

  const createMut = useCreateBooking();

  const selectedSlot = slotsQuery.data?.find((s) => s.id === slotId);

  function next() {
    if (step === 1) {
      if (!subjectCode) return;
      setStep(2);
      return;
    }
    if (step === 2) {
      if (!slotId) return;
      setStep(3);
    }
  }
  function back() {
    if (step === 2) setStep(1);
    if (step === 3) setStep(2);
  }

  async function submit() {
    if (!slotId) return;
    try {
      const created = await createMut.mutateAsync({
        profId,
        slotId,
        subjectCode,
        level,
        modality,
        durationMinutes,
      });
      toast({ description: t('booking.create.success') });
      navigate(`/student/bookings/${created.id}`);
    } catch (err) {
      toast({ variant: 'destructive', description: apiError(err) });
    }
  }

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-semibold">{t('booking.create.title')}</h1>
        {profQuery.data && (
          <p className="text-sm text-muted-foreground">
            {profQuery.data.displayName}
          </p>
        )}
      </div>

      <Stepper step={step} />

      {step === 1 && (
        <Card>
          <CardHeader>
            <CardTitle className="text-lg">{t('booking.create.step1')}</CardTitle>
          </CardHeader>
          <CardContent className="space-y-4">
            <div className="grid gap-4 sm:grid-cols-2">
              <div className="space-y-1.5">
                <Label>{t('booking.create.subject')}</Label>
                <Input
                  value={subjectCode}
                  onChange={(e) => setSubjectCode(e.target.value)}
                  placeholder={t('discover.form.subject_placeholder') ?? ''}
                />
              </div>
              <div className="space-y-1.5">
                <Label>{t('booking.create.level')}</Label>
                <Select value={level} onValueChange={(v) => setLevel(v as SchoolLevel)}>
                  <SelectTrigger>
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    {SCHOOL_LEVELS.map((l) => (
                      <SelectItem key={l} value={l}>
                        {t(`level.${l}`)}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>
              <div className="space-y-1.5">
                <Label>{t('booking.create.modality')}</Label>
                <Select value={modality} onValueChange={(v) => setModality(v as Modality)}>
                  <SelectTrigger>
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    {MODALITIES.map((m) => (
                      <SelectItem key={m} value={m}>
                        {t(`modality.${m}`)}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>
              <div className="space-y-1.5">
                <Label>{t('booking.create.duration')}</Label>
                <Input
                  type="number"
                  min={15}
                  max={240}
                  step={15}
                  value={durationMinutes}
                  onChange={(e) => setDurationMinutes(Number(e.target.value))}
                />
              </div>
            </div>

            <Separator />

            <div>
              <h3 className="mb-2 text-sm font-semibold">{t('booking.price_breakdown.title')}</h3>
              {!quoteBody && (
                <p className="text-sm text-muted-foreground">{t('booking.create.quote')}</p>
              )}
              {quoteBody && quoteQuery.isLoading && <Skeleton className="h-20 w-full" />}
              {quoteQuery.data && (
                <PriceBreakdown
                  baseAmountMinor={quoteQuery.data.baseAmountMinor}
                  platformFeeMinor={quoteQuery.data.platformFeeMinor}
                  totalAmountMinor={quoteQuery.data.totalAmountMinor}
                />
              )}
            </div>

            <div className="flex justify-end">
              <Button onClick={next} disabled={!subjectCode}>
                {t('common.next')}
              </Button>
            </div>
          </CardContent>
        </Card>
      )}

      {step === 2 && (
        <Card>
          <CardHeader>
            <CardTitle className="text-lg">{t('booking.create.step2')}</CardTitle>
          </CardHeader>
          <CardContent>
            {slotsQuery.isLoading ? (
              <Skeleton className="h-40 w-full" />
            ) : (slotsQuery.data ?? []).length === 0 ? (
              <p className="text-sm text-muted-foreground">{t('booking.create.no_slots')}</p>
            ) : (
              <ul className="grid gap-2 sm:grid-cols-2 lg:grid-cols-3">
                {slotsQuery.data?.map((s) => (
                  <li key={s.id}>
                    <button
                      type="button"
                      onClick={() => setSlotId(s.id)}
                      className={`w-full rounded-md border p-3 text-start text-sm transition ${
                        slotId === s.id
                          ? 'border-primary bg-primary/10'
                          : 'hover:bg-accent hover:text-accent-foreground'
                      }`}
                    >
                      <div className="font-medium">
                        {formatDateTime(s.startAt, i18n.language)}
                      </div>
                      <div className="text-xs text-muted-foreground">
                        {t(`modality.${s.modality}`)}
                      </div>
                    </button>
                  </li>
                ))}
              </ul>
            )}

            <div className="mt-4 flex justify-between">
              <Button variant="outline" onClick={back}>
                {t('common.back')}
              </Button>
              <Button onClick={next} disabled={!slotId}>
                {t('common.next')}
              </Button>
            </div>
          </CardContent>
        </Card>
      )}

      {step === 3 && (
        <Card>
          <CardHeader>
            <CardTitle className="text-lg">{t('booking.create.step3')}</CardTitle>
          </CardHeader>
          <CardContent className="space-y-4">
            <dl className="grid gap-2 text-sm">
              <Row label={t('booking.create.subject')} value={`${subjectCode} · ${t(`level.${level}`)}`} />
              <Row label={t('booking.create.modality')} value={t(`modality.${modality}`)} />
              <Row label={t('booking.create.duration')} value={`${durationMinutes} min`} />
              {selectedSlot && (
                <Row
                  label={t('booking.create.selected_slot')}
                  value={formatDateTime(selectedSlot.startAt, i18n.language)}
                />
              )}
            </dl>
            <Separator />
            {quoteQuery.data && (
              <>
                <h3 className="text-sm font-semibold">{t('booking.price_breakdown.title')}</h3>
                <PriceBreakdown
                  baseAmountMinor={quoteQuery.data.baseAmountMinor}
                  platformFeeMinor={quoteQuery.data.platformFeeMinor}
                  totalAmountMinor={quoteQuery.data.totalAmountMinor}
                />
                <p className="text-xs text-muted-foreground">
                  {formatMoneyMad(quoteQuery.data.totalAmountMinor, i18n.language)}
                </p>
              </>
            )}
            <div className="flex justify-between">
              <Button variant="outline" onClick={back}>
                {t('common.back')}
              </Button>
              <Button onClick={submit} disabled={createMut.isPending}>
                {t('booking.create.submit')}
              </Button>
            </div>
          </CardContent>
        </Card>
      )}
    </div>
  );
}

function Row({ label, value }: { label: string; value: string }) {
  return (
    <div className="flex items-baseline justify-between">
      <dt className="text-muted-foreground">{label}</dt>
      <dd className="font-medium">{value}</dd>
    </div>
  );
}

function Stepper({ step }: { step: Step }) {
  const { t } = useTranslation();
  const labels = [
    t('booking.create.step1'),
    t('booking.create.step2'),
    t('booking.create.step3'),
  ];
  return (
    <ol className="flex items-center gap-2 text-xs">
      {labels.map((label, i) => {
        const n = (i + 1) as Step;
        const active = n === step;
        const done = n < step;
        return (
          <li
            key={label}
            className={`flex items-center gap-2 rounded-full border px-3 py-1 ${
              active
                ? 'border-primary bg-primary/10 text-primary'
                : done
                  ? 'border-emerald-500 text-emerald-700 dark:text-emerald-300'
                  : 'text-muted-foreground'
            }`}
          >
            <span className="font-semibold">{n}.</span>
            {label}
          </li>
        );
      })}
    </ol>
  );
}

import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Plus, Trash2 } from 'lucide-react';

import {
  MODALITIES,
  SCHOOL_LEVELS,
  type Modality,
  type SchoolLevel,
} from '@/api/enums';
import type {
  CreatePackRequest,
  CreateRateRequest,
  PackView,
  RateView,
} from '@/api/pricing.api';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import {
  Dialog,
  DialogContent,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
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
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { toast } from '@/components/ui/use-toast';
import {
  useCreatePack,
  useCreateRate,
  useDeletePack,
  useDeleteRate,
  useMyPricing,
} from '@/hooks/queries/usePricing';
import { useApiError } from '@/hooks/useApiError';
import { formatMoneyMad } from '@/lib/format';

export default function PricingPage() {
  const { t } = useTranslation();
  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-semibold">{t('pricing.title')}</h1>
      <Tabs defaultValue="rates">
        <TabsList>
          <TabsTrigger value="rates">{t('pricing.tabs.rates')}</TabsTrigger>
          <TabsTrigger value="packs">{t('pricing.tabs.packs')}</TabsTrigger>
        </TabsList>
        <TabsContent value="rates">
          <RatesTab />
        </TabsContent>
        <TabsContent value="packs">
          <PacksTab />
        </TabsContent>
      </Tabs>
    </div>
  );
}

// -------------------- Rates --------------------
function RatesTab() {
  const { t, i18n } = useTranslation();
  const pricing = useMyPricing();
  const [open, setOpen] = useState(false);

  return (
    <Card>
      <CardHeader className="flex flex-row items-center justify-between space-y-0">
        <CardTitle className="text-lg">{t('pricing.tabs.rates')}</CardTitle>
        <Button onClick={() => setOpen(true)} className="gap-2">
          <Plus className="h-4 w-4" />
          {t('pricing.rate.add')}
        </Button>
      </CardHeader>
      <CardContent>
        {pricing.isLoading ? (
          <Skeleton className="h-32 w-full" />
        ) : (pricing.data?.rates ?? []).length === 0 ? (
          <p className="text-sm text-muted-foreground">{t('pricing.rate.no_rates')}</p>
        ) : (
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>{t('pricing.rate.subject')}</TableHead>
                <TableHead>{t('pricing.rate.level')}</TableHead>
                <TableHead>{t('pricing.rate.modality')}</TableHead>
                <TableHead>{t('pricing.rate.duration')}</TableHead>
                <TableHead>{t('pricing.rate.day')}</TableHead>
                <TableHead className="text-end">{t('pricing.rate.amount')}</TableHead>
                <TableHead className="text-end">{t('common.actions')}</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {(pricing.data?.rates ?? []).map((r: RateView) => (
                <RateRow key={r.id} rate={r} locale={i18n.language} />
              ))}
            </TableBody>
          </Table>
        )}
        <RateDialog open={open} onOpenChange={setOpen} />
      </CardContent>
    </Card>
  );
}

function RateRow({ rate, locale }: { rate: RateView; locale: string }) {
  const { t } = useTranslation();
  const apiError = useApiError();
  const deleteMut = useDeleteRate();
  return (
    <TableRow>
      <TableCell>{rate.subjectCode}</TableCell>
      <TableCell>{t(`level.${rate.level}`)}</TableCell>
      <TableCell>{t(`modality.${rate.modality}`)}</TableCell>
      <TableCell>{rate.durationMinutes} min</TableCell>
      <TableCell>
        {rate.dayOfWeek != null ? t(`weekday.${rate.dayOfWeek}`) : t('pricing.rate.day_any')}
      </TableCell>
      <TableCell className="text-end font-medium">
        {formatMoneyMad(rate.amountMinor, locale)}
      </TableCell>
      <TableCell className="text-end">
        <Button
          size="icon"
          variant="ghost"
          disabled={deleteMut.isPending}
          onClick={async () => {
            try {
              await deleteMut.mutateAsync(rate.id);
            } catch (err) {
              toast({ variant: 'destructive', description: apiError(err) });
            }
          }}
        >
          <Trash2 className="h-4 w-4" />
        </Button>
      </TableCell>
    </TableRow>
  );
}

function RateDialog({
  open,
  onOpenChange,
}: {
  open: boolean;
  onOpenChange(o: boolean): void;
}) {
  const { t } = useTranslation();
  const apiError = useApiError();
  const createMut = useCreateRate();

  const [subjectCode, setSubject] = useState('');
  const [level, setLevel] = useState<SchoolLevel>('LYCEE');
  const [modality, setModality] = useState<Modality>('ONLINE');
  const [duration, setDuration] = useState('60');
  const [dayOfWeek, setDayOfWeek] = useState<string>('all');
  const [amount, setAmount] = useState('150.00');

  async function onSubmit() {
    if (!subjectCode || !amount) return;
    const body: CreateRateRequest = {
      subjectCode,
      level,
      modality,
      durationMinutes: Number(duration),
      dayOfWeek: dayOfWeek === 'all' ? null : Number(dayOfWeek),
      amountMad: amount,
    };
    try {
      await createMut.mutateAsync(body);
      onOpenChange(false);
    } catch (err) {
      toast({ variant: 'destructive', description: apiError(err) });
    }
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>{t('pricing.rate.add')}</DialogTitle>
        </DialogHeader>
        <div className="grid grid-cols-2 gap-3">
          <div className="space-y-1.5 col-span-2">
            <Label>{t('pricing.rate.subject')}</Label>
            <Input value={subjectCode} onChange={(e) => setSubject(e.target.value)} />
          </div>
          <div className="space-y-1.5">
            <Label>{t('pricing.rate.level')}</Label>
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
            <Label>{t('pricing.rate.modality')}</Label>
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
            <Label>{t('pricing.rate.duration')}</Label>
            <Input type="number" value={duration} onChange={(e) => setDuration(e.target.value)} />
          </div>
          <div className="space-y-1.5">
            <Label>{t('pricing.rate.day')}</Label>
            <Select value={dayOfWeek} onValueChange={setDayOfWeek}>
              <SelectTrigger>
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="all">{t('pricing.rate.day_any')}</SelectItem>
                {[0, 1, 2, 3, 4, 5, 6].map((d) => (
                  <SelectItem key={d} value={String(d)}>
                    {t(`weekday.${d}`)}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>
          <div className="space-y-1.5 col-span-2">
            <Label>{t('pricing.rate.amount')}</Label>
            <Input
              type="number"
              inputMode="decimal"
              step="0.01"
              min="0.00"
              value={amount}
              onChange={(e) => setAmount(e.target.value)}
            />
          </div>
        </div>
        <DialogFooter>
          <Button variant="outline" onClick={() => onOpenChange(false)}>
            {t('common.cancel')}
          </Button>
          <Button onClick={onSubmit} disabled={createMut.isPending}>
            {t('common.save')}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}

// -------------------- Packs --------------------
function PacksTab() {
  const { t, i18n } = useTranslation();
  const pricing = useMyPricing();
  const [open, setOpen] = useState(false);

  return (
    <Card>
      <CardHeader className="flex flex-row items-center justify-between space-y-0">
        <CardTitle className="text-lg">{t('pricing.tabs.packs')}</CardTitle>
        <Button onClick={() => setOpen(true)} className="gap-2">
          <Plus className="h-4 w-4" />
          {t('pricing.pack.add')}
        </Button>
      </CardHeader>
      <CardContent>
        {pricing.isLoading ? (
          <Skeleton className="h-32 w-full" />
        ) : (pricing.data?.packs ?? []).length === 0 ? (
          <p className="text-sm text-muted-foreground">{t('pricing.pack.no_packs')}</p>
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
                <TableHead className="text-end">{t('common.actions')}</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {(pricing.data?.packs ?? []).map((p: PackView) => (
                <PackRow key={p.id} pack={p} locale={i18n.language} />
              ))}
            </TableBody>
          </Table>
        )}
        <PackDialog open={open} onOpenChange={setOpen} />
      </CardContent>
    </Card>
  );
}

function PackRow({ pack, locale }: { pack: PackView; locale: string }) {
  const { t } = useTranslation();
  const apiError = useApiError();
  const deleteMut = useDeletePack();
  return (
    <TableRow>
      <TableCell>{pack.subjectCode}</TableCell>
      <TableCell>{t(`level.${pack.level}`)}</TableCell>
      <TableCell>{t(`modality.${pack.modality}`)}</TableCell>
      <TableCell>{pack.sessionsCount}</TableCell>
      <TableCell>{String(pack.discountPct)}%</TableCell>
      <TableCell className="text-end font-medium">
        {formatMoneyMad(pack.totalAmountMinor, locale)}
      </TableCell>
      <TableCell className="text-end">
        <Button
          size="icon"
          variant="ghost"
          disabled={deleteMut.isPending}
          onClick={async () => {
            try {
              await deleteMut.mutateAsync(pack.id);
            } catch (err) {
              toast({ variant: 'destructive', description: apiError(err) });
            }
          }}
        >
          <Trash2 className="h-4 w-4" />
        </Button>
      </TableCell>
    </TableRow>
  );
}

function PackDialog({
  open,
  onOpenChange,
}: {
  open: boolean;
  onOpenChange(o: boolean): void;
}) {
  const { t } = useTranslation();
  const apiError = useApiError();
  const createMut = useCreatePack();

  const [subjectCode, setSubject] = useState('');
  const [level, setLevel] = useState<SchoolLevel>('LYCEE');
  const [modality, setModality] = useState<Modality>('ONLINE');
  const [sessions, setSessions] = useState('10');
  const [discount, setDiscount] = useState('10');
  const [total, setTotal] = useState('1350.00');

  async function onSubmit() {
    if (!subjectCode || !total) return;
    const body: CreatePackRequest = {
      subjectCode,
      level,
      modality,
      sessionsCount: Number(sessions),
      discountPct: discount,
      totalAmountMad: total,
    };
    try {
      await createMut.mutateAsync(body);
      onOpenChange(false);
    } catch (err) {
      toast({ variant: 'destructive', description: apiError(err) });
    }
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>{t('pricing.pack.add')}</DialogTitle>
        </DialogHeader>
        <div className="grid grid-cols-2 gap-3">
          <div className="space-y-1.5 col-span-2">
            <Label>{t('pricing.rate.subject')}</Label>
            <Input value={subjectCode} onChange={(e) => setSubject(e.target.value)} />
          </div>
          <div className="space-y-1.5">
            <Label>{t('pricing.rate.level')}</Label>
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
            <Label>{t('pricing.rate.modality')}</Label>
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
            <Label>{t('pricing.pack.sessions')}</Label>
            <Input type="number" value={sessions} onChange={(e) => setSessions(e.target.value)} />
          </div>
          <div className="space-y-1.5">
            <Label>{t('pricing.pack.discount')}</Label>
            <Input type="number" value={discount} onChange={(e) => setDiscount(e.target.value)} />
          </div>
          <div className="space-y-1.5 col-span-2">
            <Label>{t('pricing.pack.total')}</Label>
            <Input
              type="number"
              inputMode="decimal"
              step="0.01"
              value={total}
              onChange={(e) => setTotal(e.target.value)}
            />
          </div>
        </div>
        <DialogFooter>
          <Button variant="outline" onClick={() => onOpenChange(false)}>
            {t('common.cancel')}
          </Button>
          <Button onClick={onSubmit} disabled={createMut.isPending}>
            {t('common.save')}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}

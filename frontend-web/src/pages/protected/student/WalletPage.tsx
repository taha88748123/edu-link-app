import { useState } from 'react';
import { Link } from 'react-router-dom';
import { useTranslation } from 'react-i18next';

import { TOPUP_METHODS, type TopupMethod } from '@/api/enums';
import type { AuditEntryView } from '@/api/payment.api';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import {
  Dialog,
  DialogContent,
  DialogDescription,
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
import { toast } from '@/components/ui/use-toast';
import {
  useMyAuditLog,
  useMyBalance,
  useSubmitTopup,
} from '@/hooks/queries/usePayment';
import { useApiError } from '@/hooks/useApiError';
import { formatDateTime, formatMoneyMad } from '@/lib/format';

export default function WalletPage() {
  const { t, i18n } = useTranslation();
  const balanceQuery = useMyBalance();
  const auditQuery = useMyAuditLog({ size: 30 });

  const [open, setOpen] = useState(false);

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-semibold">{t('payment.wallet_title')}</h1>

      <div className="grid gap-4 sm:grid-cols-3">
        <Card className="sm:col-span-2">
          <CardHeader className="pb-2">
            <CardTitle className="text-sm font-medium text-muted-foreground">
              {t('payment.balance_card')}
            </CardTitle>
          </CardHeader>
          <CardContent>
            {balanceQuery.isLoading ? (
              <Skeleton className="h-10 w-40" />
            ) : (
              <div className="text-3xl font-semibold">
                {formatMoneyMad(balanceQuery.data?.balanceMinor ?? 0, i18n.language)}
              </div>
            )}
          </CardContent>
        </Card>
        <Card className="flex flex-col justify-center">
          <CardContent className="space-y-2 pt-6">
            <Button className="w-full" onClick={() => setOpen(true)}>
              {t('payment.topup_cta')}
            </Button>
            <Button asChild variant="outline" className="w-full">
              <Link to="/student/topups">{t('payment.topup.list_title')}</Link>
            </Button>
          </CardContent>
        </Card>
      </div>

      <Card>
        <CardHeader>
          <CardTitle className="text-lg">{t('payment.history_title')}</CardTitle>
        </CardHeader>
        <CardContent>
          {auditQuery.isLoading ? (
            <Skeleton className="h-40 w-full" />
          ) : (auditQuery.data ?? []).length === 0 ? (
            <p className="text-sm text-muted-foreground">{t('payment.history_empty')}</p>
          ) : (
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>{t('common.date')}</TableHead>
                  <TableHead>{t('common.status')}</TableHead>
                  <TableHead className="text-end">{t('common.amount')}</TableHead>
                  <TableHead className="text-end">{t('dashboard.kpi.balance')}</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {(auditQuery.data ?? []).map((row) => (
                  <AuditRow key={row.id} row={row} />
                ))}
              </TableBody>
            </Table>
          )}
        </CardContent>
      </Card>

      <TopupDialog open={open} onOpenChange={setOpen} />
    </div>
  );
}

function AuditRow({ row }: { row: AuditEntryView }) {
  const { t, i18n } = useTranslation();
  const positive = row.amountMinor >= 0;
  return (
    <TableRow>
      <TableCell className="text-xs text-muted-foreground">
        {formatDateTime(row.createdAt, i18n.language)}
      </TableCell>
      <TableCell>
        <Badge variant="outline">{t(`payment.operation.${row.operation}`, row.operation)}</Badge>
      </TableCell>
      <TableCell
        className={`text-end font-medium ${positive ? 'text-emerald-600' : 'text-rose-600'}`}
      >
        {positive ? '+' : ''}
        {formatMoneyMad(row.amountMinor, i18n.language)}
      </TableCell>
      <TableCell className="text-end text-xs text-muted-foreground">
        {formatMoneyMad(row.balanceAfter, i18n.language)}
      </TableCell>
    </TableRow>
  );
}

function TopupDialog({
  open,
  onOpenChange,
}: {
  open: boolean;
  onOpenChange(o: boolean): void;
}) {
  const { t } = useTranslation();
  const apiError = useApiError();
  const submitMut = useSubmitTopup();

  const [amount, setAmount] = useState('');
  const [method, setMethod] = useState<TopupMethod>('CASHPLUS');
  const [proofUrl, setProofUrl] = useState('');

  async function onSubmit() {
    if (!amount) return;
    try {
      await submitMut.mutateAsync({ amountMad: amount, method, proofUrl: proofUrl || undefined });
      toast({ description: t('payment.topup.success') });
      onOpenChange(false);
      setAmount('');
      setProofUrl('');
    } catch (err) {
      toast({ variant: 'destructive', description: apiError(err) });
    }
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>{t('payment.topup.title')}</DialogTitle>
          <DialogDescription>{t('payment.topup.proof_hint')}</DialogDescription>
        </DialogHeader>
        <div className="space-y-3">
          <div className="space-y-1.5">
            <Label>{t('payment.topup.amount')}</Label>
            <Input
              type="number"
              inputMode="decimal"
              step="0.01"
              min="0.01"
              value={amount}
              onChange={(e) => setAmount(e.target.value)}
            />
          </div>
          <div className="space-y-1.5">
            <Label>{t('payment.topup.method')}</Label>
            <Select value={method} onValueChange={(v) => setMethod(v as TopupMethod)}>
              <SelectTrigger>
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                {TOPUP_METHODS.map((m) => (
                  <SelectItem key={m} value={m}>
                    {t(`payment.topup.method_${m}`)}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>
          <div className="space-y-1.5">
            <Label>{t('payment.topup.proof')}</Label>
            <Input value={proofUrl} onChange={(e) => setProofUrl(e.target.value)} />
          </div>
        </div>
        <DialogFooter>
          <Button variant="outline" onClick={() => onOpenChange(false)}>
            {t('common.cancel')}
          </Button>
          <Button onClick={onSubmit} disabled={!amount || submitMut.isPending}>
            {t('payment.topup.submit')}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}

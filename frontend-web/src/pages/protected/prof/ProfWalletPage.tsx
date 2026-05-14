import { useState } from 'react';
import { useTranslation } from 'react-i18next';

import type { AuditEntryView, PayoutView } from '@/api/payment.api';
import { Badge } from '@/components/ui/badge';
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
  useMyAuditLog,
  useMyBalance,
  useMyPayouts,
  useRequestPayout,
} from '@/hooks/queries/usePayment';
import { useApiError } from '@/hooks/useApiError';
import { formatDateTime, formatMoneyMad } from '@/lib/format';

export default function ProfWalletPage() {
  const { t, i18n } = useTranslation();
  const balanceQuery = useMyBalance();
  const auditQuery = useMyAuditLog({ size: 30 });
  const payoutsQuery = useMyPayouts({ size: 50 });
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
          <CardContent className="pt-6">
            <Button className="w-full" onClick={() => setOpen(true)}>
              {t('payment.payout_cta')}
            </Button>
          </CardContent>
        </Card>
      </div>

      <Tabs defaultValue="history">
        <TabsList>
          <TabsTrigger value="history">{t('payment.history_title')}</TabsTrigger>
          <TabsTrigger value="payouts">{t('payment.payout.list_title')}</TabsTrigger>
        </TabsList>
        <TabsContent value="history">
          <Card>
            <CardContent className="p-0">
              {auditQuery.isLoading ? (
                <Skeleton className="h-40 w-full" />
              ) : (auditQuery.data ?? []).length === 0 ? (
                <p className="p-6 text-sm text-muted-foreground">{t('payment.history_empty')}</p>
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
                    {(auditQuery.data ?? []).map((row: AuditEntryView) => {
                      const positive = row.amountMinor >= 0;
                      return (
                        <TableRow key={row.id}>
                          <TableCell className="text-xs text-muted-foreground">
                            {formatDateTime(row.createdAt, i18n.language)}
                          </TableCell>
                          <TableCell>
                            <Badge variant="outline">
                              {t(`payment.operation.${row.operation}`, row.operation)}
                            </Badge>
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
                    })}
                  </TableBody>
                </Table>
              )}
            </CardContent>
          </Card>
        </TabsContent>
        <TabsContent value="payouts">
          <Card>
            <CardContent className="p-0">
              {payoutsQuery.isLoading ? (
                <Skeleton className="h-40 w-full" />
              ) : (payoutsQuery.data ?? []).length === 0 ? (
                <p className="p-6 text-sm text-muted-foreground">{t('payment.payout.list_empty')}</p>
              ) : (
                <Table>
                  <TableHeader>
                    <TableRow>
                      <TableHead>{t('common.date')}</TableHead>
                      <TableHead className="text-end">{t('common.amount')}</TableHead>
                      <TableHead>{t('common.status')}</TableHead>
                      <TableHead>IBAN</TableHead>
                    </TableRow>
                  </TableHeader>
                  <TableBody>
                    {(payoutsQuery.data ?? []).map((p: PayoutView) => (
                      <TableRow key={p.id}>
                        <TableCell className="text-xs text-muted-foreground">
                          {formatDateTime(p.requestedAt, i18n.language)}
                        </TableCell>
                        <TableCell className="text-end font-medium">
                          {formatMoneyMad(p.requestedAmountMinor, i18n.language)}
                        </TableCell>
                        <TableCell>
                          <Badge variant={payoutVariant(p.status)}>
                            {t(`payment.payout.status.${p.status}`)}
                          </Badge>
                        </TableCell>
                        <TableCell className="text-xs">{p.ibanRef ?? '—'}</TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              )}
            </CardContent>
          </Card>
        </TabsContent>
      </Tabs>

      <PayoutDialog open={open} onOpenChange={setOpen} />
    </div>
  );
}

function payoutVariant(s: string): 'success' | 'warning' | 'danger' | 'info' {
  if (s === 'PAID') return 'success';
  if (s === 'APPROVED') return 'info';
  if (s === 'REJECTED') return 'danger';
  return 'warning';
}

function PayoutDialog({
  open,
  onOpenChange,
}: {
  open: boolean;
  onOpenChange(o: boolean): void;
}) {
  const { t } = useTranslation();
  const apiError = useApiError();
  const requestMut = useRequestPayout();

  const [amount, setAmount] = useState('');
  const [iban, setIban] = useState('');

  async function onSubmit() {
    if (!amount) return;
    try {
      await requestMut.mutateAsync({ amountMad: amount, ibanRef: iban || undefined });
      toast({ description: t('payment.payout.success') });
      onOpenChange(false);
      setAmount('');
      setIban('');
    } catch (err) {
      toast({ variant: 'destructive', description: apiError(err) });
    }
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>{t('payment.payout.title')}</DialogTitle>
        </DialogHeader>
        <div className="space-y-3">
          <div className="space-y-1.5">
            <Label>{t('payment.payout.amount')}</Label>
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
            <Label>{t('payment.payout.iban')}</Label>
            <Input value={iban} onChange={(e) => setIban(e.target.value)} />
          </div>
        </div>
        <DialogFooter>
          <Button variant="outline" onClick={() => onOpenChange(false)}>
            {t('common.cancel')}
          </Button>
          <Button onClick={onSubmit} disabled={!amount || requestMut.isPending}>
            {t('payment.payout.submit')}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}

import { useState } from 'react';
import { useTranslation } from 'react-i18next';

import type { PayoutView } from '@/api/payment.api';
import type { PayoutStatus } from '@/api/enums';
import { Alert, AlertDescription } from '@/components/ui/alert';
import { Button } from '@/components/ui/button';
import { Card, CardContent } from '@/components/ui/card';
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
import { Tabs, TabsList, TabsTrigger } from '@/components/ui/tabs';
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table';
import { Textarea } from '@/components/ui/textarea';
import { formatDateTime, formatMoneyMad } from '@/lib/format';
import { useApiError } from '@/hooks/useApiError';
import {
  useDecidePayout,
  useMarkPaidPayout,
  usePayoutsAdmin,
} from '@/hooks/queries/useAdmin';

const STATUSES: PayoutStatus[] = ['REQUESTED', 'APPROVED', 'PAID', 'REJECTED'];

type Action =
  | { kind: 'DECIDE'; payout: PayoutView; decision: 'APPROVE' | 'REJECT' }
  | { kind: 'MARK_PAID'; payout: PayoutView };

export default function AdminPayoutsPage() {
  const { t, i18n } = useTranslation();
  const formatErr = useApiError();

  const [status, setStatus] = useState<PayoutStatus>('REQUESTED');
  const [action, setAction] = useState<Action | null>(null);
  const [notes, setNotes] = useState('');
  const [paymentRef, setPaymentRef] = useState('');
  const [error, setError] = useState<string | null>(null);

  const payoutsQuery = usePayoutsAdmin(status);
  const decideMut = useDecidePayout();
  const markPaidMut = useMarkPaidPayout();
  const busy = decideMut.isPending || markPaidMut.isPending;

  const closeDialog = () => {
    setAction(null);
    setNotes('');
    setPaymentRef('');
    setError(null);
  };

  const onSubmit = () => {
    if (!action) return;
    setError(null);
    if (action.kind === 'DECIDE') {
      decideMut.mutate(
        {
          payoutId: action.payout.id,
          body: { decision: action.decision, notes: notes || undefined },
        },
        { onSuccess: closeDialog, onError: (e) => setError(formatErr(e)) },
      );
    } else {
      if (paymentRef.trim().length === 0) {
        setError(t('admin.payouts.errors.payment_ref_required'));
        return;
      }
      markPaidMut.mutate(
        { payoutId: action.payout.id, body: { paymentRef } },
        { onSuccess: closeDialog, onError: (e) => setError(formatErr(e)) },
      );
    }
  };

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-semibold">{t('admin.payouts.title')}</h1>

      <Tabs value={status} onValueChange={(v) => setStatus(v as PayoutStatus)}>
        <TabsList>
          {STATUSES.map((s) => (
            <TabsTrigger key={s} value={s}>
              {t(`payment.payout.status.${s}`)}
            </TabsTrigger>
          ))}
        </TabsList>
      </Tabs>

      {payoutsQuery.isLoading ? (
        <Skeleton className="h-64 w-full" />
      ) : (payoutsQuery.data?.items.length ?? 0) === 0 ? (
        <Card>
          <CardContent className="py-10 text-center text-sm text-muted-foreground">
            {t('admin.payouts.empty')}
          </CardContent>
        </Card>
      ) : (
        <Card>
          <CardContent className="p-0">
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>{t('admin.payouts.columns.requested')}</TableHead>
                  <TableHead>{t('admin.payouts.columns.prof')}</TableHead>
                  <TableHead>{t('admin.payouts.columns.amount')}</TableHead>
                  <TableHead>{t('admin.payouts.columns.iban')}</TableHead>
                  <TableHead className="text-end">{t('common.actions')}</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {(payoutsQuery.data?.items ?? []).map((p) => (
                  <TableRow key={p.id}>
                    <TableCell>{formatDateTime(p.requestedAt, i18n.language)}</TableCell>
                    <TableCell className="text-xs text-muted-foreground">
                      {p.profId.slice(0, 8)}…
                    </TableCell>
                    <TableCell>
                      {formatMoneyMad(p.requestedAmountMinor, i18n.language)}
                    </TableCell>
                    <TableCell className="text-xs">{p.ibanRef ?? '—'}</TableCell>
                    <TableCell className="text-end">
                      <div className="flex flex-wrap justify-end gap-2">
                        {p.status === 'REQUESTED' ? (
                          <>
                            <Button
                              size="sm"
                              onClick={() =>
                                setAction({ kind: 'DECIDE', payout: p, decision: 'APPROVE' })
                              }
                            >
                              {t('admin.payouts.actions.approve')}
                            </Button>
                            <Button
                              size="sm"
                              variant="destructive"
                              onClick={() =>
                                setAction({ kind: 'DECIDE', payout: p, decision: 'REJECT' })
                              }
                            >
                              {t('admin.payouts.actions.reject')}
                            </Button>
                          </>
                        ) : null}
                        {p.status === 'APPROVED' ? (
                          <Button
                            size="sm"
                            onClick={() => setAction({ kind: 'MARK_PAID', payout: p })}
                          >
                            {t('admin.payouts.actions.mark_paid')}
                          </Button>
                        ) : null}
                      </div>
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </CardContent>
        </Card>
      )}

      <Dialog open={Boolean(action)} onOpenChange={(o) => !o && closeDialog()}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>
              {action?.kind === 'MARK_PAID'
                ? t('admin.payouts.dialog.mark_paid')
                : action?.kind === 'DECIDE'
                  ? t(`admin.payouts.dialog.${action.decision}`)
                  : ''}
            </DialogTitle>
          </DialogHeader>
          {error ? (
            <Alert variant="destructive">
              <AlertDescription>{error}</AlertDescription>
            </Alert>
          ) : null}
          {action?.kind === 'MARK_PAID' ? (
            <div className="space-y-2">
              <Label htmlFor="payment-ref">{t('admin.payouts.dialog.payment_ref')}</Label>
              <Input
                id="payment-ref"
                value={paymentRef}
                onChange={(e) => setPaymentRef(e.target.value)}
                placeholder={t('admin.payouts.dialog.payment_ref_placeholder')}
              />
            </div>
          ) : (
            <div className="space-y-2">
              <Label htmlFor="payout-notes">{t('admin.payouts.dialog.notes')}</Label>
              <Textarea
                id="payout-notes"
                rows={3}
                value={notes}
                onChange={(e) => setNotes(e.target.value)}
              />
            </div>
          )}
          <DialogFooter>
            <Button variant="outline" onClick={closeDialog}>
              {t('common.cancel')}
            </Button>
            <Button onClick={onSubmit} disabled={busy}>
              {t('common.confirm')}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}

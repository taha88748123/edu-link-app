import { useState } from 'react';
import { useTranslation } from 'react-i18next';

import type { BookingView } from '@/api/bookings.api';
import type { DisputeDecision, ResolveDisputeRequest } from '@/api/admin.api';
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
import { RadioGroup, RadioGroupItem } from '@/components/ui/radio-group';
import { Skeleton } from '@/components/ui/skeleton';
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table';
import { Textarea } from '@/components/ui/textarea';
import { disputeResolutionValidation } from '@/lib/admin';
import { formatDateTime, formatMoneyMad } from '@/lib/format';
import { useApiError } from '@/hooks/useApiError';
import { useAdminBookings, useResolveDispute } from '@/hooks/queries/useAdmin';

export default function AdminDisputesPage() {
  const { t, i18n } = useTranslation();
  const formatErr = useApiError();

  const bookingsQuery = useAdminBookings({ status: 'LITIGE' });
  const resolveMut = useResolveDispute();

  const [target, setTarget] = useState<BookingView | null>(null);
  const [disputeId, setDisputeId] = useState('');
  const [decision, setDecision] = useState<DisputeDecision>('REFUND');
  const [profShareMad, setProfShareMad] = useState('');
  const [studentRefundMad, setStudentRefundMad] = useState('');
  const [notes, setNotes] = useState('');
  const [error, setError] = useState<string | null>(null);

  const closeDialog = () => {
    setTarget(null);
    setDisputeId('');
    setDecision('REFUND');
    setProfShareMad('');
    setStudentRefundMad('');
    setNotes('');
    setError(null);
  };

  const onSubmit = () => {
    if (!target) return;
    setError(null);
    if (!disputeId.trim()) {
      setError(t('admin.disputes.errors.dispute_id_required'));
      return;
    }
    const body: ResolveDisputeRequest = {
      decision,
      profShareMad: decision === 'SPLIT' ? profShareMad : undefined,
      studentRefundMad: decision === 'SPLIT' ? studentRefundMad : undefined,
      notes: notes || undefined,
    };
    const validationKey = disputeResolutionValidation(body);
    if (validationKey) {
      setError(t(validationKey));
      return;
    }
    resolveMut.mutate(
      { bookingId: target.id, disputeId: disputeId.trim(), body },
      {
        onSuccess: closeDialog,
        onError: (e) => setError(formatErr(e)),
      },
    );
  };

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-semibold">{t('admin.disputes.title')}</h1>

      {bookingsQuery.isLoading ? (
        <Skeleton className="h-64 w-full" />
      ) : (bookingsQuery.data?.items.length ?? 0) === 0 ? (
        <Card>
          <CardContent className="py-10 text-center text-sm text-muted-foreground">
            {t('admin.disputes.empty')}
          </CardContent>
        </Card>
      ) : (
        <Card>
          <CardContent className="p-0">
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>{t('admin.disputes.columns.booking')}</TableHead>
                  <TableHead>{t('admin.disputes.columns.when')}</TableHead>
                  <TableHead>{t('admin.disputes.columns.subject')}</TableHead>
                  <TableHead>{t('admin.disputes.columns.amount')}</TableHead>
                  <TableHead className="text-end">{t('common.actions')}</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {(bookingsQuery.data?.items ?? []).map((b) => (
                  <TableRow key={b.id}>
                    <TableCell className="text-xs">{b.id.slice(0, 8)}…</TableCell>
                    <TableCell>{formatDateTime(b.scheduledStart, i18n.language)}</TableCell>
                    <TableCell>
                      {b.subjectCode}{' '}
                      <span className="text-xs text-muted-foreground">
                        · {t(`level.${b.level}`)}
                      </span>
                    </TableCell>
                    <TableCell>
                      {formatMoneyMad(b.totalAmountMinor, i18n.language)}
                    </TableCell>
                    <TableCell className="text-end">
                      <Button size="sm" onClick={() => setTarget(b)}>
                        {t('admin.disputes.resolve')}
                      </Button>
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </CardContent>
        </Card>
      )}

      <Dialog open={Boolean(target)} onOpenChange={(o) => !o && closeDialog()}>
        <DialogContent className="max-w-md">
          <DialogHeader>
            <DialogTitle>{t('admin.disputes.dialog.title')}</DialogTitle>
          </DialogHeader>
          {error ? (
            <Alert variant="destructive">
              <AlertDescription>{error}</AlertDescription>
            </Alert>
          ) : null}
          <div className="space-y-2">
            <Label htmlFor="dispute-id">{t('admin.disputes.dialog.dispute_id')}</Label>
            <Input
              id="dispute-id"
              value={disputeId}
              onChange={(e) => setDisputeId(e.target.value)}
              placeholder="00000000-…"
            />
            <p className="text-xs text-muted-foreground">
              {t('admin.disputes.dialog.dispute_id_hint')}
            </p>
          </div>
          <div className="space-y-2">
            <Label>{t('admin.disputes.dialog.decision')}</Label>
            <RadioGroup
              value={decision}
              onValueChange={(v) => setDecision(v as DisputeDecision)}
            >
              {(['REFUND', 'PAY_PROF', 'SPLIT'] as DisputeDecision[]).map((d) => (
                <label key={d} className="flex items-center gap-2 text-sm">
                  <RadioGroupItem value={d} id={`decision-${d}`} />
                  <span>{t(`admin.disputes.decision.${d}`)}</span>
                </label>
              ))}
            </RadioGroup>
          </div>
          {decision === 'SPLIT' ? (
            <div className="grid grid-cols-2 gap-3">
              <div className="space-y-2">
                <Label htmlFor="prof-share">{t('admin.disputes.dialog.prof_share')}</Label>
                <Input
                  id="prof-share"
                  type="number"
                  step="0.01"
                  min="0"
                  value={profShareMad}
                  onChange={(e) => setProfShareMad(e.target.value)}
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="student-refund">
                  {t('admin.disputes.dialog.student_refund')}
                </Label>
                <Input
                  id="student-refund"
                  type="number"
                  step="0.01"
                  min="0"
                  value={studentRefundMad}
                  onChange={(e) => setStudentRefundMad(e.target.value)}
                />
              </div>
            </div>
          ) : null}
          <div className="space-y-2">
            <Label htmlFor="dispute-notes">{t('admin.disputes.dialog.notes')}</Label>
            <Textarea
              id="dispute-notes"
              rows={3}
              value={notes}
              onChange={(e) => setNotes(e.target.value)}
            />
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={closeDialog}>
              {t('common.cancel')}
            </Button>
            <Button onClick={onSubmit} disabled={resolveMut.isPending}>
              {t('common.confirm')}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}

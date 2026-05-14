import { useState } from 'react';
import { useParams } from 'react-router-dom';
import { useTranslation } from 'react-i18next';

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
import { Skeleton } from '@/components/ui/skeleton';
import { AntiBypassNotice } from '@/components/booking/AntiBypassNotice';
import { BookingStatusBadge } from '@/components/booking/BookingStatusBadge';
import { PriceBreakdown } from '@/components/booking/PriceBreakdown';
import {
  useAcceptBooking,
  useBooking,
  useCancelBooking,
  useConfirmBooking,
  useDeclineBooking,
  useDisputeBooking,
  useStartBooking,
} from '@/hooks/queries/useBookings';
import { useApiError } from '@/hooks/useApiError';
import { useAuthStore } from '@/auth/auth.store';
import {
  allowedActions,
  isContactVisible,
  isStartWithinWindow,
  type BookingAction,
  type CallerRole,
} from '@/lib/booking';
import { formatDateTime } from '@/lib/format';
import { toast } from '@/components/ui/use-toast';

type DialogKind = 'cancel' | 'confirm' | 'decline' | 'dispute' | null;

export default function BookingDetailPage() {
  const { id = '' } = useParams<{ id: string }>();
  const { t, i18n } = useTranslation();
  const apiError = useApiError();
  const role = useAuthStore((s) => s.user?.role);
  const callerRole: CallerRole | null =
    role === 'STUDENT' || role === 'PARENT' ? 'STUDENT' : role === 'PROF' ? 'PROF' : null;

  const bookingQuery = useBooking(id);

  const cancelMut = useCancelBooking();
  const confirmMut = useConfirmBooking();
  const declineMut = useDeclineBooking();
  const disputeMut = useDisputeBooking();
  const acceptMut = useAcceptBooking();
  const startMut = useStartBooking();

  const [dialog, setDialog] = useState<DialogKind>(null);
  const [reason, setReason] = useState('');
  const [evidence, setEvidence] = useState('');

  function close() {
    setDialog(null);
    setReason('');
    setEvidence('');
  }

  if (bookingQuery.isLoading) return <Skeleton className="h-64 w-full" />;
  if (!bookingQuery.data) {
    return <p className="text-sm text-muted-foreground">{t('errors.RESOURCE_NOT_FOUND')}</p>;
  }
  const b = bookingQuery.data;

  const actions: BookingAction[] = callerRole ? allowedActions(b.status, callerRole) : [];
  const contactVisible = isContactVisible(b.status);
  const startWindowOpen = isStartWithinWindow(b.scheduledStart);

  async function runCancel() {
    try {
      await cancelMut.mutateAsync({ id, body: { reason: reason || undefined } });
      close();
      toast({ description: t('booking.status.ANNULEE') });
    } catch (err) {
      toast({ variant: 'destructive', description: apiError(err) });
    }
  }

  async function runConfirm() {
    try {
      await confirmMut.mutateAsync(id);
      close();
      toast({ description: t('booking.status.CONFIRMEE') });
    } catch (err) {
      toast({ variant: 'destructive', description: apiError(err) });
    }
  }

  async function runDecline() {
    try {
      await declineMut.mutateAsync({ id, body: { reason: reason || undefined } });
      close();
      toast({ description: t('booking.actions.decline') });
    } catch (err) {
      toast({ variant: 'destructive', description: apiError(err) });
    }
  }

  async function runDispute() {
    if (!reason) return;
    try {
      await disputeMut.mutateAsync({
        id,
        body: { cause: reason, evidenceUrls: evidence || undefined },
      });
      close();
      toast({ description: t('booking.status.LITIGE') });
    } catch (err) {
      toast({ variant: 'destructive', description: apiError(err) });
    }
  }

  async function runAccept() {
    try {
      await acceptMut.mutateAsync(id);
      toast({ description: t('booking.status.ACCEPTEE_PAR_PROF') });
    } catch (err) {
      toast({ variant: 'destructive', description: apiError(err) });
    }
  }

  async function runStart() {
    try {
      await startMut.mutateAsync(id);
      toast({ description: t('booking.status.EN_COURS') });
    } catch (err) {
      toast({ variant: 'destructive', description: apiError(err) });
    }
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between gap-3">
        <h1 className="text-2xl font-semibold">{t('booking.detail_title')}</h1>
        <BookingStatusBadge status={b.status} />
      </div>

      {!contactVisible && <AntiBypassNotice />}

      <Card>
        <CardHeader>
          <CardTitle className="text-lg">{t('booking.summary')}</CardTitle>
        </CardHeader>
        <CardContent className="space-y-2 text-sm">
          <Row label={t('booking.create.subject')} value={`${b.subjectCode} · ${t(`level.${b.level}`)}`} />
          <Row label={t('booking.create.modality')} value={t(`modality.${b.modality}`)} />
          <Row label={t('booking.create.duration')} value={`${b.durationMinutes} min`} />
          <Row label={t('booking.columns.when')} value={formatDateTime(b.scheduledStart, i18n.language)} />
          {b.cancelledAt && (
            <Row label={t('booking.actions.cancel')} value={formatDateTime(b.cancelledAt, i18n.language)} />
          )}
          {b.confirmedAt && (
            <Row label={t('booking.status.CONFIRMEE')} value={formatDateTime(b.confirmedAt, i18n.language)} />
          )}
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle className="text-lg">{t('booking.price_breakdown.title')}</CardTitle>
        </CardHeader>
        <CardContent>
          <PriceBreakdown
            baseAmountMinor={b.amountMinor}
            platformFeeMinor={b.platformFeeMinor}
            totalAmountMinor={b.totalAmountMinor}
          />
        </CardContent>
      </Card>

      {actions.length > 0 && (
        <Card>
          <CardHeader>
            <CardTitle className="text-lg">{t('common.actions')}</CardTitle>
          </CardHeader>
          <CardContent className="flex flex-wrap gap-2">
            {actions.includes('cancel') && (
              <Button variant="destructive" onClick={() => setDialog('cancel')}>
                {t('booking.actions.cancel')}
              </Button>
            )}
            {actions.includes('confirm') && (
              <Button onClick={() => setDialog('confirm')}>{t('booking.actions.confirm')}</Button>
            )}
            {actions.includes('dispute') && (
              <Button variant="outline" onClick={() => setDialog('dispute')}>
                {t('booking.actions.dispute')}
              </Button>
            )}
            {actions.includes('accept') && (
              <Button onClick={runAccept} disabled={acceptMut.isPending}>
                {t('booking.actions.accept')}
              </Button>
            )}
            {actions.includes('decline') && (
              <Button variant="destructive" onClick={() => setDialog('decline')}>
                {t('booking.actions.decline')}
              </Button>
            )}
            {actions.includes('start') && (
              <div className="space-y-1">
                <Button onClick={runStart} disabled={!startWindowOpen || startMut.isPending}>
                  {t('booking.actions.start')}
                </Button>
                {!startWindowOpen && (
                  <p className="text-xs text-muted-foreground">{t('booking.start_window_hint')}</p>
                )}
              </div>
            )}
          </CardContent>
        </Card>
      )}

      {/* ---- Dialogs ---- */}
      <Dialog open={dialog === 'cancel'} onOpenChange={(o) => !o && close()}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>{t('booking.cancel_dialog.title')}</DialogTitle>
          </DialogHeader>
          <div className="space-y-2">
            <Label>{t('booking.cancel_dialog.reason')}</Label>
            <Input value={reason} onChange={(e) => setReason(e.target.value)} />
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={close}>
              {t('common.cancel')}
            </Button>
            <Button variant="destructive" onClick={runCancel} disabled={cancelMut.isPending}>
              {t('booking.cancel_dialog.confirm')}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      <Dialog open={dialog === 'decline'} onOpenChange={(o) => !o && close()}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>{t('booking.decline_dialog.title')}</DialogTitle>
          </DialogHeader>
          <div className="space-y-2">
            <Label>{t('booking.decline_dialog.reason')}</Label>
            <Input value={reason} onChange={(e) => setReason(e.target.value)} />
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={close}>
              {t('common.cancel')}
            </Button>
            <Button variant="destructive" onClick={runDecline} disabled={declineMut.isPending}>
              {t('booking.decline_dialog.confirm')}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      <Dialog open={dialog === 'confirm'} onOpenChange={(o) => !o && close()}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>{t('booking.confirm_dialog.title')}</DialogTitle>
            <DialogDescription>{t('booking.confirm_dialog.desc')}</DialogDescription>
          </DialogHeader>
          <DialogFooter>
            <Button variant="outline" onClick={close}>
              {t('common.cancel')}
            </Button>
            <Button onClick={runConfirm} disabled={confirmMut.isPending}>
              {t('booking.confirm_dialog.confirm')}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      <Dialog open={dialog === 'dispute'} onOpenChange={(o) => !o && close()}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>{t('booking.dispute_dialog.title')}</DialogTitle>
          </DialogHeader>
          <div className="space-y-2">
            <Label>{t('booking.dispute_dialog.cause')}</Label>
            <Input value={reason} onChange={(e) => setReason(e.target.value)} />
            <Label>{t('booking.dispute_dialog.evidence')}</Label>
            <Input value={evidence} onChange={(e) => setEvidence(e.target.value)} />
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={close}>
              {t('common.cancel')}
            </Button>
            <Button onClick={runDispute} disabled={!reason || disputeMut.isPending}>
              {t('booking.dispute_dialog.confirm')}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}

function Row({ label, value }: { label: string; value: string }) {
  return (
    <div className="flex items-baseline justify-between">
      <span className="text-muted-foreground">{label}</span>
      <span className="font-medium">{value}</span>
    </div>
  );
}

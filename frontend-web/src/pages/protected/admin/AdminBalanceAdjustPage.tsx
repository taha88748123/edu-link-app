import { useState } from 'react';
import { useTranslation } from 'react-i18next';

import type { BalanceAdjustOperation } from '@/api/admin.api';
import { Alert, AlertDescription } from '@/components/ui/alert';
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
import { Textarea } from '@/components/ui/textarea';
import { useApiError } from '@/hooks/useApiError';
import { useAdjustBalance } from '@/hooks/queries/useAdmin';

const OPERATIONS: BalanceAdjustOperation[] = ['ADMIN_CREDIT', 'ADMIN_DEBIT', 'ADMIN_ADJUST'];

export default function AdminBalanceAdjustPage() {
  const { t } = useTranslation();
  const formatErr = useApiError();
  const adjustMut = useAdjustBalance();

  const [userId, setUserId] = useState('');
  const [deltaMad, setDeltaMad] = useState('');
  const [operation, setOperation] = useState<BalanceAdjustOperation>('ADMIN_CREDIT');
  const [reason, setReason] = useState('');
  const [confirming, setConfirming] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);

  const onOpenConfirm = () => {
    setError(null);
    setSuccess(null);
    if (!userId.trim()) {
      setError(t('admin.balance_adjust.errors.user_required'));
      return;
    }
    if (!deltaMad.trim() || Number(deltaMad) === 0) {
      setError(t('admin.balance_adjust.errors.amount_required'));
      return;
    }
    if (!reason.trim()) {
      setError(t('admin.balance_adjust.errors.reason_required'));
      return;
    }
    setConfirming(true);
  };

  const onSubmit = () => {
    setError(null);
    adjustMut.mutate(
      {
        userId: userId.trim(),
        body: { deltaMad, operation, reason },
      },
      {
        onSuccess: () => {
          setConfirming(false);
          setSuccess(t('admin.balance_adjust.success'));
          setUserId('');
          setDeltaMad('');
          setReason('');
        },
        onError: (e) => {
          setConfirming(false);
          setError(formatErr(e));
        },
      },
    );
  };

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-semibold">{t('admin.balance_adjust.title')}</h1>

      <Alert>
        <AlertDescription>{t('admin.balance_adjust.warning_banner')}</AlertDescription>
      </Alert>

      {error ? (
        <Alert variant="destructive">
          <AlertDescription>{error}</AlertDescription>
        </Alert>
      ) : null}
      {success ? (
        <Alert>
          <AlertDescription>{success}</AlertDescription>
        </Alert>
      ) : null}

      <Card>
        <CardHeader>
          <CardTitle className="text-lg">{t('admin.balance_adjust.form_title')}</CardTitle>
        </CardHeader>
        <CardContent className="space-y-4">
          <div className="space-y-2">
            <Label htmlFor="adjust-user">{t('admin.balance_adjust.user_id')}</Label>
            <Input
              id="adjust-user"
              value={userId}
              onChange={(e) => setUserId(e.target.value)}
              placeholder="00000000-…"
            />
          </div>
          <div className="grid gap-3 sm:grid-cols-2">
            <div className="space-y-2">
              <Label htmlFor="adjust-amount">{t('admin.balance_adjust.delta_mad')}</Label>
              <Input
                id="adjust-amount"
                type="number"
                step="0.01"
                value={deltaMad}
                onChange={(e) => setDeltaMad(e.target.value)}
                placeholder="100.00"
              />
              <p className="text-xs text-muted-foreground">
                {t('admin.balance_adjust.delta_hint')}
              </p>
            </div>
            <div className="space-y-2">
              <Label>{t('admin.balance_adjust.operation')}</Label>
              <Select value={operation} onValueChange={(v) => setOperation(v as BalanceAdjustOperation)}>
                <SelectTrigger>
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  {OPERATIONS.map((op) => (
                    <SelectItem key={op} value={op}>
                      {t(`payment.operation.${op}`)}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
          </div>
          <div className="space-y-2">
            <Label htmlFor="adjust-reason">{t('admin.balance_adjust.reason')}</Label>
            <Textarea
              id="adjust-reason"
              rows={4}
              value={reason}
              onChange={(e) => setReason(e.target.value)}
            />
          </div>
          <Button onClick={onOpenConfirm} disabled={adjustMut.isPending}>
            {t('common.submit')}
          </Button>
        </CardContent>
      </Card>

      <Dialog open={confirming} onOpenChange={setConfirming}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>{t('admin.balance_adjust.confirm_title')}</DialogTitle>
            <DialogDescription>{t('admin.balance_adjust.warning_banner')}</DialogDescription>
          </DialogHeader>
          <div className="space-y-1 text-sm">
            <div>
              <span className="font-medium">{t('admin.balance_adjust.user_id')} :</span>{' '}
              {userId.slice(0, 12)}…
            </div>
            <div>
              <span className="font-medium">{t('admin.balance_adjust.delta_mad')} :</span>{' '}
              {deltaMad}
            </div>
            <div>
              <span className="font-medium">{t('admin.balance_adjust.operation')} :</span>{' '}
              {t(`payment.operation.${operation}`)}
            </div>
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setConfirming(false)}>
              {t('common.cancel')}
            </Button>
            <Button onClick={onSubmit} disabled={adjustMut.isPending}>
              {t('common.confirm')}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}

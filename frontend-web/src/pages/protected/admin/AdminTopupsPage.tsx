import { useState } from 'react';
import { useTranslation } from 'react-i18next';

import type { TopupView } from '@/api/payment.api';
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
import { useDecideTopup, useTopupsAdmin } from '@/hooks/queries/useAdmin';

type Decision = 'APPROVE' | 'MODIFY' | 'REJECT';

export default function AdminTopupsPage() {
  const { t, i18n } = useTranslation();
  const formatErr = useApiError();

  const [decisionState, setDecisionState] = useState<{
    topup: TopupView;
    decision: Decision;
  } | null>(null);
  const [approvedAmountMad, setApprovedAmountMad] = useState('');
  const [notes, setNotes] = useState('');
  const [error, setError] = useState<string | null>(null);

  const topupsQuery = useTopupsAdmin('EN_ATTENTE_ADMIN');
  const decideMut = useDecideTopup();

  const closeDialog = () => {
    setDecisionState(null);
    setApprovedAmountMad('');
    setNotes('');
    setError(null);
  };

  const onSubmit = () => {
    if (!decisionState) return;
    setError(null);
    decideMut.mutate(
      {
        topupId: decisionState.topup.id,
        body: {
          decision: decisionState.decision,
          approvedAmountMad: decisionState.decision === 'MODIFY' ? approvedAmountMad : undefined,
          notes: notes || undefined,
        },
      },
      {
        onSuccess: () => closeDialog(),
        onError: (e) => setError(formatErr(e)),
      },
    );
  };

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-semibold">{t('admin.topups.title')}</h1>

      {topupsQuery.isLoading ? (
        <Skeleton className="h-64 w-full" />
      ) : (topupsQuery.data?.items.length ?? 0) === 0 ? (
        <Card>
          <CardContent className="py-10 text-center text-sm text-muted-foreground">
            {t('admin.topups.empty')}
          </CardContent>
        </Card>
      ) : (
        <Card>
          <CardContent className="p-0">
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>{t('admin.topups.columns.submitted')}</TableHead>
                  <TableHead>{t('admin.topups.columns.student')}</TableHead>
                  <TableHead>{t('admin.topups.columns.method')}</TableHead>
                  <TableHead>{t('admin.topups.columns.amount')}</TableHead>
                  <TableHead>{t('admin.topups.columns.proof')}</TableHead>
                  <TableHead className="text-end">{t('common.actions')}</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {(topupsQuery.data?.items ?? []).map((row) => (
                  <TableRow key={row.id}>
                    <TableCell>{formatDateTime(row.createdAt, i18n.language)}</TableCell>
                    <TableCell className="text-xs text-muted-foreground">
                      {row.studentId.slice(0, 8)}…
                    </TableCell>
                    <TableCell>{row.method}</TableCell>
                    <TableCell>{formatMoneyMad(row.amountMinor, i18n.language)}</TableCell>
                    <TableCell>
                      {row.proofUrl ? (
                        <a
                          href={row.proofUrl}
                          target="_blank"
                          rel="noreferrer"
                          className="text-primary underline-offset-4 hover:underline"
                        >
                          {t('common.view')}
                        </a>
                      ) : (
                        '—'
                      )}
                    </TableCell>
                    <TableCell className="text-end">
                      <div className="flex flex-wrap justify-end gap-2">
                        <Button
                          size="sm"
                          onClick={() => setDecisionState({ topup: row, decision: 'APPROVE' })}
                        >
                          {t('admin.topups.actions.approve')}
                        </Button>
                        <Button
                          size="sm"
                          variant="outline"
                          onClick={() => {
                            setDecisionState({ topup: row, decision: 'MODIFY' });
                            setApprovedAmountMad('');
                          }}
                        >
                          {t('admin.topups.actions.modify')}
                        </Button>
                        <Button
                          size="sm"
                          variant="destructive"
                          onClick={() => setDecisionState({ topup: row, decision: 'REJECT' })}
                        >
                          {t('admin.topups.actions.reject')}
                        </Button>
                      </div>
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </CardContent>
        </Card>
      )}

      <Dialog open={Boolean(decisionState)} onOpenChange={(o) => !o && closeDialog()}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>
              {decisionState ? t(`admin.topups.dialog.${decisionState.decision}`) : ''}
            </DialogTitle>
          </DialogHeader>
          {error ? (
            <Alert variant="destructive">
              <AlertDescription>{error}</AlertDescription>
            </Alert>
          ) : null}
          {decisionState?.decision === 'MODIFY' ? (
            <div className="space-y-2">
              <Label htmlFor="approved-amount">{t('admin.topups.dialog.approved_amount')}</Label>
              <Input
                id="approved-amount"
                type="number"
                step="0.01"
                min="0"
                value={approvedAmountMad}
                onChange={(e) => setApprovedAmountMad(e.target.value)}
              />
            </div>
          ) : null}
          <div className="space-y-2">
            <Label htmlFor="topup-notes">{t('admin.topups.dialog.notes')}</Label>
            <Textarea
              id="topup-notes"
              rows={3}
              value={notes}
              onChange={(e) => setNotes(e.target.value)}
            />
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={closeDialog}>
              {t('common.cancel')}
            </Button>
            <Button onClick={onSubmit} disabled={decideMut.isPending}>
              {t('common.confirm')}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}

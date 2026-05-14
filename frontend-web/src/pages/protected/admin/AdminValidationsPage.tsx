import { useMemo, useState } from 'react';
import { useTranslation } from 'react-i18next';

import type { ProfPrivateView } from '@/api/profile.api';
import { Alert, AlertDescription } from '@/components/ui/alert';
import { Button } from '@/components/ui/button';
import { Card, CardContent } from '@/components/ui/card';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog';
import { PendingProfRow } from '@/components/admin/PendingProfRow';
import { Skeleton } from '@/components/ui/skeleton';
import {
  Table,
  TableBody,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table';
import { Textarea } from '@/components/ui/textarea';
import { useApiError } from '@/hooks/useApiError';
import {
  usePendingProfs,
  useRejectProf,
  useValidateProf,
} from '@/hooks/queries/useAdmin';

export default function AdminValidationsPage() {
  const { t, i18n } = useTranslation();
  const formatErr = useApiError();
  const [selected, setSelected] = useState<ProfPrivateView | null>(null);
  const [rejectingFor, setRejectingFor] = useState<ProfPrivateView | null>(null);
  const [reason, setReason] = useState('');
  const [error, setError] = useState<string | null>(null);

  const pendingQuery = usePendingProfs({ size: 50 });
  const validateMut = useValidateProf();
  const rejectMut = useRejectProf();
  const busy = validateMut.isPending || rejectMut.isPending;

  const rows = useMemo(() => pendingQuery.data?.items ?? [], [pendingQuery.data]);

  const onValidate = (prof: ProfPrivateView) => {
    setError(null);
    validateMut.mutate(
      { profId: prof.userId },
      { onError: (e) => setError(formatErr(e)) },
    );
  };
  const onConfirmReject = () => {
    if (!rejectingFor) return;
    setError(null);
    rejectMut.mutate(
      { profId: rejectingFor.userId, body: { reason } },
      {
        onSuccess: () => {
          setRejectingFor(null);
          setReason('');
        },
        onError: (e) => setError(formatErr(e)),
      },
    );
  };

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-semibold">{t('admin.validations.title')}</h1>
      {error ? (
        <Alert variant="destructive">
          <AlertDescription>{error}</AlertDescription>
        </Alert>
      ) : null}

      {pendingQuery.isLoading ? (
        <Skeleton className="h-64 w-full" />
      ) : rows.length === 0 ? (
        <Card>
          <CardContent className="py-10 text-center text-sm text-muted-foreground">
            {t('admin.validations.empty')}
          </CardContent>
        </Card>
      ) : (
        <Card>
          <CardContent className="p-0">
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>{t('admin.validations.columns.submitted')}</TableHead>
                  <TableHead>{t('admin.validations.columns.prof')}</TableHead>
                  <TableHead>{t('admin.validations.columns.diplomas')}</TableHead>
                  <TableHead>{t('admin.validations.columns.score')}</TableHead>
                  <TableHead className="text-end">{t('common.actions')}</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {rows.map((p) => (
                  <PendingProfRow
                    key={p.userId}
                    prof={p}
                    locale={i18n.language}
                    onValidate={() => onValidate(p)}
                    onReject={() => {
                      setRejectingFor(p);
                      setReason('');
                    }}
                    onView={() => setSelected(p)}
                    busy={busy}
                  />
                ))}
              </TableBody>
            </Table>
          </CardContent>
        </Card>
      )}

      {/* View drawer / dialog */}
      <Dialog open={Boolean(selected)} onOpenChange={(o) => !o && setSelected(null)}>
        <DialogContent className="max-w-2xl">
          <DialogHeader>
            <DialogTitle>{t('admin.validations.detail_title')}</DialogTitle>
            <DialogDescription>
              {selected?.userId.slice(0, 12) ?? ''}…
            </DialogDescription>
          </DialogHeader>
          <div className="space-y-3 text-sm">
            <div>
              <span className="font-medium">{t('admin.validations.diplomas')} :</span>
              <ul className="mt-2 space-y-1">
                {(selected?.diplomas ?? []).length === 0 ? (
                  <li className="text-muted-foreground">{t('common.none')}</li>
                ) : null}
                {(selected?.diplomas ?? []).map((d, idx) => {
                  const obj = (d ?? {}) as { url?: string; label?: string };
                  return (
                    <li key={idx}>
                      {obj.url ? (
                        <a
                          href={obj.url}
                          target="_blank"
                          rel="noreferrer"
                          className="text-primary underline-offset-4 hover:underline"
                        >
                          {obj.label || obj.url}
                        </a>
                      ) : (
                        <span>{obj.label || JSON.stringify(d)}</span>
                      )}
                    </li>
                  );
                })}
              </ul>
            </div>
            <div>
              <span className="font-medium">{t('admin.validations.ai_score')} :</span>{' '}
              {selected?.aiQcmScore ?? '—'}
            </div>
            <div>
              <span className="font-medium">{t('admin.validations.notes')} :</span>{' '}
              {selected?.adminNotes ?? '—'}
            </div>
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setSelected(null)}>
              {t('common.close')}
            </Button>
            {selected ? (
              <Button onClick={() => onValidate(selected)} disabled={busy}>
                {t('admin.validations.validate')}
              </Button>
            ) : null}
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Reject dialog */}
      <Dialog
        open={Boolean(rejectingFor)}
        onOpenChange={(o) => {
          if (!o) {
            setRejectingFor(null);
            setReason('');
          }
        }}
      >
        <DialogContent>
          <DialogHeader>
            <DialogTitle>{t('admin.validations.reject_dialog.title')}</DialogTitle>
            <DialogDescription>
              {t('admin.validations.reject_dialog.desc')}
            </DialogDescription>
          </DialogHeader>
          <Textarea
            value={reason}
            onChange={(e) => setReason(e.target.value)}
            rows={4}
            placeholder={t('admin.validations.reject_dialog.placeholder')}
            aria-label={t('admin.validations.reject_dialog.placeholder')}
          />
          <DialogFooter>
            <Button
              variant="outline"
              onClick={() => {
                setRejectingFor(null);
                setReason('');
              }}
            >
              {t('common.cancel')}
            </Button>
            <Button variant="destructive" onClick={onConfirmReject} disabled={busy}>
              {t('admin.validations.reject')}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}

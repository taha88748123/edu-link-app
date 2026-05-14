import { useState } from 'react';
import { Download } from 'lucide-react';
import { useTranslation } from 'react-i18next';

import { Alert, AlertDescription } from '@/components/ui/alert';
import { adminApi } from '@/api/admin.api';
import { Button } from '@/components/ui/button';
import { Card, CardContent } from '@/components/ui/card';
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
import { downloadBlob, formatAuditOperation } from '@/lib/admin';
import { formatDateTime, formatMoneyMad } from '@/lib/format';
import { useApiError } from '@/hooks/useApiError';
import { useUserAudit } from '@/hooks/queries/useAdmin';

export default function AdminAuditPage() {
  const { t, i18n } = useTranslation();
  const formatErr = useApiError();

  const [userIdInput, setUserIdInput] = useState('');
  const [activeUserId, setActiveUserId] = useState<string | undefined>();
  const [from, setFrom] = useState('');
  const [to, setTo] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [exporting, setExporting] = useState(false);

  const auditQuery = useUserAudit(activeUserId, {
    from: from ? new Date(from).toISOString() : undefined,
    to: to ? new Date(to).toISOString() : undefined,
  });

  const onSearch = () => {
    setError(null);
    const trimmed = userIdInput.trim();
    if (!trimmed) {
      setError(t('admin.audit.errors.user_id_required'));
      return;
    }
    setActiveUserId(trimmed);
  };

  const onExport = async () => {
    setError(null);
    setExporting(true);
    try {
      const blob = await adminApi.exportAuditCsv({
        userId: activeUserId,
        from: from ? new Date(from).toISOString() : undefined,
        to: to ? new Date(to).toISOString() : undefined,
      });
      downloadBlob(blob, `audit-log-${new Date().toISOString().slice(0, 10)}.csv`);
    } catch (e) {
      setError(formatErr(e));
    } finally {
      setExporting(false);
    }
  };

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-semibold">{t('admin.audit.title')}</h1>

      <Card>
        <CardContent className="space-y-3 p-4">
          <div className="grid gap-3 sm:grid-cols-4">
            <div className="space-y-1 sm:col-span-2">
              <Label htmlFor="audit-user">{t('admin.audit.filter.user_id')}</Label>
              <Input
                id="audit-user"
                value={userIdInput}
                onChange={(e) => setUserIdInput(e.target.value)}
                placeholder="00000000-…"
              />
            </div>
            <div className="space-y-1">
              <Label htmlFor="audit-from">{t('admin.audit.filter.from')}</Label>
              <Input
                id="audit-from"
                type="date"
                value={from}
                onChange={(e) => setFrom(e.target.value)}
              />
            </div>
            <div className="space-y-1">
              <Label htmlFor="audit-to">{t('admin.audit.filter.to')}</Label>
              <Input
                id="audit-to"
                type="date"
                value={to}
                onChange={(e) => setTo(e.target.value)}
              />
            </div>
          </div>
          <div className="flex flex-wrap items-center gap-2">
            <Button onClick={onSearch}>{t('common.search')}</Button>
            <Button
              variant="outline"
              onClick={() => void onExport()}
              disabled={exporting}
              className="gap-2"
            >
              <Download className="h-4 w-4" />
              {t('admin.audit.export_csv')}
            </Button>
          </div>
        </CardContent>
      </Card>

      {error ? (
        <Alert variant="destructive">
          <AlertDescription>{error}</AlertDescription>
        </Alert>
      ) : null}

      {!activeUserId ? (
        <Card>
          <CardContent className="py-10 text-center text-sm text-muted-foreground">
            {t('admin.audit.intro')}
          </CardContent>
        </Card>
      ) : auditQuery.isLoading ? (
        <Skeleton className="h-64 w-full" />
      ) : (auditQuery.data?.items.length ?? 0) === 0 ? (
        <Card>
          <CardContent className="py-10 text-center text-sm text-muted-foreground">
            {t('admin.audit.empty')}
          </CardContent>
        </Card>
      ) : (
        <Card>
          <CardContent className="p-0">
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>{t('admin.audit.columns.when')}</TableHead>
                  <TableHead>{t('admin.audit.columns.operation')}</TableHead>
                  <TableHead>{t('admin.audit.columns.amount')}</TableHead>
                  <TableHead>{t('admin.audit.columns.balance')}</TableHead>
                  <TableHead>{t('admin.audit.columns.reference')}</TableHead>
                  <TableHead>{t('admin.audit.columns.reason')}</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {(auditQuery.data?.items ?? []).map((row) => (
                  <TableRow key={row.id}>
                    <TableCell>{formatDateTime(row.createdAt, i18n.language)}</TableCell>
                    <TableCell>{t(formatAuditOperation(row.operation))}</TableCell>
                    <TableCell>{formatMoneyMad(row.amountMinor, i18n.language)}</TableCell>
                    <TableCell className="text-xs text-muted-foreground">
                      {formatMoneyMad(row.balanceBefore, i18n.language)} →{' '}
                      {formatMoneyMad(row.balanceAfter, i18n.language)}
                    </TableCell>
                    <TableCell className="text-xs">
                      {row.referenceType ? `${row.referenceType} · ` : ''}
                      {row.referenceId ? row.referenceId.slice(0, 8) + '…' : '—'}
                    </TableCell>
                    <TableCell className="text-xs">{row.reason ?? '—'}</TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </CardContent>
        </Card>
      )}
    </div>
  );
}

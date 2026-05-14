import { useTranslation } from 'react-i18next';

import type { TopupView } from '@/api/payment.api';
import { Badge } from '@/components/ui/badge';
import { Card, CardContent } from '@/components/ui/card';
import { Skeleton } from '@/components/ui/skeleton';
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table';
import { useMyTopups } from '@/hooks/queries/usePayment';
import { formatDateTime, formatMoneyMad } from '@/lib/format';

export default function TopupsPage() {
  const { t, i18n } = useTranslation();
  const topupsQuery = useMyTopups({ size: 50 });

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-semibold">{t('payment.topup.list_title')}</h1>

      {topupsQuery.isLoading ? (
        <Skeleton className="h-40 w-full" />
      ) : (topupsQuery.data ?? []).length === 0 ? (
        <Card>
          <CardContent className="py-10 text-center text-sm text-muted-foreground">
            {t('payment.topup.list_empty')}
          </CardContent>
        </Card>
      ) : (
        <Card>
          <CardContent className="p-0">
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>{t('common.date')}</TableHead>
                  <TableHead>{t('payment.topup.method')}</TableHead>
                  <TableHead className="text-end">{t('common.amount')}</TableHead>
                  <TableHead>{t('common.status')}</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {(topupsQuery.data ?? []).map((row: TopupView) => (
                  <TableRow key={row.id}>
                    <TableCell className="text-xs text-muted-foreground">
                      {formatDateTime(row.createdAt, i18n.language)}
                    </TableCell>
                    <TableCell>{t(`payment.topup.method_${row.method}`)}</TableCell>
                    <TableCell className="text-end font-medium">
                      {formatMoneyMad(row.amountMinor, i18n.language)}
                    </TableCell>
                    <TableCell>
                      <Badge variant={statusVariant(row.status)}>
                        {t(`payment.topup.status.${row.status}`)}
                      </Badge>
                    </TableCell>
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

function statusVariant(s: string): 'success' | 'warning' | 'danger' | 'secondary' {
  if (s === 'APPROVED') return 'success';
  if (s === 'REJECTED') return 'danger';
  if (s === 'MODIFIED') return 'secondary';
  return 'warning';
}

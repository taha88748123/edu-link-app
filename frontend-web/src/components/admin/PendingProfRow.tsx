import { useTranslation } from 'react-i18next';

import type { ProfPrivateView } from '@/api/profile.api';
import { Button } from '@/components/ui/button';
import { TableCell, TableRow } from '@/components/ui/table';
import { formatDate } from '@/lib/format';

export function PendingProfRow({
  prof,
  locale,
  onValidate,
  onReject,
  onView,
  busy,
}: {
  prof: ProfPrivateView;
  locale: string;
  onValidate: () => void;
  onReject: () => void;
  onView: () => void;
  busy?: boolean;
}) {
  const { t } = useTranslation();
  return (
    <TableRow>
      <TableCell className="text-xs text-muted-foreground">
        {prof.submittedAt ? formatDate(prof.submittedAt, locale) : '—'}
      </TableCell>
      <TableCell className="font-medium">{prof.userId.slice(0, 8)}…</TableCell>
      <TableCell>{prof.diplomas?.length ?? 0}</TableCell>
      <TableCell>{prof.aiQcmScore ?? '—'}</TableCell>
      <TableCell className="text-end">
        <div className="flex flex-wrap justify-end gap-2">
          <Button size="sm" variant="outline" onClick={onView} disabled={busy}>
            {t('common.view')}
          </Button>
          <Button size="sm" onClick={onValidate} disabled={busy}>
            {t('admin.validations.validate')}
          </Button>
          <Button size="sm" variant="destructive" onClick={onReject} disabled={busy}>
            {t('admin.validations.reject')}
          </Button>
        </div>
      </TableCell>
    </TableRow>
  );
}

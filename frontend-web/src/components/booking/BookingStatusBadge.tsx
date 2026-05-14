import { useTranslation } from 'react-i18next';

import { Badge, type BadgeVariant } from '@/components/ui/badge';
import type { BookingStatus } from '@/api/enums';

/**
 * Mapping kept in module scope so it can be unit-tested without rendering.
 * Exported for `BookingStatusBadge.test.tsx`.
 */
export const STATUS_VARIANT: Record<BookingStatus, BadgeVariant> = {
  DEMANDEE: 'warning',
  ACCEPTEE_PAR_PROF: 'info',
  EN_COURS: 'info',
  TERMINEE: 'secondary',
  CONFIRMEE: 'success',
  ANNULEE: 'outline',
  LITIGE: 'danger',
};

export function BookingStatusBadge({ status }: { status: BookingStatus }) {
  const { t } = useTranslation();
  return (
    <Badge variant={STATUS_VARIANT[status]} aria-label={t(`booking.status.${status}`)}>
      {t(`booking.status.${status}`)}
    </Badge>
  );
}

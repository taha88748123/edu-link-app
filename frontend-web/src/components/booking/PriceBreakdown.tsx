import { useTranslation } from 'react-i18next';

import { formatMoneyMad } from '@/lib/format';

export interface PriceBreakdownProps {
  baseAmountMinor: number;
  platformFeeMinor: number;
  totalAmountMinor: number;
  className?: string;
}

/**
 * Small key/value list rendering a booking's price decomposition. All amounts
 * are in centimes; conversion to MAD is delegated to `formatMoneyMad`.
 */
export function PriceBreakdown({
  baseAmountMinor,
  platformFeeMinor,
  totalAmountMinor,
  className,
}: PriceBreakdownProps) {
  const { t, i18n } = useTranslation();
  return (
    <dl className={className}>
      <Row label={t('booking.price_breakdown.base')} value={formatMoneyMad(baseAmountMinor, i18n.language)} />
      <Row label={t('booking.price_breakdown.fee')} value={formatMoneyMad(platformFeeMinor, i18n.language)} />
      <Row
        label={t('booking.price_breakdown.total')}
        value={formatMoneyMad(totalAmountMinor, i18n.language)}
        strong
      />
    </dl>
  );
}

function Row({ label, value, strong }: { label: string; value: string; strong?: boolean }) {
  return (
    <div className="flex items-baseline justify-between border-b py-2 last:border-b-0">
      <dt className="text-sm text-muted-foreground">{label}</dt>
      <dd className={strong ? 'text-base font-semibold' : 'text-sm'}>{value}</dd>
    </div>
  );
}

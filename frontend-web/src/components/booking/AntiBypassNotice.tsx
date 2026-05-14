import { ShieldAlert } from 'lucide-react';
import { useTranslation } from 'react-i18next';

import { Alert, AlertDescription } from '@/components/ui/alert';

/**
 * Banner shown on booking-related views whenever contact info would otherwise
 * be displayed but the booking is not yet ACCEPTEE_PAR_PROF.
 * See `src/lib/booking.ts#isContactVisible`.
 */
export function AntiBypassNotice({ className }: { className?: string }) {
  const { t } = useTranslation();
  return (
    <Alert className={className}>
      <ShieldAlert className="h-4 w-4" />
      <AlertDescription>{t('booking.anti_bypass')}</AlertDescription>
    </Alert>
  );
}

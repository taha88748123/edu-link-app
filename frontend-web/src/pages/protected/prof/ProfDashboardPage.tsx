import { useMemo } from 'react';
import { Link } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { Calendar, ClipboardList, Tag } from 'lucide-react';

import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { useAuthStore } from '@/auth/auth.store';
import { useMyBookings } from '@/hooks/queries/useBookings';
import { useMyBalance } from '@/hooks/queries/usePayment';
import { useMyProfile } from '@/hooks/queries/useProfile';
import { formatMoneyMad } from '@/lib/format';

export default function ProfDashboardPage() {
  const { t, i18n } = useTranslation();
  const user = useAuthStore((s) => s.user);
  const display = user?.email || user?.phone || '';

  const balance = useMyBalance();
  const bookings = useMyBookings({ size: 100 });
  const profile = useMyProfile();

  const pending = useMemo(
    () => (bookings.data ?? []).filter((b) => b.status === 'DEMANDEE').length,
    [bookings.data],
  );

  const validationStatus = profile.data?.prof?.status ?? '—';
  const avgStars = profile.data?.prof?.avgStars ?? null;

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-semibold">{t('dashboard.welcome', { name: display })}</h1>

      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <Kpi
          title={t('dashboard.kpi.balance')}
          value={balance.isLoading ? '…' : formatMoneyMad(balance.data?.balanceMinor ?? 0, i18n.language)}
        />
        <Kpi
          title={t('dashboard.kpi.pending_bookings')}
          value={bookings.isLoading ? '…' : String(pending)}
        />
        <Kpi
          title={t('dashboard.kpi.validation_status')}
          value={profile.isLoading ? '…' : validationStatus}
        />
        <Kpi
          title={t('dashboard.kpi.rating')}
          value={avgStars != null ? Number(avgStars).toFixed(1) : '—'}
        />
      </div>

      <Card>
        <CardHeader>
          <CardTitle className="text-lg">{t('dashboard.quick_actions')}</CardTitle>
        </CardHeader>
        <CardContent className="flex flex-wrap gap-2">
          <Button asChild variant="outline" className="gap-2">
            <Link to="/prof/calendar">
              <Calendar className="h-4 w-4" />
              {t('dashboard.prof.calendar')}
            </Link>
          </Button>
          <Button asChild variant="outline" className="gap-2">
            <Link to="/prof/pricing">
              <Tag className="h-4 w-4" />
              {t('dashboard.prof.pricing')}
            </Link>
          </Button>
          <Button asChild variant="outline" className="gap-2">
            <Link to="/prof/bookings">
              <ClipboardList className="h-4 w-4" />
              {t('dashboard.prof.bookings')}
            </Link>
          </Button>
        </CardContent>
      </Card>
    </div>
  );
}

function Kpi({ title, value }: { title: string; value: string }) {
  return (
    <Card>
      <CardHeader className="pb-2">
        <CardTitle className="text-sm font-medium text-muted-foreground">{title}</CardTitle>
      </CardHeader>
      <CardContent>
        <div className="text-2xl font-semibold">{value}</div>
      </CardContent>
    </Card>
  );
}

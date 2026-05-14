import { useMemo } from 'react';
import { Link } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { ClipboardList, Search, Wallet } from 'lucide-react';

import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { useAuthStore } from '@/auth/auth.store';
import { useMyBookings } from '@/hooks/queries/useBookings';
import { useMyBalance } from '@/hooks/queries/usePayment';
import { formatMoneyMad } from '@/lib/format';
import { bookingTab } from '@/lib/booking';

export default function StudentDashboardPage() {
  const { t, i18n } = useTranslation();
  const user = useAuthStore((s) => s.user);
  const display = user?.email || user?.phone || '';

  const balance = useMyBalance();
  const bookings = useMyBookings({ size: 100 });

  const counts = useMemo(() => {
    let upcoming = 0;
    let pending = 0;
    for (const b of bookings.data ?? []) {
      if (bookingTab(b.status) === 'upcoming') upcoming++;
      if (b.status === 'DEMANDEE') pending++;
    }
    return { upcoming, pending };
  }, [bookings.data]);

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-semibold">{t('dashboard.welcome', { name: display })}</h1>

      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <Kpi
          title={t('dashboard.kpi.balance')}
          value={
            balance.isLoading
              ? '…'
              : formatMoneyMad(balance.data?.balanceMinor ?? 0, i18n.language)
          }
        />
        <Kpi
          title={t('dashboard.kpi.upcoming_sessions')}
          value={bookings.isLoading ? '…' : String(counts.upcoming)}
        />
        <Kpi
          title={t('dashboard.kpi.pending_bookings')}
          value={bookings.isLoading ? '…' : String(counts.pending)}
        />
        <Kpi title={t('dashboard.kpi.hours_done')} value="—" />
      </div>

      <Card>
        <CardHeader>
          <CardTitle className="text-lg">{t('dashboard.quick_actions')}</CardTitle>
        </CardHeader>
        <CardContent className="flex flex-wrap gap-2">
          <Button asChild variant="outline" className="gap-2">
            <Link to="/student/discover">
              <Search className="h-4 w-4" />
              {t('dashboard.student.find_tutor')}
            </Link>
          </Button>
          <Button asChild variant="outline" className="gap-2">
            <Link to="/student/wallet">
              <Wallet className="h-4 w-4" />
              {t('dashboard.student.topup_wallet')}
            </Link>
          </Button>
          <Button asChild variant="outline" className="gap-2">
            <Link to="/student/bookings">
              <ClipboardList className="h-4 w-4" />
              {t('dashboard.student.view_bookings')}
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

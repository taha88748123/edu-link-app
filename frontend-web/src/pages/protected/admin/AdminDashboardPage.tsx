import { useTranslation } from 'react-i18next';
import { Link } from 'react-router-dom';
import {
  ClipboardCheck,
  Coins,
  CreditCard,
  Gavel,
  ScrollText,
  ShieldAlert,
} from 'lucide-react';

import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { AdminKpiCard } from '@/components/admin/AdminKpiCard';
import { useAuthStore } from '@/auth/auth.store';
import {
  useAdminBookings,
  usePayoutsAdmin,
  usePendingProfs,
  useTopupsAdmin,
} from '@/hooks/queries/useAdmin';

export default function AdminDashboardPage() {
  const { t } = useTranslation();
  const user = useAuthStore((s) => s.user);
  const display = user?.email || user?.phone || '';

  const profsQuery = usePendingProfs({ size: 1 });
  const topupsQuery = useTopupsAdmin('EN_ATTENTE_ADMIN', 0);
  const payoutsQuery = usePayoutsAdmin('REQUESTED', 0);
  const disputesQuery = useAdminBookings({ status: 'LITIGE', size: 1 });

  const profsCount =
    profsQuery.data?.meta?.total ?? profsQuery.data?.items.length ?? 0;
  const topupsCount =
    topupsQuery.data?.meta?.total ?? topupsQuery.data?.items.length ?? 0;
  const payoutsCount =
    payoutsQuery.data?.meta?.total ?? payoutsQuery.data?.items.length ?? 0;
  const disputesCount =
    disputesQuery.data?.meta?.total ?? disputesQuery.data?.items.length ?? 0;

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-semibold">{t('dashboard.welcome', { name: display })}</h1>

      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <AdminKpiCard
          icon={ClipboardCheck}
          title={t('dashboard.kpi.pending_profs')}
          value={profsCount}
          to="/admin/validations"
          loading={profsQuery.isLoading}
        />
        <AdminKpiCard
          icon={Coins}
          title={t('dashboard.kpi.pending_topups')}
          value={topupsCount}
          to="/admin/topups"
          loading={topupsQuery.isLoading}
        />
        <AdminKpiCard
          icon={CreditCard}
          title={t('dashboard.kpi.pending_payouts')}
          value={payoutsCount}
          to="/admin/payouts"
          loading={payoutsQuery.isLoading}
        />
        <AdminKpiCard
          icon={Gavel}
          title={t('dashboard.kpi.open_disputes')}
          value={disputesCount}
          to="/admin/disputes"
          loading={disputesQuery.isLoading}
        />
      </div>

      <Card>
        <CardHeader>
          <CardTitle className="text-lg">{t('dashboard.quick_actions')}</CardTitle>
        </CardHeader>
        <CardContent className="flex flex-wrap gap-2">
          <Button asChild variant="outline" className="gap-2">
            <Link to="/admin/validations">
              <ClipboardCheck className="h-4 w-4" />
              {t('dashboard.admin.validations')}
            </Link>
          </Button>
          <Button asChild variant="outline" className="gap-2">
            <Link to="/admin/topups">
              <Coins className="h-4 w-4" />
              {t('dashboard.admin.topups')}
            </Link>
          </Button>
          <Button asChild variant="outline" className="gap-2">
            <Link to="/admin/payouts">
              <CreditCard className="h-4 w-4" />
              {t('dashboard.admin.payouts')}
            </Link>
          </Button>
          <Button asChild variant="outline" className="gap-2">
            <Link to="/admin/disputes">
              <Gavel className="h-4 w-4" />
              {t('dashboard.admin.disputes')}
            </Link>
          </Button>
          <Button asChild variant="outline" className="gap-2">
            <Link to="/admin/audit">
              <ScrollText className="h-4 w-4" />
              {t('nav.audit')}
            </Link>
          </Button>
          <Button asChild variant="outline" className="gap-2">
            <Link to="/admin/balance-adjust">
              <ShieldAlert className="h-4 w-4" />
              {t('admin.balance_adjust.short_title')}
            </Link>
          </Button>
        </CardContent>
      </Card>
    </div>
  );
}

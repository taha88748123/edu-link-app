import { useTranslation } from 'react-i18next';
import { Calendar, ClipboardList, Tag } from 'lucide-react';

import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { useAuthStore } from '@/auth/auth.store';
import { formatMoneyMad } from '@/lib/format';

export default function ProfDashboardPage() {
  const { t, i18n } = useTranslation();
  const user = useAuthStore((s) => s.user);
  const display = user?.email || user?.phone || '';

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-semibold">{t('dashboard.welcome', { name: display })}</h1>

      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <Kpi title={t('dashboard.kpi.balance')} value={formatMoneyMad(0, i18n.language)} />
        <Kpi title={t('dashboard.kpi.upcoming_sessions')} value="0" />
        <Kpi title={t('dashboard.kpi.validation_status')} value="—" />
        <Kpi title={t('dashboard.kpi.rating')} value="—" />
      </div>

      <Card>
        <CardHeader>
          <CardTitle className="text-lg">{t('dashboard.quick_actions')}</CardTitle>
        </CardHeader>
        <CardContent className="flex flex-wrap gap-2">
          <SoonButton icon={<Calendar className="h-4 w-4" />} label={t('dashboard.prof.calendar')} />
          <SoonButton icon={<Tag className="h-4 w-4" />} label={t('dashboard.prof.pricing')} />
          <SoonButton icon={<ClipboardList className="h-4 w-4" />} label={t('dashboard.prof.bookings')} />
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

function SoonButton({ icon, label }: { icon: React.ReactNode; label: string }) {
  const { t } = useTranslation();
  return (
    <Button variant="outline" disabled className="gap-2">
      {icon}
      <span>{label}</span>
      <span className="ms-2 rounded bg-muted px-1.5 py-0.5 text-[10px] uppercase">
        {t('common.soon')}
      </span>
    </Button>
  );
}

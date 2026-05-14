import { Link } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { GraduationCap, Home, Building2 } from 'lucide-react';

import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';

export default function LandingPage() {
  const { t } = useTranslation();
  return (
    <div className="space-y-10 text-center">
      <section className="space-y-4">
        <h1 className="text-3xl font-bold sm:text-4xl">{t('landing.hero_title')}</h1>
        <p className="text-muted-foreground sm:text-lg">{t('landing.hero_subtitle')}</p>
        <div className="flex flex-col items-stretch justify-center gap-2 sm:flex-row sm:items-center">
          <Button asChild size="lg">
            <Link to="/register">{t('landing.cta_start')}</Link>
          </Button>
          <Button asChild size="lg" variant="outline">
            <Link to="/login">{t('landing.cta_login')}</Link>
          </Button>
        </div>
      </section>

      <section className="grid grid-cols-2 gap-3 text-sm text-muted-foreground sm:grid-cols-2">
        <div className="rounded-md border bg-card p-3">
          {t('landing.stats_profs', { count: 250 })}
        </div>
        <div className="rounded-md border bg-card p-3">
          {t('landing.stats_sessions', { count: 4200 })}
        </div>
      </section>

      <section className="space-y-4">
        <h2 className="text-xl font-semibold">{t('landing.modalities_title')}</h2>
        <div className="grid gap-3 sm:grid-cols-3">
          <ModalityCard
            icon={<GraduationCap className="h-5 w-5" />}
            title={t('modality.ONLINE')}
            description={t('landing.modality_online_desc')}
          />
          <ModalityCard
            icon={<Home className="h-5 w-5" />}
            title={t('modality.DOMICILE')}
            description={t('landing.modality_domicile_desc')}
          />
          <ModalityCard
            icon={<Building2 className="h-5 w-5" />}
            title={t('modality.LOCAL')}
            description={t('landing.modality_local_desc')}
          />
        </div>
      </section>
    </div>
  );
}

function ModalityCard({
  icon,
  title,
  description,
}: {
  icon: React.ReactNode;
  title: string;
  description: string;
}) {
  return (
    <Card>
      <CardHeader className="pb-2">
        <CardTitle className="flex items-center gap-2 text-base">
          {icon}
          <span>{title}</span>
        </CardTitle>
      </CardHeader>
      <CardContent className="text-sm text-muted-foreground text-start">{description}</CardContent>
    </Card>
  );
}

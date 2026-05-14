import { Link } from 'react-router-dom';
import { useTranslation } from 'react-i18next';

import { Button } from '@/components/ui/button';

export default function NotFoundPage() {
  const { t } = useTranslation();
  return (
    <div className="flex min-h-screen flex-col items-center justify-center gap-4 px-4 text-center">
      <h1 className="text-5xl font-bold">404</h1>
      <h2 className="text-xl font-semibold">{t('page.not_found_title')}</h2>
      <p className="max-w-md text-muted-foreground">{t('page.not_found_desc')}</p>
      <Button asChild>
        <Link to="/">{t('page.not_found_cta')}</Link>
      </Button>
    </div>
  );
}

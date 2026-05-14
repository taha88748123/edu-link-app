import { useTranslation } from 'react-i18next';
import { useNavigate } from 'react-router-dom';

import { Button } from '@/components/ui/button';
import { useAuth } from '@/auth/useAuth';

export default function ForbiddenPage() {
  const { t } = useTranslation();
  const { logout } = useAuth();
  const navigate = useNavigate();

  return (
    <div className="flex min-h-[60vh] flex-col items-center justify-center gap-4 text-center">
      <h1 className="text-5xl font-bold">403</h1>
      <h2 className="text-xl font-semibold">{t('page.forbidden_title')}</h2>
      <p className="max-w-md text-muted-foreground">{t('page.forbidden_desc')}</p>
      <div className="flex gap-2">
        <Button variant="outline" onClick={() => navigate(-1)}>
          {t('common.back')}
        </Button>
        <Button onClick={() => void logout()}>{t('common.logout')}</Button>
      </div>
    </div>
  );
}

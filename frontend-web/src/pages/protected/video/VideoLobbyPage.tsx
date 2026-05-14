import { Link, useParams } from 'react-router-dom';
import { useTranslation } from 'react-i18next';

import { Alert, AlertDescription } from '@/components/ui/alert';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { DeviceCheckPanel } from '@/components/video/DeviceCheckPanel';
import { Skeleton } from '@/components/ui/skeleton';
import { useSession } from '@/hooks/queries/useVideo';
import { formatDateTime } from '@/lib/format';

export default function VideoLobbyPage() {
  const { sessionId = '' } = useParams<{ sessionId: string }>();
  const { t, i18n } = useTranslation();
  const sessionQuery = useSession(sessionId);

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-semibold">{t('video.lobby.title')}</h1>
      </div>

      {sessionQuery.isLoading ? (
        <Skeleton className="h-64 w-full" />
      ) : !sessionQuery.data ? (
        <Alert variant="destructive">
          <AlertDescription>{t('errors.RESOURCE_NOT_FOUND')}</AlertDescription>
        </Alert>
      ) : (
        <div className="grid gap-6 lg:grid-cols-2">
          <DeviceCheckPanel />
          <Card>
            <CardHeader>
              <CardTitle className="text-lg">{t('video.lobby.session_info')}</CardTitle>
            </CardHeader>
            <CardContent className="space-y-3 text-sm">
              <div className="grid grid-cols-2 gap-2">
                <span className="text-muted-foreground">{t('video.field.scheduled')}</span>
                <span>{formatDateTime(sessionQuery.data.scheduledStart, i18n.language)}</span>
              </div>
              <div className="grid grid-cols-2 gap-2">
                <span className="text-muted-foreground">{t('video.field.status')}</span>
                <span>{t(`video.status.${sessionQuery.data.status}`)}</span>
              </div>
              <p className="text-xs text-muted-foreground">{t('video.lobby.privacy_notice')}</p>
              <Button asChild size="lg" className="w-full">
                <Link to={`/video/sessions/${sessionId}`}>{t('video.lobby.join_now')}</Link>
              </Button>
            </CardContent>
          </Card>
        </div>
      )}
    </div>
  );
}

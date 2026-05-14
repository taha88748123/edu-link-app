import { useEffect, useMemo, useRef, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import {
  GridLayout,
  LiveKitRoom,
  ParticipantTile,
  RoomAudioRenderer,
  useTracks,
} from '@livekit/components-react';
import { Track } from 'livekit-client';
import '@livekit/components-styles';

import { Alert, AlertDescription } from '@/components/ui/alert';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Skeleton } from '@/components/ui/skeleton';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { Textarea } from '@/components/ui/textarea';
import { VideoControlsBar, type VideoControlsState } from '@/components/video/VideoControlsBar';
import { useApiError } from '@/hooks/useApiError';
import {
  useCreateArtifact,
  useEndSession,
  useGrantConsent,
  useIssueToken,
  useSession,
  useStartSession,
} from '@/hooks/queries/useVideo';

/**
 * Video room page. Logical flow:
 *   1. Fetch session metadata (poll every 5s so we pick up peer's consent).
 *   2. Auto-call :start on the first render if status === 'SCHEDULED'.
 *   3. Mint a LiveKit JWT via :token, then mount <LiveKitRoom> with it.
 *   4. If LiveKit connect fails (dev mode with `driver=logging`, no real server),
 *      we surface an alert but the rest of the page (controls, side panel,
 *      :end button) still works.
 */
export default function VideoRoomPage() {
  const { sessionId = '' } = useParams<{ sessionId: string }>();
  const { t } = useTranslation();
  const navigate = useNavigate();
  const formatApiError = useApiError();

  // Poll session metadata every 5s so consent state stays fresh.
  const sessionQuery = useSession(sessionId, 5_000);

  const issueTokenMut = useIssueToken();
  const startMut = useStartSession();
  const endMut = useEndSession();
  const consentMut = useGrantConsent();
  const createArtifactMut = useCreateArtifact();

  const [token, setToken] = useState<string | null>(null);
  const [serverUrl, setServerUrl] = useState<string | null>(null);
  const [livekitError, setLivekitError] = useState<string | null>(null);
  const [controls, setControls] = useState<VideoControlsState>({
    camOn: true,
    micOn: true,
    screenShare: false,
  });
  const [noteDraft, setNoteDraft] = useState('');
  const autoStartedRef = useRef(false);
  const tokenRequestedRef = useRef(false);

  // Auto-start on first render if SCHEDULED.
  useEffect(() => {
    const data = sessionQuery.data;
    if (!data || autoStartedRef.current) return;
    if (data.status === 'SCHEDULED') {
      autoStartedRef.current = true;
      startMut.mutate(sessionId);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [sessionQuery.data?.status]);

  // Issue token once we have a session.
  useEffect(() => {
    if (!sessionQuery.data || tokenRequestedRef.current) return;
    if (sessionQuery.data.status === 'ENDED' || sessionQuery.data.status === 'EXPIRED') return;
    tokenRequestedRef.current = true;
    issueTokenMut.mutate(sessionId, {
      onSuccess: (res) => {
        setToken(res.token);
        setServerUrl(res.livekitUrl);
      },
      onError: (err) => {
        setLivekitError(formatApiError(err));
      },
    });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [sessionQuery.data?.id]);

  const handleEnd = () => {
    endMut.mutate(sessionId, {
      onSettled: () => {
        navigate(-1);
      },
    });
  };

  const handleGrantConsent = () => {
    consentMut.mutate({ id: sessionId, body: { scope: 'RECORDING' } });
  };

  const handleSaveNote = () => {
    const trimmed = noteDraft.trim();
    if (!trimmed) return;
    createArtifactMut.mutate(
      {
        id: sessionId,
        body: { type: 'NOTE', content: JSON.stringify({ text: trimmed }) },
      },
      {
        onSuccess: () => setNoteDraft(''),
      },
    );
  };

  const handleAddWhiteboardSnapshot = () => {
    createArtifactMut.mutate({
      id: sessionId,
      body: {
        type: 'WHITEBOARD',
        content: JSON.stringify({ schema: 'v1', strokes: [] }),
      },
    });
  };

  const handleAddQuiz = () => {
    createArtifactMut.mutate({
      id: sessionId,
      body: { type: 'QUIZ', content: JSON.stringify({ schema: 'v1', questions: [] }) },
    });
  };

  if (sessionQuery.isLoading) {
    return <Skeleton className="h-96 w-full" />;
  }
  if (!sessionQuery.data) {
    return (
      <Alert variant="destructive">
        <AlertDescription>{t('errors.RESOURCE_NOT_FOUND')}</AlertDescription>
      </Alert>
    );
  }

  const session = sessionQuery.data;
  const recordingEnabled = session.recordingEnabled ?? false;

  return (
    <div className="space-y-4">
      <header className="flex flex-wrap items-center justify-between gap-2">
        <div className="flex items-center gap-3">
          <h1 className="text-xl font-semibold">{t('video.room.title')}</h1>
          <Badge variant="outline">{t(`video.status.${session.status}`)}</Badge>
        </div>
        <Button type="button" variant="destructive" onClick={handleEnd} disabled={endMut.isPending}>
          {t('video.controls.leave')}
        </Button>
      </header>

      {livekitError ? (
        <Alert variant="destructive">
          <AlertDescription>{livekitError}</AlertDescription>
        </Alert>
      ) : null}

      <div className="grid gap-4 lg:grid-cols-[1fr_320px]">
        <Card>
          <CardHeader className="pb-2">
            <CardTitle className="text-base">{t('video.room.grid_title')}</CardTitle>
          </CardHeader>
          <CardContent className="space-y-3">
            {token && serverUrl ? (
              <LiveKitRoomShell
                token={token}
                serverUrl={serverUrl}
                controls={controls}
                onError={(err) => setLivekitError(err.message || t('video.errors.connect_failed'))}
              />
            ) : (
              <Alert>
                <AlertDescription>
                  {t('video.room.connecting')}
                </AlertDescription>
              </Alert>
            )}

            {!recordingEnabled ? (
              <Alert>
                <AlertDescription className="flex items-center justify-between gap-2">
                  <span>{t('video.consent.banner')}</span>
                  <Button
                    type="button"
                    size="sm"
                    variant="outline"
                    onClick={handleGrantConsent}
                    disabled={consentMut.isPending}
                  >
                    {t('video.consent.grant')}
                  </Button>
                </AlertDescription>
              </Alert>
            ) : null}

            <VideoControlsBar
              state={controls}
              onToggleCam={() => setControls((c) => ({ ...c, camOn: !c.camOn }))}
              onToggleMic={() => setControls((c) => ({ ...c, micOn: !c.micOn }))}
              onToggleScreen={() => setControls((c) => ({ ...c, screenShare: !c.screenShare }))}
              onEnd={handleEnd}
              disabled={endMut.isPending}
            />
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="pb-2">
            <CardTitle className="text-base">{t('video.side_panel.title')}</CardTitle>
          </CardHeader>
          <CardContent>
            <Tabs defaultValue="whiteboard">
              <TabsList className="grid grid-cols-3">
                <TabsTrigger value="whiteboard">{t('video.side_panel.whiteboard')}</TabsTrigger>
                <TabsTrigger value="quiz">{t('video.side_panel.quiz')}</TabsTrigger>
                <TabsTrigger value="notes">{t('video.side_panel.notes')}</TabsTrigger>
              </TabsList>
              <TabsContent value="whiteboard" className="space-y-2">
                <p className="text-xs text-muted-foreground">
                  {t('video.side_panel.whiteboard_placeholder')}
                </p>
                <Button
                  type="button"
                  variant="outline"
                  size="sm"
                  onClick={handleAddWhiteboardSnapshot}
                  disabled={createArtifactMut.isPending}
                >
                  {t('video.side_panel.snapshot')}
                </Button>
              </TabsContent>
              <TabsContent value="quiz" className="space-y-2">
                <p className="text-xs text-muted-foreground">
                  {t('video.side_panel.quiz_placeholder')}
                </p>
                <Button
                  type="button"
                  variant="outline"
                  size="sm"
                  onClick={handleAddQuiz}
                  disabled={createArtifactMut.isPending}
                >
                  {t('video.side_panel.add_quiz')}
                </Button>
              </TabsContent>
              <TabsContent value="notes" className="space-y-2">
                <Textarea
                  value={noteDraft}
                  onChange={(e) => setNoteDraft(e.target.value)}
                  rows={4}
                  placeholder={t('video.side_panel.notes_placeholder')}
                  aria-label={t('video.side_panel.notes')}
                />
                <Button
                  type="button"
                  variant="outline"
                  size="sm"
                  onClick={handleSaveNote}
                  disabled={createArtifactMut.isPending || noteDraft.trim().length === 0}
                >
                  {t('common.save')}
                </Button>
              </TabsContent>
            </Tabs>
          </CardContent>
        </Card>
      </div>
    </div>
  );
}

/**
 * Wraps the `<LiveKitRoom>` provider so the inner `<VideoGrid>` can call hooks
 * (`useTracks`) that require the LiveKit context.
 */
function LiveKitRoomShell({
  token,
  serverUrl,
  controls,
  onError,
}: {
  token: string;
  serverUrl: string;
  controls: VideoControlsState;
  onError: (err: Error) => void;
}) {
  const audio = useMemo(() => ({ deviceId: undefined }), []);
  return (
    <div
      className="overflow-hidden rounded-md bg-black"
      style={{ aspectRatio: '16 / 9' }}
      data-testid="livekit-shell"
    >
      <LiveKitRoom
        data-lk-theme="default"
        serverUrl={serverUrl}
        token={token}
        connect
        audio={controls.micOn && audio}
        video={controls.camOn}
        screen={controls.screenShare}
        onError={(err) => {
          // Never log the raw error — may contain JWT.
          onError(err);
        }}
        style={{ height: '100%' }}
      >
        <VideoGrid />
        <RoomAudioRenderer />
      </LiveKitRoom>
    </div>
  );
}

function VideoGrid() {
  const { t } = useTranslation();
  const tracks = useTracks(
    [
      { source: Track.Source.Camera, withPlaceholder: true },
      { source: Track.Source.ScreenShare, withPlaceholder: false },
    ],
    { onlySubscribed: false },
  );
  if (tracks.length === 0) {
    return (
      <div className="flex h-full items-center justify-center text-sm text-muted-foreground">
        {t('video.room.no_participants')}
      </div>
    );
  }
  return (
    <GridLayout tracks={tracks} style={{ height: '100%' }}>
      <ParticipantTile />
    </GridLayout>
  );
}

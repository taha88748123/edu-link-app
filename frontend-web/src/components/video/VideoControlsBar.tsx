import { Mic, MicOff, MonitorUp, PhoneOff, Video, VideoOff } from 'lucide-react';
import { useTranslation } from 'react-i18next';

import { Button } from '@/components/ui/button';

export interface VideoControlsState {
  camOn: boolean;
  micOn: boolean;
  screenShare: boolean;
}

export function VideoControlsBar({
  state,
  onToggleCam,
  onToggleMic,
  onToggleScreen,
  onEnd,
  disabled,
}: {
  state: VideoControlsState;
  onToggleCam: () => void;
  onToggleMic: () => void;
  onToggleScreen: () => void;
  onEnd: () => void;
  disabled?: boolean;
}) {
  const { t } = useTranslation();
  return (
    <div className="flex flex-wrap items-center justify-center gap-2 border-t bg-background p-3">
      <Button
        type="button"
        variant={state.micOn ? 'outline' : 'destructive'}
        size="icon"
        onClick={onToggleMic}
        disabled={disabled}
        aria-label={state.micOn ? t('video.controls.mic_off') : t('video.controls.mic_on')}
      >
        {state.micOn ? <Mic className="h-4 w-4" /> : <MicOff className="h-4 w-4" />}
      </Button>
      <Button
        type="button"
        variant={state.camOn ? 'outline' : 'destructive'}
        size="icon"
        onClick={onToggleCam}
        disabled={disabled}
        aria-label={state.camOn ? t('video.controls.camera_off') : t('video.controls.camera_on')}
      >
        {state.camOn ? <Video className="h-4 w-4" /> : <VideoOff className="h-4 w-4" />}
      </Button>
      <Button
        type="button"
        variant={state.screenShare ? 'default' : 'outline'}
        size="icon"
        onClick={onToggleScreen}
        disabled={disabled}
        aria-label={t('video.controls.screen_share')}
      >
        <MonitorUp className="h-4 w-4" />
      </Button>
      <Button
        type="button"
        variant="destructive"
        onClick={onEnd}
        disabled={disabled}
        className="gap-2"
      >
        <PhoneOff className="h-4 w-4" />
        <span>{t('video.controls.leave')}</span>
      </Button>
    </div>
  );
}
